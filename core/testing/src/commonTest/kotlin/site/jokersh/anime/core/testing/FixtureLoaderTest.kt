package site.jokersh.anime.core.testing

import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.FieldId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FixtureLoaderTest {
    @Test
    fun `activate switches scenario atomically and reset restores default`() =
        runTest {
            val loader = JsonFixtureLoader(validBundle())

            assertEquals("happy", loader.currentScenario().value.id)
            loader.activate("offline-empty")
            assertEquals(FixtureNetwork.Offline, loader.currentScenario().value.network)
            loader.reset()
            assertEquals("happy", loader.currentScenario().value.id)
        }

    @Test
    fun `unknown scenario keeps current value and exposes domain validation`() =
        runTest {
            val loader = JsonFixtureLoader(validBundle())

            val error = assertFailsWith<FixtureActivationException> { loader.activate("missing") }

            assertEquals(FieldId.Identifier, error.error.field)
            assertEquals("happy", loader.currentScenario().value.id)
        }

    @Test
    fun `invalid schema fails before a scenario becomes observable`() {
        val bundle =
            validBundle().copy(
                manifest = validBundle().manifest.replace(FIXTURE_SCHEMA_V1, "anime.fixture/v2"),
            )

        assertFailsWith<FixtureLoadException> { JsonFixtureLoader(bundle) }
    }
}

private fun validBundle(): FixtureBundle =
    FixtureBundle(
        manifest =
            """
            {
              "schema": "anime.fixture/v1",
              "clock": "2026-07-19T08:00:00Z",
              "seed": 20260719,
              "defaultScenario": "happy",
              "files": {}
            }
            """.trimIndent(),
        scenarios =
            """
            {
              "schema": "anime.fixture/v1",
              "scenarios": [
                {"id":"happy","readDelayMs":0,"writeDelayMs":0,"network":"online","failurePlan":[]},
                {"id":"offline-empty","readDelayMs":0,"writeDelayMs":0,"network":"offline","failurePlan":[]}
              ]
            }
            """.trimIndent(),
    )
