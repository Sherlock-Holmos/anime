package site.jokersh.anime.data.catalog

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.DiscoverySection
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.Freshness
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.Page
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.ResourceKind
import site.jokersh.anime.core.model.ResourceState
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val catalogNow = Instant.parse("2026-07-19T08:00:00Z")
private val fixtureSubjects =
    (1001L..1006L).map { id ->
        SubjectSummary(
            id = SubjectId(id),
            title = "作品 $id",
            originalTitle = null,
            aliases = emptyList(),
            poster = null,
            year = 2026,
            type = SubjectType.Tv,
            airingStatus = AiringStatus.Airing,
            rating = null,
            collection = null,
        )
    }

abstract class CatalogRepositoryContract {
    protected abstract fun createRepository(): CatalogRepository

    @Test
    fun `CT-CAT-001 first observation exposes deterministic fresh data`() =
        runTest {
            val repository = createRepository()
            repository.refreshDiscovery(RefreshPolicy.IfMissing).getOrThrow()
            val state = repository.observeDiscovery().first()

            assertEquals(FreshnessKind.Fresh, state.freshness?.kind)
            assertEquals(
                listOf(1001L, 1006L, 1008L, 1011L, 1012L).map(::SubjectId),
                state.value
                    ?.sections
                    ?.first { it.id == "airing" }
                    ?.subjects
                    ?.map { it.id },
            )
        }

    @Test
    fun `CT-CAT-002 forced refresh advances fixture clock without reordering`() =
        runTest {
            val repository = createRepository()
            repository.refreshDiscovery(RefreshPolicy.IfMissing).getOrThrow()
            val before = repository.observeDiscovery().first().value

            assertTrue(repository.refreshDiscovery(RefreshPolicy.Force).isSuccess)

            val after = repository.observeDiscovery().first().value
            assertEquals(catalogNow + 60.seconds, after?.generatedAt)
            assertEquals(before?.sections, after?.sections)
        }

    @Test
    fun `CT-CAT-003 discovery subjects open as detail resources`() =
        runTest {
            val repository = createRepository()
            val state = repository.observeSubject(SubjectId(1001)).first()

            val detail = assertNotNull(state.value)
            assertEquals(SubjectId(1001), detail.summary.id)
            assertEquals("https://bangumi.tv/subject/1001", detail.sourceUrl)
            assertEquals(null, state.error)
        }
}

abstract class SearchRepositoryContract {
    protected abstract fun createRepository(): SearchRepository

    @Test
    fun `CT-SEA-004 cursor pages are stable and contain no duplicate subjects`() =
        runTest {
            val repository = createRepository()
            val first = repository.search(SearchRequest(query = "作品", pageSize = 5)).getOrThrow()
            val second =
                repository
                    .search(
                        SearchRequest(query = "作品", pageSize = 5, cursor = first.nextCursor),
                    ).getOrThrow()

            assertEquals(5, first.items.size)
            assertEquals(1, second.items.size)
            assertEquals(6, (first.items + second.items).distinctBy { it.id }.size)
            assertTrue(!second.hasMore)
        }

    @Test
    fun `CT-SEA-001 repeated history moves query to front and caps at ten`() =
        runTest {
            val repository = createRepository()
            repeat(11) { repository.saveHistory("query-$it") }
            repository.saveHistory("query-3")

            val history = repository.observeHistory().first()
            assertEquals(10, history.size)
            assertEquals("query-3", history.first().query)
            assertEquals(1, history.count { it.query == "query-3" })
        }
}

class FixtureCatalogRepositoryContractTest : CatalogRepositoryContract() {
    override fun createRepository(): CatalogRepository = FixtureCatalogRepository(readDelayMs = 0)
}

class FixtureSearchRepositoryContractTest : SearchRepositoryContract() {
    override fun createRepository(): SearchRepository = InMemorySearchRepository()
}

class FixtureSearchRepositoryTest {
    @Test
    fun `CT-SEA-006 fixture search supports deterministic pagination`() =
        runTest {
            val repository = FixtureSearchRepository(readDelayMs = 0)
            val first = repository.search(SearchRequest(query = "bangumi", pageSize = 5)).getOrThrow()
            val second =
                repository
                    .search(SearchRequest(query = "bangumi", pageSize = 5, cursor = first.nextCursor))
                    .getOrThrow()
            val third =
                repository
                    .search(SearchRequest(query = "bangumi", pageSize = 5, cursor = second.nextCursor))
                    .getOrThrow()

            assertEquals(5, first.items.size)
            assertEquals(5, second.items.size)
            assertEquals(2, third.items.size)
            assertEquals(12, (first.items + second.items + third.items).distinctBy { it.id }.size)
            assertFalse(third.hasMore)
        }

    @Test
    fun `CT-SEA-007 fixture search can open a result by Bangumi subject id`() =
        runTest {
            val repository = FixtureSearchRepository(readDelayMs = 0)
            val page = repository.search(SearchRequest(query = "1001", pageSize = 5)).getOrThrow()

            assertEquals(listOf(SubjectId(1001)), page.items.map { it.id })
            assertFalse(page.hasMore)
        }
}

private class InMemoryCatalogRepository : CatalogRepository {
    private val state = MutableStateFlow(discoveryState(catalogNow))

    override fun observeDiscovery(): Flow<ResourceState<DiscoveryFeed>> = state

    override fun observeSubject(id: SubjectId): Flow<ResourceState<SubjectDetail>> =
        MutableStateFlow(notFound(ResourceKind.Subject))

    override fun observeEpisodes(id: SubjectId): Flow<ResourceState<List<Episode>>> =
        MutableStateFlow(ResourceState(emptyList(), fresh(), refreshing = false, error = null))

    override fun observeCredits(id: SubjectId): Flow<ResourceState<SubjectCredits>> =
        MutableStateFlow(ResourceState(SubjectCredits(emptyList(), emptyList()), fresh(), false, null))

    override fun observeRelations(id: SubjectId): Flow<ResourceState<List<SubjectRelation>>> =
        MutableStateFlow(ResourceState(emptyList(), fresh(), refreshing = false, error = null))

    override suspend fun refreshDiscovery(policy: RefreshPolicy): Result<Unit> {
        if (policy == RefreshPolicy.Force) state.value = discoveryState(catalogNow + 60.seconds)
        return Result.success(Unit)
    }

    override suspend fun refreshSubject(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun refreshSection(
        id: SubjectId,
        section: SubjectSection,
        policy: RefreshPolicy,
    ): Result<Unit> = Result.success(Unit)
}

private class InMemorySearchRepository : SearchRepository {
    private val history = MutableStateFlow<List<SearchHistoryItem>>(emptyList())

    override fun observeHistory(): Flow<List<SearchHistoryItem>> = history

    override suspend fun discovery(): Result<site.jokersh.anime.core.model.SearchDiscovery> =
        Result.success(
            site.jokersh.anime.core.model
                .SearchDiscovery(emptyList(), fixtureSubjects.take(3), false),
        )

    override fun observeSuggestions(query: String): Flow<ResourceState<List<SearchSuggestion>>> =
        MutableStateFlow(
            ResourceState(
                value = fixtureSubjects.filter { it.title.contains(query) }.map { SearchSuggestion(it.title) },
                freshness = fresh(),
                refreshing = false,
                error = null,
            ),
        )

    override suspend fun search(request: SearchRequest): Result<Page<SubjectSummary>> {
        val matching = fixtureSubjects.filter { it.title.contains(request.query, ignoreCase = true) }
        val offset = request.cursor?.value?.toIntOrNull() ?: 0
        val items = matching.drop(offset).take(request.pageSize)
        val nextOffset = offset + items.size
        val nextCursor = if (nextOffset < matching.size) Cursor(nextOffset.toString()) else null
        return Result.success(Page(items, nextCursor, hasMore = nextCursor != null))
    }

    override suspend fun saveHistory(query: String) {
        history.value =
            (
                listOf(SearchHistoryItem("history-$query", query, catalogNow)) +
                    history.value.filterNot { it.query == query }
            ).take(10)
    }

    override suspend fun deleteHistory(id: String) {
        history.value = history.value.filterNot { it.id == id }
    }

    override suspend fun clearHistory() {
        history.value = emptyList()
    }
}

private fun discoveryState(now: Instant): ResourceState<DiscoveryFeed> =
    ResourceState(
        value = DiscoveryFeed(listOf(DiscoverySection("airing", "正在热播", fixtureSubjects)), now),
        freshness = Freshness(FreshnessKind.Fresh, now),
        refreshing = false,
        error = null,
    )

private fun fresh(): Freshness = Freshness(FreshnessKind.Fresh, catalogNow)

private fun <T> notFound(kind: ResourceKind): ResourceState<T> =
    ResourceState(value = null, freshness = null, refreshing = false, error = AppError.NotFound(kind))
