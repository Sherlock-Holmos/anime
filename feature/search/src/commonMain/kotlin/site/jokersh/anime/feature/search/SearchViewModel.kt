package site.jokersh.anime.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.Page
import site.jokersh.anime.core.model.SearchRequest
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.data.catalog.SearchRepository

public class SearchViewModel(
    private val repository: SearchRepository,
    initialQuery: String? = null,
    private val pageSize: Int = 12,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SearchUiState(query = initialQuery.orEmpty()))
    private val mutableEffects = Channel<SearchEffect>(capacity = Channel.BUFFERED)
    private var suggestionJob: Job? = null
    private var searchJob: Job? = null

    public val state: StateFlow<SearchUiState> = mutableState.asStateFlow()
    public val effects: Flow<SearchEffect> = mutableEffects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeHistory().collect { history ->
                mutableState.update { state ->
                    state.copy(recentQueries = history.map(SearchUiMapper::history))
                }
            }
        }
        initialQuery?.trim()?.takeIf { it.isNotBlank() }?.let(::submit)
    }

    public fun accept(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                onQueryChanged(intent.value)
            }

            is SearchIntent.Submit -> {
                submit(intent.value)
            }

            is SearchIntent.SuggestionClicked -> {
                submit(intent.value)
            }

            is SearchIntent.RecentClicked -> {
                submit(intent.value)
            }

            is SearchIntent.RemoveRecent -> {
                viewModelScope.launch { repository.deleteHistory(intent.id) }
            }

            SearchIntent.ClearAllRecent -> {
                viewModelScope.launch { repository.clearHistory() }
            }

            SearchIntent.ClearQuery -> {
                clearQuery()
            }

            SearchIntent.LoadNextPage,
            SearchIntent.RetryNextPage,
            -> {
                loadNextPage()
            }

            is SearchIntent.SubjectClicked -> {
                viewModelScope.launch { mutableEffects.send(SearchEffect.NavigateToSubject(intent.id)) }
            }
        }
    }

    private fun onQueryChanged(value: String) {
        val next = value.take(100)
        mutableState.update { state ->
            state.copy(
                query = next,
                suggestions = emptyList(),
                mode = if (next.isBlank()) SearchMode.Idle else SearchMode.Suggesting,
                loadMoreError = null,
            )
        }
        suggestionJob?.cancel()
        val normalized = next.trim()
        if (normalized.isBlank()) return
        suggestionJob =
            viewModelScope.launch {
                repository
                    .observeSuggestions(normalized)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        mutableState.update { it.copy(suggestions = emptyList()) }
                    }.collect { resource ->
                        mutableState.update { state ->
                            state.copy(suggestions = resource.value.orEmpty().map(SearchUiMapper::suggestion))
                        }
                    }
            }
    }

    private fun submit(rawQuery: String) {
        val normalized = rawQuery.trim()
        if (normalized.isBlank()) {
            clearQuery()
            return
        }
        suggestionJob?.cancel()
        searchJob?.cancel()
        mutableState.update { state ->
            state.copy(
                query = normalized,
                mode = SearchMode.Loading,
                suggestions = emptyList(),
                results = emptyList(),
                nextCursor = null,
                resultQuery = normalized,
                loadMoreError = null,
            )
        }
        viewModelScope.launch {
            repository.saveHistory(normalized)
            mutableEffects.send(SearchEffect.NavigateToResults(normalized))
        }
        searchJob = viewModelScope.launch { executeSearch(query = normalized, cursor = null) }
    }

    private fun loadNextPage() {
        val current = mutableState.value
        val cursor = current.nextCursor ?: return
        val query = current.resultQuery ?: return
        if (current.isLoadingMore) return
        searchJob = viewModelScope.launch { executeSearch(query = query, cursor = cursor) }
    }

    private suspend fun executeSearch(
        query: String,
        cursor: Cursor?,
    ) {
        if (cursor != null) {
            mutableState.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        }
        val result = repository.search(SearchRequest(query = query, cursor = cursor, pageSize = pageSize))
        result.fold(
            onSuccess = { page -> applyPage(page, cursor) },
            onFailure = {
                mutableState.update { state ->
                    if (cursor == null) {
                        state.copy(mode = SearchMode.BlockingError, isLoadingMore = false)
                    } else {
                        state.copy(isLoadingMore = false, loadMoreError = AppError.Unknown("search-page"))
                    }
                }
            },
        )
    }

    private fun applyPage(
        page: Page<SubjectSummary>,
        cursor: Cursor?,
    ) {
        val mapped = page.items.map(SearchUiMapper::subject)
        mutableState.update { state ->
            val merged =
                if (cursor == null) {
                    mapped
                } else {
                    (state.results + mapped).distinctBy { subject -> subject.id }
                }
            state.copy(
                mode = if (merged.isEmpty()) SearchMode.Empty else SearchMode.Results,
                results = merged,
                nextCursor = page.nextCursor,
                isLoadingMore = false,
                loadMoreError = null,
            )
        }
    }

    private fun clearQuery() {
        suggestionJob?.cancel()
        searchJob?.cancel()
        mutableState.update { state ->
            state.copy(
                query = "",
                suggestions = emptyList(),
                mode = SearchMode.Idle,
                results = emptyList(),
                nextCursor = null,
                resultQuery = null,
                isLoadingMore = false,
                loadMoreError = null,
            )
        }
    }
}
