package site.jokersh.anime.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnimeNavigationTest {
    @Test
    fun dragPositionSnapsToNearestTab() {
        assertEquals(0, resolveDraggedTab(0.49f, 4))
        assertEquals(1, resolveDraggedTab(0.5f, 4))
        assertEquals(2, resolveDraggedTab(2.4f, 4))
        assertEquals(3, resolveDraggedTab(2.8f, 4))
    }

    @Test
    fun dragPositionIsClampedToTabRange() {
        assertEquals(0, resolveDraggedTab(-10f, 4))
        assertEquals(3, resolveDraggedTab(10f, 4))
    }

    @Test
    fun emptyTabBarCannotResolveTarget() {
        assertFailsWith<IllegalArgumentException> {
            resolveDraggedTab(0f, 0)
        }
    }
}
