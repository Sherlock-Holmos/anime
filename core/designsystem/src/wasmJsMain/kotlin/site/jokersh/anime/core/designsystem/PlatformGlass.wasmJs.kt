package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
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
    Box(modifier) {
        background()
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAnimeGlassEnabled provides glassEnabled,
            LocalAnimeLiquidGlassEnabled provides glassEnabled,
            LocalAnimeReduceMotion provides reduceMotion,
        ) { content() }
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
