package site.jokersh.anime.data.catalog

import kotlinx.coroutines.flow.Flow
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.Page
import site.jokersh.anime.core.model.RefreshPolicy
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

public interface CatalogRepository {
    public fun observeDiscovery(): Flow<ResourceState<DiscoveryFeed>>

    public fun observeSubject(id: SubjectId): Flow<ResourceState<SubjectDetail>>

    public fun observeEpisodes(id: SubjectId): Flow<ResourceState<List<Episode>>>

    public fun observeCredits(id: SubjectId): Flow<ResourceState<SubjectCredits>>

    public fun observeRelations(id: SubjectId): Flow<ResourceState<List<SubjectRelation>>>

    public suspend fun refreshDiscovery(policy: RefreshPolicy): Result<Unit>

    public suspend fun refreshSubject(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit>

    public suspend fun refreshSection(
        id: SubjectId,
        section: SubjectSection,
        policy: RefreshPolicy,
    ): Result<Unit>
}

public interface SearchRepository {
    public fun observeHistory(): Flow<List<SearchHistoryItem>>

    public fun observeSuggestions(query: String): Flow<ResourceState<List<SearchSuggestion>>>

    public suspend fun search(request: SearchRequest): Result<Page<SubjectSummary>>

    public suspend fun saveHistory(query: String)

    public suspend fun deleteHistory(id: String)

    public suspend fun clearHistory()
}

public fun Cursor?.isFirstPage(): Boolean = this == null
