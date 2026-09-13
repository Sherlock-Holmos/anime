package site.jokersh.anime.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.navigation.AppRoot
import site.jokersh.anime.data.catalog.CatalogCacheStore
import site.jokersh.anime.data.catalog.RemoteCatalogRepository
import site.jokersh.anime.data.catalog.RemoteSearchRepository
import site.jokersh.anime.data.collection.CollectionStore
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.CommentDraftStore
import site.jokersh.anime.data.comment.OfflineFirstCommunityRepository
import site.jokersh.anime.data.comment.RatingOutboxStore
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.comment.RemoteCommunityRepository
import site.jokersh.anime.data.session.RemoteSessionRepository
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

    @OptIn(ExperimentalComposeUiApi::class)
    public fun rootViewController(
        rootIndex: Int,
        openExternalUrl: (String) -> Unit,
        readSecret: (String) -> String?,
        writeSecret: (String, String) -> Unit,
        removeSecret: (String) -> Unit,
        onNativeGlassStateChanged: (Boolean) -> Unit,
        onNativeRootNavigationVisibilityChanged: (Boolean) -> Unit,
        onNativeContentHeightChanged: (Double) -> Unit,
        handlesAuthCallback: Boolean,
    ): UIViewController {
        val appContainer = appContainer(readSecret, writeSecret, removeSecret)
        val initialRoot = rootIndex.toAppRoot()
        return ComposeUIViewController {
            if (handlesAuthCallback) {
                val callback by pendingAuthCallback.collectAsState()
                LaunchedEffect(callback) {
                    val current = callback ?: return@LaunchedEffect
                    appContainer.sessionRepository.completeLogin(current)
                    if (pendingAuthCallback.value == current) pendingAuthCallback.value = null
                }
            }
            AnimeApp(
                appContainer = appContainer,
                initialRoot = initialRoot,
                openExternalUrl = openExternalUrl,
                nativeRootNavigation = true,
                onNativeGlassStateChanged = onNativeGlassStateChanged,
                onNativeRootNavigationVisibilityChanged = onNativeRootNavigationVisibilityChanged,
                onNativeContentHeightChanged = onNativeContentHeightChanged,
                lifecycleOwner = rootIndex == 0,
            )
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val json = Json { encodeDefaults = true }
    private var sessionObservation: Job? = null
    private var startupJob: Job? = null

    /** Starts the same app-level restore and background sync work used by the Compose host. */
    public fun start() {
        if (startupJob?.isActive == true) return
        startupJob =
            scope.launch {
                appContainer.sessionRepository.refresh()
                appContainer.collectionRepository.requestSync()
                appContainer.communityRepository.retryPendingRatings()
            }
    }

    public fun startSessionObservation(onChanged: (String) -> Unit) {
        sessionObservation?.cancel()
        sessionObservation =
            scope.launch {
                appContainer.sessionRepository.observeSession().collect { state ->
                    onChanged(json.encodeToString(state.toNativeSnapshot()))
                }
            }
    }

    public fun stopSessionObservation() {
        sessionObservation?.cancel()
        sessionObservation = null
    }

    public fun refreshSession(completion: (String?, String?) -> Unit) {
        scope.launch {
            val result = appContainer.sessionRepository.refresh()
            completion(
                result.getOrNull()?.let { json.encodeToString(it.toNativeSnapshot()) },
                result.exceptionOrNull()?.message,
            )
        }
    }

    public fun loadDiscovery(
        force: Boolean,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val result =
                appContainer.catalogRepository.refreshDiscovery(
                    if (force) RefreshPolicy.Force else RefreshPolicy.IfStale,
                )
            val state = appContainer.catalogRepository.observeDiscovery().first()
            completion(
                state.value?.let { json.encodeToString(it.toNativeSnapshot()) },
                if (state.value == null) result.exceptionOrNull()?.message ?: state.error?.nativeMessage() else null,
            )
        }
    }

    public fun loadSubject(
        subjectId: Long,
        force: Boolean,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            val id = runCatching { SubjectId(subjectId) }.getOrElse {
                completion(null, "作品 ID 无效")
                return@launch
            }
            val result =
                appContainer.catalogRepository.refreshSubject(
                    id,
                    if (force) RefreshPolicy.Force else RefreshPolicy.IfStale,
                )
            val state = appContainer.catalogRepository.observeSubject(id).first()
            completion(
                state.value?.let { json.encodeToString(it.toNativeSnapshot()) },
                if (state.value == null) result.exceptionOrNull()?.message ?: state.error?.nativeMessage() else null,
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
            appContainer.sessionRepository.completeLogin(callback)
            if (IosBridge.pendingCallback() == callback) IosBridge.clearPendingCallback()
        }
    }

    public fun logout(completion: (String?) -> Unit) {
        scope.launch { completion(appContainer.sessionRepository.logout().exceptionOrNull()?.message) }
    }
}

@Serializable
private data class NativeSessionSnapshot(
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
private data class NativeDiscoverySnapshot(
    val sections: List<NativeDiscoverySection>,
    val generatedAtEpochSeconds: Long,
)

@Serializable
private data class NativeDiscoverySection(
    val id: String,
    val title: String,
    val subjects: List<NativeSubjectSummary>,
)

@Serializable
private data class NativeSubjectSummary(
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
private data class NativeSubjectDetailSnapshot(
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

private fun DiscoveryFeed.toNativeSnapshot(): NativeDiscoverySnapshot =
    NativeDiscoverySnapshot(
        sections = sections.map { section ->
            NativeDiscoverySection(section.id, section.title, section.subjects.map { it.toNativeSummary() })
        },
        generatedAtEpochSeconds = generatedAt.epochSeconds,
    )

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

private fun Int.toAppRoot(): AppRoot =
    when (this) {
        1 -> AppRoot.Library
        2 -> AppRoot.Activity
        3 -> AppRoot.Profile
        else -> AppRoot.Discover
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
    val tokenStore = IosKeychainSessionTokenStore(readSecret, writeSecret, removeSecret)
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
) : SessionTokenStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun load(): StoredSessionToken? {
        val token = readSecret(ACCESS_TOKEN_ACCOUNT)?.takeIf(String::isNotBlank) ?: return null
        // Older builds stored this as a string, while a partially completed save can
        // leave only the Keychain values behind. Treat a missing/invalid timestamp as
        // already expired so a saved refresh token gets a chance to restore the session.
        val expiresAt =
            defaults.stringForKey(EXPIRY_KEY)?.toLongOrNull()
                ?: defaults.objectForKey(EXPIRY_KEY)?.toString()?.toLongOrNull()
                ?: 0L
        return StoredSessionToken(
            token = token,
            expiresAt = Instant.fromEpochSeconds(expiresAt),
            refreshToken = readSecret(REFRESH_TOKEN_ACCOUNT)?.takeIf(String::isNotBlank),
        )
    }

    override fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String?,
    ) {
        writeSecret(ACCESS_TOKEN_ACCOUNT, token)
        defaults.setObject(expiresAt.epochSeconds.toString(), forKey = EXPIRY_KEY)
        if (refreshToken ==
            null
        ) {
            removeSecret(REFRESH_TOKEN_ACCOUNT)
        } else {
            writeSecret(REFRESH_TOKEN_ACCOUNT, refreshToken)
        }
    }

    override fun clear() {
        removeSecret(ACCESS_TOKEN_ACCOUNT)
        removeSecret(REFRESH_TOKEN_ACCOUNT)
        defaults.removeObjectForKey(EXPIRY_KEY)
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
private const val ACCESS_TOKEN_ACCOUNT = "access-token"
private const val REFRESH_TOKEN_ACCOUNT = "refresh-token"
private const val EXPIRY_KEY = "anime.session.expiresAt"
