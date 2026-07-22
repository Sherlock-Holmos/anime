package site.jokersh.anime.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.data.catalog.CatalogRepository

@Composable
public fun DiscoverRoute(
    repository: CatalogRepository,
    onSubjectClick: (SubjectId) -> Unit,
    onSeeAll: (String) -> Unit,
    onMessage: (DiscoverMessageUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel(key = "discover") { DiscoverViewModel(repository) }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.accept(DiscoverIntent.Entered)
        viewModel.effects.collect { effect ->
            when (effect) {
                is DiscoverEffect.NavigateToSubject -> onSubjectClick(effect.subjectId)
                is DiscoverEffect.NavigateToSection -> onSeeAll(effect.sectionId)
                is DiscoverEffect.ShowMessage -> onMessage(effect.message)
            }
        }
    }

    DiscoverScreen(
        state = state,
        onRefresh = { viewModel.accept(DiscoverIntent.Refresh) },
        onRetry = { viewModel.accept(DiscoverIntent.Retry) },
        onSubjectClick = { viewModel.accept(DiscoverIntent.SubjectClicked(it)) },
        onSeeAll = { viewModel.accept(DiscoverIntent.SectionMoreClicked(it)) },
        modifier = modifier,
    )
}
