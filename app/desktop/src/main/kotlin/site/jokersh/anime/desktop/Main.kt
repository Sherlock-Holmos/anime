package site.jokersh.anime.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import site.jokersh.anime.app.AnimeApp
import site.jokersh.anime.app.BuildProfile
import site.jokersh.anime.app.DataModePolicy
import site.jokersh.anime.app.Environment
import site.jokersh.anime.app.createAppContainer

fun main() =
    application {
        val appContainer = remember { createAppContainer(desktopBuildProfile()) }
        val windowState =
            rememberWindowState(
                width = 1180.dp,
                height = 820.dp,
            )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Anime",
        ) {
            AnimeApp(appContainer)
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
