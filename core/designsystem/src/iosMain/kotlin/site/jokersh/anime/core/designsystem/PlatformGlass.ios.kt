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
) {
    Box(modifier) {
        background()
        // AndroidLiquidGlass still flickers on physical iOS devices even with a static backdrop:
        // its RuntimeShader/GraphicsLayer path is the remaining trigger. Keep iOS on the stable
        // translucent material path until the upstream renderer issue is fixed.
        CompositionLocalProvider(
            LocalAnimeBackdrop provides null,
            LocalLiquidComponentsEnabled provides false,
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
