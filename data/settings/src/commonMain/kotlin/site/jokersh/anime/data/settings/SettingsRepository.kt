package site.jokersh.anime.data.settings

import kotlinx.coroutines.flow.Flow
import site.jokersh.anime.core.model.AppSettings
import site.jokersh.anime.core.model.GlassPreference
import site.jokersh.anime.core.model.ReduceMotionPreference
import site.jokersh.anime.core.model.ThemePreference

public interface SettingsRepository {
    public fun observeSettings(): Flow<AppSettings>

    public suspend fun setTheme(value: ThemePreference)

    public suspend fun setDynamicColor(value: Boolean)

    public suspend fun setGlass(value: GlassPreference)

    public suspend fun setReduceMotion(value: ReduceMotionPreference)

    public suspend fun setDiagnosticsConsent(value: Boolean)

    public suspend fun clearPublicCache(): Result<Unit>

    public suspend fun resetDemo(): Result<Unit>
}
