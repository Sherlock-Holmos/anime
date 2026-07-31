package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

@Composable
internal actual fun PlatformAnimeBackdropHost(
    modifier: Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            background()
        }
        CompositionLocalProvider(LocalAnimeBackdrop provides backdrop) {
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
): Modifier {
    val backdrop = LocalAnimeBackdrop.current ?: return this
    if (tier < GlassTier.Blur) return this

    val density = LocalDensity.current
    val blurRadius = with(density) { role.blurRadius.toPx() }
    if (tier == GlassTier.Liquid) {
        return drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadius)
                lens(
                    refractionHeight = with(density) { 10.dp.toPx() },
                    refractionAmount = with(density) { 14.dp.toPx() },
                    depthEffect = true,
                    chromaticAberration = false,
                )
            },
        )
    }

    return drawPlainBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = { blur(blurRadius) },
    )
}

private val GlassRole.blurRadius: Dp
    get() =
        when (this) {
            GlassRole.TopBar -> 20.dp
            GlassRole.BottomBar -> 24.dp
            GlassRole.FloatingPanel -> 16.dp
            GlassRole.Dialog, GlassRole.StaticHero -> 28.dp
        }
