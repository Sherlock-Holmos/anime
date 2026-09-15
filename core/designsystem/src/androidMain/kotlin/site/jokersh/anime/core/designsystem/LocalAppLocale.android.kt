package site.jokersh.anime.core.designsystem

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

private var systemLocale: Locale? = null

@Composable
public actual fun providePlatformAppLocale(value: String?): ProvidedValue<*> {
    val configuration = Configuration(LocalConfiguration.current)
    if (systemLocale == null) {
        systemLocale = Locale.getDefault()
    }
    val locale = value?.let(Locale::forLanguageTag) ?: systemLocale!!
    Locale.setDefault(locale)
    configuration.setLocale(locale)
    return LocalConfiguration.provides(configuration)
}
