package site.jokersh.anime.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.FieldId
import site.jokersh.anime.core.model.ValidationReason
import kotlin.time.Instant

public const val FIXTURE_SCHEMA_V1: String = "anime.fixture/v1"

public data class FixtureBundle(
    public val manifest: String,
    public val scenarios: String,
    public val files: Map<String, String> = emptyMap(),
)

public data class FixtureManifest(
    public val schema: String,
    public val clock: Instant,
    public val seed: Long,
    public val defaultScenario: String,
    public val files: Map<String, String>,
)

public data class FixtureScenario(
    public val id: String,
    public val readDelayMs: Long,
    public val writeDelayMs: Long,
    public val network: FixtureNetwork,
    public val failurePlan: List<String>,
)

public enum class FixtureNetwork { Online, Offline }

public interface FixtureLoader {
    public val manifest: FixtureManifest

    public suspend fun activate(scenarioId: String)

    public suspend fun reset()

    public fun currentScenario(): StateFlow<FixtureScenario>
}

public class FixtureActivationException(
    public val error: AppError.Validation,
) : IllegalArgumentException("Unknown Fixture scenario")

public class FixtureLoadException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

public class JsonFixtureLoader(
    bundle: FixtureBundle,
    json: Json = Json { ignoreUnknownKeys = false },
) : FixtureLoader {
    private val scenarios: Map<String, FixtureScenario>
    private val scenarioState: MutableStateFlow<FixtureScenario>

    override val manifest: FixtureManifest

    init {
        try {
            val manifestDto = json.parseManifest(bundle.manifest)
            val scenarioFile = json.parseScenarios(bundle.scenarios)
            require(manifestDto.schema == FIXTURE_SCHEMA_V1)
            require(scenarioFile.schema == FIXTURE_SCHEMA_V1)
            require(manifestDto.files.keys == bundle.files.keys || bundle.files.isEmpty())

            manifest = manifestDto.toDomain()
            scenarios = scenarioFile.scenarios.map(FixtureScenarioDto::toDomain).associateBy(FixtureScenario::id)
            require(scenarios.size == scenarioFile.scenarios.size)
            scenarioState = MutableStateFlow(requireNotNull(scenarios[manifest.defaultScenario]))
        } catch (error: FixtureLoadException) {
            throw error
        } catch (error: Exception) {
            throw FixtureLoadException("Fixture manifest or scenarios are invalid", error)
        }
    }

    override suspend fun activate(scenarioId: String) {
        val scenario =
            scenarios[scenarioId]
                ?: throw FixtureActivationException(
                    AppError.Validation(FieldId.Identifier, ValidationReason.Unsupported),
                )
        scenarioState.value = scenario
    }

    override suspend fun reset() {
        scenarioState.value = requireNotNull(scenarios[manifest.defaultScenario])
    }

    override fun currentScenario(): StateFlow<FixtureScenario> = scenarioState.asStateFlow()
}

private data class FixtureManifestDto(
    val schema: String,
    val clock: String,
    val seed: Long,
    val defaultScenario: String,
    val files: Map<String, String>,
) {
    fun toDomain(): FixtureManifest =
        FixtureManifest(
            schema = schema,
            clock = Instant.parse(clock),
            seed = seed,
            defaultScenario = defaultScenario,
            files = files,
        )
}

private data class FixtureScenarioFileDto(
    val schema: String,
    val scenarios: List<FixtureScenarioDto>,
)

private data class FixtureScenarioDto(
    val id: String,
    val readDelayMs: Long,
    val writeDelayMs: Long,
    val network: String,
    val failurePlan: List<String>,
) {
    fun toDomain(): FixtureScenario =
        FixtureScenario(
            id = id,
            readDelayMs = readDelayMs,
            writeDelayMs = writeDelayMs,
            network =
                when (network) {
                    "online" -> FixtureNetwork.Online
                    "offline" -> FixtureNetwork.Offline
                    else -> throw FixtureLoadException("Unknown Fixture network: $network")
                },
            failurePlan = failurePlan,
        ).also {
            require(readDelayMs >= 0 && writeDelayMs >= 0)
            require(id.isNotBlank())
        }
}

private fun Json.parseManifest(source: String): FixtureManifestDto {
    val root = parseToJsonElement(source).jsonObject
    return FixtureManifestDto(
        schema = root.string("schema"),
        clock = root.string("clock"),
        seed = root.value("seed").jsonPrimitive.long,
        defaultScenario = root.string("defaultScenario"),
        files = root.value("files").jsonObject.mapValues { it.value.jsonPrimitive.content },
    )
}

private fun Json.parseScenarios(source: String): FixtureScenarioFileDto {
    val root = parseToJsonElement(source).jsonObject
    return FixtureScenarioFileDto(
        schema = root.string("schema"),
        scenarios =
            root.value("scenarios").jsonArray.map { element ->
                val scenario = element.jsonObject
                FixtureScenarioDto(
                    id = scenario.string("id"),
                    readDelayMs = scenario.value("readDelayMs").jsonPrimitive.long,
                    writeDelayMs = scenario.value("writeDelayMs").jsonPrimitive.long,
                    network = scenario.string("network"),
                    failurePlan = scenario.value("failurePlan").jsonArray.map { it.jsonPrimitive.content },
                )
            },
    )
}

private fun JsonObject.value(name: String) = requireNotNull(get(name)) { "Missing Fixture field: $name" }

private fun JsonObject.string(name: String): String = value(name).jsonPrimitive.content
