package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop

@Composable
internal actual fun PlatformAnimeBackdropHost(
    modifier: Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val glassColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
    // Keep AndroidLiquidGlass's official components while avoiding full scrolling-layer replay.
    val backdrop =
        rememberCanvasBackdrop {
            drawRect(glassColor)
        }
    Box(modifier) {
        background()
        CompositionLocalProvider(
            LocalAnimeBackdrop provides backdrop,
        ) {
            content()
        }
    }
}

@Composable
internal actual fun platformGlassCapabilities(): GlassCapabilities = GlassCapabilities(maximumTier = GlassTier.Liquid)

@Composable
internal actual fun Modifier.platformGlassEffect(
    role: GlassRole,
    tier: GlassTier,
    shape: Shape,
): Modifier = this
