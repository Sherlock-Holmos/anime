package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    if (!glassEnabled) {
        Box(modifier) {
            background()
            CompositionLocalProvider(
                LocalAnimeBackdrop provides null,
                LocalAnimeGlassEnabled provides false,
                LocalAnimeLiquidGlassEnabled provides false,
                LocalAnimeReduceMotion provides reduceMotion,
            ) { content() }
        }
        return
    }
    val backdrop = rememberLayerBackdrop()
    Box(modifier) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            background()
        }
        CompositionLocalProvider(
            LocalAnimeBackdrop provides backdrop,
            LocalAnimeGlassEnabled provides glassEnabled,
            LocalAnimeLiquidGlassEnabled provides glassEnabled,
            LocalAnimeReduceMotion provides reduceMotion,
        ) {
            content()
        }
    }
}

@Composable
internal actual fun platformGlassCapabilities(): GlassCapabilities =
    GlassCapabilities(
        // Liquid rendering on Desktop is reserved for the unmodified upstream
        // AndroidLiquidGlass catalog components, such as LiquidBottomTabs.
        maximumTier = GlassTier.Translucent,
    )

@Composable
internal actual fun Modifier.platformGlassEffect(
    role: GlassRole,
    tier: GlassTier,
    shape: Shape,
): Modifier = this

internal fun desktopMaximumGlassTier(
    requestedTier: String?,
    remoteSession: Boolean,
): GlassTier =
    when (requestedTier?.trim()?.lowercase()) {
        "none" -> GlassTier.None
        "translucent" -> GlassTier.Translucent
        "blur" -> GlassTier.Blur
        "liquid" -> GlassTier.Liquid
        else -> if (remoteSession) GlassTier.Translucent else GlassTier.Liquid
    }
