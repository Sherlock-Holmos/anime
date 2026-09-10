package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
    var liquidGlassReady by remember { mutableStateOf(false) }
    var liquidGlassWarmup by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Let the first content frame commit before creating the expensive liquid pipeline.
        withFrameNanos { }
        liquidGlassWarmup = true
        // The hidden warmup component draws once on the dedicated render thread.
        withFrameNanos { }
        liquidGlassWarmup = false
        liquidGlassReady = true
    }
    Box(modifier) {
        background()
        CompositionLocalProvider(
            LocalAnimeBackdrop provides backdrop,
            LocalAnimeLiquidGlassEnabled provides liquidGlassReady,
            LocalAnimeLiquidGlassWarmup provides liquidGlassWarmup,
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
