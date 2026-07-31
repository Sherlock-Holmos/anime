package site.jokersh.anime.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import site.jokersh.anime.app.AnimeApp
import site.jokersh.anime.app.AnimeAppShortcutDispatcher
import site.jokersh.anime.app.BuildProfile
import site.jokersh.anime.app.DataModePolicy
import site.jokersh.anime.app.Environment
import site.jokersh.anime.app.createAppContainer
import site.jokersh.anime.core.navigation.AppRoot
import java.awt.Dimension
import java.util.prefs.Preferences

fun main() =
    application {
        val appContainer = remember { createAppContainer(desktopBuildProfile()) }
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
                exitApplication()
            },
            state = windowState,
            title = "Anime",
            onPreviewKeyEvent = shortcutDispatcher::dispatch,
        ) {
            val density = LocalDensity.current
            LaunchedEffect(window, density.density) {
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
            )
        }
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

private fun desktopBuildProfile(): BuildProfile =
    BuildProfile(
        environment = Environment.Demo,
        dataModePolicy = DataModePolicy.FixtureOnly,
        apiBaseUrl = null,
        diagnosticsEnabled = true,
        searchPageSize = 10,
    )

private fun desktopInitialRoot(): AppRoot =
    when (
        (
            System.getProperty("anime.desktop.initialRoot")
                ?: System.getenv("ANIME_DESKTOP_INITIAL_ROOT")
        )?.trim()?.lowercase()
    ) {
        "search" -> AppRoot.Search
        "collection" -> AppRoot.Collection
        "profile" -> AppRoot.Profile
        else -> AppRoot.Discover
    }
