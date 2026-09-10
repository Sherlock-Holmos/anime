package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
internal actual fun PlatformAnimeBackdropHost(
    modifier: Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
    glassEnabled: Boolean,
    reduceMotion: Boolean,
) {
    val backdrop = if (glassEnabled) rememberLayerBackdrop() else null
    var liquidGlassReady by remember { mutableStateOf(false) }
    var liquidGlassWarmup by remember { mutableStateOf(false) }
    LaunchedEffect(glassEnabled) {
        liquidGlassReady = false
        liquidGlassWarmup = false
        if (!glassEnabled) return@LaunchedEffect
        // Let the first content frame commit before creating the expensive liquid pipeline.
        withFrameNanos { }
        liquidGlassWarmup = true
        // The hidden warmup component draws once on the dedicated render thread.
        withFrameNanos { }
        liquidGlassWarmup = false
        liquidGlassReady = true
    }
    Box(modifier) {
        if (backdrop != null) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                background()
            }
        } else {
            background()
        }
        CompositionLocalProvider(
            LocalAnimeBackdrop provides backdrop,
            LocalAnimeGlassEnabled provides glassEnabled,
            LocalAnimeLiquidGlassEnabled provides liquidGlassReady,
            LocalAnimeLiquidGlassWarmup provides liquidGlassWarmup,
            LocalAnimeReduceMotion provides reduceMotion,
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
