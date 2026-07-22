package site.jokersh.anime.core.model

import kotlin.time.Instant

public data class Comment(
    public val id: CommentId,
    public val subjectId: SubjectId,
    public val parentId: CommentId?,
    public val author: UserSummary,
    public val body: String,
    public val spoiler: Boolean,
    public val createdAt: Instant,
    public val editedAt: Instant?,
    public val ownership: Ownership,
    public val pending: Boolean,
) {
    init {
        require(body == body.trim() && body.unicodeCodePointCount() in 1..300) {
            "comment body must be trimmed and contain 1..300 Unicode code points"
        }
    }
}

public data class UserSummary(
    public val id: UserId,
    public val displayName: String,
    public val avatar: ImageRef?,
) {
    init {
        require(displayName.isNotBlank()) { "displayName must not be blank" }
    }
}

public data class UserProfile(
    public val summary: UserSummary,
    public val collectionCounts: Map<CollectionStatus, Int>,
    public val connectedProvider: Provider,
) {
    init {
        require(collectionCounts.values.all { it >= 0 }) { "collection counts must be non-negative" }
    }
}

public enum class Ownership { Self, Other }

public enum class Provider { Bangumi, Anime }

public sealed interface SessionState {
    public data object Guest : SessionState

    public data class Authenticated(
        public val user: UserProfile,
        public val expiresAt: Instant,
    ) : SessionState

    public data class Expired(
        public val lastUser: UserSummary?,
    ) : SessionState
}

public enum class CommentSort { Newest, Oldest }

public data class CommentDraft(
    public val subjectId: SubjectId,
    public val parentId: CommentId?,
    public val text: String,
    public val spoiler: Boolean,
    public val updatedAt: Instant,
) {
    init {
        require(text.unicodeCodePointCount() <= 300) { "draft text exceeds 300 Unicode code points" }
    }
}

public data class LoginRequest(
    public val requestId: String,
    public val pendingActionId: String?,
)

public data class ExternalAuthRequest(
    public val requestId: String,
    public val authorizeUrl: String,
    public val expiresAt: Instant,
) {
    init {
        require(authorizeUrl.startsWith("https://")) { "authorizeUrl must use HTTPS" }
    }
}

public data class AuthCallback(
    public val requestId: String,
    public val ticket: String,
)
