package site.jokersh.anime.data.collection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.CollectionConflict
import site.jokersh.anime.core.model.CollectionItem
import site.jokersh.anime.core.model.CollectionSnapshot
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.ConflictChoice
import site.jokersh.anime.core.model.FieldId
import site.jokersh.anime.core.model.Freshness
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.MutationId
import site.jokersh.anime.core.model.MutationResult
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SyncPhase
import site.jokersh.anime.core.model.SyncState
import site.jokersh.anime.core.model.SyncSummary
import site.jokersh.anime.core.model.ValidationReason
import kotlin.time.Clock

public interface CollectionStore {
    public fun read(): String?

    public fun write(value: String)
}

public class InMemoryCollectionStore : CollectionStore {
    private var value: String? = null

    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }
}

public class OfflineFirstCollectionRepository(
    private val store: CollectionStore,
    private val pushStatus: suspend (Long, String, Int) -> Result<Unit>,
) : CollectionRepository {
    private val snapshots = MutableStateFlow(load())
    private val conflicts = MutableStateFlow<Map<String, CollectionConflict>>(emptyMap())
    private val lastSuccessfulAt = MutableStateFlow<kotlin.time.Instant?>(null)
    private var sequence: Long = 0

    override fun observeCollections(status: CollectionStatus?): Flow<ResourceState<List<CollectionItem>>> =
        snapshots.map { values ->
            ResourceState(
                value = emptyList(),
                freshness =
                    Freshness(
                        if (values.values.any {
                                it.sync.phase != SyncPhase.Synced
                            }
                        ) {
                            FreshnessKind.OfflineCache
                        } else {
                            FreshnessKind.Fresh
                        },
                        Clock.System.now(),
                    ),
                refreshing = values.values.any { it.sync.phase == SyncPhase.Syncing },
                error = values.values.firstNotNullOfOrNull { it.sync.error },
            )
        }

    override fun observeCollection(id: SubjectId): Flow<CollectionSnapshot?> = snapshots.map { it[id] }

    override fun observeSyncSummary(): Flow<SyncSummary> =
        snapshots.map { values ->
            SyncSummary(
                pendingCount =
                    values.values.count {
                        it.sync.phase == SyncPhase.Pending ||
                            it.sync.phase == SyncPhase.Syncing
                    },
                failedCount = values.values.count { it.sync.phase == SyncPhase.Failed },
                conflictCount = conflicts.value.size,
                lastSuccessfulAt = lastSuccessfulAt.value,
            )
        }

    override fun observeConflict(id: String): Flow<CollectionConflict?> = conflicts.map { it[id] }

    override suspend fun setStatus(
        id: SubjectId,
        status: CollectionStatus?,
        completeProgress: Boolean,
    ): MutationResult {
        val mutationId = nextMutationId()
        if (status == null) {
            snapshots.value = snapshots.value - id
            persist()
            return MutationResult.Accepted(mutationId, SyncState(SyncPhase.Synced))
        }
        val previous = snapshots.value[id]
        val pending = SyncState(SyncPhase.Pending, pendingCount = 1)
        snapshots.value = snapshots.value + (
            id to
                CollectionSnapshot(
                    subjectId = id,
                    status = status,
                    watchedEpisodes =
                        if (completeProgress) {
                            maxOf(1, previous?.watchedEpisodes ?: 0)
                        } else {
                            previous?.watchedEpisodes
                                ?: 0
                        },
                    note = previous?.note,
                    updatedAt = Clock.System.now(),
                    sync = pending,
                )
        )
        persist()
        sync(id)
        return MutationResult.Accepted(mutationId, snapshots.value.getValue(id).sync)
    }

    override suspend fun setProgress(
        id: SubjectId,
        watched: Int,
    ): MutationResult {
        if (watched <
            0
        ) {
            return MutationResult.Rejected(AppError.Validation(FieldId.Progress, ValidationReason.OutOfRange))
        }
        val current =
            snapshots.value[id]
                ?: return MutationResult.Rejected(AppError.Validation(FieldId.Progress, ValidationReason.Conflict))
        snapshots.value =
            snapshots.value +
            (
                id to
                    current.copy(
                        watchedEpisodes = watched,
                        updatedAt = Clock.System.now(),
                        sync = SyncState(SyncPhase.Pending, 1),
                    )
            )
        persist()
        sync(id)
        return MutationResult.Accepted(nextMutationId(), snapshots.value.getValue(id).sync)
    }

    override suspend fun retry(mutationId: MutationId): MutationResult {
        val subjectId =
            mutationId.value
                .substringAfterLast(':')
                .toLongOrNull()
                ?.let(::SubjectId)
                ?: snapshots.value.values
                    .firstOrNull { it.sync.phase == SyncPhase.Failed }
                    ?.subjectId
                ?: return MutationResult.Rejected(AppError.Validation(FieldId.Identifier, ValidationReason.Unsupported))
        sync(subjectId)
        return MutationResult.Accepted(mutationId, snapshots.value.getValue(subjectId).sync)
    }

    override suspend fun resolveConflict(
        id: String,
        choice: ConflictChoice,
    ): MutationResult {
        val conflict =
            conflicts.value[id]
                ?: return MutationResult.Rejected(AppError.Validation(FieldId.Identifier, ValidationReason.Unsupported))
        if (choice ==
            ConflictChoice.Later
        ) {
            return MutationResult.Accepted(nextMutationId(), SyncState(SyncPhase.Conflict, conflictId = id))
        }
        val selected = if (choice == ConflictChoice.UseRemote) conflict.remote else conflict.local
        snapshots.value =
            snapshots.value +
            (
                conflict.subjectId to
                    CollectionSnapshot(
                        conflict.subjectId,
                        selected.status,
                        selected.watchedEpisodes,
                        null,
                        selected.updatedAt,
                        SyncState(SyncPhase.Pending, 1),
                    )
            )
        conflicts.value = conflicts.value - id
        persist()
        sync(conflict.subjectId)
        return MutationResult.Accepted(nextMutationId(), snapshots.value.getValue(conflict.subjectId).sync)
    }

    override suspend fun requestSync(): Result<Unit> =
        runCatching {
            snapshots.value.values
                .filter { it.sync.phase != SyncPhase.Synced }
                .forEach { sync(it.subjectId) }
        }

    private suspend fun sync(id: SubjectId) {
        val current = snapshots.value[id] ?: return
        val attempting =
            current.copy(
                sync = current.sync.copy(phase = SyncPhase.Syncing, lastAttemptAt = Clock.System.now(), error = null),
            )
        snapshots.value = snapshots.value + (id to attempting)
        pushStatus(id.value, current.status.apiValue(), current.watchedEpisodes).fold(
            onSuccess = {
                lastSuccessfulAt.value = Clock.System.now()
                snapshots.value = snapshots.value + (id to attempting.copy(sync = SyncState(SyncPhase.Synced)))
            },
            onFailure = {
                snapshots.value =
                    snapshots.value +
                    (
                        id to
                            attempting.copy(
                                sync =
                                    SyncState(
                                        SyncPhase.Failed,
                                        pendingCount = 1,
                                        lastAttemptAt = Clock.System.now(),
                                        error = AppError.Offline,
                                    ),
                            )
                    )
            },
        )
        persist()
    }

    private fun persist() =
        store.write(
            snapshots.value.values.joinToString("\n") { item ->
                listOf(
                    item.subjectId.value,
                    item.status.name,
                    item.watchedEpisodes,
                    item.updatedAt.epochSeconds,
                    item.sync.phase.name,
                ).joinToString("|")
            },
        )

    private fun load(): Map<SubjectId, CollectionSnapshot> =
        store
            .read()
            .orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val fields = line.split('|')
                if (fields.size != 5) return@mapNotNull null
                runCatching {
                    val id = SubjectId(fields[0].toLong())
                    id to
                        CollectionSnapshot(
                            id,
                            CollectionStatus.valueOf(fields[1]),
                            fields[2].toInt(),
                            null,
                            kotlin.time.Instant.fromEpochSeconds(fields[3].toLong()),
                            SyncState(
                                SyncPhase.valueOf(fields[4]),
                                if (fields[4] ==
                                    SyncPhase.Synced.name
                                ) {
                                    0
                                } else {
                                    1
                                },
                            ),
                        )
                }.getOrNull()
            }.toMap()

    private fun nextMutationId(): MutationId {
        sequence += 1
        return MutationId("collection-${Clock.System.now().toEpochMilliseconds()}-$sequence")
    }
}

private fun CollectionStatus.apiValue(): String =
    when (this) {
        CollectionStatus.Wish -> "wish"
        CollectionStatus.Watching -> "watching"
        CollectionStatus.Completed -> "completed"
        CollectionStatus.OnHold -> "on_hold"
        CollectionStatus.Dropped -> "dropped"
    }
