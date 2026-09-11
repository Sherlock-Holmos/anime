package site.jokersh.anime.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.Backdrop

internal val LocalAnimeBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalAnimeGlassEnabled = staticCompositionLocalOf { true }

/** iOS owns Liquid Glass in the native SwiftUI shell; Compose platforms opt into this local. */
internal val LocalAnimeLiquidGlassEnabled = staticCompositionLocalOf { true }
internal val LocalAnimeLiquidGlassWarmup = staticCompositionLocalOf { false }
internal val LocalAnimeReduceMotion = staticCompositionLocalOf { false }

@Composable
internal expect fun PlatformAnimeBackdropHost(
    modifier: Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
    glassEnabled: Boolean,
    reduceMotion: Boolean,
)

@Composable
internal expect fun platformGlassCapabilities(): GlassCapabilities

@Composable
internal expect fun Modifier.platformGlassEffect(
    role: GlassRole,
    tier: GlassTier,
    shape: Shape,
): Modifier
