package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

@Composable
internal actual fun PlatformAnimeBackdropHost(
    modifier: Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
    glassEnabled: Boolean,
    reduceMotion: Boolean,
) {
    // iOS uses the native SwiftUI Liquid Glass shell. Compose content deliberately stays
    // free of the Skia Backdrop pipeline so the iOS surface is not a second glass renderer.
    Box(modifier) {
        background()
        CompositionLocalProvider(
            LocalAnimeBackdrop provides null,
            LocalAnimeGlassEnabled provides glassEnabled,
            LocalAnimeLiquidGlassEnabled provides false,
            LocalAnimeLiquidGlassWarmup provides false,
            LocalAnimeReduceMotion provides reduceMotion,
        ) {
            content()
        }
    }
}

@Composable
internal actual fun platformGlassCapabilities(): GlassCapabilities =
    GlassCapabilities(maximumTier = GlassTier.Translucent)

@Composable
internal actual fun Modifier.platformGlassEffect(
    role: GlassRole,
    tier: GlassTier,
    shape: Shape,
): Modifier = this
