package site.jokersh.anime.data.comment

import kotlinx.coroutines.flow.Flow
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.Comment
import site.jokersh.anime.core.model.CommentDraft
import site.jokersh.anime.core.model.CommentId
import site.jokersh.anime.core.model.CommentSort
import site.jokersh.anime.core.model.MutationResult
import site.jokersh.anime.core.model.Page
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectId

public interface CommentRepository {
    public fun observeComments(
        id: SubjectId,
        sort: CommentSort,
    ): Flow<ResourceState<List<Comment>>>

    public fun observeDraft(id: SubjectId): Flow<CommentDraft?>

    public suspend fun loadNext(
        id: SubjectId,
        sort: CommentSort,
    ): Result<Page<Comment>>

    public suspend fun saveDraft(
        id: SubjectId,
        parentId: CommentId?,
        text: String,
        spoiler: Boolean,
    )

    public suspend fun deleteDraft(id: SubjectId)

    public suspend fun create(
        id: SubjectId,
        parentId: CommentId?,
        text: String,
        spoiler: Boolean,
    ): MutationResult

    public suspend fun delete(id: CommentId): MutationResult

    public suspend fun report(
        id: CommentId,
        reasonCode: String,
        details: String? = null,
    ): MutationResult = MutationResult.Failed(AppError.Offline)
}
