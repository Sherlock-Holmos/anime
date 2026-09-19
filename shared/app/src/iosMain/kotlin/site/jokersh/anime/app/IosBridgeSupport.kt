package site.jokersh.anime.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CharacterCredit
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.SearchRequest
import site.jokersh.anime.core.model.SearchDiscovery
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.SubjectCredits
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.core.model.SubjectRelation
import site.jokersh.anime.core.model.SubjectSection
import site.jokersh.anime.core.model.UserCollectionSummary
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.data.catalog.CalendarPage
import site.jokersh.anime.data.catalog.CatalogCacheStore
import site.jokersh.anime.data.catalog.RemoteCatalogRepository
import site.jokersh.anime.data.catalog.RemoteSearchRepository
import site.jokersh.anime.data.collection.CollectionStore
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.CommentDraftStore
import site.jokersh.anime.data.comment.CommunityActivity
import site.jokersh.anime.data.comment.CommunityComment
import site.jokersh.anime.data.comment.CommunityListDetail
import site.jokersh.anime.data.comment.CommunityListItem
import site.jokersh.anime.data.comment.CommunityListSummary
import site.jokersh.anime.data.comment.CommunityNotification
import site.jokersh.anime.data.comment.CommunityReaction
import site.jokersh.anime.data.comment.CommunityReview
import site.jokersh.anime.data.comment.CommunityReviewPage
import site.jokersh.anime.data.comment.CommunityUserProfile
import site.jokersh.anime.data.comment.OfflineFirstCommunityRepository
import site.jokersh.anime.data.comment.RatingOutboxStore
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.comment.RemoteCommunityRepository
import site.jokersh.anime.data.session.RemoteSessionRepository
import site.jokersh.anime.data.session.BangumiSyncConflict
import site.jokersh.anime.data.session.BangumiSyncStatus
import site.jokersh.anime.data.session.AdminOverview
import site.jokersh.anime.data.session.AdminComment
import site.jokersh.anime.data.session.AdminReport
import site.jokersh.anime.data.session.ServiceDiagnostic
import site.jokersh.anime.data.session.ServiceDiagnosticEndpoint
import site.jokersh.anime.data.session.SessionTokenStore
import site.jokersh.anime.data.session.StoredSessionToken
import site.jokersh.anime.data.session.SyncConflictChoice
import site.jokersh.anime.data.session.UserRatingPage
import site.jokersh.anime.data.session.UserRatingSummary
import site.jokersh.anime.data.settings.PersistentSettingsRepository
import site.jokersh.anime.data.settings.SettingsStore
import kotlin.time.Instant

internal fun parseSubjectIds(subjectIdsCsv: String): List<Long> =
    subjectIdsCsv
        .split(',')
        .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toLongOrNull() }

internal fun parseCsv(value: String?): List<String> =
    value
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()

internal fun String.toNativeSubjectType(): SubjectType? =
    when (trim().lowercase()) {
        "tv" -> SubjectType.Tv
        "web" -> SubjectType.Web
        "ova" -> SubjectType.Ova
        "movie" -> SubjectType.Movie
        "other" -> SubjectType.Other
        else -> null
    }

internal fun String.toNativeAiringStatus(): AiringStatus? =
    when (trim().lowercase()) {
        "announced" -> AiringStatus.Announced
        "airing" -> AiringStatus.Airing
        "finished" -> AiringStatus.Finished
        "unknown" -> AiringStatus.Unknown
        else -> null
    }

internal fun String.toNativeSearchSort(): SearchSort =
    when (trim().lowercase()) {
        "rating" -> SearchSort.Rating
        "updated" -> SearchSort.Updated
        else -> SearchSort.Relevance
    }

internal fun String.toSyncConflictChoice(): SyncConflictChoice =
    when (lowercase()) {
        "keep_local" -> SyncConflictChoice.KeepLocal
        "use_remote" -> SyncConflictChoice.UseRemote
        else -> SyncConflictChoice.Later
    }

@Serializable
internal data class NativeSessionSnapshot(
    val status: String,
    val userId: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val expiresAtEpochSeconds: Long? = null,
    val message: String? = null,
)

internal fun SessionState.toNativeSnapshot(): NativeSessionSnapshot =
    when (this) {
        SessionState.Guest -> NativeSessionSnapshot(status = "guest")
        SessionState.Restoring -> NativeSessionSnapshot(status = "restoring")
        is SessionState.Authenticated ->
            NativeSessionSnapshot(
                status = "authenticated",
                userId = user.summary.id.value,
                displayName = user.summary.displayName,
                avatarUrl = user.summary.avatar.nativeUrl(),
                expiresAtEpochSeconds = expiresAt.epochSeconds,
            )
        is SessionState.Expired -> NativeSessionSnapshot(status = "expired")
        is SessionState.Failed -> NativeSessionSnapshot(status = "failed", message = message)
    }

@Serializable
internal data class NativeDiscoverySnapshot(
    val sections: List<NativeDiscoverySection>,
    val generatedAtEpochSeconds: Long,
)

@Serializable
internal data class NativeDiscoverySection(
    val id: String,
    val title: String,
    val subjects: List<NativeSubjectSummary>,
)

@Serializable
internal data class NativeSubjectSummary(
    val id: Long,
    val title: String,
    val originalTitle: String? = null,
    val posterUrl: String? = null,
    val year: Int? = null,
    val type: String,
    val airingStatus: String,
    val rating: Double? = null,
    val ratingVotes: Int = 0,
)

@Serializable
internal data class NativeSubjectDetailSnapshot(
    val summary: NativeSubjectSummary,
    val summaryText: String? = null,
    val airDate: String? = null,
    val endDate: String? = null,
    val totalEpisodes: Int? = null,
    val tags: List<String> = emptyList(),
    val backdropUrl: String? = null,
    val sourceUrl: String,
    val dataUpdatedAtEpochSeconds: Long,
)

@Serializable
internal data class NativeCalendarSnapshot(
    val date: String,
    val items: List<NativeSubjectSummary>,
    val generatedAtEpochSeconds: Long,
)

@Serializable
internal data class NativeSubjectSectionsSnapshot(
    val episodes: List<NativeEpisodeSnapshot>,
    val characters: List<NativeCharacterSnapshot>,
    val persons: List<NativePersonSnapshot>,
    val relations: List<NativeRelationSnapshot>,
)

@Serializable
internal data class NativeEpisodeSnapshot(
    val id: Long,
    val number: Double? = null,
    val title: String? = null,
    val originalTitle: String? = null,
    val type: String,
    val airDate: String? = null,
    val airStatus: String,
)

@Serializable
internal data class NativeCharacterSnapshot(
    val id: Long,
    val name: String,
    val imageUrl: String? = null,
    val relation: String,
    val actors: List<NativePersonSnapshot> = emptyList(),
)

@Serializable
internal data class NativePersonSnapshot(
    val id: Long,
    val name: String,
    val imageUrl: String? = null,
    val role: String? = null,
)

@Serializable
internal data class NativeRelationSnapshot(
    val subject: NativeSubjectSummary,
    val kind: String,
    val label: String,
)

@Serializable
internal data class NativeSearchDiscoverySnapshot(
    val trending: List<String>,
    val recommendations: List<NativeSubjectSummary>,
    val personalized: Boolean,
)

@Serializable
internal data class NativeSearchResultsSnapshot(
    val items: List<NativeSubjectSummary>,
    val nextCursor: String? = null,
    val hasMore: Boolean,
)

@Serializable
internal data class NativeCollectionPageSnapshot(
    val items: List<NativeCollectionItemSnapshot>,
    val nextCursor: String? = null,
)

@Serializable
internal data class NativeCollectionItemSnapshot(
    val subjectId: Long,
    val title: String,
    val originalTitle: String,
    val posterUrl: String? = null,
    val airDate: String? = null,
    val score: Double,
    val status: String,
    val userRating: Int,
    val comment: String,
    val episodeProgress: Int,
    val totalEpisodes: Int,
    val updatedAt: String,
)

@Serializable
internal data class NativeProfileRatingPageSnapshot(
    val items: List<NativeProfileRatingSnapshot>,
    val nextCursor: String? = null,
)

@Serializable
internal data class NativeProfileRatingSnapshot(
    val id: String,
    val subjectId: Long,
    val title: String,
    val posterUrl: String? = null,
    val score: Int,
    val tags: List<String> = emptyList(),
    val visibility: String,
    val updatedAt: String,
)

@Serializable
internal data class NativeAdminOverviewSnapshot(
    val role: String,
    val usersTotal: Long,
    val usersActive: Long,
    val reviewsPublished: Long,
    val commentsPublished: Long,
    val openCommentReports: Long,
)

@Serializable
internal data class NativeAdminCommentSnapshot(
    val id: String,
    val subjectId: Long? = null,
    val subjectTitle: String? = null,
    val authorId: String,
    val authorName: String,
    val body: String,
    val spoiler: Boolean,
    val moderationStatus: String,
    val createdAt: String,
    val editedAt: String? = null,
)

@Serializable
internal data class NativeAdminReportSnapshot(
    val id: String,
    val commentId: String,
    val reporterId: String,
    val reporterName: String,
    val authorId: String,
    val authorName: String,
    val subjectId: Long? = null,
    val subjectTitle: String? = null,
    val body: String,
    val spoiler: Boolean,
    val moderationStatus: String,
    val reasonCode: String,
    val details: String? = null,
    val status: String,
    val assignedTo: String? = null,
    val createdAt: String,
    val resolvedAt: String? = null,
)

@Serializable
internal data class NativeActivityPageSnapshot(
    val items: List<NativeActivityItemSnapshot>,
    val nextCursor: String? = null,
)

@Serializable
internal data class NativeActivityItemSnapshot(
    val id: String,
    val actorId: String? = null,
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val kind: String,
    val subjectId: Long? = null,
    val subjectTitle: String? = null,
    val posterUrl: String? = null,
    val reviewId: String? = null,
    val listId: String? = null,
    val commentId: String? = null,
    val summary: String,
    val occurredAt: String,
    val owned: Boolean = false,
)

@Serializable
internal data class NativeNotificationSnapshot(
    val id: String,
    val kind: String,
    val actorId: String? = null,
    val actorName: String? = null,
    val subjectId: Long? = null,
    val commentId: String? = null,
    val listId: String? = null,
    val readAt: String? = null,
    val createdAt: String,
    val reviewId: String? = null,
)

@Serializable
internal data class NativeProfileSnapshot(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val provider: String,
    val reviewCount: Int,
    val ratingCount: Int,
    val listCount: Int,
    val wishCount: Int,
    val watchingCount: Int,
    val completedCount: Int,
    val onHoldCount: Int,
    val droppedCount: Int,
    val syncedAt: String? = null,
    val role: String = "user",
)

@Serializable
internal data class NativeNetworkContext(
    @SerialName("country_code") val countryCode: String? = null,
    val region: String? = null,
    val city: String? = null,
    @SerialName("display_location") val displayLocation: String? = null,
    @SerialName("is_domestic") val isDomestic: Boolean = false,
    @SerialName("recommended_route") val recommendedRoute: String = "cloudflare",
)

@Serializable
internal data class NativeDiagnosticSnapshot(
    val endpoint: String,
    val statusCode: Int? = null,
    val healthy: Boolean,
    val body: String,
    val latencyMs: Long? = null,
    val errorMessage: String? = null,
)

@Serializable
internal data class NativeSubjectCommunitySnapshot(
    val rating: NativeRatingSnapshot,
    val reviews: List<NativeReviewSnapshot>,
    val comments: List<NativeCommentSnapshot>,
    val personal: NativeSubjectPersonalStateSnapshot? = null,
)

@Serializable
internal data class NativeSubjectPersonalStateSnapshot(
    val userRating: Int? = null,
    val collectionStatus: String? = null,
    val collectionEpisodeProgress: Int? = null,
    val isCollected: Boolean = false,
)

@Serializable
internal data class NativeRatingSnapshot(
    val score: Double? = null,
    val votes: Long,
)

@Serializable
internal data class NativeReviewSnapshot(
    val id: String,
    val subjectId: Long,
    val authorId: String,
    val kind: String,
    val title: String? = null,
    val body: String,
    val spoiler: Boolean,
    val likeCount: Long,
    val createdAt: String,
    val owned: Boolean,
    val bookmarkCount: Long,
    val editedAt: String? = null,
    val visibility: String,
)

@Serializable
internal data class NativeReviewPageSnapshot(
    val items: List<NativeReviewSnapshot>,
    val nextCursor: String? = null,
)

@Serializable
internal data class NativeCommentSnapshot(
    val id: String,
    val parentId: String? = null,
    val authorId: String,
    val authorName: String,
    val body: String,
    val spoiler: Boolean,
    val createdAt: String,
    val owned: Boolean,
    val likeCount: Long,
    val bookmarkCount: Long,
)

@Serializable
internal data class NativeUserProfileSnapshot(
    val id: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val createdAt: String,
    val reviewCount: Long,
    val ratingCount: Long,
    val listCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val following: Boolean,
)

@Serializable
internal data class NativeListSummarySnapshot(
    val id: String,
    val ownerId: String? = null,
    val ownerName: String,
    val title: String,
    val description: String,
    val itemCount: Long,
    val followerCount: Long,
    val updatedAt: String,
    val owned: Boolean,
    val following: Boolean,
)

@Serializable
internal data class NativeListItemSnapshot(
    val subjectId: Long,
    val title: String,
    val posterUrl: String? = null,
    val note: String? = null,
    val position: Int,
    val score: Double? = null,
)

@Serializable
internal data class NativeListDetailSnapshot(
    val summary: NativeListSummarySnapshot,
    val items: List<NativeListItemSnapshot>,
)

@Serializable
internal data class NativeFollowSnapshot(
    val following: Boolean,
)

@Serializable
internal data class NativeSyncStatusSnapshot(
    val pendingCount: Long,
    val failedCount: Long,
    val conflictCount: Long,
    val lastSuccessfulAt: String? = null,
    val bangumiLinked: Boolean,
)

@Serializable
internal data class NativeSyncConflictSnapshot(
    val id: String,
    val subjectId: Long,
    val localVersion: Long,
    val fieldName: String,
    val localValue: String,
    val remoteValue: String,
    val detectedAt: String,
)

@Serializable
internal data class NativeReactionSnapshot(
    val reaction: String,
    val active: Boolean,
    val likeCount: Long,
    val bookmarkCount: Long,
)

internal fun DiscoveryFeed.toNativeSnapshot(): NativeDiscoverySnapshot =
    NativeDiscoverySnapshot(
        sections = sections.map { section ->
            NativeDiscoverySection(section.id, section.title, section.subjects.map { it.toNativeSummary() })
        },
        generatedAtEpochSeconds = generatedAt.epochSeconds,
    )

internal fun CalendarPage.toNativeSnapshot(): NativeCalendarSnapshot =
    NativeCalendarSnapshot(
        date = date.toString(),
        items = items.map { it.toNativeSummary() },
        generatedAtEpochSeconds = generatedAt.epochSeconds,
    )

internal fun Episode.toNativeSnapshot(): NativeEpisodeSnapshot =
    NativeEpisodeSnapshot(
        id = id.value,
        number = number,
        title = title,
        originalTitle = originalTitle,
        type = type.name,
        airDate = airDate?.toString(),
        airStatus = airStatus.name,
    )

internal fun CharacterCredit.toNativeSnapshot(): NativeCharacterSnapshot =
    NativeCharacterSnapshot(
        id = characterId.value,
        name = name,
        imageUrl = image.nativeUrl(),
        relation = relation,
        actors = actors.map { it.toNativeSnapshot() },
    )

internal fun site.jokersh.anime.core.model.PersonCredit.toNativeSnapshot(): NativePersonSnapshot =
    NativePersonSnapshot(
        id = personId.value,
        name = name,
        imageUrl = image.nativeUrl(),
        role = role,
    )

internal fun SubjectRelation.toNativeSnapshot(): NativeRelationSnapshot =
    NativeRelationSnapshot(subject = subject.toNativeSummary(), kind = kind.name, label = label)

internal fun SearchDiscovery.toNativeSnapshot(): NativeSearchDiscoverySnapshot =
    NativeSearchDiscoverySnapshot(
        trending = trending,
        recommendations = recommendations.map { it.toNativeSummary() },
        personalized = personalized,
    )

internal fun <T> site.jokersh.anime.core.model.Page<T>.toNativeSearchSnapshot(
    map: (T) -> NativeSubjectSummary,
): NativeSearchResultsSnapshot =
    NativeSearchResultsSnapshot(
        items = items.map(map),
        nextCursor = nextCursor?.value,
        hasMore = hasMore,
    )

internal fun site.jokersh.anime.data.session.UserCollectionPage.toNativeSnapshot(): NativeCollectionPageSnapshot =
    NativeCollectionPageSnapshot(items = items.map(UserCollectionSummary::toNativeSnapshot), nextCursor = nextCursor)

internal fun UserCollectionSummary.toNativeSnapshot(): NativeCollectionItemSnapshot =
    NativeCollectionItemSnapshot(
        subjectId = subjectId.value,
        title = title,
        originalTitle = originalTitle,
        posterUrl = posterUrl,
        airDate = airDate,
        score = score,
        status = status.name,
        userRating = userRating,
        comment = comment,
        episodeProgress = episodeProgress,
        totalEpisodes = totalEpisodes,
        updatedAt = updatedAt,
    )

internal fun site.jokersh.anime.data.comment.CommunityFeedPage.toNativeSnapshot(): NativeActivityPageSnapshot =
    NativeActivityPageSnapshot(items = items.map(CommunityActivity::toNativeSnapshot), nextCursor = nextCursor)

internal fun CommunityActivity.toNativeSnapshot(): NativeActivityItemSnapshot =
    NativeActivityItemSnapshot(
        id = id,
        actorId = actorId,
        actorName = actorName,
        actorAvatarUrl = actorAvatarUrl,
        kind = kind,
        subjectId = subjectId,
        subjectTitle = subjectTitle,
        posterUrl = posterUrl,
        reviewId = reviewId,
        listId = listId,
        commentId = commentId,
        summary = summary,
        occurredAt = occurredAt,
        owned = owned,
    )

internal fun CommunityNotification.toNativeSnapshot(): NativeNotificationSnapshot =
    NativeNotificationSnapshot(
        id = id,
        kind = kind,
        actorId = actorId,
        actorName = actorName,
        subjectId = subjectId,
        commentId = commentId,
        listId = listId,
        readAt = readAt,
        createdAt = createdAt,
        reviewId = reviewId,
    )

internal fun UserProfile.toNativeSnapshot(): NativeProfileSnapshot =
    NativeProfileSnapshot(
        userId = summary.id.value,
        displayName = summary.displayName,
        avatarUrl = summary.avatar.nativeUrl(),
        provider = connectedProvider.name,
        reviewCount = reviewCount,
        ratingCount = ratingCount,
        listCount = listCount,
        wishCount = collectionCounts[CollectionStatus.Wish] ?: 0,
        watchingCount = collectionCounts[CollectionStatus.Watching] ?: 0,
        completedCount = collectionCounts[CollectionStatus.Completed] ?: 0,
        onHoldCount = collectionCounts[CollectionStatus.OnHold] ?: 0,
        droppedCount = collectionCounts[CollectionStatus.Dropped] ?: 0,
        syncedAt = syncedAt?.toString(),
        role = role,
    )

internal fun UserRatingPage.toNativeSnapshot(): NativeProfileRatingPageSnapshot =
    NativeProfileRatingPageSnapshot(
        items = items.map(UserRatingSummary::toNativeSnapshot),
        nextCursor = nextCursor,
    )

internal fun UserRatingSummary.toNativeSnapshot(): NativeProfileRatingSnapshot =
    NativeProfileRatingSnapshot(
        id = id,
        subjectId = subjectId,
        title = title,
        posterUrl = posterUrl,
        score = score,
        tags = tags,
        visibility = visibility,
        updatedAt = updatedAt,
    )

internal fun AdminOverview.toNativeSnapshot(): NativeAdminOverviewSnapshot =
    NativeAdminOverviewSnapshot(
        role = role,
        usersTotal = usersTotal,
        usersActive = usersActive,
        reviewsPublished = reviewsPublished,
        commentsPublished = commentsPublished,
        openCommentReports = openCommentReports,
    )

internal fun AdminComment.toNativeSnapshot(): NativeAdminCommentSnapshot =
    NativeAdminCommentSnapshot(
        id = id,
        subjectId = subjectId,
        subjectTitle = subjectTitle,
        authorId = authorId,
        authorName = authorName,
        body = body,
        spoiler = spoiler,
        moderationStatus = moderationStatus,
        createdAt = createdAt,
        editedAt = editedAt,
    )

internal fun AdminReport.toNativeSnapshot(): NativeAdminReportSnapshot =
    NativeAdminReportSnapshot(
        id = id,
        commentId = commentId,
        reporterId = reporterId,
        reporterName = reporterName,
        authorId = authorId,
        authorName = authorName,
        subjectId = subjectId,
        subjectTitle = subjectTitle,
        body = body,
        spoiler = spoiler,
        moderationStatus = moderationStatus,
        reasonCode = reasonCode,
        details = details,
        status = status,
        assignedTo = assignedTo,
        createdAt = createdAt,
        resolvedAt = resolvedAt,
    )

internal fun ServiceDiagnostic.toNativeSnapshot(): NativeDiagnosticSnapshot =
    NativeDiagnosticSnapshot(endpoint, statusCode, healthy, body, latencyMs, errorMessage)

internal fun site.jokersh.anime.data.comment.CommunityRating.toNativeSnapshot(): NativeRatingSnapshot =
    NativeRatingSnapshot(score, votes)

internal fun site.jokersh.anime.data.comment.CommunityRating.toNativePersonalSnapshot(): NativeSubjectPersonalStateSnapshot =
    NativeSubjectPersonalStateSnapshot(
        userRating = userScore?.takeIf { it in 1..10 },
        collectionStatus = collectionStatus?.let(::nativeCollectionStatus)?.apiValueForIos(),
        collectionEpisodeProgress = collectionEpisodeProgress,
        isCollected = isCollected,
    )

internal fun CommunityReview.toNativeSnapshot(): NativeReviewSnapshot =
    NativeReviewSnapshot(id, subjectId, authorId, kind, title, body, spoiler, likeCount, createdAt, owned, bookmarkCount, editedAt, visibility)

internal fun CommunityReviewPage.toNativeSnapshot(): NativeReviewPageSnapshot =
    NativeReviewPageSnapshot(items = items.map { it.toNativeSnapshot() }, nextCursor = nextCursor)

internal fun CommunityComment.toNativeSnapshot(): NativeCommentSnapshot =
    NativeCommentSnapshot(id, parentId, authorId, authorName, body, spoiler, createdAt, owned, likeCount, bookmarkCount)

internal fun CommunityUserProfile.toNativeSnapshot(): NativeUserProfileSnapshot =
    NativeUserProfileSnapshot(
        id = id,
        displayName = displayName,
        avatarUrl = avatarUrl,
        createdAt = createdAt,
        reviewCount = reviewCount,
        ratingCount = ratingCount,
        listCount = listCount,
        followerCount = followerCount,
        followingCount = followingCount,
        following = following,
    )

internal fun CommunityListSummary.toNativeSnapshot(): NativeListSummarySnapshot =
    NativeListSummarySnapshot(
        id = id,
        ownerId = ownerId,
        ownerName = ownerName,
        title = title,
        description = description,
        itemCount = itemCount,
        followerCount = followerCount,
        updatedAt = updatedAt,
        owned = owned,
        following = following,
    )

internal fun CommunityListItem.toNativeSnapshot(): NativeListItemSnapshot =
    NativeListItemSnapshot(
        subjectId = subjectId,
        title = title,
        posterUrl = posterUrl,
        note = note,
        position = position,
        score = score,
    )

internal fun CommunityListDetail.toNativeSnapshot(): NativeListDetailSnapshot =
    NativeListDetailSnapshot(summary = summary.toNativeSnapshot(), items = items.map { it.toNativeSnapshot() })

internal fun BangumiSyncStatus.toNativeSnapshot(): NativeSyncStatusSnapshot =
    NativeSyncStatusSnapshot(
        pendingCount = pendingCount,
        failedCount = failedCount,
        conflictCount = conflictCount,
        lastSuccessfulAt = lastSuccessfulAt,
        bangumiLinked = bangumiLinked,
    )

internal fun BangumiSyncConflict.toNativeSnapshot(): NativeSyncConflictSnapshot =
    NativeSyncConflictSnapshot(
        id = id,
        subjectId = subjectId,
        localVersion = localVersion,
        fieldName = fieldName,
        localValue = localValue,
        remoteValue = remoteValue,
        detectedAt = detectedAt,
    )

internal fun CommunityReaction.toNativeSnapshot(): NativeReactionSnapshot =
    NativeReactionSnapshot(reaction, active, likeCount, bookmarkCount)

internal fun SubjectDetail.toNativeSnapshot(): NativeSubjectDetailSnapshot =
    NativeSubjectDetailSnapshot(
        summary = summary.toNativeSummary(),
        summaryText = summaryText,
        airDate = airDate?.toString(),
        endDate = endDate?.toString(),
        totalEpisodes = totalEpisodes,
        tags = tags.sortedBy { it.order }.map { it.name },
        backdropUrl = backdrop.nativeUrl(),
        sourceUrl = sourceUrl,
        dataUpdatedAtEpochSeconds = dataUpdatedAt.epochSeconds,
    )

internal fun site.jokersh.anime.core.model.SubjectSummary.toNativeSummary(): NativeSubjectSummary =
    NativeSubjectSummary(
        id = id.value,
        title = title,
        originalTitle = originalTitle,
        posterUrl = poster.nativeUrl(),
        year = year,
        type = type.name,
        airingStatus = airingStatus.name,
        rating = rating?.score,
        ratingVotes = rating?.votes ?: 0,
    )

internal fun ImageRef?.nativeUrl(): String? =
    when (this) {
        is ImageRef.Remote -> url
        else -> null
    }

internal fun AppError?.nativeMessage(): String? =
    when (this) {
        null -> null
        AppError.Offline -> "当前显示的是离线缓存"
        AppError.Timeout -> "请求超时"
        is AppError.Unauthorized -> "登录状态已失效"
        is AppError.NotFound -> "内容不存在"
        is AppError.RateLimited -> "请求过于频繁"
        is AppError.Validation -> "请求参数无效"
        is AppError.Upstream -> "上游数据服务暂时不可用"
        is AppError.Server -> "服务器暂时不可用"
        is AppError.Data -> "数据格式异常"
        is AppError.Unknown -> "发生未知错误"
    }

internal fun nativeCollectionStatus(value: String): CollectionStatus? =
    when (value.trim().lowercase()) {
        "wish", "想看" -> CollectionStatus.Wish
        "watching", "在看" -> CollectionStatus.Watching
        "completed", "看过" -> CollectionStatus.Completed
        "on_hold", "onhold", "搁置" -> CollectionStatus.OnHold
        "dropped", "抛弃" -> CollectionStatus.Dropped
        else -> null
    }

internal fun CollectionStatus.apiValueForIos(): String =
    when (this) {
        CollectionStatus.Wish -> "wish"
        CollectionStatus.Watching -> "watching"
        CollectionStatus.Completed -> "completed"
        CollectionStatus.OnHold -> "on_hold"
        CollectionStatus.Dropped -> "dropped"
    }
