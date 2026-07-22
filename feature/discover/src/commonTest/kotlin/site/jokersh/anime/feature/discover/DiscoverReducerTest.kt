package site.jokersh.anime.feature.discover

import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.BangumiRating
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.DiscoverySection
import site.jokersh.anime.core.model.Freshness
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.core.model.SubjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class DiscoverReducerTest {
    @Test
    fun `VM-DIS-001 maps repository content and keeps Bangumi attribution`() {
        val state = DiscoverReducer.repositoryChanged(DiscoverUiState(), resource(feed()))

        val content = assertIs<AsyncContent.Content<DiscoverContentUi>>(state.content).value
        assertEquals("8.6  Bangumi", content.hero.rating)
        assertEquals(listOf("airing"), content.sections.map { it.id })
        assertFalse(state.isRefreshing)
        assertEquals("08:00", state.lastUpdatedLabel)
    }

    @Test
    fun `VM-STA-002 refresh keeps previous content visible`() {
        val loaded = DiscoverReducer.repositoryChanged(DiscoverUiState(), resource(feed()))
        val refreshing =
            DiscoverReducer.repositoryChanged(
                loaded,
                resource(feed()).copy(refreshing = true),
            )

        assertIs<AsyncContent.Content<DiscoverContentUi>>(refreshing.content)
        assertTrue(refreshing.isRefreshing)
    }

    @Test
    fun `VM-OFF-002 offline without cache becomes blocking failure`() {
        val state =
            DiscoverReducer.repositoryChanged(
                DiscoverUiState(),
                ResourceState(
                    value = null,
                    freshness = null,
                    refreshing = false,
                    error = AppError.Offline,
                ),
            )

        val failure = assertIs<AsyncContent.Failure>(state.content)
        assertEquals(DiscoverErrorUi.Offline, failure.error)
        assertTrue(state.isOffline)
    }
}

private val now = Instant.parse("2026-07-19T08:00:00Z")

private fun feed(): DiscoveryFeed =
    DiscoveryFeed(
        sections = listOf(DiscoverySection("airing", "正在热播", listOf(subject()))),
        generatedAt = now,
    )

private fun subject(): SubjectSummary =
    SubjectSummary(
        id = SubjectId(1001),
        title = "星海邮差",
        originalTitle = null,
        aliases = emptyList(),
        poster = null,
        year = 2026,
        type = SubjectType.Tv,
        airingStatus = AiringStatus.Airing,
        rating = BangumiRating(8.6, 1234, emptyMap(), now),
        collection = null,
    )

private fun resource(feed: DiscoveryFeed): ResourceState<DiscoveryFeed> =
    ResourceState(
        value = feed,
        freshness = Freshness(FreshnessKind.Fresh, now),
        refreshing = false,
        error = null,
    )
