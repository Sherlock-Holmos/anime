@file:Suppress("ktlint:standard:no-wildcard-imports")

package site.jokersh.anime.data.comment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import site.jokersh.anime.core.model.*
import kotlin.time.Clock
import kotlin.time.Instant

public interface CommentDraftStore {
    public fun read(): String?

    public fun write(value: String)
}

public class InMemoryCommentDraftStore : CommentDraftStore {
    private var value: String? = null

    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }
}

public class RemoteCommentRepository(
    private val community: CommunityRepository,
    private val draftStore: CommentDraftStore,
    private val currentUserId: () -> String?,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CommentRepository {
    private val states = mutableMapOf<String, MutableStateFlow<ResourceState<List<Comment>>>>()
    private val offsets = mutableMapOf<String, Int>()
    private val drafts = MutableStateFlow(loadDrafts())

    override fun observeComments(
        id: SubjectId,
        sort: CommentSort,
    ): Flow<ResourceState<List<Comment>>> = state(id, sort).asStateFlow()

    override fun observeDraft(id: SubjectId): Flow<CommentDraft?> = drafts.map { it[id] }

    override suspend fun loadNext(
        id: SubjectId,
        sort: CommentSort,
    ): Result<Page<Comment>> {
        val key = key(id, sort)
        val state = state(id, sort)
        val current = state.value
        state.value = current.copy(refreshing = true, error = null)
        val offset = offsets[key] ?: 0
        return community
            .comments(id.value, PAGE_SIZE, offset, sort == CommentSort.Oldest)
            .map { remote ->
                val page = remote.map { it.toModel(id) }
                offsets[key] = offset + page.size
                val merged =
                    (current.value.orEmpty() + page)
                        .distinctBy { it.id }
                        .let {
                            if (sort ==
                                CommentSort.Oldest
                            ) {
                                it.sortedBy(Comment::createdAt)
                            } else {
                                it.sortedByDescending(Comment::createdAt)
                            }
                        }
                state.value = ResourceState(merged, Freshness(FreshnessKind.Fresh, Clock.System.now()), false, null)
                Page(
                    page,
                    if (page.size ==
                        PAGE_SIZE
                    ) {
                        Cursor((offset + page.size).toString())
                    } else {
                        null
                    },
                    page.size == PAGE_SIZE,
                )
            }.onFailure { state.value = current.copy(refreshing = false, error = AppError.Offline) }
    }

    override suspend fun saveDraft(
        id: SubjectId,
        parentId: CommentId?,
        text: String,
        spoiler: Boolean,
    ) {
        drafts.value = drafts.value + (id to CommentDraft(id, parentId, text.take(300), spoiler, Clock.System.now()))
        persistDrafts()
    }

    override suspend fun deleteDraft(id: SubjectId) {
        drafts.value = drafts.value - id
        persistDrafts()
    }

    override suspend fun create(
        id: SubjectId,
        parentId: CommentId?,
        text: String,
        spoiler: Boolean,
    ): MutationResult {
        val normalized = text.trim()
        if (normalized.isBlank() ||
            normalized.length > 300
        ) {
            return MutationResult.Rejected(AppError.Validation(FieldId.Comment, ValidationReason.OutOfRange))
        }
        return community.createComment(id.value, normalized, spoiler, parentId?.value).fold(
            onSuccess = { created ->
                states.filterKeys { it.startsWith("${id.value}:") }.values.forEach { flow ->
                    flow.value =
                        flow.value.copy(value = listOf(created.toModel(id)) + flow.value.value.orEmpty())
                }
                deleteDraft(id)
                MutationResult.Accepted(MutationId("comment-${created.id}"), SyncState(SyncPhase.Synced))
            },
            onFailure = { MutationResult.Failed(AppError.Offline) },
        )
    }

    override suspend fun delete(id: CommentId): MutationResult =
        community.deleteComment(id.value).fold(
            onSuccess = {
                states.values.forEach { flow ->
                    flow.value =
                        flow.value.copy(
                            value =
                                flow.value.value
                                    .orEmpty()
                                    .filterNot { it.id == id },
                        )
                }
                MutationResult.Accepted(MutationId("delete-${id.value}"), SyncState(SyncPhase.Synced))
            },
            onFailure = { MutationResult.Failed(AppError.Offline) },
        )

    private fun state(
        id: SubjectId,
        sort: CommentSort,
    ): MutableStateFlow<ResourceState<List<Comment>>> =
        states.getOrPut(key(id, sort)) {
            MutableStateFlow(ResourceState(null, null, false, null))
        }

    private fun key(
        id: SubjectId,
        sort: CommentSort,
    ): String = "${id.value}:${sort.name}"

    private fun CommunityComment.toModel(subjectId: SubjectId): Comment =
        Comment(
            id = CommentId(id),
            subjectId = subjectId,
            parentId = parentId?.let(::CommentId),
            author = UserSummary(UserId(authorId), authorName, null),
            body =
                body.trim().take(300).ifBlank {
                    "[已删除]"
                },
            spoiler = spoiler,
            createdAt = runCatching { Instant.parse(createdAt) }.getOrDefault(Clock.System.now()),
            editedAt = null,
            ownership = if (owned || authorId == currentUserId()) Ownership.Self else Ownership.Other,
            pending = false,
        )

    private fun persistDrafts(): Unit =
        draftStore.write(json.encodeToString(drafts.value.values.map(DraftRecord::from)))

    private fun loadDrafts(): Map<SubjectId, CommentDraft> =
        runCatching {
            json.decodeFromString<List<DraftRecord>>(draftStore.read().orEmpty()).associate { record ->
                record.toModel().let {
                    it.subjectId to
                        it
                }
            }
        }.getOrDefault(emptyMap())

    private companion object {
        const val PAGE_SIZE: Int = 20
    }
}

@Serializable
private data class DraftRecord(
    val subjectId: Long,
    val parentId: String?,
    val text: String,
    val spoiler: Boolean,
    val updatedAt: Long,
) {
    fun toModel(): CommentDraft =
        CommentDraft(
            SubjectId(subjectId),
            parentId?.let(::CommentId),
            text,
            spoiler,
            Instant.fromEpochSeconds(updatedAt),
        )

    companion object {
        fun from(value: CommentDraft): DraftRecord =
            DraftRecord(
                value.subjectId.value,
                value.parentId?.value,
                value.text,
                value.spoiler,
                value.updatedAt.epochSeconds,
            )
    }
}
