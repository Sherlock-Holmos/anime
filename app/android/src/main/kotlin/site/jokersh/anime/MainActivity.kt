package site.jokersh.anime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import site.jokersh.anime.app.AnimeApp
import site.jokersh.anime.app.BuildProfile
import site.jokersh.anime.app.DataModePolicy
import site.jokersh.anime.app.Environment
import site.jokersh.anime.app.createAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = createAppContainer(buildProfile())
        setContent {
            AnimeApp(appContainer)
        }
    }
}

private fun buildProfile(): BuildProfile =
    BuildProfile(
        environment = Environment.valueOf(BuildConfig.ANIME_ENVIRONMENT),
        dataModePolicy = DataModePolicy.valueOf(BuildConfig.ANIME_DATA_MODE_POLICY),
        apiBaseUrl = BuildConfig.ANIME_API_BASE_URL.ifBlank { null },
        diagnosticsEnabled = BuildConfig.ANIME_DIAGNOSTICS_ENABLED,
        searchPageSize = BuildConfig.ANIME_SEARCH_PAGE_SIZE,
    )
