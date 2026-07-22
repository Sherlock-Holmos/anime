package site.jokersh.anime.data.collection

import kotlinx.coroutines.flow.Flow
import site.jokersh.anime.core.model.CollectionConflict
import site.jokersh.anime.core.model.CollectionItem
import site.jokersh.anime.core.model.CollectionSnapshot
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.ConflictChoice
import site.jokersh.anime.core.model.MutationId
import site.jokersh.anime.core.model.MutationResult
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SyncSummary

public interface CollectionRepository {
    public fun observeCollections(status: CollectionStatus?): Flow<ResourceState<List<CollectionItem>>>

    public fun observeCollection(id: SubjectId): Flow<CollectionSnapshot?>

    public fun observeSyncSummary(): Flow<SyncSummary>

    public fun observeConflict(id: String): Flow<CollectionConflict?>

    public suspend fun setStatus(
        id: SubjectId,
        status: CollectionStatus?,
        completeProgress: Boolean = false,
    ): MutationResult

    public suspend fun setProgress(
        id: SubjectId,
        watched: Int,
    ): MutationResult

    public suspend fun retry(mutationId: MutationId): MutationResult

    public suspend fun resolveConflict(
        id: String,
        choice: ConflictChoice,
    ): MutationResult

    public suspend fun requestSync(): Result<Unit>
}
