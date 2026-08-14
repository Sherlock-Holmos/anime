package site.jokersh.anime.feature.search

import androidx.compose.runtime.Immutable
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectType

@Immutable
public data class SearchUiState(
    val query: String = "",
    val recentQueries: List<SearchHistoryUi> = emptyList(),
    val suggestions: List<SearchSuggestionUi> = emptyList(),
    val mode: SearchMode = SearchMode.Idle,
    val results: List<SubjectCardUi> = emptyList(),
    val nextCursor: Cursor? = null,
    val resultQuery: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: AppError? = null,
    val trending: List<String> = emptyList(),
    val recommendations: List<SubjectCardUi> = emptyList(),
    val recommendationsPersonalized: Boolean = false,
    val discoveryLoading: Boolean = false,
    val types: Set<SubjectType> = emptySet(),
    val years: IntRange? = null,
    val airing: Set<AiringStatus> = emptySet(),
    val sort: SearchSort = SearchSort.Relevance,
)

public enum class SearchMode {
    Idle,
    Suggesting,
    Loading,
    Results,
    Empty,
    BlockingError,
}

@Immutable
public data class SearchHistoryUi(
    val id: String,
    val query: String,
)

@Immutable
public data class SearchSuggestionUi(
    val value: String,
)

public sealed interface SearchIntent {
    public data class QueryChanged(
        val value: String,
    ) : SearchIntent

    public data class Submit(
        val value: String,
    ) : SearchIntent

    public data class SuggestionClicked(
        val value: String,
    ) : SearchIntent

    public data class RecentClicked(
        val value: String,
    ) : SearchIntent

    public data class RemoveRecent(
        val id: String,
    ) : SearchIntent

    public data object ClearAllRecent : SearchIntent

    public data object ClearQuery : SearchIntent

    public data class ToggleType(
        val value: SubjectType,
    ) : SearchIntent

    public data class ToggleAiring(
        val value: AiringStatus,
    ) : SearchIntent

    public data object ToggleRecentYears : SearchIntent

    public data class SortChanged(
        val value: SearchSort,
    ) : SearchIntent

    public data object LoadNextPage : SearchIntent

    public data object RetryNextPage : SearchIntent

    public data class SubjectClicked(
        val id: SubjectId,
    ) : SearchIntent
}

public sealed interface SearchEffect {
    public data class NavigateToResults(
        val query: String,
    ) : SearchEffect

    public data class NavigateToSubject(
        val subjectId: SubjectId,
    ) : SearchEffect
}
