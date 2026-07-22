package site.jokersh.anime.feature.subject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.data.catalog.CatalogRepository

@Composable
public fun SubjectRoute(
    subjectId: Long,
    repository: CatalogRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        viewModel(key = "subject-$subjectId") {
            SubjectViewModel(SubjectId(subjectId), repository)
        }
    val state by viewModel.state.collectAsState()

    SubjectScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}
