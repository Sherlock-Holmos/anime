package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

public data class AnimeColorScheme(
    public val accent: Color,
    public val success: Color,
    public val warning: Color,
    public val error: Color,
    public val info: Color,
    public val scrim: Color,
)

public object AnimeSpacing {
    public val xxs: Dp = 2.dp
    public val xs: Dp = 4.dp
    public val sm: Dp = 8.dp
    public val md: Dp = 12.dp
    public val lg: Dp = 16.dp
    public val xl: Dp = 20.dp
    public val xxl: Dp = 24.dp
    public val xxxl: Dp = 32.dp
    public val huge: Dp = 40.dp
    public val giant: Dp = 48.dp
    public val massive: Dp = 64.dp
}

public object AnimeRadius {
    public val chip: Dp = 10.dp
    public val control: Dp = 14.dp
    public val card: Dp = 18.dp
    public val panel: Dp = 24.dp
    public val round: Dp = 999.dp
}

public object AnimeSize {
    public val border: Dp = 1.dp
    public val touch: Dp = 48.dp
    public val iconSm: Dp = 18.dp
    public val icon: Dp = 24.dp
    public val iconLg: Dp = 32.dp
    public val posterCompactWidth: Dp = 80.dp
    public val posterCompactHeight: Dp = 112.dp
    public val posterWidth: Dp = 132.dp
    public val posterHeight: Dp = 198.dp
    public val contentMax: Dp = 1320.dp
    public val readingMax: Dp = 720.dp
}

@Suppress("ktlint:standard:property-naming")
public object AnimeMotion {
    public const val instant: Int = 0
    public const val fast: Int = 120
    public const val standard: Int = 220
    public const val emphasized: Int = 360
    public const val debounceSearch: Int = 300
}

public val AnimeTypography: Typography =
    Typography(
        displayLarge =
            TextStyle(
                fontSize = 34.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.Bold,
            ),
        headlineLarge =
            TextStyle(
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            ),
        titleLarge =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight(650),
            ),
        titleMedium =
            TextStyle(
                fontSize = 17.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelLarge =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        labelSmall =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
    )

internal fun Typography.withFontFamily(fontFamily: FontFamily): Typography =
    copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
    )

public val AnimeShapes: Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(AnimeRadius.chip),
        small = RoundedCornerShape(AnimeRadius.control),
        medium = RoundedCornerShape(AnimeRadius.card),
        large = RoundedCornerShape(AnimeRadius.panel),
        extraLarge = RoundedCornerShape(AnimeRadius.panel),
    )
