package site.jokersh.anime.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf

// The native iOS shell owns the user-facing locale through SwiftUI's Locale
// environment. Keep the Compose bridge deterministic for any shared Compose
// surface that is embedded later without mutating the process-wide language.
private val compositionLocale = staticCompositionLocalOf { "zh-Hans" }

@Composable
public actual fun providePlatformAppLocale(value: String?): ProvidedValue<*> =
        compositionLocale.provides(value ?: compositionLocale.current)
