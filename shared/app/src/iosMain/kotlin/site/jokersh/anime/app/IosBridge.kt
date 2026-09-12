package site.jokersh.anime.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeUIViewController
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIViewController
import site.jokersh.anime.core.model.AuthCallback
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

    @OptIn(ExperimentalComposeUiApi::class)
    public fun rootViewController(
        rootIndex: Int,
        openExternalUrl: (String) -> Unit,
        readSecret: (String) -> String?,
        writeSecret: (String, String) -> Unit,
        removeSecret: (String) -> Unit,
        onNativeGlassStateChanged: (Boolean) -> Unit,
        handlesAuthCallback: Boolean,
    ): UIViewController {
        val appContainer =
            sharedAppContainer
                ?: createIosContainer(readSecret, writeSecret, removeSecret).also { sharedAppContainer = it }
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
        return true
    }
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
                pushStatus = { subjectId, status, progress -> remoteCommunity.setCollection(subjectId, status, progress) },
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
        val token = readSecret(ACCESS_TOKEN_ACCOUNT) ?: return null
        val expiresAt = defaults.objectForKey(EXPIRY_KEY)?.toString()?.toLongOrNull() ?: return null
        return StoredSessionToken(
            token = token,
            expiresAt = Instant.fromEpochSeconds(expiresAt),
            refreshToken = readSecret(REFRESH_TOKEN_ACCOUNT),
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
