package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

public enum class GlassTier { None, Translucent, Blur, Liquid }

public enum class GlassRole { TopBar, BottomBar, FloatingPanel, Dialog, StaticHero }

public data class GlassCapabilities(
    public val maximumTier: GlassTier,
    public val reduceTransparency: Boolean = false,
    public val powerSave: Boolean = false,
    public val lowMemory: Boolean = false,
    public val slowFramesDetected: Boolean = false,
)

public fun resolveGlassTier(
    preferred: GlassTier,
    capabilities: GlassCapabilities,
): GlassTier {
    if (capabilities.reduceTransparency || capabilities.lowMemory) return GlassTier.None

    val constrainedMaximum =
        when {
            capabilities.powerSave || capabilities.slowFramesDetected -> GlassTier.Translucent
            else -> capabilities.maximumTier
        }
    return GlassTier.entries[minOf(preferred.ordinal, constrainedMaximum.ordinal)]
}

public fun GlassRole.preferredTier(): GlassTier =
    when (this) {
        GlassRole.TopBar, GlassRole.BottomBar, GlassRole.Dialog -> GlassTier.Blur
        GlassRole.FloatingPanel -> GlassTier.Translucent
        GlassRole.StaticHero -> GlassTier.Liquid
    }

@Composable
public fun AnimeBackdropHost(
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    PlatformAnimeBackdropHost(
        modifier = modifier,
        background = background,
        content = content,
    )
}

@Composable
public fun AnimeGlassPanel(
    role: GlassRole,
    modifier: Modifier = Modifier,
    tierOverride: GlassTier? = null,
    capabilities: GlassCapabilities? = null,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(AnimeSpacing.lg),
    content: @Composable () -> Unit,
) {
    val resolved =
        resolveGlassTier(
            preferred = tierOverride ?: role.preferredTier(),
            capabilities = capabilities ?: platformGlassCapabilities(),
        )
    val alpha =
        when (resolved) {
            GlassTier.None -> 1f
            GlassTier.Translucent -> 0.82f
            GlassTier.Blur -> 0.74f
            GlassTier.Liquid -> 0.62f
        }
    val borderAlpha = if (resolved == GlassTier.None) 0.2f else 0.34f

    Surface(
        modifier = modifier.platformGlassEffect(role, resolved, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)),
        tonalElevation = if (resolved == GlassTier.None) AnimeSpacing.xxs else AnimeSpacing.sm,
    ) {
        Box(Modifier.padding(contentPadding)) {
            content()
        }
    }
}
