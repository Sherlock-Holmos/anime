package site.jokersh.anime.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.kyant.backdrop.Backdrop

internal val LocalAnimeBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
internal expect fun PlatformAnimeBackdropHost(
    modifier: Modifier,
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
)

@Composable
internal expect fun platformGlassCapabilities(): GlassCapabilities

@Composable
internal expect fun Modifier.platformGlassEffect(
    role: GlassRole,
    tier: GlassTier,
    shape: Shape,
): Modifier
