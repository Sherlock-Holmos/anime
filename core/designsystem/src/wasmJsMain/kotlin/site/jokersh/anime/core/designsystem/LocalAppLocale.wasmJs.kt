package site.jokersh.anime.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private val compositionLocale = staticCompositionLocalOf { Locale.current.toString() }

@Composable
public actual fun providePlatformAppLocale(value: String?): ProvidedValue<*> {
    updateCustomLocale(value?.replace('_', '-'))
    return compositionLocale.provides(value ?: Locale.current.toString())
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun updateCustomLocale(value: String?) {
    js(
        """
        if (window.__customLocale !== value) {
            window.__customLocale = value;
            window.dispatchEvent(new Event('languagechange'));
        }
        """,
    )
}
