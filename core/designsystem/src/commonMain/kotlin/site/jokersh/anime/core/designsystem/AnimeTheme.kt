package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightMaterialColors =
    lightColorScheme(
        primary = Color(0xFF007AFF),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF00A7C4),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFFAF52DE),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF2F2F7),
        onBackground = Color(0xFF1D1D1F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1D1D1F),
        surfaceVariant = Color(0xFFE9E9EE),
        onSurfaceVariant = Color(0xFF6E6E73),
        error = Color(0xFFFF3B30),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFFB8B8BD),
        outlineVariant = Color(0xFFD8D8DC),
        scrim = Color(0x73000000),
    )

private val DarkMaterialColors =
    darkColorScheme(
        primary = Color(0xFF0A84FF),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF64D2FF),
        onSecondary = Color(0xFF001E28),
        tertiary = Color(0xFFBF5AF2),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFF0C0C0E),
        onBackground = Color(0xFFF5F5F7),
        surface = Color(0xFF1C1C1E),
        onSurface = Color(0xFFF5F5F7),
        surfaceVariant = Color(0xFF2C2C2E),
        onSurfaceVariant = Color(0xFFA1A1A6),
        error = Color(0xFFFF453A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFF545458),
        outlineVariant = Color(0xFF363638),
        scrim = Color(0x99000000),
    )

private val LightAnimeColors =
    AnimeColorScheme(
        accent = Color(0xFFAF52DE),
        success = Color(0xFF34C759),
        warning = Color(0xFFFF9500),
        error = Color(0xFFFF3B30),
        info = Color(0xFF007AFF),
        scrim = Color(0x73000000),
    )

private val DarkAnimeColors =
    AnimeColorScheme(
        accent = Color(0xFFBF5AF2),
        success = Color(0xFF30D158),
        warning = Color(0xFFFF9F0A),
        error = Color(0xFFFF453A),
        info = Color(0xFF64D2FF),
        scrim = Color(0x99000000),
    )

private val LocalAnimeColorScheme = staticCompositionLocalOf { LightAnimeColors }

public val MaterialTheme.animeColors: AnimeColorScheme
    @Composable get() = LocalAnimeColorScheme.current

@Composable
public fun AnimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val typography = platformAnimeTypography()
    CompositionLocalProvider(
        LocalAnimeColorScheme provides if (darkTheme) DarkAnimeColors else LightAnimeColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkMaterialColors else LightMaterialColors,
            typography = typography,
            shapes = AnimeShapes,
            content = content,
        )
    }
}

@Composable
internal expect fun platformAnimeTypography(): Typography
