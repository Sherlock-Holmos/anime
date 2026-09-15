package site.jokersh.anime.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

private var systemLocale: Locale? = null
private val compositionLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

@Composable
public actual fun providePlatformAppLocale(value: String?): ProvidedValue<*> {
    if (systemLocale == null) {
        systemLocale = Locale.getDefault()
    }
    val locale = value?.let(Locale::forLanguageTag) ?: systemLocale!!
    Locale.setDefault(locale)
    return compositionLocale.provides(locale.toLanguageTag())
}
