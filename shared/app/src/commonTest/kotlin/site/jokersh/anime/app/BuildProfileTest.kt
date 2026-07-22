package site.jokersh.anime.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BuildProfileTest {
    @Test
    fun demoProfileCreatesFixtureContainer() {
        val profile =
            BuildProfile(
                environment = Environment.Demo,
                dataModePolicy = DataModePolicy.FixtureOnly,
                apiBaseUrl = null,
                diagnosticsEnabled = true,
                searchPageSize = 5,
            )

        assertEquals(profile, createAppContainer(profile).profile)
    }

    @Test
    fun rejectsPageSizeOutsideContract() {
        assertFailsWith<IllegalArgumentException> {
            BuildProfile(
                environment = Environment.Demo,
                dataModePolicy = DataModePolicy.FixtureOnly,
                apiBaseUrl = null,
                diagnosticsEnabled = true,
                searchPageSize = 0,
            )
        }
    }

    @Test
    fun rejectsProdWithNonRemotePolicy() {
        assertFailsWith<IllegalArgumentException> {
            BuildProfile(
                environment = Environment.Prod,
                dataModePolicy = DataModePolicy.FixtureOnly,
                apiBaseUrl = "https://example.invalid",
                diagnosticsEnabled = false,
                searchPageSize = 20,
            )
        }
    }
}
