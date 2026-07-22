package site.jokersh.anime.data.comment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.Comment
import site.jokersh.anime.core.model.CommentDraft
import site.jokersh.anime.core.model.CommentId
import site.jokersh.anime.core.model.CommentSort
import site.jokersh.anime.core.model.FieldId
import site.jokersh.anime.core.model.MutationId
import site.jokersh.anime.core.model.MutationResult
import site.jokersh.anime.core.model.Page
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SyncPhase
import site.jokersh.anime.core.model.SyncState
import site.jokersh.anime.core.model.ValidationReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

abstract class CommentRepositoryContract {
    protected abstract fun createRepository(): CommentRepository

    @Test
    fun `CT-COM-001 draft save and delete are immediately observable`() =
        runTest {
            val repository = createRepository()
            val subjectId = SubjectId(1001)

            repository.saveDraft(subjectId, null, "准备发布的短评", spoiler = true)
            assertEquals("准备发布的短评", repository.observeDraft(subjectId).first()?.text)

            repository.deleteDraft(subjectId)
            assertNull(repository.observeDraft(subjectId).first())
        }

    @Test
    fun `CT-COM-003 invalid create preserves the draft`() =
        runTest {
            val repository = createRepository()
            val subjectId = SubjectId(1002)
            repository.saveDraft(subjectId, null, "保留我", spoiler = false)

            val result = repository.create(subjectId, null, "   ", spoiler = false)

            assertIs<MutationResult.Rejected>(result)
            assertEquals("保留我", repository.observeDraft(subjectId).first()?.text)
        }

    @Test
    fun `CT-COM-004 accepted create clears draft exactly once`() =
        runTest {
            val repository = createRepository()
            val subjectId = SubjectId(1003)
            repository.saveDraft(subjectId, null, "发布内容", spoiler = false)

            assertIs<MutationResult.Accepted>(repository.create(subjectId, null, "发布内容", spoiler = false))
            assertNull(repository.observeDraft(subjectId).first())
        }
}

class FixtureCommentRepositoryContractTest : CommentRepositoryContract() {
    override fun createRepository(): CommentRepository = InMemoryCommentRepository()
}

private class InMemoryCommentRepository : CommentRepository {
    private val drafts = MutableStateFlow<Map<SubjectId, CommentDraft>>(emptyMap())
    private var mutationCounter = 0

    override fun observeComments(
        id: SubjectId,
        sort: CommentSort,
    ): Flow<ResourceState<List<Comment>>> =
        MutableStateFlow(ResourceState(emptyList(), null, refreshing = false, error = null))

    override fun observeDraft(id: SubjectId): Flow<CommentDraft?> = drafts.map { it[id] }

    override suspend fun loadNext(
        id: SubjectId,
        sort: CommentSort,
    ): Result<Page<Comment>> = Result.success(Page(emptyList(), nextCursor = null, hasMore = false))

    override suspend fun saveDraft(
        id: SubjectId,
        parentId: CommentId?,
        text: String,
        spoiler: Boolean,
    ) {
        drafts.value =
            drafts.value +
            (id to CommentDraft(id, parentId, text, spoiler, Instant.parse("2026-07-19T08:00:00Z")))
    }

    override suspend fun deleteDraft(id: SubjectId) {
        drafts.value = drafts.value - id
    }

    override suspend fun create(
        id: SubjectId,
        parentId: CommentId?,
        text: String,
        spoiler: Boolean,
    ): MutationResult {
        if (text.trim().isEmpty() || text.unicodeCount() > 300) {
            return MutationResult.Rejected(AppError.Validation(FieldId.Comment, ValidationReason.OutOfRange))
        }
        deleteDraft(id)
        return accepted()
    }

    override suspend fun delete(id: CommentId): MutationResult = accepted()

    private fun accepted(): MutationResult.Accepted {
        mutationCounter += 1
        return MutationResult.Accepted(
            MutationId("comment-mutation-$mutationCounter"),
            SyncState(SyncPhase.Pending, pendingCount = 1),
        )
    }
}

private fun String.unicodeCount(): Int =
    fold(0) { count, character -> count + if (character.isHighSurrogate()) 0 else 1 }
