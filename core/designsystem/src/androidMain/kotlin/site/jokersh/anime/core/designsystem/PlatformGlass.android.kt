package site.jokersh.anime.core.designsystem

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.isRuntimeShaderSupported
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

private val LocalAnimeBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

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
internal actual fun platformGlassCapabilities(): GlassCapabilities =
    GlassCapabilities(maximumTier = platformMaximumGlassTier())

@Composable
internal actual fun Modifier.platformGlassEffect(
    role: GlassRole,
    tier: GlassTier,
    shape: Shape,
): Modifier {
    val backdrop = LocalAnimeBackdrop.current ?: return this
    if (tier < GlassTier.Blur || !isRenderEffectSupported()) return this

    val density = LocalDensity.current
    val blurRadius = with(density) { role.blurRadius.toPx() }
    if (tier == GlassTier.Liquid && isRuntimeShaderSupported()) {
        val refractionHeight = with(density) { 10.dp.toPx() }
        val refractionAmount = with(density) { 14.dp.toPx() }
        return drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadius)
                lens(
                    refractionHeight = refractionHeight,
                    refractionAmount = refractionAmount,
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

@Composable
internal actual fun Modifier.platformLiquidSelectionEffect(
    shape: Shape,
    tint: Color,
    interactionProgress: Float,
): Modifier {
    val backdrop = LocalAnimeBackdrop.current ?: return this
    if (!isRuntimeShaderSupported()) return this

    val density = LocalDensity.current
    val progress = interactionProgress.coerceIn(0f, 1f)
    val refractionHeight = with(density) { (6.dp + 6.dp * progress).toPx() }
    val refractionAmount = with(density) { (9.dp + 7.dp * progress).toPx() }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            lens(
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                depthEffect = true,
                chromaticAberration = progress > 0.55f,
            )
        },
        highlight = {
            Highlight.Default.copy(alpha = 0.25f + 0.55f * progress)
        },
        shadow = {
            Shadow(alpha = 0.16f + 0.34f * progress)
        },
        innerShadow = {
            InnerShadow(
                radius = 4.dp + 4.dp * progress,
                alpha = 0.2f + 0.5f * progress,
            )
        },
        onDrawSurface = {
            drawRect(tint.copy(alpha = 0.12f + 0.05f * progress))
        },
    )
}

internal fun platformMaximumGlassTier(): GlassTier = maximumGlassTierForAndroidSdk(Build.VERSION.SDK_INT)

internal fun maximumGlassTierForAndroidSdk(sdkInt: Int): GlassTier =
    when {
        sdkInt >= 33 -> GlassTier.Liquid
        sdkInt >= 31 -> GlassTier.Blur
        else -> GlassTier.Translucent
    }

private val GlassRole.blurRadius: Dp
    get() =
        when (this) {
            GlassRole.TopBar -> 20.dp
            GlassRole.BottomBar -> 24.dp
            GlassRole.FloatingPanel -> 16.dp
            GlassRole.Dialog, GlassRole.StaticHero -> 28.dp
        }
