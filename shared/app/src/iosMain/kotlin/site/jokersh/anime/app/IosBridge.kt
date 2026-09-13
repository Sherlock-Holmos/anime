package site.jokersh.anime.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.SearchRequest
import site.jokersh.anime.core.model.SearchDiscovery
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.UserCollectionSummary
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.data.catalog.CatalogCacheStore
import site.jokersh.anime.data.catalog.RemoteCatalogRepository
import site.jokersh.anime.data.catalog.RemoteSearchRepository
import site.jokersh.anime.data.collection.CollectionStore
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.CommentDraftStore
import site.jokersh.anime.data.comment.CommunityActivity
import site.jokersh.anime.data.comment.CommunityComment
import site.jokersh.anime.data.comment.CommunityNotification
import site.jokersh.anime.data.comment.CommunityReaction
import site.jokersh.anime.data.comment.CommunityReview
import site.jokersh.anime.data.comment.OfflineFirstCommunityRepository
import site.jokersh.anime.data.comment.RatingOutboxStore
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.comment.RemoteCommunityRepository
import site.jokersh.anime.data.session.RemoteSessionRepository
import site.jokersh.anime.data.session.ServiceDiagnostic
import site.jokersh.anime.data.session.SessionTokenStore
import site.jokersh.anime.data.session.StoredSessionToken
import site.jokersh.anime.data.settings.PersistentSettingsRepository
import site.jokersh.anime.data.settings.SettingsStore
import kotlin.time.Instant

/** iOS host entry exported by AnimeShared.framework. */
public object IosBridge {
    private val pendingAuthCallback = MutableStateFlow<AuthCallback?>(null)
    private var sharedAppContainer: AppContainer? = null
    private var sharedNativeFacade: IosNativeAppFacade? = null

    /** SwiftUI entry point backed by the same KMP container as the legacy Compose host. */
    public fun nativeAppFacade(
        openExternalUrl: (String) -> Unit,
        readSecret: (String) -> String?,
        writeSecret: (String, String) -> Unit,
        removeSecret: (String) -> Unit,
    ): IosNativeAppFacade {
        sharedNativeFacade?.let { return it }
        return IosNativeAppFacade(
            appContainer(readSecret, writeSecret, removeSecret),
            openExternalUrl,
        ).also {
            sharedNativeFacade = it
            it.consumePendingAuthCallback()
        }
    }

    /** Handles both ASWebAuthenticationSession callbacks and app URL callbacks. */
    public fun handleOpenUrl(rawUrl: String): Boolean {
        val components = NSURLComponents(string = rawUrl)
        if (!components.scheme.equals("anime", ignoreCase = true) ||
            !components.host.equals("bangumi-auth", ignoreCase = true)
        ) {
            return false
        }
        @Suppress("UNCHECKED_CAST")
        val queryItems = components.queryItems as? List<NSURLQueryItem> ?: emptyList()
        val code = queryItems.firstOrNull { it.name == "code" }?.value?.takeIf(String::isNotBlank) ?: return false
        val state = queryItems.firstOrNull { it.name == "state" }?.value?.takeIf(String::isNotBlank) ?: return false
        pendingAuthCallback.value = AuthCallback(authorizationCode = code, state = state)
        sharedNativeFacade?.consumePendingAuthCallback()
        return true
    }

    private fun appContainer(
        readSecret: (String) -> String?,
        writeSecret: (String, String) -> Unit,
        removeSecret: (String) -> Unit,
    ): AppContainer =
        sharedAppContainer
            ?: createIosContainer(readSecret, writeSecret, removeSecret).also { sharedAppContainer = it }

    internal fun pendingCallback(): AuthCallback? = pendingAuthCallback.value

    internal fun clearPendingCallback() {
        pendingAuthCallback.value = null
    }
}

/**
 * Swift-friendly bridge for the native iOS vertical slice.
 *
 * The facade keeps repository, cache, session and refresh logic in KMP. It exposes small
 * JSON snapshots instead of Kotlin sealed classes and Flow types so SwiftUI does not need
 * to depend on Kotlin/Native implementation details.
 */
public class IosNativeAppFacade internal constructor(
    private val appContainer: AppContainer,
    private val openExternalUrl: (String) -> Unit,
) {
    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, failure ->
            // SwiftUI must remain alive when a best-effort background request or
            // session Flow fails. Individual API methods already return failures
            // through their completion closures; this is the last boundary for
            // observation/startup work that has no direct caller to report to.
            println(
                "[Anime iOS] uncaught facade coroutine failure: " +
                    "${failure::class.simpleName}: ${failure.message ?: "unknown error"}",
            )
        }
    // Network and JSON work must not share SwiftUI's main executor. SwiftUI receives
    // completion callbacks and explicitly hops back to MainActor, so keeping this
    // scope on Default avoids starving the native view tree while a Darwin request
    // or a large catalog response is in flight.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + coroutineExceptionHandler)
    private val json = Json { encodeDefaults = true }
    private val sessionOperationMutex = Mutex()
    private var sessionObservation: Job? = null
    private var startupJob: Job? = null

    private companion object {
        const val IOS_OPERATION_TIMEOUT_MILLIS: Long = 35_000
    }

    private fun launchTextOperation(
        name: String,
        completion: (String?, String?) -> Unit,
        block: suspend () -> Pair<String?, String?>,
    ) {
        scope.launch {
            val outcome =
                try {
                    withTimeout(IOS_OPERATION_TIMEOUT_MILLIS) {
                        println("[Anime iOS] operation begin: $name")
                        block()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Throwable) {
                    println(
                        "[Anime iOS] operation failed: $name: " +
                            "${failure::class.simpleName}: ${failure.message ?: "unknown error"}",
                    )
                    null to (failure.message ?: "请求超时或服务暂时不可用")
                }
            completion(outcome.first, outcome.second)
        }
    }

    /** Starts the same app-level restore and background sync work used by the Compose host. */
    public fun start() {
        if (startupJob?.isActive == true) return
        startupJob =
            scope.launch {
                startupStep("session restore") {
                    sessionOperationMutex.withLock { appContainer.sessionRepository.refresh() }
                }
                startupStep("collection sync") { appContainer.collectionRepository.requestSync() }
                startupStep("rating outbox") { appContainer.communityRepository.retryPendingRatings() }
            }
    }

    private suspend fun startupStep(
        name: String,
        block: suspend () -> Any?,
    ) {
        try {
            println("[Anime iOS] startup step begin: $name")
            block()
            println("[Anime iOS] startup step complete: $name")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            // Startup work is best effort. A missing network, unavailable simulator
            // Keychain, or stale local cache must never terminate the SwiftUI process.
            println("[Anime iOS] startup step failed: $name: ${failure.message ?: failure::class.simpleName}")
        }
    }

    public fun startSessionObservation(onChanged: (String) -> Unit) {
        sessionObservation?.cancel()
        sessionObservation =
            scope.launch {
                appContainer.sessionRepository.observeSession().collect { state ->
                    onChanged(json.encodeToString(NativeSessionSnapshot.serializer(), state.toNativeSnapshot()))
                }
            }
    }

    public fun stopSessionObservation() {
        sessionObservation?.cancel()
        sessionObservation = null
    }

    public fun refreshSession(completion: (String?, String?) -> Unit) {
        scope.launch {
            val result = sessionOperationMutex.withLock { appContainer.sessionRepository.refresh() }
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeSessionSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun loadDiscovery(
        force: Boolean,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("discovery", completion) {
            val result =
                appContainer.catalogRepository.refreshDiscovery(
                    if (force) RefreshPolicy.Force else RefreshPolicy.IfStale,
                )
            val state = appContainer.catalogRepository.observeDiscovery().first()
            state.value?.let { json.encodeToString(NativeDiscoverySnapshot.serializer(), it.toNativeSnapshot()) } to
                if (state.value == null) result.exceptionOrNull()?.message ?: state.error?.nativeMessage() else null
        }

    public fun loadSubject(
        subjectId: Long,
        force: Boolean,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("subject:$subjectId", completion) {
            val id = SubjectId(subjectId)
            val result =
                appContainer.catalogRepository.refreshSubject(
                    id,
                    if (force) RefreshPolicy.Force else RefreshPolicy.IfStale,
                )
            val state = appContainer.catalogRepository.observeSubject(id).first()
            state.value?.let { json.encodeToString(NativeSubjectDetailSnapshot.serializer(), it.toNativeSnapshot()) } to
                if (state.value == null) result.exceptionOrNull()?.message ?: state.error?.nativeMessage() else null
        }

    public fun loadSearchDiscovery(completion: (String?, String?) -> Unit) =
        launchTextOperation("search discovery", completion) {
            val result = appContainer.searchRepository.discovery()
            result.getOrNull()?.let { json.encodeToString(NativeSearchDiscoverySnapshot.serializer(), it.toNativeSnapshot()) } to result.exceptionOrNull()?.message
        }

    public fun searchSubjects(
        query: String,
        cursor: String?,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("search", completion) {
            val normalized = query.trim()
            if (normalized.isBlank()) {
                null to "请输入搜索内容"
            } else {
                val result =
                    runCatching {
                        appContainer.searchRepository.search(
                            SearchRequest(
                                query = normalized,
                                sort = SearchSort.Relevance,
                                cursor = cursor?.takeIf(String::isNotBlank)?.let(::Cursor),
                                pageSize = appContainer.profile.searchPageSize,
                            ),
                        ).getOrThrow()
                    }
                result.getOrNull()?.let {
                    json.encodeToString(
                        NativeSearchResultsSnapshot.serializer(),
                        it.toNativeSearchSnapshot { subject -> subject.toNativeSummary() },
                    )
                } to
                    result.exceptionOrNull()?.message
            }
        }

    public fun saveSearchHistory(query: String) {
        scope.launch { appContainer.searchRepository.saveHistory(query) }
    }

    public fun loadCollection(
        status: String?,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("collection", completion) {
            val result =
                appContainer.sessionRepository.collectionPage(
                    status = status?.let(::nativeCollectionStatus),
                    limit = 50,
                )
            result.getOrNull()?.let { json.encodeToString(NativeCollectionPageSnapshot.serializer(), it.toNativeSnapshot()) } to result.exceptionOrNull()?.message
        }

    public fun loadActivity(
        feed: String,
        cursor: String?,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("activity:$feed", completion) {
            val result = appContainer.communityRepository.feedPage(feed = feed, limit = 20, cursor = cursor)
            result.getOrNull()?.let { json.encodeToString(NativeActivityPageSnapshot.serializer(), it.toNativeSnapshot()) } to result.exceptionOrNull()?.message
        }

    public fun loadNotifications(completion: (String?, String?) -> Unit) =
        launchTextOperation("notifications", completion) {
            val result = appContainer.communityRepository.notifications(50)
            result.getOrNull()?.let {
                json.encodeToString(
                    ListSerializer(NativeNotificationSnapshot.serializer()),
                    it.map(CommunityNotification::toNativeSnapshot),
                )
            } to
                result.exceptionOrNull()?.message
        }

    public fun markNotificationRead(id: String, completion: (String?) -> Unit) {
        scope.launch { completion(appContainer.communityRepository.markNotificationRead(id).exceptionOrNull()?.message) }
    }

    public fun loadProfile(completion: (String?, String?) -> Unit) =
        launchTextOperation("profile", completion) {
            val result =
                runCatching {
                    appContainer.sessionRepository.refresh().getOrThrow().user.toNativeSnapshot()
                }
            result.getOrNull()?.let { json.encodeToString(NativeProfileSnapshot.serializer(), it) } to result.exceptionOrNull()?.message
        }

    public fun loadSubjectCommunity(
        subjectId: Long,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("subject community:$subjectId", completion) {
            val rating = appContainer.communityRepository.rating(subjectId)
            val reviews = appContainer.communityRepository.reviews(subjectId, 20)
            val comments = appContainer.communityRepository.comments(subjectId, 30)
            val failure = rating.exceptionOrNull() ?: reviews.exceptionOrNull() ?: comments.exceptionOrNull()
            val snapshot =
                if (failure == null) {
                    NativeSubjectCommunitySnapshot(
                        rating = rating.getOrThrow().toNativeSnapshot(),
                        reviews = reviews.getOrThrow().map(CommunityReview::toNativeSnapshot),
                        comments = comments.getOrThrow().map(CommunityComment::toNativeSnapshot),
                    )
                } else {
                    null
                }
            snapshot?.let { json.encodeToString(NativeSubjectCommunitySnapshot.serializer(), it) } to failure?.message
        }

    public fun saveRating(
        subjectId: Long,
        score: Int,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            completion(
                appContainer.communityRepository
                    .saveRating(subjectId, score, emptySet(), "public")
                    .exceptionOrNull()
                    ?.message,
            )
        }
    }

    public fun setCollection(
        subjectId: Long,
        status: String?,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            val message =
                if (status == null) {
                    appContainer.communityRepository.deleteCollection(subjectId).exceptionOrNull()?.message
                } else {
                    appContainer.communityRepository
                        .setCollection(subjectId, status, null)
                        .exceptionOrNull()
                        ?.message
                }
            completion(message)
        }
    }

    public fun createComment(
        subjectId: Long,
        body: String,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.createComment(subjectId, body, false)
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeCommentSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun createReview(
        subjectId: Long,
        title: String?,
        body: String,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.createReview(
                subjectId = subjectId,
                kind = "review",
                title = title,
                body = body,
                spoiler = false,
                visibility = "public",
            )
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeReviewSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun reactComment(
        id: String,
        reaction: String,
        active: Boolean,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.reactComment(id, reaction, active)
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeReactionSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun reactReview(
        id: String,
        reaction: String,
        active: Boolean,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.reactReview(id, reaction, active)
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeReactionSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun loginWithAnime(
        username: String,
        password: String,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result =
                sessionOperationMutex.withLock {
                    appContainer.sessionRepository.loginWithAnime(AnimeLoginCredentials(username, password))
                }
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeSessionSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun registerAnime(
        username: String,
        password: String,
        displayName: String,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result =
                sessionOperationMutex.withLock {
                    appContainer.sessionRepository.registerAnime(
                        AnimeRegistration(username, password, displayName),
                    )
                }
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeSessionSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun updateProfile(displayName: String, completion: (String?) -> Unit) {
        scope.launch {
            completion(
                sessionOperationMutex.withLock {
                    appContainer.sessionRepository.updateProfile(displayName)
                }.exceptionOrNull()?.message,
            )
        }
    }

    public fun diagnostics(completion: (String?, String?) -> Unit) {
        scope.launch {
            val result = appContainer.sessionRepository.diagnostics()
            completion(
                result.getOrNull()?.let {
                    json.encodeToString(
                        ListSerializer(NativeDiagnosticSnapshot.serializer()),
                        it.map(ServiceDiagnostic::toNativeSnapshot),
                    )
                },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun beginBangumiLogin(completion: (String?, String?) -> Unit) {
        scope.launch {
            val result =
                appContainer.sessionRepository.beginLogin(
                    LoginRequest(
                        requestId = "ios-${kotlin.time.Clock.System.now()}",
                        pendingActionId = null,
                    ),
                )
            result.onSuccess {
                openExternalUrl(it.authorizeUrl)
                completion(it.authorizeUrl, null)
            }.onFailure { completion(null, it.message ?: "无法开始登录") }
        }
    }

    public fun consumePendingAuthCallback() {
        scope.launch {
            val callback = IosBridge.pendingCallback()
            if (callback == null) return@launch
            sessionOperationMutex.withLock { appContainer.sessionRepository.completeLogin(callback) }
            if (IosBridge.pendingCallback() == callback) IosBridge.clearPendingCallback()
        }
    }

    public fun logout(completion: (String?) -> Unit) {
        scope.launch {
            completion(
                sessionOperationMutex.withLock { appContainer.sessionRepository.logout() }.exceptionOrNull()?.message,
            )
        }
    }
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

private fun SessionState.toNativeSnapshot(): NativeSessionSnapshot =
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
    val summary: String,
    val occurredAt: String,
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
)

@Serializable
internal data class NativeDiagnosticSnapshot(
    val endpoint: String,
    val statusCode: Int? = null,
    val healthy: Boolean,
    val body: String,
)

@Serializable
internal data class NativeSubjectCommunitySnapshot(
    val rating: NativeRatingSnapshot,
    val reviews: List<NativeReviewSnapshot>,
    val comments: List<NativeCommentSnapshot>,
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
internal data class NativeReactionSnapshot(
    val reaction: String,
    val active: Boolean,
    val likeCount: Long,
    val bookmarkCount: Long,
)

private fun DiscoveryFeed.toNativeSnapshot(): NativeDiscoverySnapshot =
    NativeDiscoverySnapshot(
        sections = sections.map { section ->
            NativeDiscoverySection(section.id, section.title, section.subjects.map { it.toNativeSummary() })
        },
        generatedAtEpochSeconds = generatedAt.epochSeconds,
    )

private fun SearchDiscovery.toNativeSnapshot(): NativeSearchDiscoverySnapshot =
    NativeSearchDiscoverySnapshot(
        trending = trending,
        recommendations = recommendations.map { it.toNativeSummary() },
        personalized = personalized,
    )

private fun <T> site.jokersh.anime.core.model.Page<T>.toNativeSearchSnapshot(
    map: (T) -> NativeSubjectSummary,
): NativeSearchResultsSnapshot =
    NativeSearchResultsSnapshot(
        items = items.map(map),
        nextCursor = nextCursor?.value,
        hasMore = hasMore,
    )

private fun site.jokersh.anime.data.session.UserCollectionPage.toNativeSnapshot(): NativeCollectionPageSnapshot =
    NativeCollectionPageSnapshot(items = items.map(UserCollectionSummary::toNativeSnapshot), nextCursor = nextCursor)

private fun UserCollectionSummary.toNativeSnapshot(): NativeCollectionItemSnapshot =
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

private fun site.jokersh.anime.data.comment.CommunityFeedPage.toNativeSnapshot(): NativeActivityPageSnapshot =
    NativeActivityPageSnapshot(items = items.map(CommunityActivity::toNativeSnapshot), nextCursor = nextCursor)

private fun CommunityActivity.toNativeSnapshot(): NativeActivityItemSnapshot =
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
        summary = summary,
        occurredAt = occurredAt,
    )

private fun CommunityNotification.toNativeSnapshot(): NativeNotificationSnapshot =
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

private fun UserProfile.toNativeSnapshot(): NativeProfileSnapshot =
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
    )

private fun ServiceDiagnostic.toNativeSnapshot(): NativeDiagnosticSnapshot =
    NativeDiagnosticSnapshot(endpoint, statusCode, healthy, body)

private fun site.jokersh.anime.data.comment.CommunityRating.toNativeSnapshot(): NativeRatingSnapshot =
    NativeRatingSnapshot(score, votes)

private fun CommunityReview.toNativeSnapshot(): NativeReviewSnapshot =
    NativeReviewSnapshot(id, subjectId, authorId, kind, title, body, spoiler, likeCount, createdAt, owned, bookmarkCount, editedAt, visibility)

private fun CommunityComment.toNativeSnapshot(): NativeCommentSnapshot =
    NativeCommentSnapshot(id, parentId, authorId, authorName, body, spoiler, createdAt, owned, likeCount, bookmarkCount)

private fun CommunityReaction.toNativeSnapshot(): NativeReactionSnapshot =
    NativeReactionSnapshot(reaction, active, likeCount, bookmarkCount)

private fun SubjectDetail.toNativeSnapshot(): NativeSubjectDetailSnapshot =
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

private fun site.jokersh.anime.core.model.SubjectSummary.toNativeSummary(): NativeSubjectSummary =
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

private fun ImageRef?.nativeUrl(): String? =
    when (this) {
        is ImageRef.Remote -> url
        else -> null
    }

private fun AppError?.nativeMessage(): String? =
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

private fun nativeCollectionStatus(value: String): CollectionStatus? =
    when (value.lowercase()) {
        "wish" -> CollectionStatus.Wish
        "watching" -> CollectionStatus.Watching
        "completed" -> CollectionStatus.Completed
        "on_hold", "onhold" -> CollectionStatus.OnHold
        "dropped" -> CollectionStatus.Dropped
        else -> null
    }

private fun createIosContainer(
    readSecret: (String) -> String?,
    writeSecret: (String, String) -> Unit,
    removeSecret: (String) -> Unit,
): AppContainer {
    val baseUrl = PRODUCTION_API_BASE_URL
    val client =
        HttpClient(Darwin) {
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
        }
    val tokenStore =
        IosKeychainSessionTokenStore(
            readSecret = readSecret,
            writeSecret = writeSecret,
            removeSecret = removeSecret,
            clearLocalUserData = { IosLocalUserDataStore().clear() },
        )
    val remoteSession = RemoteSessionRepository(client, baseUrl, tokenStore)
    val remoteCommunity =
        RemoteCommunityRepository(
            client = client,
            apiBaseUrl = baseUrl,
            tokenProvider = { tokenStore.load()?.token },
        )
    return createAppContainer(
        profile =
            BuildProfile(
                environment = Environment.Prod,
                dataModePolicy = DataModePolicy.RemoteOnly,
                apiBaseUrl = baseUrl,
                diagnosticsEnabled = false,
                searchPageSize = 20,
            ),
        catalogRepository = RemoteCatalogRepository(client, baseUrl, cacheStore = IosCatalogCacheStore()),
        searchRepository = RemoteSearchRepository(client, baseUrl, tokenProvider = { tokenStore.load()?.token }),
        sessionRepository = remoteSession,
        communityRepository = OfflineFirstCommunityRepository(remoteCommunity, IosRatingOutboxStore()),
        settingsRepository = PersistentSettingsRepository(IosSettingsStore()),
        collectionRepository =
            OfflineFirstCollectionRepository(
                store = IosCollectionStore(),
                pushStatus = {
                    subjectId,
                    status,
                    progress,
                    ->
                    remoteCommunity.setCollection(subjectId, status, progress)
                },
                pullCollections = { remoteSession.loadAllCollectionItems() },
            ),
        commentRepository =
            RemoteCommentRepository(
                remoteCommunity,
                IosCommentDraftStore(),
                currentUserId = { remoteSession.currentUserId() },
            ),
    )
}

private class IosKeychainSessionTokenStore(
    private val readSecret: (String) -> String?,
    private val writeSecret: (String, String) -> Unit,
    private val removeSecret: (String) -> Unit,
    private val clearLocalUserData: () -> Unit,
) : SessionTokenStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): StoredSessionToken? {
        readSecret(SESSION_RECORD_ACCOUNT)?.let { value ->
            runCatching { json.decodeFromString<IosStoredSessionRecord>(value) }
                .getOrNull()
                ?.takeIf { it.accessToken.isNotBlank() }
                ?.let {
                    return StoredSessionToken(
                        token = it.accessToken,
                        expiresAt = Instant.fromEpochSeconds(it.expiresAtEpochSeconds),
                        refreshToken = it.refreshToken?.takeIf(String::isNotBlank),
                    )
                }
            // A damaged bundle should not prevent a legacy item from being recovered.
            removeSecret(SESSION_RECORD_ACCOUNT)
        }

        val token = readSecret(ACCESS_TOKEN_ACCOUNT)?.takeIf(String::isNotBlank) ?: return null
        // Older builds stored this as a string, while a partially completed save can
        // leave only the Keychain values behind. Treat a missing/invalid timestamp as
        // already expired so a saved refresh token gets a chance to restore the session.
        val expiresAt =
            defaults.stringForKey(EXPIRY_KEY)?.toLongOrNull()
                ?: defaults.objectForKey(EXPIRY_KEY)?.toString()?.toLongOrNull()
                ?: 0L
        val legacy = StoredSessionToken(
            token = token,
            expiresAt = Instant.fromEpochSeconds(expiresAt),
            refreshToken = readSecret(REFRESH_TOKEN_ACCOUNT)?.takeIf(String::isNotBlank),
        )
        // Migrate the old split representation after it has been read successfully.
        // The legacy values remain as a fallback for one upgrade cycle and are removed
        // together with the new record by clear().
        save(legacy.token, legacy.expiresAt, legacy.refreshToken)
        return legacy
    }

    override fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String?,
    ) {
        // Keep all session fields in one Keychain item. This prevents a process
        // interruption between three independent writes from creating a token with
        // a mismatched expiry or refresh token.
        writeSecret(
            SESSION_RECORD_ACCOUNT,
            json.encodeToString(
                IosStoredSessionRecord(
                    accessToken = token,
                    refreshToken = refreshToken,
                    expiresAtEpochSeconds = expiresAt.epochSeconds,
                ),
            ),
        )
    }

    override fun clear() {
        removeSecret(SESSION_RECORD_ACCOUNT)
        removeSecret(ACCESS_TOKEN_ACCOUNT)
        removeSecret(REFRESH_TOKEN_ACCOUNT)
        defaults.removeObjectForKey(EXPIRY_KEY)
        // These stores contain user-owned offline work. They must not survive a
        // logout/expired session and become visible or replayed under another account.
        clearLocalUserData()
    }
}

@Serializable
private data class IosStoredSessionRecord(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochSeconds: Long,
)

private class IosLocalUserDataStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    fun clear() {
        defaults.removeObjectForKey(COLLECTION_SNAPSHOT_KEY)
        defaults.removeObjectForKey(COMMENT_DRAFTS_KEY)
        defaults.removeObjectForKey(RATING_OUTBOX_KEY)
    }

    private companion object {
        const val COLLECTION_SNAPSHOT_KEY = "anime.collection.snapshots"
        const val COMMENT_DRAFTS_KEY = "anime.comment.drafts"
        const val RATING_OUTBOX_KEY = "anime.rating.outbox"
    }
}

private abstract class IosStringStore(
    private val key: String,
) {
    private val defaults = NSUserDefaults.standardUserDefaults

    protected fun readValue(): String? = defaults.stringForKey(key)

    protected fun writeValue(value: String) = defaults.setObject(value, forKey = key)
}

private class IosSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey("anime.settings.$key")

    override fun write(
        key: String,
        value: String,
    ) = defaults.setObject(value, forKey = "anime.settings.$key")

    override fun remove(key: String) = defaults.removeObjectForKey("anime.settings.$key")
}

private class IosCollectionStore :
    IosStringStore("anime.collection.snapshots"),
    CollectionStore {
    override fun read(): String? = readValue()

    override fun write(value: String) = writeValue(value)
}

private class IosCommentDraftStore :
    IosStringStore("anime.comment.drafts"),
    CommentDraftStore {
    override fun read(): String? = readValue()

    override fun write(value: String) = writeValue(value)
}

private class IosCatalogCacheStore : CatalogCacheStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey("anime.catalog.$key")

    override fun write(
        key: String,
        value: String,
    ) = defaults.setObject(value, forKey = "anime.catalog.$key")
}

private class IosRatingOutboxStore :
    IosStringStore("anime.rating.outbox"),
    RatingOutboxStore {
    override fun read(): String? = readValue()

    override fun write(value: String) = writeValue(value)
}

private const val PRODUCTION_API_BASE_URL = "https://api.jokersh.site"
private const val SESSION_RECORD_ACCOUNT = "session-record"
private const val ACCESS_TOKEN_ACCOUNT = "access-token"
private const val REFRESH_TOKEN_ACCOUNT = "refresh-token"
private const val EXPIRY_KEY = "anime.session.expiresAt"
