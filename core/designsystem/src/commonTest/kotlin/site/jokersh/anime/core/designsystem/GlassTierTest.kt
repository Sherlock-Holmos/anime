package site.jokersh.anime.core.designsystem

import kotlin.test.Test
import kotlin.test.assertEquals

class GlassTierTest {
    @Test
    fun preferredTierIsLimitedByDeviceCapability() {
        assertEquals(
            GlassTier.Blur,
            resolveGlassTier(
                preferred = GlassTier.Liquid,
                capabilities = GlassCapabilities(maximumTier = GlassTier.Blur),
            ),
        )
    }

    @Test
    fun reducedTransparencyAlwaysUsesOpaqueSurface() {
        assertEquals(
            GlassTier.None,
            resolveGlassTier(
                preferred = GlassTier.Liquid,
                capabilities =
                    GlassCapabilities(
                        maximumTier = GlassTier.Liquid,
                        reduceTransparency = true,
                    ),
            ),
        )
    }

    @Test
    fun slowFramesDegradeToTranslucent() {
        assertEquals(
            GlassTier.Translucent,
            resolveGlassTier(
                preferred = GlassTier.Liquid,
                capabilities =
                    GlassCapabilities(
                        maximumTier = GlassTier.Liquid,
                        slowFramesDetected = true,
                    ),
            ),
        )
    }
}
