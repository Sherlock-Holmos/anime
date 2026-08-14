package site.jokersh.anime.core.model

import kotlin.time.Instant

public enum class Visibility { Public, Followers, Private }

public data class UserRating(
    public val id: RatingId,
    public val subjectId: SubjectId,
    public val userId: UserId,
    public val score: Int,
    public val tags: Set<String>,
    public val visibility: Visibility,
    public val createdAt: Instant,
    public val updatedAt: Instant,
) {
    init {
        require(score in 1..10) { "Anime score must be in 1..10" }
        require(tags.size <= 10 && tags.all { it == it.trim() && it.unicodeCodePointCount() in 1..20 }) {
            "rating tags must contain at most 10 trimmed tags of 1..20 code points"
        }
        require(updatedAt >= createdAt) { "updatedAt must not be earlier than createdAt" }
    }
}

public data class CommunityRatingSummary(
    public val subjectId: SubjectId,
    public val score: Double,
    public val voteCount: Int,
    public val distribution: Map<Int, Int>,
    public val updatedAt: Instant,
) {
    init {
        require(score in 0.0..10.0) { "community score must be in 0..10" }
        require(voteCount >= 0) { "voteCount must be non-negative" }
        require(distribution.keys.all { it in 1..10 } && distribution.values.all { it >= 0 }) {
            "rating distribution is invalid"
        }
    }
}

public enum class ReviewKind { Short, Long }

public data class Review(
    public val id: ReviewId,
    public val subjectId: SubjectId,
    public val author: UserSummary,
    public val kind: ReviewKind,
    public val title: String?,
    public val body: String,
    public val spoiler: Boolean,
    public val visibility: Visibility,
    public val likeCount: Int,
    public val replyCount: Int,
    public val createdAt: Instant,
    public val editedAt: Instant?,
) {
    init {
        val bodyLength = body.unicodeCodePointCount()
        require(body == body.trim()) { "review body must be trimmed" }
        require(kind != ReviewKind.Short || bodyLength in 1..500) { "short review must contain 1..500 code points" }
        require(
            kind != ReviewKind.Long || bodyLength in 200..20_000,
        ) { "long review must contain 200..20000 code points" }
        require(kind != ReviewKind.Long || !title.isNullOrBlank()) { "long review requires a title" }
        require(
            title == null || (title == title.trim() && title.unicodeCodePointCount() <= 80),
        ) { "review title is invalid" }
        require(likeCount >= 0 && replyCount >= 0) { "interaction counts must be non-negative" }
        require(editedAt == null || editedAt >= createdAt) { "editedAt must not be earlier than createdAt" }
    }
}

public data class CuratedListItem(
    public val subjectId: SubjectId,
    public val note: String?,
    public val position: Int,
) {
    init {
        require(position >= 0) { "list item position must be non-negative" }
        require(
            note == null || (note == note.trim() && note.unicodeCodePointCount() <= 300),
        ) { "list item note is invalid" }
    }
}

public data class CuratedList(
    public val id: CuratedListId,
    public val owner: UserSummary,
    public val title: String,
    public val description: String,
    public val items: List<CuratedListItem>,
    public val visibility: Visibility,
    public val followerCount: Int,
    public val updatedAt: Instant,
) {
    init {
        require(title == title.trim() && title.unicodeCodePointCount() in 1..60) { "list title is invalid" }
        require(description == description.trim() && description.unicodeCodePointCount() <= 500) {
            "list description is invalid"
        }
        require(items.map { it.subjectId }.distinct().size == items.size) { "a subject may occur only once in a list" }
        require(items.map { it.position }.distinct().size == items.size) { "list positions must be unique" }
        require(followerCount >= 0) { "followerCount must be non-negative" }
    }
}

public data class FollowRelation(
    public val followerId: UserId,
    public val followedId: UserId,
    public val createdAt: Instant,
) {
    init {
        require(followerId != followedId) { "a user cannot follow themselves" }
    }
}

public enum class ActivityKind { Rated, Reviewed, Collected, CreatedList, FollowedList }

public data class ActivityEntry(
    public val id: ActivityId,
    public val actor: UserSummary,
    public val kind: ActivityKind,
    public val subjectId: SubjectId?,
    public val reviewId: ReviewId?,
    public val curatedListId: CuratedListId?,
    public val summary: String,
    public val visibility: Visibility,
    public val occurredAt: Instant,
) {
    init {
        require(
            summary == summary.trim() && summary.unicodeCodePointCount() in 1..300,
        ) { "activity summary is invalid" }
        require(subjectId != null || reviewId != null || curatedListId != null) { "activity requires a target" }
    }
}

public data class ActivityFeed(
    public val items: List<ActivityEntry>,
    public val nextCursor: Cursor?,
    public val generatedAt: Instant,
)
