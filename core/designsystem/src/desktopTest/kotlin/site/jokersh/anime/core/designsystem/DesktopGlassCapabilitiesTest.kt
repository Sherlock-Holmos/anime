package site.jokersh.anime.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopGlassCapabilitiesTest {
    @Test
    fun remoteDesktopDefaultsToTranslucentGlass() {
        assertEquals(
            GlassTier.Translucent,
            desktopMaximumGlassTier(requestedTier = null, remoteSession = true),
        )
    }

    @Test
    fun explicitGlassTierOverridesSessionDefault() {
        assertEquals(
            GlassTier.Blur,
            desktopMaximumGlassTier(requestedTier = "blur", remoteSession = true),
        )
        assertEquals(
            GlassTier.Liquid,
            desktopMaximumGlassTier(requestedTier = "liquid", remoteSession = true),
        )
    }
}
