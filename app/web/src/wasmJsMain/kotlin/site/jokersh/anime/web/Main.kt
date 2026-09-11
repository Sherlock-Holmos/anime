package site.jokersh.anime.web

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import kotlinx.browser.document
import kotlinx.browser.window
import site.jokersh.anime.app.AnimeApp
import site.jokersh.anime.app.BuildProfile
import site.jokersh.anime.app.DataModePolicy
import site.jokersh.anime.app.Environment
import site.jokersh.anime.app.createAppContainer
import site.jokersh.anime.app.loadAllCollectionItems
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.navigation.AnimeDeepLink
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

@OptIn(ExperimentalComposeUiApi::class, kotlin.js.ExperimentalWasmJsInterop::class)
fun main() {
    val baseUrl = webApiBaseUrl()
    val profile = webBuildProfile(baseUrl)
    val client =
        HttpClient(Js) {
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                requestTimeoutMillis = 30_000
            }
        }
    val tokenStore = BrowserSessionTokenStore()
    val remoteSession = RemoteSessionRepository(client, baseUrl, tokenStore)
    val appContainer =
        createAppContainer(
            profile = profile,
            catalogRepository = RemoteCatalogRepository(client, baseUrl, cacheStore = BrowserCatalogCacheStore()),
            searchRepository = RemoteSearchRepository(client, baseUrl, tokenProvider = { tokenStore.load()?.token }),
            sessionRepository = remoteSession,
            communityRepository =
                OfflineFirstCommunityRepository(
                    RemoteCommunityRepository(
                        client = client,
                        apiBaseUrl = baseUrl,
                        tokenProvider = { tokenStore.load()?.token },
                    ),
                    BrowserRatingOutboxStore(),
                ),
            settingsRepository = PersistentSettingsRepository(BrowserSettingsStore()),
            collectionRepository =
                OfflineFirstCollectionRepository(
                    store = BrowserCollectionStore(),
                    pushStatus = { subjectId, status, progress ->
                        RemoteCommunityRepository(client, baseUrl, tokenProvider = { tokenStore.load()?.token })
                            .setCollection(subjectId, status, progress)
                    },
                    pullCollections = { remoteSession.loadAllCollectionItems() },
                ),
            commentRepository =
                RemoteCommentRepository(
                    RemoteCommunityRepository(client, baseUrl, tokenProvider = { tokenStore.load()?.token }),
                    BrowserCommentDraftStore(),
                    currentUserId = { remoteSession.currentUserId() },
                ),
        )
    val callback = parseAuthCallback(window.location.search)
    val deepLinkRoute =
        AnimeDeepLink.parse(
            rawUrl = window.location.href,
            trustedHosts = setOf(AnimeDeepLink.PRODUCTION_HOST, window.location.hostname),
        )

    ComposeViewport(document.body!!) {
        remember { appContainer }
        AnimeApp(
            appContainer = appContainer,
            deepLinkRoute = deepLinkRoute,
            openExternalUrl = { url -> window.location.href = url },
        )
        LaunchedEffect(callback) {
            callback?.let {
                appContainer.sessionRepository.completeLogin(it)
                window.history.replaceState(null, document.title, window.location.pathname)
            }
        }
    }
}

private fun webBuildProfile(apiBaseUrl: String): BuildProfile =
    BuildProfile(
        environment = Environment.Dev,
        dataModePolicy = DataModePolicy.RemoteWithFixtureSwitch,
        apiBaseUrl = apiBaseUrl,
        diagnosticsEnabled = true,
        searchPageSize = 20,
    )

private fun webApiBaseUrl(): String {
    queryParameters(window.location.search)["api"]?.takeIf(String::isNotBlank)?.let { configured ->
        window.localStorage.setItem(API_BASE_URL_KEY, configured.trimEnd('/'))
    }
    window.localStorage
        .getItem(API_BASE_URL_KEY)
        ?.takeIf(String::isNotBlank)
        ?.let { return it }
    return if (window.location.hostname == "localhost" || window.location.hostname == "127.0.0.1") {
        "http://127.0.0.1:8080"
    } else {
        window.location.origin
    }
}

private fun parseAuthCallback(search: String): AuthCallback? {
    val parameters = queryParameters(search)
    val code = parameters["code"]?.takeIf(String::isNotBlank) ?: return null
    val state = parameters["state"]?.takeIf(String::isNotBlank) ?: return null
    return AuthCallback(authorizationCode = code, state = state)
}

private fun queryParameters(search: String): Map<String, String> =
    search
        .removePrefix("?")
        .split('&')
        .mapNotNull { item ->
            item.split('=', limit = 2).takeIf { it.size == 2 }?.let { parts ->
                decodeURIComponent(parts[0]) to decodeURIComponent(parts[1])
            }
        }.toMap()

private class BrowserSessionTokenStore : SessionTokenStore {
    override fun load(): StoredSessionToken? {
        val token = window.localStorage.getItem(SESSION_TOKEN_KEY) ?: return null
        val expiry = window.localStorage.getItem(SESSION_EXPIRY_KEY)?.toLongOrNull() ?: return null
        return StoredSessionToken(
            token,
            Instant.fromEpochSeconds(expiry),
            window.localStorage.getItem(SESSION_REFRESH_KEY),
        )
    }

    override fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String?,
    ) {
        window.localStorage.setItem(SESSION_TOKEN_KEY, token)
        window.localStorage.setItem(SESSION_EXPIRY_KEY, expiresAt.epochSeconds.toString())
        if (refreshToken ==
            null
        ) {
            window.localStorage.removeItem(SESSION_REFRESH_KEY)
        } else {
            window.localStorage.setItem(SESSION_REFRESH_KEY, refreshToken)
        }
    }

    override fun clear() {
        window.localStorage.removeItem(SESSION_TOKEN_KEY)
        window.localStorage.removeItem(SESSION_EXPIRY_KEY)
        window.localStorage.removeItem(SESSION_REFRESH_KEY)
    }
}

private class BrowserSettingsStore : SettingsStore {
    override fun read(key: String): String? = window.localStorage.getItem("anime.settings.$key")

    override fun write(
        key: String,
        value: String,
    ) = window.localStorage.setItem("anime.settings.$key", value)

    override fun remove(key: String) = window.localStorage.removeItem("anime.settings.$key")
}

private class BrowserCollectionStore : CollectionStore {
    override fun read(): String? = window.localStorage.getItem("anime.collection.snapshots")

    override fun write(value: String) = window.localStorage.setItem("anime.collection.snapshots", value)
}

private class BrowserCommentDraftStore : CommentDraftStore {
    override fun read(): String? = window.localStorage.getItem("anime.comment.drafts")

    override fun write(value: String) = window.localStorage.setItem("anime.comment.drafts", value)
}

private class BrowserCatalogCacheStore : CatalogCacheStore {
    override fun read(key: String): String? = window.localStorage.getItem("anime.catalog.$key")

    override fun write(
        key: String,
        value: String,
    ) = window.localStorage.setItem("anime.catalog.$key", value)
}

private class BrowserRatingOutboxStore : RatingOutboxStore {
    override fun read(): String? = window.localStorage.getItem("anime.rating.outbox")

    override fun write(value: String) = window.localStorage.setItem("anime.rating.outbox", value)
}

private external fun decodeURIComponent(encodedURIComponent: String): String

private const val API_BASE_URL_KEY = "anime.api.baseUrl"
private const val SESSION_TOKEN_KEY = "anime.session.token"
private const val SESSION_EXPIRY_KEY = "anime.session.expiresAt"
private const val SESSION_REFRESH_KEY = "anime.session.refreshToken"
