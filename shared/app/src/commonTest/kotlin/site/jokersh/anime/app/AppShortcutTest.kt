package site.jokersh.anime.app

import androidx.compose.ui.input.key.Key
import site.jokersh.anime.core.navigation.AppRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppShortcutTest {
    @Test
    fun rootShortcutsUseControlDigitsAndControlK() {
        assertEquals(AppRoot.Discover, shortcutRoot(Key.One, ctrlPressed = true))
        assertEquals(AppRoot.Search, shortcutRoot(Key.Two, ctrlPressed = true))
        assertEquals(AppRoot.Search, shortcutRoot(Key.K, ctrlPressed = true))
        assertEquals(AppRoot.Collection, shortcutRoot(Key.Three, ctrlPressed = true))
        assertEquals(AppRoot.Profile, shortcutRoot(Key.Four, ctrlPressed = true))
    }

    @Test
    fun shortcutDoesNotConsumeUnmodifiedKeys() {
        assertNull(shortcutRoot(Key.K, ctrlPressed = false))
        assertNull(shortcutRoot(Key.Five, ctrlPressed = true))
    }
}
