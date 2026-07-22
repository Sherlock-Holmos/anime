package site.jokersh.anime.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.AppSettings
import site.jokersh.anime.core.model.GlassPreference
import site.jokersh.anime.core.model.ReduceMotionPreference
import site.jokersh.anime.core.model.ThemePreference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val defaultSettings =
    AppSettings(
        theme = ThemePreference.System,
        dynamicColor = true,
        glass = GlassPreference.Auto,
        reduceMotion = ReduceMotionPreference.FollowSystem,
        diagnosticsConsent = false,
    )

abstract class SettingsRepositoryContract {
    protected abstract fun createRepository(): SettingsRepository

    @Test
    fun `CT-SET-001 updates are immediately observable and preserve other fields`() =
        runTest {
            val repository = createRepository()

            repository.setTheme(ThemePreference.Dark)
            repository.setGlass(GlassPreference.Off)

            val settings = repository.observeSettings().first()
            assertEquals(ThemePreference.Dark, settings.theme)
            assertEquals(GlassPreference.Off, settings.glass)
            assertTrue(settings.dynamicColor)
        }

    @Test
    fun `CT-SET-004 reset is atomic and restores defaults`() =
        runTest {
            val repository = createRepository()
            repository.setDiagnosticsConsent(true)
            repository.setDynamicColor(false)

            assertTrue(repository.resetDemo().isSuccess)

            val settings = repository.observeSettings().first()
            assertFalse(settings.diagnosticsConsent)
            assertTrue(settings.dynamicColor)
            assertEquals(ThemePreference.System, settings.theme)
        }
}

class FixtureSettingsRepositoryContractTest : SettingsRepositoryContract() {
    override fun createRepository(): SettingsRepository = InMemorySettingsRepository()
}

private class InMemorySettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(defaultSettings)

    override fun observeSettings(): Flow<AppSettings> = state

    override suspend fun setTheme(value: ThemePreference) {
        state.value = state.value.copy(theme = value)
    }

    override suspend fun setDynamicColor(value: Boolean) {
        state.value = state.value.copy(dynamicColor = value)
    }

    override suspend fun setGlass(value: GlassPreference) {
        state.value = state.value.copy(glass = value)
    }

    override suspend fun setReduceMotion(value: ReduceMotionPreference) {
        state.value = state.value.copy(reduceMotion = value)
    }

    override suspend fun setDiagnosticsConsent(value: Boolean) {
        state.value = state.value.copy(diagnosticsConsent = value)
    }

    override suspend fun clearPublicCache(): Result<Unit> = Result.success(Unit)

    override suspend fun resetDemo(): Result<Unit> {
        state.value = defaultSettings
        return Result.success(Unit)
    }
}
