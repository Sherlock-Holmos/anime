package site.jokersh.anime.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.data.catalog.SearchRepository

@Composable
public fun SearchRoute(
    repository: SearchRepository,
    initialQuery: String?,
    initialTypes: Set<SubjectType> = emptySet(),
    initialYears: IntRange? = null,
    initialAiring: Set<AiringStatus> = emptySet(),
    initialSort: SearchSort = SearchSort.Relevance,
    pageSize: Int,
    onResultsRequested: (SearchEffect.NavigateToResults) -> Unit,
    onSubjectClick: (SubjectId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        viewModel(key = "search-${initialQuery.orEmpty()}-$initialTypes-$initialYears-$initialAiring") {
            SearchViewModel(
                repository = repository,
                initialQuery = initialQuery,
                initialTypes = initialTypes,
                initialYears = initialYears,
                initialAiring = initialAiring,
                initialSort = initialSort,
                pageSize = pageSize,
            )
        }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.NavigateToResults -> onResultsRequested(effect)
                is SearchEffect.NavigateToSubject -> onSubjectClick(effect.subjectId)
            }
        }
    }

    SearchScreen(
        state = state,
        onIntent = viewModel::accept,
        modifier = modifier,
    )
}
