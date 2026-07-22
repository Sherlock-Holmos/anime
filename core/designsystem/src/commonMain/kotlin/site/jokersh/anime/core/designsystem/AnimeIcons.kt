package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/** Project-owned, round-stroke back chevron for iOS-inspired navigation surfaces. */
@Composable
public fun AnimeBackIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val centerX = size.width * 0.43f
        drawLine(
            color = color,
            start = Offset(size.width * 0.64f, size.height * 0.22f),
            end = Offset(centerX, size.height * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(centerX, size.height * 0.5f),
            end = Offset(size.width * 0.64f, size.height * 0.78f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
