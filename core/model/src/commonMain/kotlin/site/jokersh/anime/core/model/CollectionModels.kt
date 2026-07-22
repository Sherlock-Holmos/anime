package site.jokersh.anime.core.model

import kotlin.time.Instant

public enum class CollectionStatus { Wish, Watching, Completed, OnHold, Dropped }

public enum class SyncPhase { Synced, Pending, Syncing, Failed, Conflict }

public data class SyncState(
    public val phase: SyncPhase,
    public val pendingCount: Int = 0,
    public val lastAttemptAt: Instant? = null,
    public val nextRetryAt: Instant? = null,
    public val error: AppError? = null,
    public val conflictId: String? = null,
) {
    init {
        require(pendingCount >= 0) { "pendingCount must be non-negative" }
    }
}

public data class CollectionSnapshot(
    public val subjectId: SubjectId,
    public val status: CollectionStatus,
    public val watchedEpisodes: Int,
    public val note: String?,
    public val updatedAt: Instant,
    public val sync: SyncState,
) {
    init {
        require(watchedEpisodes >= 0) { "watchedEpisodes must be non-negative" }
    }
}

public data class CollectionConflict(
    public val id: String,
    public val subjectId: SubjectId,
    public val local: CollectionVersion,
    public val remote: CollectionVersion,
    public val detectedAt: Instant,
)

public data class CollectionVersion(
    public val status: CollectionStatus,
    public val watchedEpisodes: Int,
    public val updatedAt: Instant,
    public val source: ChangeSource,
) {
    init {
        require(watchedEpisodes >= 0) { "watchedEpisodes must be non-negative" }
    }
}

public enum class ChangeSource { ThisDevice, AnimeServer, Bangumi }

public enum class ConflictChoice { KeepLocal, UseRemote, Later }

public data class CollectionItem(
    public val subject: SubjectSummary,
    public val collection: CollectionSnapshot,
)

public data class SyncSummary(
    public val pendingCount: Int,
    public val failedCount: Int,
    public val conflictCount: Int,
    public val lastSuccessfulAt: Instant?,
) {
    init {
        require(pendingCount >= 0 && failedCount >= 0 && conflictCount >= 0) { "sync counts must be non-negative" }
    }
}

public sealed interface MutationResult {
    public data class Accepted(
        public val id: MutationId,
        public val sync: SyncState,
    ) : MutationResult

    public data class Rejected(
        public val error: AppError.Validation,
    ) : MutationResult

    public data class RequiresAuth(
        public val pendingActionId: String,
    ) : MutationResult

    public data class Failed(
        public val error: AppError,
    ) : MutationResult
}
