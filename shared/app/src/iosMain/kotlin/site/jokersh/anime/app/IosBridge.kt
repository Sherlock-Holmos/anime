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

/** iOS host entry exported by AnimeShared.framework. */
public object IosBridge {
    private val pendingAuthCallback = MutableStateFlow<AuthCallback?>(null)
    private var sharedAppContainer: AppContainer? = null
    private var sharedNativeFacade: IosNativeAppFacade? = null
    private var sharedApiRouteState: IosApiRouteState? = null

    /** SwiftUI entry point backed by the same KMP container as the legacy Compose host. */
    public fun nativeAppFacade(
        openExternalUrl: (String) -> Unit,
        readSecret: (String) -> String?,
        writeSecret: (String, String) -> Unit,
        removeSecret: (String) -> Unit,
    ): IosNativeAppFacade {
        sharedNativeFacade?.let { return it }
        val container = appContainer(readSecret, writeSecret, removeSecret)
        val routeState = sharedApiRouteState ?: error("iOS API route state was not initialized")
        return IosNativeAppFacade(container, openExternalUrl, routeState).also {
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
    ): AppContainer {
        sharedAppContainer?.let { return it }
        val routeState = IosApiRouteState()
        val container =
            createIosContainer(
                readSecret = readSecret,
                writeSecret = writeSecret,
                removeSecret = removeSecret,
                apiBaseUrlProvider = { routeState.baseUrl },
            )
        sharedApiRouteState = routeState
        sharedAppContainer = container
        return container
    }

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
    private val apiRouteState: IosApiRouteState,
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
    private val routeSelectionMutex = Mutex()
    private val routeClient =
        HttpClient(Darwin) {
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 8_000
                socketTimeoutMillis = 8_000
            }
        }
    private var routeReady = false
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
                        ensureApiRoute()
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
                startupStep("network route selection") { ensureApiRoute() }
                startupStep("session restore") {
                    sessionOperationMutex.withLock { appContainer.sessionRepository.refresh() }
                }
                startupStep("collection sync") { appContainer.collectionRepository.requestSync() }
                startupStep("rating outbox") { appContainer.communityRepository.retryPendingRatings() }
            }
    }

    /** Resolves visitor location through Cloudflare and exposes the selected entrance to SwiftUI. */
    public fun loadNetworkContext(completion: (String?, String?) -> Unit) {
        scope.launch {
            val result =
                runCatching {
                    withTimeout(10_000) { ensureApiRoute() }
                    apiRouteState.context ?: NativeNetworkContext()
                }
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeNetworkContext.serializer(), it) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    private suspend fun ensureApiRoute() {
        routeSelectionMutex.withLock {
            if (routeReady) return@withLock
            try {
                val response = routeClient.get("$PRODUCTION_API_BASE_URL/api/v1/network/context")
                check(response.status.value in 200..299) {
                    "Network context request failed with ${response.status.value}"
                }
                val context = json.decodeFromString<NativeNetworkContext>(response.bodyAsText())
                apiRouteState.context = context
                apiRouteState.baseUrl =
                    if (context.recommendedRoute == "direct") DIRECT_API_BASE_URL else PRODUCTION_API_BASE_URL
                println(
                    "[Anime iOS] API route selected: ${context.recommendedRoute}, " +
                        "location=${context.displayLocation ?: "unknown"}",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                // An unavailable geo bootstrap must never block the app. Unknown location is
                // treated as non-domestic and stays on the Cloudflare entrance.
                apiRouteState.context = NativeNetworkContext()
                apiRouteState.baseUrl = PRODUCTION_API_BASE_URL
                println("[Anime iOS] API route selection failed: ${failure.message ?: failure::class.simpleName}")
            }
            routeReady = true
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
            ensureApiRoute()
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

    public fun loadCalendar(
        date: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("calendar:$date", completion) {
            val result = runCatching { appContainer.catalogRepository.calendar(LocalDate.parse(date)).getOrThrow() }
            result.getOrNull()?.let { json.encodeToString(NativeCalendarSnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun loadSubjectSections(
        subjectId: Long,
        force: Boolean,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("subject sections:$subjectId", completion) {
            val repository = appContainer.catalogRepository
            val id = SubjectId(subjectId)
            val policy = if (force) RefreshPolicy.Force else RefreshPolicy.IfStale
            val (episodeResult, creditResult, relationResult) = coroutineScope {
                val episode = async { repository.refreshSection(id, SubjectSection.Episodes, policy) }
                val credits = async { repository.refreshSection(id, SubjectSection.Credits, policy) }
                val relations = async { repository.refreshSection(id, SubjectSection.Relations, policy) }
                Triple(episode.await(), credits.await(), relations.await())
            }
            val episodes = repository.observeEpisodes(id).first().value
            val credits = repository.observeCredits(id).first().value
            val relations = repository.observeRelations(id).first().value
            val failure =
                episodeResult.exceptionOrNull() ?: creditResult.exceptionOrNull() ?: relationResult.exceptionOrNull()
            val snapshot =
                if (episodes != null && credits != null && relations != null) {
                    NativeSubjectSectionsSnapshot(
                        episodes = episodes.map(Episode::toNativeSnapshot),
                        characters = credits.characters.map(CharacterCredit::toNativeSnapshot),
                        persons = credits.persons.map { it.toNativeSnapshot() },
                        relations = relations.map(SubjectRelation::toNativeSnapshot),
                    )
                } else {
                    null
                }
            snapshot?.let { json.encodeToString(NativeSubjectSectionsSnapshot.serializer(), it) } to
                if (snapshot == null) failure?.message ?: "作品资料暂时无法加载" else null
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
    ) = searchSubjectsFiltered(query, cursor, "||||relevance", completion)

    /** Keep the Swift-facing ABI small; filters use type|from|to|airing|sort. */
    public fun searchSubjectsFiltered(
        query: String,
        cursor: String?,
        filtersCsv: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("search", completion) {
            val normalized = query.trim()
            val result =
                runCatching {
                        val filters = filtersCsv.split('|')
                        val typesCsv = filters.getOrNull(0)
                        val yearStart = filters.getOrNull(1)?.toIntOrNull()
                        val yearEnd = filters.getOrNull(2)?.toIntOrNull()
                        val airingCsv = filters.getOrNull(3)
                        val sort = filters.getOrNull(4).orEmpty()
                        val normalizedYearStart = yearStart?.takeIf { it >= 1900 }
                        val normalizedYearEnd = yearEnd?.takeIf { it >= 1900 }
                        require(yearStart == null || normalizedYearStart != null) { "开始年份无效" }
                        require(yearEnd == null || normalizedYearEnd != null) { "结束年份无效" }
                        require(
                            normalizedYearStart == null ||
                                normalizedYearEnd == null ||
                                normalizedYearStart <= normalizedYearEnd,
                        ) { "年份范围无效" }
                        val years =
                            when {
                                normalizedYearStart == null && normalizedYearEnd == null -> null
                                else -> (normalizedYearStart ?: normalizedYearEnd!!)..(normalizedYearEnd ?: normalizedYearStart!!)
                            }
                        appContainer.searchRepository.search(
                            SearchRequest(
                                query = normalized,
                                types = parseCsv(typesCsv).mapNotNull(String::toNativeSubjectType).toSet(),
                                years = years,
                                airing = parseCsv(airingCsv).mapNotNull(String::toNativeAiringStatus).toSet(),
                                sort = sort.toNativeSearchSort(),
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

    public fun saveSearchHistory(query: String) {
        scope.launch { appContainer.searchRepository.saveHistory(query) }
    }

    public fun loadCollection(
        status: String?,
        cursor: String?,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("collection", completion) {
            val result =
                appContainer.sessionRepository.collectionPage(
                    status = status?.let(::nativeCollectionStatus),
                    cursor = cursor,
                    limit = 50,
                )
            result.getOrNull()?.let { json.encodeToString(NativeCollectionPageSnapshot.serializer(), it.toNativeSnapshot()) } to result.exceptionOrNull()?.message
        }

    public fun loadMyRatings(
        cursor: String?,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("my ratings", completion) {
            val result = appContainer.sessionRepository.ratingsPage(cursor = cursor, limit = 30)
            result.getOrNull()?.let {
                json.encodeToString(NativeProfileRatingPageSnapshot.serializer(), it.toNativeSnapshot())
            } to result.exceptionOrNull()?.message
        }

    public fun loadAdminOverview(completion: (String?, String?) -> Unit) =
        launchTextOperation("admin overview", completion) {
            val result = appContainer.sessionRepository.adminOverview()
            result.getOrNull()?.let {
                json.encodeToString(NativeAdminOverviewSnapshot.serializer(), it.toNativeSnapshot())
            } to result.exceptionOrNull()?.message
        }

    public fun loadAdminComments(status: String?, completion: (String?, String?) -> Unit) =
        launchTextOperation("admin comments", completion) {
            val result = appContainer.sessionRepository.adminComments(status = status, limit = 100)
            result.getOrNull()?.let {
                json.encodeToString(
                    ListSerializer(NativeAdminCommentSnapshot.serializer()),
                    it.map(AdminComment::toNativeSnapshot),
                )
            } to result.exceptionOrNull()?.message
        }

    public fun loadAdminReports(status: String?, completion: (String?, String?) -> Unit) =
        launchTextOperation("admin reports", completion) {
            val result = appContainer.sessionRepository.adminReports(status = status, limit = 100)
            result.getOrNull()?.let {
                json.encodeToString(
                    ListSerializer(NativeAdminReportSnapshot.serializer()),
                    it.map(AdminReport::toNativeSnapshot),
                )
            } to result.exceptionOrNull()?.message
        }

    public fun adminReportAction(
        id: String,
        action: String,
        reason: String?,
        contentAction: String?,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            completion(
                appContainer.sessionRepository.adminReportAction(id, action, reason, contentAction)
                    .exceptionOrNull()?.message,
            )
        }
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

    public fun withdrawActivity(id: String, completion: (String?) -> Unit) {
        scope.launch { completion(appContainer.communityRepository.withdrawActivity(id).exceptionOrNull()?.message) }
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
                    // community-rating is readable by guests and, when an optional
                    // token is present, is the source of truth for the viewer's
                    // rating and collection state. Do not infer personal state from
                    // a bounded collection page: a rated-but-not-collected subject
                    // must also restore correctly across devices.
                    val communityRating = rating.getOrThrow()
                    NativeSubjectCommunitySnapshot(
                        rating = communityRating.toNativeSnapshot(),
                        reviews = reviews.getOrThrow().map(CommunityReview::toNativeSnapshot),
                        comments = comments.getOrThrow().map(CommunityComment::toNativeSnapshot),
                        personal = communityRating.toNativePersonalSnapshot(),
                    )
                } else {
                    null
                }
            snapshot?.let { json.encodeToString(NativeSubjectCommunitySnapshot.serializer(), it) } to failure?.message
        }

    public fun loadReview(
        id: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("review:$id", completion) {
            val result = appContainer.communityRepository.review(id)
            result.getOrNull()?.let { json.encodeToString(NativeReviewSnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun updateReview(
        id: String,
        title: String?,
        body: String?,
        spoiler: Boolean,
        visibility: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("update review:$id", completion) {
            val result = appContainer.communityRepository.updateReview(id, title, body, spoiler, visibility)
            result.getOrNull()?.let { json.encodeToString(NativeReviewSnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun deleteReview(
        id: String,
        completion: (String?) -> Unit,
    ) {
        scope.launch { completion(appContainer.communityRepository.deleteReview(id).exceptionOrNull()?.message) }
    }

    public fun updateComment(
        id: String,
        body: String,
        spoiler: Boolean,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.updateComment(id, body, spoiler)
            completion(
                result.getOrNull()?.let { json.encodeToString(NativeCommentSnapshot.serializer(), it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun deleteComment(
        id: String,
        completion: (String?) -> Unit,
    ) {
        scope.launch { completion(appContainer.communityRepository.deleteComment(id).exceptionOrNull()?.message) }
    }

    public fun deleteRating(
        subjectId: Long,
        completion: (String?) -> Unit,
    ) {
        scope.launch { completion(appContainer.communityRepository.deleteRating(subjectId).exceptionOrNull()?.message) }
    }

    public fun moderateComment(
        id: String,
        action: String,
        reason: String?,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            completion(appContainer.sessionRepository.moderateComment(id, action, reason).exceptionOrNull()?.message)
        }
    }

    public fun reportComment(
        id: String,
        reasonCode: String,
        details: String?,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            completion(
                appContainer.communityRepository.reportComment(id, reasonCode, details).exceptionOrNull()?.message,
            )
        }
    }

    public fun loadUserProfile(
        id: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("user:$id", completion) {
            val result = appContainer.communityRepository.userProfile(id)
            result.getOrNull()?.let { json.encodeToString(NativeUserProfileSnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun loadUserReviews(
        id: String,
        cursor: String?,
        oldestFirst: Boolean,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("user reviews:$id", completion) {
            val result = appContainer.communityRepository.userReviews(id, 20, cursor, oldestFirst)
            result.getOrNull()?.let { json.encodeToString(NativeReviewPageSnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun loadUserLists(
        id: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("user lists:$id", completion) {
            val result = appContainer.communityRepository.userLists(id, 30)
            result.getOrNull()?.let {
                json.encodeToString(ListSerializer(NativeListSummarySnapshot.serializer()), it.map { item -> item.toNativeSnapshot() })
            } to result.exceptionOrNull()?.message
        }

    public fun loadLists(completion: (String?, String?) -> Unit) =
        launchTextOperation("lists", completion) {
            val result = appContainer.communityRepository.lists(30)
            result.getOrNull()?.let {
                json.encodeToString(ListSerializer(NativeListSummarySnapshot.serializer()), it.map { item -> item.toNativeSnapshot() })
            } to result.exceptionOrNull()?.message
        }

    public fun loadList(
        id: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("list:$id", completion) {
            val result = appContainer.communityRepository.list(id)
            result.getOrNull()?.let { json.encodeToString(NativeListDetailSnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun createList(
        title: String,
        description: String,
        visibility: String,
        subjectIdsCsv: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("create list", completion) {
            val result = appContainer.communityRepository.createList(
                title,
                description,
                parseSubjectIds(subjectIdsCsv),
                visibility,
            )
            result.getOrNull()?.let { json.encodeToString(NativeListSummarySnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun updateList(
        id: String,
        title: String,
        description: String,
        visibility: String,
        subjectIdsCsv: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("update list:$id", completion) {
            val result = appContainer.communityRepository.updateList(
                id,
                title,
                description,
                visibility,
                parseSubjectIds(subjectIdsCsv),
            )
            result.getOrNull()?.let { json.encodeToString(NativeListSummarySnapshot.serializer(), it.toNativeSnapshot()) } to
                result.exceptionOrNull()?.message
        }

    public fun deleteList(
        id: String,
        completion: (String?) -> Unit,
    ) {
        scope.launch { completion(appContainer.communityRepository.deleteList(id).exceptionOrNull()?.message) }
    }

    public fun loadFollowUserStatus(
        id: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("user follow:$id", completion) {
            val result = appContainer.communityRepository.followUserStatus(id)
            result.getOrNull()?.let { json.encodeToString(NativeFollowSnapshot.serializer(), NativeFollowSnapshot(it)) } to
                result.exceptionOrNull()?.message
        }

    public fun followUser(
        id: String,
        following: Boolean,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("user follow mutation:$id", completion) {
            val result =
                if (following) appContainer.communityRepository.followUser(id) else appContainer.communityRepository.unfollowUser(id)
            result.getOrNull()?.let { json.encodeToString(NativeFollowSnapshot.serializer(), NativeFollowSnapshot(it)) } to
                result.exceptionOrNull()?.message
        }

    public fun loadFollowListStatus(
        id: String,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("list follow:$id", completion) {
            val result = appContainer.communityRepository.followListStatus(id)
            result.getOrNull()?.let { json.encodeToString(NativeFollowSnapshot.serializer(), NativeFollowSnapshot(it)) } to
                result.exceptionOrNull()?.message
        }

    public fun followList(
        id: String,
        following: Boolean,
        completion: (String?, String?) -> Unit,
    ) =
        launchTextOperation("list follow mutation:$id", completion) {
            val result =
                if (following) appContainer.communityRepository.followList(id) else appContainer.communityRepository.unfollowList(id)
            result.getOrNull()?.let { json.encodeToString(NativeFollowSnapshot.serializer(), NativeFollowSnapshot(it)) } to
                result.exceptionOrNull()?.message
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
        episodeProgress: Int?,
        completion: (String?) -> Unit,
    ) = setCollectionWithProgress(subjectId, status, episodeProgress ?: -1, completion)

    public fun setCollectionWithProgress(
        subjectId: Long,
        status: String?,
        episodeProgress: Int,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            val result =
                if (status == null) {
                    appContainer.communityRepository.deleteCollection(subjectId)
                } else {
                    nativeCollectionStatus(status)?.let { normalizedStatus ->
                        appContainer.communityRepository
                            .setCollection(subjectId, normalizedStatus.apiValueForIos(), episodeProgress.takeIf { it >= 0 })
                    } ?: Result.failure(IllegalArgumentException("不支持的收藏状态"))
                }
            // The SwiftUI page reads the remote collection endpoint, while the shared
            // offline-first store is used by startup sync and other platforms. Refresh
            // that store after a successful mutation so a later offline read cannot
            // resurrect the old status or progress.
            if (result.isSuccess) {
                appContainer.collectionRepository.requestSync()
            }
            completion(result.exceptionOrNull()?.message)
        }
    }

    public fun createComment(
        subjectId: Long,
        body: String,
        completion: (String?, String?) -> Unit,
    ) {
        createCommentAdvanced(subjectId, body, false, null, completion)
    }

    public fun createCommentAdvanced(
        subjectId: Long,
        body: String,
        spoiler: Boolean,
        parentId: String?,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.createComment(subjectId, body, spoiler, parentId)
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
        // R1 exposes short reviews only. Keep this compatibility wrapper aligned
        // with the public iOS composer instead of sending the legacy kind value.
        createReviewAdvanced(subjectId, "short", null, body, false, "public", completion)
    }

    public fun createReviewAdvanced(
        subjectId: Long,
        kind: String,
        title: String?,
        body: String,
        spoiler: Boolean,
        visibility: String,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result = appContainer.communityRepository.createReview(
                subjectId = subjectId,
                kind = kind,
                title = title,
                body = body,
                spoiler = spoiler,
                visibility = visibility,
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

    public fun uploadAvatar(base64: String, contentType: String, completion: (String?) -> Unit) {
        scope.launch {
            completion(
                sessionOperationMutex.withLock {
                    appContainer.sessionRepository.uploadAvatar(base64, contentType)
                }.exceptionOrNull()?.message,
            )
        }
    }

    public fun changePassword(
        currentPassword: String,
        newPassword: String,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            completion(
                sessionOperationMutex.withLock {
                    appContainer.sessionRepository.changePassword(currentPassword, newPassword)
                }.exceptionOrNull()?.message,
            )
        }
    }

    public fun exportMyData(completion: (String?, String?) -> Unit) =
        launchTextOperation("export account data", completion) {
            val result = appContainer.sessionRepository.exportMyData()
            result.getOrNull() to result.exceptionOrNull()?.message
        }

    public fun deleteAccount(completion: (String?) -> Unit) {
        scope.launch {
            completion(
                sessionOperationMutex.withLock { appContainer.sessionRepository.deleteAccount() }
                    .exceptionOrNull()
                    ?.message,
            )
        }
    }

    public fun loadSyncStatus(completion: (String?, String?) -> Unit) =
        launchTextOperation("sync status", completion) {
            val result = appContainer.sessionRepository.syncStatus()
            result.getOrNull()?.let {
                json.encodeToString(NativeSyncStatusSnapshot.serializer(), it.toNativeSnapshot())
            } to result.exceptionOrNull()?.message
        }

    public fun startSync(completion: (String?) -> Unit) {
        scope.launch { completion(appContainer.sessionRepository.startSync().exceptionOrNull()?.message) }
    }

    public fun loadSyncConflicts(completion: (String?, String?) -> Unit) =
        launchTextOperation("sync conflicts", completion) {
            val result = appContainer.sessionRepository.syncConflicts()
            result.getOrNull()?.let {
                json.encodeToString(
                    ListSerializer(NativeSyncConflictSnapshot.serializer()),
                    it.map(BangumiSyncConflict::toNativeSnapshot),
                )
            } to result.exceptionOrNull()?.message
        }

    public fun resolveSyncConflict(
        id: String,
        expectedVersion: Long,
        choice: String,
        completion: (String?) -> Unit,
    ) {
        scope.launch {
            completion(
                appContainer.sessionRepository
                    .resolveSyncConflict(id, expectedVersion, choice.toSyncConflictChoice())
                    .exceptionOrNull()
                    ?.message,
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
