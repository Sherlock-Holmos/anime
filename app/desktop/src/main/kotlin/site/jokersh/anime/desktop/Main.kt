package site.jokersh.anime.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import org.jetbrains.skia.Image
import site.jokersh.anime.app.AnimeApp
import site.jokersh.anime.app.AnimeAppShortcutDispatcher
import site.jokersh.anime.app.BuildProfile
import site.jokersh.anime.app.DataModePolicy
import site.jokersh.anime.app.Environment
import site.jokersh.anime.app.createAppContainer
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.navigation.AnimeDeepLink
import site.jokersh.anime.core.navigation.AppRoot
import site.jokersh.anime.core.navigation.AppRoute
import site.jokersh.anime.data.catalog.RemoteCatalogRepository
import site.jokersh.anime.data.catalog.RemoteSearchRepository
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.OfflineFirstCommunityRepository
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.comment.RemoteCommunityRepository
import site.jokersh.anime.data.session.RemoteSessionRepository
import site.jokersh.anime.data.settings.PersistentSettingsRepository
import java.awt.Dimension
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.prefs.Preferences
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val initialDeepLink = args.firstOrNull(::isSupportedDeepLink)
    val startupMessage = initialDeepLink ?: DesktopDeepLinkBroker.ACTIVATE_MESSAGE
    if (DesktopDeepLinkBroker.forwardToRunningApp(startupMessage)) {
        return
    }

    val deepLinkBroker = DesktopDeepLinkBroker.start()
    application {
        val profile = remember { desktopBuildProfile() }
        val appContainer =
            remember {
                val baseUrl = profile.apiBaseUrl
                if (baseUrl == null) {
                    createAppContainer(profile)
                } else {
                    val client =
                        HttpClient(OkHttp) {
                            install(HttpTimeout) {
                                connectTimeoutMillis = 5_000
                                requestTimeoutMillis = 30_000
                                socketTimeoutMillis = 30_000
                            }
                        }
                    val tokenStore = WindowsProtectedSessionTokenStore()
                    createAppContainer(
                        profile = profile,
                        catalogRepository =
                            RemoteCatalogRepository(
                                client,
                                baseUrl,
                                cacheStore = DesktopCatalogCacheStore(),
                            ),
                        searchRepository =
                            RemoteSearchRepository(
                                client,
                                baseUrl,
                                tokenProvider = { tokenStore.load()?.token },
                            ),
                        sessionRepository =
                            RemoteSessionRepository(
                                client = client,
                                apiBaseUrl = baseUrl,
                                tokenStore = tokenStore,
                            ),
                        communityRepository =
                            OfflineFirstCommunityRepository(
                                RemoteCommunityRepository(
                                    client = client,
                                    apiBaseUrl = baseUrl,
                                    tokenProvider = { tokenStore.load()?.token },
                                ),
                                DesktopRatingOutboxStore(),
                            ),
                        settingsRepository = PersistentSettingsRepository(DesktopSettingsStore()),
                        collectionRepository =
                            OfflineFirstCollectionRepository(DesktopCollectionStore()) { subjectId, status, progress ->
                                RemoteCommunityRepository(client, baseUrl, tokenProvider = {
                                    tokenStore.load()?.token
                                }).setCollection(subjectId, status, progress)
                            },
                        commentRepository =
                            RemoteCommentRepository(
                                RemoteCommunityRepository(
                                    client,
                                    baseUrl,
                                    tokenProvider = { tokenStore.load()?.token },
                                ),
                                DesktopCommentDraftStore(),
                                currentUserId = { null },
                            ),
                    )
                }
            }
        val authCallback = remember(initialDeepLink) { initialDeepLink?.let(::parseAuthCallback) }
        var deepLinkRoute by remember(initialDeepLink) {
            mutableStateOf(initialDeepLink?.let(AnimeDeepLink::parse))
        }
        val authorizationBrowser =
            remember(profile.apiBaseUrl) {
                profile.apiBaseUrl?.let(::BangumiAuthorizationBrowser)
            }
        val appIcon = remember { loadAppIcon() }
        val shortcutDispatcher = remember { AnimeAppShortcutDispatcher() }
        val savedWindow = remember { DesktopWindowPreferences.load() }
        val windowState =
            rememberWindowState(
                placement = savedWindow.placement,
                position = savedWindow.position,
                width = savedWindow.width.dp,
                height = savedWindow.height.dp,
            )

        Window(
            onCloseRequest = {
                DesktopWindowPreferences.save(windowState)
                authorizationBrowser?.close()
                deepLinkBroker.close()
                exitApplication()
            },
            state = windowState,
            title = "Anime",
            icon = appIcon,
            onPreviewKeyEvent = shortcutDispatcher::dispatch,
        ) {
            val density = LocalDensity.current
            LaunchedEffect(window, density.density) {
                applyWindowsChrome(window)
                window.minimumSize =
                    Dimension(
                        with(density) { 840.dp.roundToPx() },
                        with(density) { 640.dp.roundToPx() },
                    )
            }
            AnimeApp(
                appContainer = appContainer,
                shortcutDispatcher = shortcutDispatcher,
                initialRoot = desktopInitialRoot(),
                initialSearchQuery =
                    System.getProperty("anime.desktop.initialQuery")
                        ?: System.getenv("ANIME_DESKTOP_INITIAL_QUERY"),
                deepLinkRoute = deepLinkRoute,
                openExternalUrl = { url ->
                    if (url.startsWith("https://bgm.tv/oauth/authorize")) {
                        authorizationBrowser?.open(url)
                    } else {
                        java.awt.Desktop
                            .getDesktop()
                            .browse(URI(url))
                    }
                },
            )
            LaunchedEffect(authCallback) {
                authCallback?.let { appContainer.sessionRepository.completeLogin(it) }
            }
            LaunchedEffect(deepLinkBroker, window) {
                deepLinkBroker.messages.collect { message ->
                    if (isBangumiAuthDeepLink(message)) authorizationBrowser?.close()
                    window.toFront()
                    window.requestFocus()
                    parseAuthCallback(message)?.let { callback ->
                        appContainer.sessionRepository.completeLogin(callback)
                    }
                    AnimeDeepLink.parse(message)?.let { route -> deepLinkRoute = route }
                }
            }
        }
    }
}

private class DesktopDeepLinkBroker private constructor(
    private val serverSocket: ServerSocket,
) : AutoCloseable {
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    init {
        thread(name = "anime-deep-link-listener", isDaemon = true) {
            while (!serverSocket.isClosed) {
                runCatching {
                    serverSocket.accept().use { socket ->
                        val message =
                            BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                                .readLine()
                        if (message == ACTIVATE_MESSAGE || message?.let(::isSupportedDeepLink) == true) {
                            messages.tryEmit(message)
                        }
                    }
                }.onFailure {
                    if (!serverSocket.isClosed) {
                        System.err.println("Desktop deep-link listener failed: ${it.message}")
                    }
                }
            }
        }
    }

    override fun close() {
        runCatching(serverSocket::close)
    }

    companion object {
        private const val PORT = 45832
        const val ACTIVATE_MESSAGE: String = "ANIME_ACTIVATE"
        private val LOOPBACK: InetAddress = InetAddress.getLoopbackAddress()

        fun start(): DesktopDeepLinkBroker = DesktopDeepLinkBroker(ServerSocket(PORT, 8, LOOPBACK))

        fun forwardToRunningApp(message: String): Boolean {
            if ((message != ACTIVATE_MESSAGE && !isSupportedDeepLink(message)) ||
                message.any { it == '\r' || it == '\n' }
            ) {
                return false
            }
            return runCatching {
                Socket(LOOPBACK, PORT).use { socket ->
                    BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)).use { writer ->
                        writer.write(message)
                        writer.newLine()
                    }
                }
            }.isSuccess
        }
    }
}

private fun loadAppIcon(): BitmapPainter {
    val bytes =
        checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("app-icon.png")) {
            "Missing Desktop application icon resource"
        }.use { it.readBytes() }
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

private data class SavedWindow(
    val width: Float,
    val height: Float,
    val position: WindowPosition,
    val placement: WindowPlacement,
)

private object DesktopWindowPreferences {
    private const val DEFAULT_WIDTH = 1180f
    private const val DEFAULT_HEIGHT = 820f
    private const val MIN_WIDTH = 840f
    private const val MIN_HEIGHT = 640f
    private const val MAX_WIDTH = 3840f
    private const val MAX_HEIGHT = 2160f
    private val preferences = Preferences.userRoot().node("site/jokersh/anime/desktop/window")

    fun load(): SavedWindow {
        val width = preferences.getFloat("width", DEFAULT_WIDTH).coerceIn(MIN_WIDTH, MAX_WIDTH)
        val height = preferences.getFloat("height", DEFAULT_HEIGHT).coerceIn(MIN_HEIGHT, MAX_HEIGHT)
        val x = preferences.getFloat("x", Float.NaN)
        val y = preferences.getFloat("y", Float.NaN)
        val position =
            if (x.isFinite() && y.isFinite()) {
                WindowPosition(x.dp, y.dp)
            } else {
                WindowPosition.PlatformDefault
            }
        val placement =
            runCatching {
                WindowPlacement.valueOf(preferences.get("placement", WindowPlacement.Floating.name))
            }.getOrDefault(WindowPlacement.Floating)

        return SavedWindow(
            width = width,
            height = height,
            position = position,
            placement = placement,
        )
    }

    fun save(state: WindowState) {
        preferences.put("placement", state.placement.name)
        if (state.placement != WindowPlacement.Floating) return

        preferences.putFloat(
            "width",
            state.size.width.value
                .coerceIn(MIN_WIDTH, MAX_WIDTH),
        )
        preferences.putFloat(
            "height",
            state.size.height.value
                .coerceIn(MIN_HEIGHT, MAX_HEIGHT),
        )
        val position = state.position
        if (position is WindowPosition.Absolute) {
            preferences.putFloat("x", position.x.value)
            preferences.putFloat("y", position.y.value)
        }
    }
}

private const val PRODUCTION_API_BASE_URL = "https://api.jokersh.site"

private fun desktopBuildProfile(): BuildProfile {
    val developmentApiOverride =
        (System.getProperty("anime.api.baseUrl") ?: System.getenv("ANIME_API_BASE_URL"))
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    return if (developmentApiOverride == null) {
        BuildProfile(
            environment = Environment.Prod,
            dataModePolicy = DataModePolicy.RemoteOnly,
            apiBaseUrl = PRODUCTION_API_BASE_URL,
            diagnosticsEnabled = false,
            searchPageSize = 10,
        )
    } else {
        BuildProfile(
            environment = Environment.Dev,
            dataModePolicy = DataModePolicy.RemoteWithFixtureSwitch,
            apiBaseUrl = developmentApiOverride,
            diagnosticsEnabled = true,
            searchPageSize = 10,
        )
    }
}

private fun parseAuthCallback(argument: String): AuthCallback? {
    if (!isBangumiAuthDeepLink(argument)) return null
    val query = URI(argument).rawQuery ?: return null
    val parameters =
        query
            .split('&')
            .mapNotNull { part ->
                part.split('=', limit = 2).takeIf { it.size == 2 }?.let {
                    it[0] to
                        java.net.URLDecoder.decode(it[1], Charsets.UTF_8)
                }
            }.toMap()
    val code = parameters["code"] ?: return null
    val state = parameters["state"] ?: return null
    return AuthCallback(authorizationCode = code, state = state)
}

private fun isBangumiAuthDeepLink(argument: String): Boolean =
    runCatching {
        val uri = URI(argument)
        uri.scheme.equals("anime", ignoreCase = true) && uri.host.equals("bangumi-auth", ignoreCase = true)
    }.getOrDefault(false)

private fun isSupportedDeepLink(argument: String): Boolean =
    isBangumiAuthDeepLink(argument) || AnimeDeepLink.parse(argument) is AppRoute.Subject

private fun desktopInitialRoot(): AppRoot =
    when (
        (
            System.getProperty("anime.desktop.initialRoot")
                ?: System.getenv("ANIME_DESKTOP_INITIAL_ROOT")
        )?.trim()?.lowercase()
    ) {
        "library", "search" -> AppRoot.Library
        "activity", "collection", "timeline" -> AppRoot.Activity
        "profile" -> AppRoot.Profile
        else -> AppRoot.Discover
    }
