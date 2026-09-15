package site.jokersh.anime.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Locale bridge used by Compose Multiplatform resources.
 *
 * The value is a BCP-47 tag (for example `en`, `ja`, `zh-Hans`, or `zh-TW`).
 * A null value means that the platform locale should be used.
 */
private val LocalAppLocaleOverride = staticCompositionLocalOf<String?> { null }

@Composable
public expect fun providePlatformAppLocale(value: String?): ProvidedValue<*>

@Composable
public fun AppLocale(
    locale: String?,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppLocaleOverride provides locale,
        providePlatformAppLocale(locale),
    ) {
        // Resource selection is part of the composition identity. Re-keying here
        // prevents a previous language's string snapshots from surviving a switch.
        key(locale) {
            content()
        }
    }
}
