package site.jokersh.anime.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import site.jokersh.anime.core.model.AppSettings
import site.jokersh.anime.core.model.GlassPreference
import site.jokersh.anime.core.model.ReduceMotionPreference
import site.jokersh.anime.core.model.ThemePreference

public interface SettingsStore {
    public fun read(key: String): String?

    public fun write(
        key: String,
        value: String,
    )

    public fun remove(key: String)
}

public class InMemorySettingsStore : SettingsStore {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}

public class PersistentSettingsRepository(
    private val store: SettingsStore,
    private val clearPublicCacheAction: suspend () -> Result<Unit> = { Result.success(Unit) },
) : SettingsRepository {
    private val state = MutableStateFlow(load())

    override fun observeSettings(): Flow<AppSettings> = state.asStateFlow()

    override suspend fun setTheme(value: ThemePreference): Unit = update(state.value.copy(theme = value))

    override suspend fun setDynamicColor(value: Boolean): Unit = update(state.value.copy(dynamicColor = value))

    override suspend fun setGlass(value: GlassPreference): Unit = update(state.value.copy(glass = value))

    override suspend fun setReduceMotion(value: ReduceMotionPreference): Unit =
        update(state.value.copy(reduceMotion = value))

    override suspend fun setDiagnosticsConsent(value: Boolean): Unit =
        update(state.value.copy(diagnosticsConsent = value))

    override suspend fun clearPublicCache(): Result<Unit> = clearPublicCacheAction()

    override suspend fun resetDemo(): Result<Unit> =
        runCatching {
            KEYS.forEach(store::remove)
            state.value = DEFAULT_SETTINGS
        }

    private fun update(settings: AppSettings) {
        store.write(THEME, settings.theme.name)
        store.write(DYNAMIC_COLOR, settings.dynamicColor.toString())
        store.write(GLASS, settings.glass.name)
        store.write(REDUCE_MOTION, settings.reduceMotion.name)
        store.write(DIAGNOSTICS, settings.diagnosticsConsent.toString())
        state.value = settings
    }

    private fun load(): AppSettings =
        AppSettings(
            theme = store.read(THEME).enumOrDefault(ThemePreference.System),
            dynamicColor = store.read(DYNAMIC_COLOR)?.toBooleanStrictOrNull() ?: true,
            glass = store.read(GLASS).enumOrDefault(GlassPreference.Auto),
            reduceMotion = store.read(REDUCE_MOTION).enumOrDefault(ReduceMotionPreference.FollowSystem),
            diagnosticsConsent = store.read(DIAGNOSTICS)?.toBooleanStrictOrNull() ?: false,
        )

    private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T =
        this?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

    private companion object {
        const val THEME = "theme"
        const val DYNAMIC_COLOR = "dynamic_color"
        const val GLASS = "glass"
        const val REDUCE_MOTION = "reduce_motion"
        const val DIAGNOSTICS = "diagnostics_consent"
        val KEYS = listOf(THEME, DYNAMIC_COLOR, GLASS, REDUCE_MOTION, DIAGNOSTICS)
        val DEFAULT_SETTINGS =
            AppSettings(
                theme = ThemePreference.System,
                dynamicColor = true,
                glass = GlassPreference.Auto,
                reduceMotion = ReduceMotionPreference.FollowSystem,
                diagnosticsConsent = false,
            )
    }
}
