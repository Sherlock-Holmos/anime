package site.jokersh.anime.data.catalog

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.BangumiRating
import site.jokersh.anime.core.model.CharacterCredit
import site.jokersh.anime.core.model.CharacterId
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.DiscoverySection
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.EpisodeAirStatus
import site.jokersh.anime.core.model.EpisodeId
import site.jokersh.anime.core.model.EpisodeType
import site.jokersh.anime.core.model.Freshness
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.Page
import site.jokersh.anime.core.model.PersonCredit
import site.jokersh.anime.core.model.PersonId
import site.jokersh.anime.core.model.Provider
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.RelationKind
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SearchDiscovery
import site.jokersh.anime.core.model.SearchHistoryItem
import site.jokersh.anime.core.model.SearchRequest
import site.jokersh.anime.core.model.SearchSuggestion
import site.jokersh.anime.core.model.SubjectCredits
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectRelation
import site.jokersh.anime.core.model.SubjectSection
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.core.model.Tag
import kotlin.time.Clock
import kotlin.time.Instant

public class RemoteCatalogRepository(
    private val client: HttpClient,
    apiBaseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val cacheStore: CatalogCacheStore = InMemoryCatalogCacheStore(),
) : CatalogRepository {
    private val baseUrl = apiBaseUrl.trimEnd('/')
    private val discovery = MutableStateFlow(ResourceState<DiscoveryFeed>(null, null, false, null))
    private val subjects = mutableMapOf<Long, MutableStateFlow<ResourceState<SubjectDetail>>>()
    private val episodes = mutableMapOf<Long, MutableStateFlow<ResourceState<List<Episode>>>>()
    private val credits = mutableMapOf<Long, MutableStateFlow<ResourceState<SubjectCredits>>>()
    private val relations = mutableMapOf<Long, MutableStateFlow<ResourceState<List<SubjectRelation>>>>()
    private val refreshMutex = Mutex()

    init {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "apiBaseUrl must use HTTP or HTTPS"
        }
    }

    override fun observeDiscovery(): Flow<ResourceState<DiscoveryFeed>> = discovery.asStateFlow()

    override fun observeSubject(id: SubjectId): Flow<ResourceState<SubjectDetail>> = subjectState(id).asStateFlow()

    override fun observeEpisodes(id: SubjectId): Flow<ResourceState<List<Episode>>> = episodeState(id).asStateFlow()

    override fun observeCredits(id: SubjectId): Flow<ResourceState<SubjectCredits>> = creditState(id).asStateFlow()

    override fun observeRelations(id: SubjectId): Flow<ResourceState<List<SubjectRelation>>> =
        relationState(id).asStateFlow()

    override suspend fun refreshDiscovery(policy: RefreshPolicy): Result<Unit> =
        refreshMutex.withLock {
            val current = discovery.value
            if (!shouldRefresh(policy, current.value != null, current.freshness)) return@withLock Result.success(Unit)
            discovery.value = current.copy(refreshing = true, error = null)
            runCatching {
                val response = client.get("$baseUrl/api/v1/home")
                val text = response.bodyAsText()
                check(response.status.value in 200..299) { "Catalog request failed with ${response.status.value}" }
                val body = json.decodeFromString<RemoteHome>(text)
                cacheStore.write(CACHE_DISCOVERY, text)
                val generatedAt = Instant.fromEpochSeconds(body.generatedAt)
                discovery.value =
                    ResourceState(
                        value =
                            DiscoveryFeed(
                                body.sections.map { section ->
                                    DiscoverySection(
                                        id = section.id,
                                        title = section.title,
                                        subjects = section.items.map { it.toSummary(baseUrl) },
                                    )
                                },
                                generatedAt,
                            ),
                        freshness = Freshness(FreshnessKind.Fresh, generatedAt),
                        refreshing = false,
                        error = null,
                    )
            }.onFailure {
                discovery.value =
                    cachedDiscovery()
                        ?: current.copy(refreshing = false, error = AppError.Upstream(Provider.Bangumi, null))
            }
        }

    override suspend fun refreshSubject(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit> {
        val state = subjectState(id)
        val current = state.value
        if (!shouldRefresh(policy, current.value != null, current.freshness)) return Result.success(Unit)
        state.value = current.copy(refreshing = true, error = null)
        return runCatching {
            val response = client.get("$baseUrl/api/v1/subjects/${id.value}")
            val text = response.bodyAsText()
            check(response.status.value in 200..299) { "Subject request failed with ${response.status.value}" }
            val remote = json.decodeFromString<RemoteSubject>(text)
            check(remote.id.toLongOrNull() == id.value) { "Subject response ID does not match the route" }
            cacheStore.write(subjectCacheKey(id), text)
            val updatedAt = Instant.fromEpochSeconds(remote.dataUpdatedAt)
            state.value =
                ResourceState(
                    value = remote.toDetail(baseUrl),
                    freshness =
                        Freshness(
                            if (remote.dataFreshness == "stale") FreshnessKind.Stale else FreshnessKind.Fresh,
                            updatedAt,
                        ),
                    refreshing = false,
                    error = null,
                )
        }.onFailure {
            state.value =
                cachedSubject(id) ?: current.copy(refreshing = false, error = AppError.Upstream(Provider.Bangumi, null))
        }
    }

    override suspend fun refreshSection(
        id: SubjectId,
        section: SubjectSection,
        policy: RefreshPolicy,
    ): Result<Unit> =
        when (section) {
            SubjectSection.Episodes -> refreshEpisodes(id, policy)
            SubjectSection.Credits -> refreshCredits(id, policy)
            SubjectSection.Relations -> refreshRelations(id, policy)
        }

    private suspend fun refreshEpisodes(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit> {
        val state = episodeState(id)
        if (!shouldRefresh(policy, state.value.value != null, state.value.freshness)) return Result.success(Unit)
        val current = state.value
        state.value = current.copy(refreshing = true, error = null)
        return runCatching {
            val response = client.get("$baseUrl/api/v1/subjects/${id.value}/episodes")
            check(response.status.value in 200..299) { "Episode request failed with ${response.status.value}" }
            val text = response.bodyAsText()
            val body = json.decodeFromString<RemoteEpisodePage>(text)
            cacheStore.write(sectionCacheKey(id, "episodes"), text)
            state.value = ResourceState(body.data.mapNotNull { it.toModel(id) }, currentFreshness(), false, null)
        }.onFailure {
            state.value =
                cachedEpisodes(id)
                    ?: current.copy(refreshing = false, error = AppError.Upstream(Provider.Bangumi, null))
        }
    }

    private suspend fun refreshCredits(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit> {
        val state = creditState(id)
        if (!shouldRefresh(policy, state.value.value != null, state.value.freshness)) return Result.success(Unit)
        val current = state.value
        state.value = current.copy(refreshing = true, error = null)
        return runCatching {
            val response = client.get("$baseUrl/api/v1/subjects/${id.value}/credits")
            check(response.status.value in 200..299) { "Credit request failed with ${response.status.value}" }
            val text = response.bodyAsText()
            val body = json.decodeFromString<RemoteCredits>(text)
            cacheStore.write(sectionCacheKey(id, "credits"), text)
            state.value = ResourceState(body.toModel(), freshness(body.generatedAt), false, null)
        }.onFailure {
            state.value =
                cachedCredits(id) ?: current.copy(refreshing = false, error = AppError.Upstream(Provider.Bangumi, null))
        }
    }

    private suspend fun refreshRelations(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit> {
        val state = relationState(id)
        if (!shouldRefresh(policy, state.value.value != null, state.value.freshness)) return Result.success(Unit)
        val current = state.value
        state.value = current.copy(refreshing = true, error = null)
        return runCatching {
            val response = client.get("$baseUrl/api/v1/subjects/${id.value}/relations")
            check(response.status.value in 200..299) { "Relation request failed with ${response.status.value}" }
            val text = response.bodyAsText()
            val body = json.decodeFromString<List<RemoteRelation>>(text)
            cacheStore.write(sectionCacheKey(id, "relations"), text)
            state.value = ResourceState(body.mapNotNull { it.toModel() }, currentFreshness(), false, null)
        }.onFailure {
            state.value =
                cachedRelations(id)
                    ?: current.copy(refreshing = false, error = AppError.Upstream(Provider.Bangumi, null))
        }
    }

    private fun cachedDiscovery(): ResourceState<DiscoveryFeed>? =
        cacheStore.read(CACHE_DISCOVERY)?.let { text ->
            runCatching {
                val body = json.decodeFromString<RemoteHome>(text)
                val generatedAt = Instant.fromEpochSeconds(body.generatedAt)
                ResourceState(
                    DiscoveryFeed(
                        body.sections.map { section ->
                            DiscoverySection(section.id, section.title, section.items.map { it.toSummary(baseUrl) })
                        },
                        generatedAt,
                    ),
                    Freshness(FreshnessKind.OfflineCache, generatedAt),
                    false,
                    AppError.Offline,
                )
            }.getOrNull()
        }

    private fun cachedSubject(id: SubjectId): ResourceState<SubjectDetail>? =
        cacheStore.read(subjectCacheKey(id))?.let { text ->
            runCatching {
                val body = json.decodeFromString<RemoteSubject>(text)
                val updatedAt = Instant.fromEpochSeconds(body.dataUpdatedAt)
                ResourceState(
                    body.toDetail(baseUrl),
                    Freshness(FreshnessKind.OfflineCache, updatedAt),
                    false,
                    AppError.Offline,
                )
            }.getOrNull()
        }

    private fun cachedEpisodes(id: SubjectId): ResourceState<List<Episode>>? =
        cacheStore.read(sectionCacheKey(id, "episodes"))?.let { text ->
            runCatching {
                ResourceState(
                    json.decodeFromString<RemoteEpisodePage>(text).data.mapNotNull { it.toModel(id) },
                    offlineFreshness(),
                    false,
                    AppError.Offline,
                )
            }.getOrNull()
        }

    private fun cachedCredits(id: SubjectId): ResourceState<SubjectCredits>? =
        cacheStore.read(sectionCacheKey(id, "credits"))?.let { text ->
            runCatching {
                val body = json.decodeFromString<RemoteCredits>(text)
                ResourceState(
                    body.toModel(),
                    Freshness(FreshnessKind.OfflineCache, Instant.fromEpochSeconds(body.generatedAt)),
                    false,
                    AppError.Offline,
                )
            }.getOrNull()
        }

    private fun cachedRelations(id: SubjectId): ResourceState<List<SubjectRelation>>? =
        cacheStore.read(sectionCacheKey(id, "relations"))?.let { text ->
            runCatching {
                ResourceState(
                    json.decodeFromString<List<RemoteRelation>>(text).mapNotNull { it.toModel() },
                    offlineFreshness(),
                    false,
                    AppError.Offline,
                )
            }.getOrNull()
        }

    private fun subjectState(id: SubjectId): MutableStateFlow<ResourceState<SubjectDetail>> =
        subjects.getOrPut(id.value) { MutableStateFlow(ResourceState(null, null, false, null)) }

    private fun episodeState(id: SubjectId) =
        episodes.getOrPut(id.value) { MutableStateFlow(ResourceState(null, null, false, null)) }

    private fun creditState(id: SubjectId) =
        credits.getOrPut(id.value) { MutableStateFlow(ResourceState(null, null, false, null)) }

    private fun relationState(id: SubjectId) =
        relations.getOrPut(id.value) { MutableStateFlow(ResourceState(null, null, false, null)) }
}

public interface CatalogCacheStore {
    public fun read(key: String): String?

    public fun write(
        key: String,
        value: String,
    )
}

public class InMemoryCatalogCacheStore : CatalogCacheStore {
    private val values = mutableMapOf<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(
        key: String,
        value: String,
    ) {
        values[key] = value
    }
}

private const val CACHE_DISCOVERY = "discovery"

private fun subjectCacheKey(id: SubjectId): String = "subject-${id.value}"

private fun sectionCacheKey(
    id: SubjectId,
    section: String,
): String = "subject-${id.value}-$section"

private fun offlineFreshness(): Freshness = Freshness(FreshnessKind.OfflineCache, Clock.System.now())

public class RemoteSearchRepository(
    private val client: HttpClient,
    apiBaseUrl: String,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val tokenProvider: () -> String? = { null },
) : SearchRepository {
    private val baseUrl = apiBaseUrl.trimEnd('/')
    private val history = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    private val historyMutex = Mutex()

    init {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "apiBaseUrl must use HTTP or HTTPS"
        }
    }

    override fun observeHistory(): Flow<List<SearchHistoryItem>> = history.asStateFlow()

    override suspend fun discovery(): Result<SearchDiscovery> =
        runCatching {
            val response =
                client.get("$baseUrl/api/v1/search/discovery") {
                    tokenProvider()?.let { header("Authorization", "Bearer $it") }
                }
            val body = json.decodeFromString<RemoteSearchDiscovery>(response.bodyAsText())
            check(response.status.value in 200..299) { "Search discovery failed with ${response.status.value}" }
            SearchDiscovery(
                trending = body.trending,
                recommendations = body.recommendations.map { it.toSummary(baseUrl) },
                personalized = body.personalized,
            )
        }

    override fun observeSuggestions(query: String): Flow<ResourceState<List<SearchSuggestion>>> {
        val normalized = query.trim()
        val suggestions =
            history.value
                .asSequence()
                .map(SearchHistoryItem::query)
                .filter { normalized.isNotEmpty() && it.contains(normalized, ignoreCase = true) }
                .distinct()
                .take(5)
                .map(::SearchSuggestion)
                .toList()
        return MutableStateFlow(
            ResourceState(
                value = suggestions,
                freshness = Freshness(FreshnessKind.Fresh, Clock.System.now()),
                refreshing = false,
                error = null,
            ),
        )
    }

    override suspend fun search(request: SearchRequest): Result<Page<SubjectSummary>> =
        runCatching {
            val offset =
                request.cursor
                    ?.value
                    ?.toLongOrNull()
                    ?.coerceAtLeast(0) ?: 0
            val response =
                client.get("$baseUrl/api/v1/search/subjects") {
                    tokenProvider()?.let { header("Authorization", "Bearer $it") }
                    parameter("q", request.query)
                    parameter("limit", request.pageSize)
                    parameter("offset", offset)
                    if (request.types.isNotEmpty()) {
                        parameter("types", request.types.joinToString(",") { it.name.lowercase() })
                    }
                    if (request.airing.isNotEmpty()) {
                        parameter("airing", request.airing.joinToString(",") { it.name.lowercase() })
                    }
                    request.years?.let {
                        parameter("year_from", it.first)
                        parameter("year_to", it.last)
                    }
                    parameter("sort", request.sort.name.lowercase())
                }
            val text = response.bodyAsText()
            check(response.status.value in 200..299) { "Search request failed with ${response.status.value}" }
            val body = json.decodeFromString<RemoteSubjectList>(text)
            val items = body.items.map { it.toSummary(baseUrl) }
            val nextCursor = body.nextOffset?.let { Cursor(it.toString()) }
            Page(items = items, nextCursor = nextCursor, hasMore = nextCursor != null)
        }

    override suspend fun saveHistory(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        historyMutex.withLock {
            history.value =
                (
                    listOf(
                        SearchHistoryItem(
                            id = "history-${normalized.hashCode()}",
                            query = normalized,
                            usedAt = Clock.System.now(),
                        ),
                    ) + history.value.filterNot { it.query == normalized }
                ).take(10)
        }
    }

    override suspend fun deleteHistory(id: String) {
        historyMutex.withLock { history.value = history.value.filterNot { it.id == id } }
    }

    override suspend fun clearHistory() {
        historyMutex.withLock { history.value = emptyList() }
    }
}

private fun shouldRefresh(
    policy: RefreshPolicy,
    hasValue: Boolean,
    freshness: Freshness?,
): Boolean =
    when (policy) {
        RefreshPolicy.IfMissing -> !hasValue
        RefreshPolicy.IfStale -> !hasValue || freshness?.kind != FreshnessKind.Fresh
        RefreshPolicy.Force -> true
    }

private fun nowFreshness(): Freshness = Freshness(FreshnessKind.Fresh, Instant.fromEpochSeconds(0))

private fun currentFreshness(): Freshness = freshness(Clock.System.now().epochSeconds)

private fun freshness(epochSeconds: Long): Freshness =
    Freshness(FreshnessKind.Fresh, Instant.fromEpochSeconds(epochSeconds))

private fun <T> emptyListResource(): ResourceState<List<T>> = ResourceState(emptyList(), nowFreshness(), false, null)

@Serializable
private data class RemoteSubjectList(
    val items: List<RemoteSubject>,
    @SerialName("generated_at") val generatedAt: Long,
    @SerialName("next_offset") val nextOffset: Long? = null,
)

@Serializable
private data class RemoteHome(
    val sections: List<RemoteHomeSection>,
    @SerialName("generated_at") val generatedAt: Long,
)

@Serializable
private data class RemoteSearchDiscovery(
    val trending: List<String>,
    val recommendations: List<RemoteSubject>,
    val personalized: Boolean,
)

@Serializable
private data class RemoteHomeSection(
    val id: String,
    val title: String,
    val items: List<RemoteSubject>,
)

@Serializable
private data class RemoteSubject(
    val id: String,
    val title: String,
    @SerialName("original_title") val originalTitle: String?,
    val aliases: List<String> = emptyList(),
    val summary: String = "",
    @SerialName("poster_url") val posterUrl: String?,
    val year: Int?,
    @SerialName("subject_type") val subjectType: String,
    @SerialName("airing_status") val airingStatus: String,
    @SerialName("bangumi_rating") val bangumiRating: RemoteRating,
    @SerialName("air_date") val airDate: String?,
    @SerialName("total_episodes") val totalEpisodes: Int?,
    val tags: List<String> = emptyList(),
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("data_updated_at") val dataUpdatedAt: Long,
    @SerialName("data_freshness") val dataFreshness: String,
) {
    fun toSummary(baseUrl: String): SubjectSummary {
        val subjectId = SubjectId(id.toLong())
        return SubjectSummary(
            id = subjectId,
            title = title.trim(),
            originalTitle = originalTitle?.trim()?.takeIf(String::isNotEmpty),
            aliases = aliases.map(String::trim).filter(String::isNotEmpty).distinct(),
            poster = posterUrl?.let { ImageRef.Remote(resolveUrl(baseUrl, it), "bangumi-poster-${subjectId.value}") },
            year = year,
            type = subjectType.toSubjectType(),
            airingStatus = airingStatus.toAiringStatus(),
            rating = bangumiRating.toModel(dataUpdatedAt),
            collection = null,
        )
    }

    fun toDetail(baseUrl: String): SubjectDetail =
        SubjectDetail(
            summary = toSummary(baseUrl),
            summaryText = summary.takeIf(String::isNotBlank),
            airDate = airDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            endDate = null,
            totalEpisodes = totalEpisodes,
            tags = tags.distinct().take(100).mapIndexed { index, name -> Tag(name, index) },
            backdrop = null,
            sourceUrl = sourceUrl,
            dataUpdatedAt = Instant.fromEpochSeconds(dataUpdatedAt),
        )
}

@Serializable
private data class RemoteRating(
    val score: Double?,
    @SerialName("vote_count") val voteCount: Int,
    val distribution: Map<Int, Int> = emptyMap(),
) {
    fun toModel(updatedAt: Long): BangumiRating? =
        if (voteCount <= 0) {
            null
        } else {
            BangumiRating(score, voteCount.coerceAtLeast(0), distribution, Instant.fromEpochSeconds(updatedAt))
        }
}

@Serializable
private data class RemoteEpisodePage(
    val data: List<RemoteEpisode> = emptyList(),
)

@Serializable
private data class RemoteEpisode(
    val id: Long,
    val type: Int = 0,
    val name: String = "",
    @SerialName("name_cn") val nameCn: String = "",
    val sort: Double = 0.0,
    val ep: Double? = null,
    val airdate: String? = null,
) {
    fun toModel(subjectId: SubjectId): Episode? =
        runCatching {
            Episode(
                id = EpisodeId(id),
                subjectId = subjectId,
                number = ep,
                sort = sort.toInt(),
                title = nameCn.trim().ifEmpty { null },
                originalTitle = name.trim().ifEmpty { null },
                type =
                    when (type) {
                        0 -> EpisodeType.Main
                        1 -> EpisodeType.Special
                        2 -> EpisodeType.Opening
                        3 -> EpisodeType.Ending
                        else -> EpisodeType.Other
                    },
                airDate = airdate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                airStatus = if (airdate.isNullOrBlank()) EpisodeAirStatus.Unknown else EpisodeAirStatus.Aired,
            )
        }.getOrNull()
}

@Serializable
private data class RemoteCredits(
    val characters: List<RemoteCharacter> = emptyList(),
    val persons: List<RemotePerson> = emptyList(),
    @SerialName("generated_at") val generatedAt: Long,
) {
    fun toModel(): SubjectCredits =
        SubjectCredits(
            characters = characters.mapNotNull(RemoteCharacter::toModel),
            persons = persons.mapNotNull { it.toModel(it.relation) },
        )
}

@Serializable
private data class RemoteCharacter(
    val id: Long,
    val name: String,
    val relation: String = "",
    val images: RemoteCreditImages? = null,
    val actors: List<RemotePerson> = emptyList(),
) {
    fun toModel(): CharacterCredit? =
        runCatching {
            CharacterCredit(
                CharacterId(id),
                name.trim(),
                images?.best()?.let { ImageRef.Remote(it, "bangumi-character-$id") },
                relation.trim(),
                actors.mapNotNull { it.toModel(it.relation) },
            )
        }.getOrNull()
}

@Serializable
private data class RemotePerson(
    val id: Long,
    val name: String,
    val relation: String? = null,
    val images: RemoteCreditImages? = null,
) {
    fun toModel(role: String?): PersonCredit? =
        runCatching {
            PersonCredit(
                PersonId(id),
                name.trim(),
                images?.best()?.let { ImageRef.Remote(it, "bangumi-person-$id") },
                role?.trim()?.takeIf(String::isNotEmpty),
            )
        }.getOrNull()
}

@Serializable
private data class RemoteCreditImages(
    val medium: String? = null,
    val large: String? = null,
    val small: String? = null,
    val grid: String? = null,
) {
    fun best(): String? = listOf(medium, large, small, grid).firstOrNull { !it.isNullOrBlank() }
}

@Serializable
private data class RemoteRelation(
    val id: Long,
    val name: String,
    @SerialName("name_cn") val nameCn: String = "",
    val type: Int = 0,
    val relation: String = "",
    val images: RemoteCreditImages? = null,
) {
    fun toModel(): SubjectRelation? =
        runCatching {
            val title = nameCn.trim().ifEmpty { name.trim() }
            SubjectRelation(
                subject =
                    SubjectSummary(
                        id = SubjectId(id),
                        title = title,
                        originalTitle = name.trim().takeIf { it.isNotEmpty() && it != title },
                        aliases = emptyList(),
                        poster = images?.best()?.let { ImageRef.Remote(it, "bangumi-relation-$id") },
                        year = null,
                        type = if (type == 2) SubjectType.Tv else SubjectType.Other,
                        airingStatus = AiringStatus.Unknown,
                        rating = null,
                        collection = null,
                    ),
                kind = relation.toRelationKind(),
                label = relation.trim().ifEmpty { "关联作品" },
            )
        }.getOrNull()
}

private fun resolveUrl(
    baseUrl: String,
    value: String,
): String =
    if (value.startsWith("http://") || value.startsWith("https://")) value else "$baseUrl/${value.trimStart('/')}"

private fun String.toSubjectType(): SubjectType =
    when (lowercase()) {
        "tv" -> SubjectType.Tv
        "web" -> SubjectType.Web
        "ova" -> SubjectType.Ova
        "movie" -> SubjectType.Movie
        else -> SubjectType.Other
    }

private fun String.toAiringStatus(): AiringStatus =
    when (lowercase()) {
        "announced" -> AiringStatus.Announced
        "airing" -> AiringStatus.Airing
        "finished" -> AiringStatus.Finished
        else -> AiringStatus.Unknown
    }

private fun String.toRelationKind(): RelationKind =
    when (lowercase()) {
        "前传", "prequel" -> RelationKind.Prequel
        "续集", "sequel" -> RelationKind.Sequel
        "相同世界观", "same setting", "same world" -> RelationKind.SameWorld
        "不同演绎", "alternative version", "alternative" -> RelationKind.Alternative
        "角色出演", "character" -> RelationKind.Character
        "总集篇", "summary" -> RelationKind.Summary
        else -> RelationKind.Other
    }
