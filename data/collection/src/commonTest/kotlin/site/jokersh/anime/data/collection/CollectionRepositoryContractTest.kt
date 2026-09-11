package site.jokersh.anime.data.collection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.ChangeSource
import site.jokersh.anime.core.model.CollectionConflict
import site.jokersh.anime.core.model.CollectionItem
import site.jokersh.anime.core.model.CollectionSnapshot
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.CollectionVersion
import site.jokersh.anime.core.model.ConflictChoice
import site.jokersh.anime.core.model.FieldId
import site.jokersh.anime.core.model.MutationId
import site.jokersh.anime.core.model.MutationResult
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SyncPhase
import site.jokersh.anime.core.model.SyncState
import site.jokersh.anime.core.model.SyncSummary
import site.jokersh.anime.core.model.ValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

private val fixtureNow = Instant.parse("2026-07-19T08:00:00Z")

abstract class CollectionRepositoryContract {
    protected abstract fun createRepository(conflict: CollectionConflict? = null): CollectionRepository

    @Test
    fun `CT-COL-004 optimistic status and progress are immediately observable`() =
        runTest {
            val repository = createRepository()
            val subjectId = SubjectId(1001)

            assertIs<MutationResult.Accepted>(repository.setStatus(subjectId, CollectionStatus.Watching))
            assertEquals(CollectionStatus.Watching, repository.observeCollection(subjectId).first()?.status)

            assertIs<MutationResult.Accepted>(repository.setProgress(subjectId, 3))
            assertEquals(3, repository.observeCollection(subjectId).first()?.watchedEpisodes)
        }

    @Test
    fun `CT-COL-003 invalid progress is rejected without changing snapshot`() =
        runTest {
            val repository = createRepository()
            val subjectId = SubjectId(1002)
            repository.setStatus(subjectId, CollectionStatus.Watching)

            val result = repository.setProgress(subjectId, -1)

            assertIs<MutationResult.Rejected>(result)
            assertEquals(0, repository.observeCollection(subjectId).first()?.watchedEpisodes)
        }

    @Test
    fun `CT-COL-004 remote conflict choice replaces local and clears conflict`() =
        runTest {
            val conflict = fixtureConflict()
            val repository = createRepository(conflict)

            assertIs<MutationResult.Accepted>(repository.resolveConflict(conflict.id, ConflictChoice.UseRemote))

            assertEquals(CollectionStatus.Completed, repository.observeCollection(conflict.subjectId).first()?.status)
            assertNull(repository.observeConflict(conflict.id).first())
        }
}

class FixtureCollectionRepositoryContractTest : CollectionRepositoryContract() {
    override fun createRepository(conflict: CollectionConflict?): CollectionRepository =
        InMemoryCollectionRepository(conflict)
}

class OfflineFirstCollectionRepositoryTest {
    @Test
    fun `request sync hydrates remote collection items and preserves subject metadata`() =
        runTest {
            val subjectId = SubjectId(2001)
            val snapshot =
                CollectionSnapshot(
                    subjectId = subjectId,
                    status = CollectionStatus.Watching,
                    watchedEpisodes = 2,
                    note = null,
                    updatedAt = fixtureNow,
                    sync = SyncState(SyncPhase.Synced),
                )
            val remoteItem =
                CollectionItem(
                    subject =
                        site.jokersh.anime.core.model.SubjectSummary(
                            id = subjectId,
                            title = "Remote collection item",
                            originalTitle = null,
                            aliases = emptyList(),
                            poster = null,
                            year = 2026,
                            type = site.jokersh.anime.core.model.SubjectType.Other,
                            airingStatus = site.jokersh.anime.core.model.AiringStatus.Unknown,
                            rating = null,
                            collection = snapshot,
                        ),
                    collection = snapshot,
                )
            val repository =
                OfflineFirstCollectionRepository(
                    store = InMemoryCollectionStore(),
                    pushStatus = { _, _, _ -> Result.success(Unit) },
                    pullCollections = { Result.success(listOf(remoteItem)) },
                )

            assertEquals(Unit, repository.requestSync().getOrThrow())

            val page = repository.observeCollections(CollectionStatus.Watching).first()
            val items = page.value.orEmpty()
            assertEquals(1, items.size)
            assertEquals("Remote collection item", items.single().subject.title)
            assertEquals(2, items.single().collection.watchedEpisodes)
        }
}

private class InMemoryCollectionRepository(
    initialConflict: CollectionConflict?,
) : CollectionRepository {
    private val snapshots = MutableStateFlow<Map<SubjectId, CollectionSnapshot>>(emptyMap())
    private val conflicts = MutableStateFlow(initialConflict?.let { mapOf(it.id to it) }.orEmpty())
    private var mutationCounter = 0

    init {
        if (initialConflict != null) {
            snapshots.value =
                mapOf(
                    initialConflict.subjectId to initialConflict.local.toSnapshot(initialConflict.subjectId),
                )
        }
    }

    override fun observeCollections(status: CollectionStatus?): Flow<ResourceState<List<CollectionItem>>> =
        snapshots.map {
            ResourceState(value = emptyList(), freshness = null, refreshing = false, error = null)
        }

    override fun observeCollection(id: SubjectId): Flow<CollectionSnapshot?> = snapshots.map { it[id] }

    override fun observeSyncSummary(): Flow<SyncSummary> =
        snapshots.map { values ->
            SyncSummary(
                pendingCount = values.values.count { it.sync.phase == SyncPhase.Pending },
                failedCount = 0,
                conflictCount = conflicts.value.size,
                lastSuccessfulAt = fixtureNow,
            )
        }

    override fun observeConflict(id: String): Flow<CollectionConflict?> = conflicts.map { it[id] }

    override suspend fun setStatus(
        id: SubjectId,
        status: CollectionStatus?,
        completeProgress: Boolean,
    ): MutationResult {
        if (status == null) {
            snapshots.value = snapshots.value - id
            return accepted()
        }
        val previous = snapshots.value[id]
        snapshots.value =
            snapshots.value +
            (
                id to
                    CollectionSnapshot(
                        subjectId = id,
                        status = status,
                        watchedEpisodes = if (completeProgress) maxOf(previous?.watchedEpisodes ?: 0, 1) else 0,
                        note = previous?.note,
                        updatedAt = fixtureNow,
                        sync = pendingSync(),
                    )
            )
        return accepted()
    }

    override suspend fun setProgress(
        id: SubjectId,
        watched: Int,
    ): MutationResult {
        val previous =
            snapshots.value[id]
                ?: return rejected(FieldId.Progress, ValidationReason.Conflict)
        if (watched < 0) return rejected(FieldId.Progress, ValidationReason.OutOfRange)
        snapshots.value = snapshots.value + (id to previous.copy(watchedEpisodes = watched, sync = pendingSync()))
        return accepted()
    }

    override suspend fun retry(mutationId: MutationId): MutationResult = accepted()

    override suspend fun resolveConflict(
        id: String,
        choice: ConflictChoice,
    ): MutationResult {
        val conflict =
            conflicts.value[id]
                ?: return rejected(FieldId.Identifier, ValidationReason.Unsupported)
        if (choice == ConflictChoice.Later) return accepted()
        val selected = if (choice == ConflictChoice.KeepLocal) conflict.local else conflict.remote
        snapshots.value = snapshots.value + (conflict.subjectId to selected.toSnapshot(conflict.subjectId))
        conflicts.value = conflicts.value - id
        return accepted()
    }

    override suspend fun requestSync(): Result<Unit> = Result.success(Unit)

    private fun accepted(): MutationResult.Accepted {
        mutationCounter += 1
        return MutationResult.Accepted(MutationId("mutation-$mutationCounter"), pendingSync())
    }
}

private fun pendingSync(): SyncState = SyncState(SyncPhase.Pending, pendingCount = 1)

private fun rejected(
    field: FieldId,
    reason: ValidationReason,
): MutationResult.Rejected = MutationResult.Rejected(AppError.Validation(field, reason))

private fun CollectionVersion.toSnapshot(subjectId: SubjectId): CollectionSnapshot =
    CollectionSnapshot(subjectId, status, watchedEpisodes, null, updatedAt, pendingSync())

private fun fixtureConflict(): CollectionConflict =
    CollectionConflict(
        id = "conflict-1",
        subjectId = SubjectId(1010),
        local = CollectionVersion(CollectionStatus.Watching, 3, fixtureNow, ChangeSource.ThisDevice),
        remote = CollectionVersion(CollectionStatus.Completed, 12, fixtureNow, ChangeSource.AnimeServer),
        detectedAt = fixtureNow,
    )
