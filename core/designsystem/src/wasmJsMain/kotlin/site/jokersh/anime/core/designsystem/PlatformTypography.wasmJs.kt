package site.jokersh.anime.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import anime.core.designsystem.generated.resources.Res
import anime.core.designsystem.generated.resources.noto_sans_sc_subset
import org.jetbrains.compose.resources.Font

@Composable
internal actual fun platformAnimeTypography(): Typography {
    val webFontFamily =
        FontFamily(
            Font(Res.font.noto_sans_sc_subset, weight = FontWeight.Normal),
            Font(Res.font.noto_sans_sc_subset, weight = FontWeight.Medium),
            Font(Res.font.noto_sans_sc_subset, weight = FontWeight.SemiBold),
            Font(Res.font.noto_sans_sc_subset, weight = FontWeight.Bold),
        )
    return AnimeTypography.withFontFamily(webFontFamily)
}
