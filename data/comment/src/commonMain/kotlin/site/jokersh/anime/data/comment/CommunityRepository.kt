package site.jokersh.anime.data.comment

public data class CommunityActivity(
    val id: String,
    val actorId: String?,
    val actorName: String,
    val actorAvatarUrl: String?,
    val kind: String,
    val subjectId: Long?,
    val subjectTitle: String?,
    val posterUrl: String?,
    val reviewId: String?,
    val listId: String?,
    val summary: String,
    val occurredAt: String,
)

public data class CommunityFeedPage(
    val items: List<CommunityActivity>,
    val nextCursor: String?,
)

public data class CommunityUserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val createdAt: String,
    val reviewCount: Long,
    val ratingCount: Long,
    val listCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val following: Boolean,
)

public data class CommunityNotification(
    val id: String,
    val kind: String,
    val actorId: String?,
    val actorName: String?,
    val subjectId: Long?,
    val commentId: String?,
    val listId: String?,
    val readAt: String?,
    val createdAt: String,
    val reviewId: String? = null,
)

public data class CommunityReview(
    val id: String,
    val subjectId: Long,
    val authorId: String,
    val kind: String,
    val title: String?,
    val body: String,
    val spoiler: Boolean,
    val likeCount: Long,
    val createdAt: String,
    val owned: Boolean = false,
    val bookmarkCount: Long = 0,
    val editedAt: String? = null,
    val visibility: String = "public",
)

public data class CommunityReviewPage(
    val items: List<CommunityReview>,
    val nextCursor: String?,
)

public data class CommunityComment(
    val id: String,
    val parentId: String?,
    val authorId: String,
    val authorName: String,
    val body: String,
    val spoiler: Boolean,
    val createdAt: String,
    val owned: Boolean,
    val likeCount: Long = 0,
    val bookmarkCount: Long = 0,
)

public data class CommunityListSummary(
    val id: String,
    val ownerName: String,
    val title: String,
    val description: String,
    val itemCount: Long,
    val followerCount: Long,
    val updatedAt: String,
    val ownerId: String? = null,
    val owned: Boolean = false,
    val following: Boolean = false,
)

public data class CommunityListItem(
    val subjectId: Long,
    val title: String,
    val posterUrl: String?,
    val note: String?,
    val position: Int,
    val score: Double?,
)

public data class CommunityListDetail(
    val summary: CommunityListSummary,
    val items: List<CommunityListItem>,
)

public data class CommunityRating(
    val score: Double?,
    val votes: Long,
)

public data class CommunityReaction(
    val reaction: String,
    val active: Boolean,
    val likeCount: Long,
    val bookmarkCount: Long,
)

public interface CommunityRepository {
    public suspend fun feed(limit: Int = 20): Result<List<CommunityActivity>>

    public suspend fun feedPage(
        feed: String = "public",
        limit: Int = 20,
        cursor: String? = null,
    ): Result<CommunityFeedPage> = feed(limit).map { CommunityFeedPage(it, null) }

    public suspend fun notifications(limit: Int = 20): Result<List<CommunityNotification>> = Result.success(emptyList())

    public suspend fun markNotificationRead(id: String): Result<Unit> =
        Result.failure(IllegalStateException("Notification service is unavailable"))

    public suspend fun followUser(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("Social service is unavailable"))

    public suspend fun unfollowUser(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("Social service is unavailable"))

    public suspend fun followList(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("Social service is unavailable"))

    public suspend fun unfollowList(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("Social service is unavailable"))

    public suspend fun followUserStatus(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("Social service is unavailable"))

    public suspend fun followListStatus(id: String): Result<Boolean> =
        Result.failure(IllegalStateException("Social service is unavailable"))

    public suspend fun userProfile(id: String): Result<CommunityUserProfile> =
        Result.failure(IllegalStateException("User profile service is unavailable"))

    public suspend fun userReviews(
        id: String,
        limit: Int = 20,
        cursor: String? = null,
        oldestFirst: Boolean = false,
    ): Result<CommunityReviewPage> =
        Result.failure(IllegalStateException("User review service is unavailable"))

    public suspend fun userLists(id: String, limit: Int = 20): Result<List<CommunityListSummary>> =
        Result.failure(IllegalStateException("User list service is unavailable"))

    public suspend fun reviews(
        subjectId: Long,
        limit: Int = 20,
    ): Result<List<CommunityReview>>

    public suspend fun reviewPage(
        subjectId: Long,
        limit: Int = 20,
        cursor: String? = null,
        oldestFirst: Boolean = false,
    ): Result<CommunityReviewPage> = reviews(subjectId, limit).map { CommunityReviewPage(it, null) }

    public suspend fun review(id: String): Result<CommunityReview>

    public suspend fun deleteReview(id: String): Result<Unit> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun updateReview(
        id: String,
        title: String?,
        body: String?,
        spoiler: Boolean?,
        visibility: String?,
    ): Result<CommunityReview> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun reactReview(
        id: String,
        reaction: String,
        active: Boolean,
    ): Result<CommunityReaction> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun comments(
        subjectId: Long,
        limit: Int = 20,
        offset: Int = 0,
        oldestFirst: Boolean = false,
    ): Result<List<CommunityComment>>

    public suspend fun rating(subjectId: Long): Result<CommunityRating>

    public suspend fun lists(limit: Int = 20): Result<List<CommunityListSummary>>

    public suspend fun list(id: String): Result<CommunityListDetail>

    public suspend fun saveRating(
        subjectId: Long,
        score: Int,
        tags: Set<String>,
        visibility: String,
    ): Result<Unit>

    public suspend fun createReview(
        subjectId: Long,
        kind: String,
        title: String?,
        body: String,
        spoiler: Boolean,
        visibility: String,
    ): Result<CommunityReview>

    public suspend fun createComment(
        subjectId: Long,
        body: String,
        spoiler: Boolean,
        parentId: String? = null,
    ): Result<CommunityComment>

    public suspend fun deleteComment(id: String): Result<Unit>

    public suspend fun updateComment(
        id: String,
        body: String?,
        spoiler: Boolean?,
    ): Result<CommunityComment> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun reactComment(
        id: String,
        reaction: String,
        active: Boolean,
    ): Result<CommunityReaction> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun reportComment(
        id: String,
        reasonCode: String,
        details: String? = null,
    ): Result<Unit> = Result.failure(IllegalStateException("Report service is unavailable"))

    public suspend fun setCollection(
        subjectId: Long,
        status: String,
        episodeProgress: Int? = null,
    ): Result<Unit>

    public suspend fun deleteCollection(subjectId: Long): Result<Unit> =
        Result.failure(IllegalStateException("Collection service is unavailable"))

    public suspend fun createList(
        title: String,
        description: String,
        subjectIds: List<Long>,
    ): Result<CommunityListSummary>

    public suspend fun updateList(
        id: String,
        title: String? = null,
        description: String? = null,
        visibility: String? = null,
        subjectIds: List<Long>? = null,
    ): Result<CommunityListSummary> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun deleteList(id: String): Result<Unit> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public suspend fun retryPendingRatings(): Result<Unit> = Result.success(Unit)
}

public class EmptyCommunityRepository : CommunityRepository {
    public override suspend fun feed(limit: Int): Result<List<CommunityActivity>> = Result.success(emptyList())

    public override suspend fun reviews(
        subjectId: Long,
        limit: Int,
    ): Result<List<CommunityReview>> = Result.success(emptyList())

    public override suspend fun review(id: String): Result<CommunityReview> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public override suspend fun comments(
        subjectId: Long,
        limit: Int,
        offset: Int,
        oldestFirst: Boolean,
    ): Result<List<CommunityComment>> = Result.success(emptyList())

    public override suspend fun rating(subjectId: Long): Result<CommunityRating> =
        Result.success(CommunityRating(null, 0))

    public override suspend fun lists(limit: Int): Result<List<CommunityListSummary>> = Result.success(emptyList())

    public override suspend fun list(id: String): Result<CommunityListDetail> =
        Result.failure(IllegalStateException("Community service is unavailable"))

    public override suspend fun saveRating(
        subjectId: Long,
        score: Int,
        tags: Set<String>,
        visibility: String,
    ): Result<Unit> = unavailable()

    public override suspend fun createReview(
        subjectId: Long,
        kind: String,
        title: String?,
        body: String,
        spoiler: Boolean,
        visibility: String,
    ): Result<CommunityReview> = Result.failure(IllegalStateException("Community service is unavailable"))

    public override suspend fun createComment(
        subjectId: Long,
        body: String,
        spoiler: Boolean,
        parentId: String?,
    ): Result<CommunityComment> = Result.failure(IllegalStateException("Community service is unavailable"))

    public override suspend fun deleteComment(id: String): Result<Unit> = unavailable()

    public override suspend fun setCollection(
        subjectId: Long,
        status: String,
        episodeProgress: Int?,
    ): Result<Unit> = unavailable()

    public override suspend fun createList(
        title: String,
        description: String,
        subjectIds: List<Long>,
    ): Result<CommunityListSummary> = Result.failure(IllegalStateException("Community service is unavailable"))

    private fun unavailable(): Result<Unit> = Result.failure(IllegalStateException("Community service is unavailable"))
}
