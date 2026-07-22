package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightMaterialColors =
    lightColorScheme(
        primary = Color(0xFF5B5CE2),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF007F80),
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFFC93669),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF7F7FB),
        onBackground = Color(0xFF191A22),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF191A22),
        surfaceVariant = Color(0xFFEEF0F7),
        onSurfaceVariant = Color(0xFF5D6070),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        outline = Color(0xFFCBCDD9),
        scrim = Color(0x73000000),
    )

private val DarkMaterialColors =
    darkColorScheme(
        primary = Color(0xFFB8B8FF),
        onPrimary = Color(0xFF171750),
        secondary = Color(0xFF77DAD7),
        onSecondary = Color(0xFF003737),
        tertiary = Color(0xFFFFAFCC),
        onTertiary = Color(0xFF65002F),
        background = Color(0xFF0C0D13),
        onBackground = Color(0xFFE7E8F1),
        surface = Color(0xFF151720),
        onSurface = Color(0xFFE7E8F1),
        surfaceVariant = Color(0xFF20232E),
        onSurfaceVariant = Color(0xFFC5C6D2),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        outline = Color(0xFF8E909F),
        scrim = Color(0x99000000),
    )

private val LightAnimeColors =
    AnimeColorScheme(
        accent = Color(0xFFC93669),
        success = Color(0xFF16865B),
        warning = Color(0xFFA76500),
        error = Color(0xFFBA1A1A),
        info = Color(0xFF007F80),
        scrim = Color(0x73000000),
    )

private val DarkAnimeColors =
    AnimeColorScheme(
        accent = Color(0xFFFFAFCC),
        success = Color(0xFF6DDBA6),
        warning = Color(0xFFFFB95C),
        error = Color(0xFFFFB4AB),
        info = Color(0xFF77DAD7),
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
    CompositionLocalProvider(
        LocalAnimeColorScheme provides if (darkTheme) DarkAnimeColors else LightAnimeColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkMaterialColors else LightMaterialColors,
            typography = AnimeTypography,
            shapes = AnimeShapes,
            content = content,
        )
    }
}
