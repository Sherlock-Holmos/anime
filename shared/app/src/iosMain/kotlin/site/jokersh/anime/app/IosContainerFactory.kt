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

internal fun createIosContainer(
    readSecret: (String) -> String?,
    writeSecret: (String, String) -> Unit,
    removeSecret: (String) -> Unit,
    apiBaseUrlProvider: () -> String = { PRODUCTION_API_BASE_URL },
): AppContainer {
    val baseUrl = apiBaseUrlProvider()
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
    val remoteSession =
        RemoteSessionRepository(
            client = client,
            apiBaseUrl = baseUrl,
            tokenStore = tokenStore,
            diagnosticEndpoints =
                listOf(
                    ServiceDiagnosticEndpoint("Cloudflare 入口", PRODUCTION_API_BASE_URL),
                    ServiceDiagnosticEndpoint("腾讯云直连", DIRECT_API_BASE_URL),
                ),
            apiBaseUrlProvider = apiBaseUrlProvider,
        )
    val remoteCommunity =
        RemoteCommunityRepository(
            client = client,
            apiBaseUrl = baseUrl,
            tokenProvider = { tokenStore.load()?.token },
            apiBaseUrlProvider = apiBaseUrlProvider,
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
        catalogRepository =
            RemoteCatalogRepository(
                client,
                baseUrl,
                cacheStore = IosCatalogCacheStore(),
                apiBaseUrlProvider = apiBaseUrlProvider,
            ),
        searchRepository =
            RemoteSearchRepository(
                client,
                baseUrl,
                tokenProvider = { tokenStore.load()?.token },
                apiBaseUrlProvider = apiBaseUrlProvider,
            ),
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

internal const val PRODUCTION_API_BASE_URL = "https://api.jokersh.site"
internal const val DIRECT_API_BASE_URL = "https://124.223.14.130"
internal const val SESSION_RECORD_ACCOUNT = "session-record"
internal const val ACCESS_TOKEN_ACCOUNT = "access-token"
internal const val REFRESH_TOKEN_ACCOUNT = "refresh-token"
internal const val EXPIRY_KEY = "anime.session.expiresAt"
