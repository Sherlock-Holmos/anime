package site.jokersh.anime.data.comment

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public class RemoteCommunityRepository(
    private val client: HttpClient,
    apiBaseUrl: String,
    private val tokenProvider: () -> String?,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CommunityRepository {
    private val baseUrl = apiBaseUrl.trimEnd('/')

    public override suspend fun feed(limit: Int): Result<List<CommunityActivity>> =
        feedPage(limit = limit).map { it.items }

    public override suspend fun feedPage(
        feed: String,
        limit: Int,
        cursor: String?,
    ): Result<CommunityFeedPage> =
        runCatching {
            val cursorQuery = cursor?.let { "&cursor=${it.encodeURLParameter()}" }.orEmpty()
            val response =
                client.get(
                    "$baseUrl/api/v1/community/feed?limit=$limit&feed=${feed.encodeURLParameter()}$cursorQuery",
                ) {
                    optionalAuth()
                }
            CommunityFeedPage(
                items = decode<List<ActivityDto>>(response).map { it.toModel() },
                nextCursor = response.headers["x-next-cursor"],
            )
        }

    public override suspend fun notifications(limit: Int): Result<List<CommunityNotification>> =
        runCatching {
            decode<List<NotificationDto>>(
                client.get("$baseUrl/api/v1/notifications?limit=$limit") { auth() },
            ).map { it.toModel() }
        }

    public override suspend fun markNotificationRead(id: String): Result<Unit> =
        authenticatedRequest { client.post("$baseUrl/api/v1/notifications/$id/read") { auth() } }

    public override suspend fun followUser(id: String): Result<Boolean> = follow("/api/v1/users/$id/follow")

    public override suspend fun unfollowUser(id: String): Result<Boolean> =
        follow("/api/v1/users/$id/follow", delete = true)

    public override suspend fun followList(id: String): Result<Boolean> = follow("/api/v1/community/lists/$id/follow")

    public override suspend fun unfollowList(id: String): Result<Boolean> =
        follow("/api/v1/community/lists/$id/follow", delete = true)

    public override suspend fun followUserStatus(id: String): Result<Boolean> =
        followStatus("/api/v1/users/$id/follow")

    public override suspend fun followListStatus(id: String): Result<Boolean> =
        followStatus("/api/v1/community/lists/$id/follow")

    public override suspend fun userProfile(id: String): Result<CommunityUserProfile> =
        runCatching {
            decode<UserProfileDto>(client.get("$baseUrl/api/v1/users/$id") { optionalAuth() }).toModel()
        }

    public override suspend fun userReviews(
        id: String,
        limit: Int,
        cursor: String?,
        oldestFirst: Boolean,
    ): Result<CommunityReviewPage> =
        runCatching {
            val sort = if (oldestFirst) "oldest" else "newest"
            val cursorQuery = cursor?.let { "&cursor=${it.encodeURLParameter()}" }.orEmpty()
            val response = client.get("$baseUrl/api/v1/users/$id/reviews?limit=$limit&sort=$sort$cursorQuery") {
                optionalAuth()
            }
            val page = decode<ReviewPageDto>(response)
            CommunityReviewPage(page.items.map { it.toModel() }, page.nextCursor)
        }

    public override suspend fun reviews(
        subjectId: Long,
        limit: Int,
    ): Result<List<CommunityReview>> =
        runCatching {
            decode<ReviewPageDto>(
                client.get("$baseUrl/api/v1/subjects/$subjectId/reviews?limit=$limit"),
            ).items.map { it.toModel() }
        }

    public override suspend fun reviewPage(
        subjectId: Long,
        limit: Int,
        cursor: String?,
        oldestFirst: Boolean,
    ): Result<CommunityReviewPage> =
        runCatching {
            val sort = if (oldestFirst) "oldest" else "newest"
            val cursorQuery = cursor?.let { "&cursor=${it.encodeURLParameter()}" }.orEmpty()
            val dto =
                decode<ReviewPageDto>(
                    client.get(
                        "$baseUrl/api/v1/subjects/$subjectId/reviews?limit=$limit&sort=$sort$cursorQuery",
                    ) {
                        tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                    },
                )
            CommunityReviewPage(dto.items.map { it.toModel() }, dto.nextCursor)
        }

    public override suspend fun review(id: String): Result<CommunityReview> =
        runCatching {
            decode<ReviewDto>(client.get("$baseUrl/api/v1/reviews/$id")).toModel()
        }

    public override suspend fun deleteReview(id: String): Result<Unit> =
        authenticatedRequest {
            client.delete("$baseUrl/api/v1/reviews/$id") { auth() }
        }

    public override suspend fun updateReview(
        id: String,
        title: String?,
        body: String?,
        spoiler: Boolean?,
        visibility: String?,
    ): Result<CommunityReview> =
        runCatching {
            decode<ReviewDto>(client.patch("$baseUrl/api/v1/reviews/$id") {
                auth()
                jsonBody(UpdateReviewRequest(title, body, spoiler, visibility))
            }).toModel()
        }

    public override suspend fun reactReview(
        id: String,
        reaction: String,
        active: Boolean,
    ): Result<CommunityReaction> =
        runCatching {
            val response =
                if (active) {
                    client.put("$baseUrl/api/v1/reviews/$id/reactions/$reaction") { auth() }
                } else {
                    client.delete("$baseUrl/api/v1/reviews/$id/reactions/$reaction") { auth() }
                }
            decode<ReactionDto>(response).toModel()
        }

    public override suspend fun comments(
        subjectId: Long,
        limit: Int,
        offset: Int,
        oldestFirst: Boolean,
    ): Result<List<CommunityComment>> =
        runCatching {
            val sort = if (oldestFirst) "oldest" else "newest"
            decode<List<CommentDto>>(
                client.get(
                    "$baseUrl/api/v1/subjects/$subjectId/comments?limit=$limit&offset=$offset&sort=$sort",
                ) {
                    tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                },
            ).map { it.toModel() }
        }

    public override suspend fun rating(subjectId: Long): Result<CommunityRating> =
        runCatching {
            decode<RatingSummaryDto>(client.get("$baseUrl/api/v1/subjects/$subjectId/community-rating")).let {
                CommunityRating(it.score, it.votes)
            }
        }

    public override suspend fun lists(limit: Int): Result<List<CommunityListSummary>> =
        runCatching {
            decode<List<ListSummaryDto>>(
                client.get("$baseUrl/api/v1/community/lists?limit=$limit"),
            ).map { it.toModel() }
        }

    public override suspend fun list(id: String): Result<CommunityListDetail> =
        runCatching {
            val dto = decode<ListDetailDto>(client.get("$baseUrl/api/v1/community/lists/$id"))
            CommunityListDetail(dto.toSummary(), dto.items.map { it.toModel() })
        }

    public override suspend fun saveRating(
        subjectId: Long,
        score: Int,
        tags: Set<String>,
        visibility: String,
    ): Result<Unit> =
        authenticatedRequest {
            client.put("$baseUrl/api/v1/me/ratings/$subjectId") {
                auth()
                jsonBody(RatingRequest(score, tags.toList(), visibility))
            }
        }

    public override suspend fun createReview(
        subjectId: Long,
        kind: String,
        title: String?,
        body: String,
        spoiler: Boolean,
        visibility: String,
    ): Result<CommunityReview> =
        runCatching {
            decode<ReviewDto>(
                client.post("$baseUrl/api/v1/subjects/$subjectId/reviews") {
                    auth()
                    jsonBody(ReviewRequest(kind, title, body, spoiler, visibility))
                },
            ).toModel()
        }

    public override suspend fun createComment(
        subjectId: Long,
        body: String,
        spoiler: Boolean,
        parentId: String?,
    ): Result<CommunityComment> =
        runCatching {
            decode<CommentDto>(
                client.post("$baseUrl/api/v1/subjects/$subjectId/comments") {
                    auth()
                    jsonBody(CommentRequest(body, spoiler, parentId))
                },
            ).toModel()
        }

    public override suspend fun deleteComment(id: String): Result<Unit> =
        authenticatedRequest {
            client.delete("$baseUrl/api/v1/comments/$id") { auth() }
        }

    public override suspend fun updateComment(
        id: String,
        body: String?,
        spoiler: Boolean?,
    ): Result<CommunityComment> =
        runCatching {
            decode<CommentDto>(client.patch("$baseUrl/api/v1/comments/$id") {
                auth()
                jsonBody(UpdateCommentRequest(body, spoiler))
            }).toModel()
        }

    public override suspend fun reactComment(
        id: String,
        reaction: String,
        active: Boolean,
    ): Result<CommunityReaction> =
        runCatching {
            val response =
                if (active) {
                    client.put("$baseUrl/api/v1/comments/$id/reactions/$reaction") { auth() }
                } else {
                    client.delete("$baseUrl/api/v1/comments/$id/reactions/$reaction") { auth() }
                }
            decode<ReactionDto>(response).toModel()
        }

    public override suspend fun reportComment(
        id: String,
        reasonCode: String,
        details: String?,
    ): Result<Unit> =
        authenticatedRequest {
            client.post("$baseUrl/api/v1/comments/$id/reports") {
                auth()
                jsonBody(ReportCommentRequest(reasonCode, details?.trim()?.takeIf(String::isNotBlank)))
            }
        }

    public override suspend fun setCollection(
        subjectId: Long,
        status: String,
        episodeProgress: Int?,
    ): Result<Unit> =
        authenticatedRequest {
            client.put("$baseUrl/api/v1/me/collections/$subjectId") {
                auth()
                jsonBody(CollectionRequest(status, episodeProgress))
            }
        }

    public override suspend fun deleteCollection(subjectId: Long): Result<Unit> =
        authenticatedRequest { client.delete("$baseUrl/api/v1/me/collections/$subjectId") { auth() } }

    public override suspend fun createList(
        title: String,
        description: String,
        subjectIds: List<Long>,
    ): Result<CommunityListSummary> =
        runCatching {
            decode<ListSummaryDto>(
                client.post("$baseUrl/api/v1/community/lists") {
                    auth()
                    jsonBody(CreateListRequest(title, description, subjectIds))
                },
            ).toModel()
        }

    public override suspend fun userLists(id: String, limit: Int): Result<List<CommunityListSummary>> =
        runCatching {
            decode<List<ListSummaryDto>>(client.get("$baseUrl/api/v1/users/$id/lists?limit=$limit") { optionalAuth() })
                .map { it.toModel() }
        }

    public override suspend fun updateList(
        id: String,
        title: String?,
        description: String?,
        visibility: String?,
        subjectIds: List<Long>?,
    ): Result<CommunityListSummary> =
        runCatching {
            decode<ListSummaryDto>(client.put("$baseUrl/api/v1/community/lists/$id") {
                auth()
                jsonBody(UpdateListRequest(title, description, visibility, subjectIds))
            }).toModel()
        }

    public override suspend fun deleteList(id: String): Result<Unit> =
        authenticatedRequest { client.delete("$baseUrl/api/v1/community/lists/$id") { auth() } }

    private suspend inline fun authenticatedRequest(crossinline block: suspend () -> HttpResponse): Result<Unit> =
        runCatching { ensureSuccess(block()) }

    private suspend fun follow(
        path: String,
        delete: Boolean = false,
    ): Result<Boolean> =
        runCatching {
            val response =
                if (delete) {
                    client.delete("$baseUrl$path") { auth() }
                } else {
                    client.put("$baseUrl$path") { auth() }
                }
            decode<FollowResponse>(response).following
        }

    private suspend fun followStatus(path: String): Result<Boolean> =
        runCatching { decode<FollowResponse>(client.get("$baseUrl$path") { auth() }).following }

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        val token = tokenProvider() ?: error("请先登录 Anime")
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun io.ktor.client.request.HttpRequestBuilder.optionalAuth() {
        tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private inline fun <reified T> io.ktor.client.request.HttpRequestBuilder.jsonBody(value: T) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(value))
    }

    private suspend inline fun <reified T> decode(response: HttpResponse): T = json.decodeFromString(check(response))

    private suspend fun check(response: HttpResponse): String {
        val body = response.bodyAsText()
        check(response.status.value in 200..299) { body.ifBlank { "请求失败：${response.status.value}" } }
        return body
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        val body = response.bodyAsText()
        check(response.status.value in 200..299) { body.ifBlank { "请求失败：${response.status.value}" } }
    }

    private fun resolve(value: String?): String? =
        value?.let {
            if (it.startsWith("http://") ||
                it.startsWith("https://")
            ) {
                it
            } else {
                "$baseUrl/${it.trimStart('/')}"
            }
        }

    private fun ActivityDto.toModel() =
        CommunityActivity(
            id,
            actorId,
            actorName,
            resolve(actorAvatarUrl),
            kind,
            subjectId,
            subjectTitle,
            resolve(posterUrl),
            reviewId,
            listId,
            summary,
            occurredAt,
        )

    private fun ReviewDto.toModel() =
        CommunityReview(id, subjectId, authorId, kind, title, body, spoiler, likeCount, createdAt, owned, bookmarkCount, editedAt, visibility)

    private fun CommentDto.toModel() =
        CommunityComment(id, parentId, authorId, authorName, body, spoiler, createdAt, owned, likeCount, bookmarkCount)

    private fun ListSummaryDto.toModel() =
        CommunityListSummary(id, ownerName, title, description, itemCount, followerCount, updatedAt, ownerId, owned, following)

    private fun ListDetailDto.toSummary() =
        CommunityListSummary(id, ownerName, title, description, itemCount, followerCount, updatedAt, ownerId, owned, following)

    private fun ListItemDto.toModel() = CommunityListItem(subjectId, title, resolve(posterUrl), note, position, score)

    private fun UserProfileDto.toModel() =
        CommunityUserProfile(id, displayName, resolve(avatarUrl), createdAt, reviewCount, ratingCount, listCount, followerCount, followingCount, following)

    private fun ReactionDto.toModel() = CommunityReaction(reaction, active, likeCount, bookmarkCount)
}

@Serializable private data class ActivityDto(
    val id: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_name") val actorName: String,
    @SerialName("actor_avatar_url") val actorAvatarUrl: String? = null,
    val kind: String,
    @SerialName("subject_id") val subjectId: Long? = null,
    @SerialName("subject_title") val subjectTitle: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("review_id") val reviewId: String? = null,
    @SerialName("curated_list_id") val listId: String? = null,
    val summary: String,
    @SerialName("occurred_at") val occurredAt: String,
)

@Serializable private data class NotificationDto(
    val id: String,
    val kind: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_name") val actorName: String? = null,
    @SerialName("subject_id") val subjectId: Long? = null,
    @SerialName("comment_id") val commentId: String? = null,
    @SerialName("curated_list_id") val listId: String? = null,
    @SerialName("review_id") val reviewId: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun toModel() = CommunityNotification(id, kind, actorId, actorName, subjectId, commentId, listId, readAt, createdAt, reviewId)
}

@Serializable private data class ReviewPageDto(
    val items: List<ReviewDto>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable private data class ReviewDto(
    val id: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("subject_id") val subjectId: Long,
    val kind: String,
    val title: String? = null,
    val body: String,
    val spoiler: Boolean,
    @SerialName("like_count") val likeCount: Long = 0,
    @SerialName("bookmark_count") val bookmarkCount: Long = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    val visibility: String = "public",
    val owned: Boolean = false,
)

@Serializable private data class CommentDto(
    val id: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("parent_id") val parentId: String? = null,
    val body: String,
    val spoiler: Boolean,
    @SerialName("created_at") val createdAt: String,
    val owned: Boolean = false,
    @SerialName("like_count") val likeCount: Long = 0,
    @SerialName("bookmark_count") val bookmarkCount: Long = 0,
)

@Serializable private data class RatingSummaryDto(
    val score: Double? = null,
    val votes: Long = 0,
)

@Serializable private data class ListSummaryDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("owner_name") val ownerName: String,
    val title: String,
    val description: String,
    @SerialName("item_count") val itemCount: Long,
    @SerialName("follower_count") val followerCount: Long,
    @SerialName("updated_at") val updatedAt: String,
    val owned: Boolean = false,
    val following: Boolean = false,
)

@Serializable private data class ListDetailDto(
    val id: String,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("owner_name") val ownerName: String,
    val title: String,
    val description: String,
    @SerialName("item_count") val itemCount: Long,
    @SerialName("follower_count") val followerCount: Long,
    @SerialName("updated_at") val updatedAt: String,
    val owned: Boolean = false,
    val following: Boolean = false,
    val items: List<ListItemDto>,
)

@Serializable private data class ListItemDto(
    @SerialName("subject_id") val subjectId: Long,
    val title: String,
    @SerialName("poster_url") val posterUrl: String? = null,
    val note: String? = null,
    val position: Int,
    val score: Double? = null,
)

@Serializable private data class RatingRequest(
    val score: Int,
    val tags: List<String>,
    val visibility: String,
)

@Serializable private data class ReviewRequest(
    val kind: String,
    val title: String?,
    val body: String,
    val spoiler: Boolean,
    val visibility: String,
)

@Serializable private data class CommentRequest(
    val body: String,
    val spoiler: Boolean,
    @SerialName("parent_id") val parentId: String?,
)

@Serializable private data class CollectionRequest(
    val status: String,
    @SerialName("episode_progress") val episodeProgress: Int? = null,
)

@Serializable private data class CreateListRequest(
    val title: String,
    val description: String,
    @SerialName("subject_ids") val subjectIds: List<Long>,
)

@Serializable private data class UpdateListRequest(
    val title: String? = null,
    val description: String? = null,
    val visibility: String? = null,
    @SerialName("subject_ids") val subjectIds: List<Long>? = null,
)

@Serializable private data class UpdateReviewRequest(
    val title: String? = null,
    val body: String? = null,
    val spoiler: Boolean? = null,
    val visibility: String? = null,
)

@Serializable private data class UpdateCommentRequest(
    val body: String? = null,
    val spoiler: Boolean? = null,
)

@Serializable private data class FollowResponse(
    val following: Boolean,
)

@Serializable private data class ReactionDto(
    val reaction: String,
    val active: Boolean,
    @SerialName("like_count") val likeCount: Long,
    @SerialName("bookmark_count") val bookmarkCount: Long,
)

@Serializable private data class UserProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("review_count") val reviewCount: Long,
    @SerialName("rating_count") val ratingCount: Long,
    @SerialName("list_count") val listCount: Long,
    @SerialName("follower_count") val followerCount: Long,
    @SerialName("following_count") val followingCount: Long,
    val following: Boolean,
)

@Serializable private data class ReportCommentRequest(
    @SerialName("reason_code") val reasonCode: String,
    val details: String?,
)
