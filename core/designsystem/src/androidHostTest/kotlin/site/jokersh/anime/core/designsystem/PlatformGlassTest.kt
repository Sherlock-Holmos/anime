package site.jokersh.anime.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformGlassTest {
    @Test
    fun android26To30UsesTranslucentTier() {
        assertEquals(GlassTier.Translucent, maximumGlassTierForAndroidSdk(26))
        assertEquals(GlassTier.Translucent, maximumGlassTierForAndroidSdk(30))
    }

    @Test
    fun android31To32UsesBlurTier() {
        assertEquals(GlassTier.Blur, maximumGlassTierForAndroidSdk(31))
        assertEquals(GlassTier.Blur, maximumGlassTierForAndroidSdk(32))
    }

    @Test
    fun android33AndAboveUsesLiquidTier() {
        assertEquals(GlassTier.Liquid, maximumGlassTierForAndroidSdk(33))
        assertEquals(GlassTier.Liquid, maximumGlassTierForAndroidSdk(36))
    }
}
