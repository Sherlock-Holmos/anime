package site.jokersh.anime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import site.jokersh.anime.app.AnimeApp
import site.jokersh.anime.app.AppContainer
import site.jokersh.anime.app.BuildProfile
import site.jokersh.anime.app.DataModePolicy
import site.jokersh.anime.app.Environment
import site.jokersh.anime.app.createAppContainer
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.navigation.AnimeDeepLink
import site.jokersh.anime.core.navigation.AppRoute
import site.jokersh.anime.data.catalog.RemoteCatalogRepository
import site.jokersh.anime.data.catalog.RemoteSearchRepository
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.OfflineFirstCommunityRepository
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.comment.RemoteCommunityRepository
import site.jokersh.anime.data.session.RemoteSessionRepository
import site.jokersh.anime.data.settings.PersistentSettingsRepository

class MainActivity : ComponentActivity() {
    private var pendingAuthCallback by mutableStateOf<AuthCallback?>(null)
    private var pendingDeepLinkRoute by mutableStateOf<AppRoute?>(null)
    private var httpClient: HttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingAuthCallback = intent.toAuthCallback()
        pendingDeepLinkRoute = intent.toAnimeRoute()
        val appContainer = createAndroidContainer()
        setContent {
            AnimeApp(
                appContainer = appContainer,
                deepLinkRoute = pendingDeepLinkRoute,
                openExternalUrl = { url ->
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                },
            )
            val callback = pendingAuthCallback
            LaunchedEffect(callback) {
                callback?.let { appContainer.sessionRepository.completeLogin(it) }
                if (pendingAuthCallback === callback) pendingAuthCallback = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAuthCallback = intent.toAuthCallback()
        pendingDeepLinkRoute = intent.toAnimeRoute()
    }

    override fun onDestroy() {
        httpClient?.close()
        super.onDestroy()
    }

    private fun createAndroidContainer(): AppContainer {
        val profile = buildProfile()
        val baseUrl = profile.apiBaseUrl ?: return createAppContainer(profile)
        val client =
            HttpClient(OkHttp) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 5_000
                    requestTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }
            }.also { httpClient = it }
        val tokenStore = AndroidEncryptedSessionTokenStore(this)
        return createAppContainer(
            profile = profile,
            catalogRepository = RemoteCatalogRepository(client, baseUrl, cacheStore = AndroidCatalogCacheStore(this)),
            searchRepository = RemoteSearchRepository(client, baseUrl, tokenProvider = { tokenStore.load()?.token }),
            sessionRepository = RemoteSessionRepository(client, baseUrl, tokenStore),
            communityRepository =
                OfflineFirstCommunityRepository(
                    RemoteCommunityRepository(
                        client = client,
                        apiBaseUrl = baseUrl,
                        tokenProvider = { tokenStore.load()?.token },
                    ),
                    AndroidRatingOutboxStore(this),
                ),
            settingsRepository = PersistentSettingsRepository(AndroidSettingsStore(this)),
            collectionRepository =
                OfflineFirstCollectionRepository(AndroidCollectionStore(this)) { subjectId, status, progress ->
                    RemoteCommunityRepository(client, baseUrl, tokenProvider = {
                        tokenStore.load()?.token
                    }).setCollection(subjectId, status, progress)
                },
            commentRepository =
                RemoteCommentRepository(
                    RemoteCommunityRepository(client, baseUrl, tokenProvider = { tokenStore.load()?.token }),
                    AndroidCommentDraftStore(this),
                    currentUserId = { null },
                ),
        )
    }
}

private fun Intent.toAnimeRoute(): AppRoute? =
    AnimeDeepLink.parse(
        rawUrl = dataString,
        trustedHosts = setOf(AnimeDeepLink.PRODUCTION_HOST),
    )

private fun buildProfile(): BuildProfile =
    BuildProfile(
        environment = Environment.valueOf(BuildConfig.ANIME_ENVIRONMENT),
        dataModePolicy = DataModePolicy.valueOf(BuildConfig.ANIME_DATA_MODE_POLICY),
        apiBaseUrl =
            BuildConfig.ANIME_API_BASE_URL.ifBlank {
                if (BuildConfig.ANIME_ENVIRONMENT == "Dev") "http://10.0.2.2:8080" else null
            },
        diagnosticsEnabled = BuildConfig.ANIME_DIAGNOSTICS_ENABLED,
        searchPageSize = BuildConfig.ANIME_SEARCH_PAGE_SIZE,
    )

private fun Intent.toAuthCallback(): AuthCallback? {
    val callbackUri = data ?: return null
    if (!callbackUri.scheme.equals("anime", ignoreCase = true) ||
        !callbackUri.host.equals("bangumi-auth", ignoreCase = true)
    ) {
        return null
    }
    val code = callbackUri.getQueryParameter("code")?.takeIf(String::isNotBlank) ?: return null
    val state = callbackUri.getQueryParameter("state")?.takeIf(String::isNotBlank) ?: return null
    return AuthCallback(authorizationCode = code, state = state)
}
