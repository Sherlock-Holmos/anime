package site.jokersh.anime.feature.subject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.data.catalog.CatalogRepository
import site.jokersh.anime.data.comment.CommunityRepository
import site.jokersh.anime.data.session.SessionRepository

@Composable
public fun SubjectRoute(
    subjectId: Long,
    repository: CatalogRepository,
    communityRepository: CommunityRepository,
    sessionRepository: SessionRepository,
    onBack: () -> Unit,
    onCollect: (Long) -> Unit,
    onRate: (Long) -> Unit,
    onEpisodesClick: (Long) -> Unit,
    onCharactersClick: (Long) -> Unit,
    onRelationsClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onReviewsClick: (Long) -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        viewModel(key = "subject-$subjectId") {
            SubjectViewModel(SubjectId(subjectId), repository, communityRepository, sessionRepository)
        }
    val state by viewModel.state.collectAsState()

    SubjectScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRate = { onRate(subjectId) },
        onCollect = { onCollect(subjectId) },
        onEpisodesClick = { onEpisodesClick(subjectId) },
        onCharactersClick = { onCharactersClick(subjectId) },
        onRelationsClick = { onRelationsClick(subjectId) },
        onCommentsClick = { onCommentsClick(subjectId) },
        onReviewsClick = { onReviewsClick(subjectId) },
        onReviewClick = onReviewClick,
        onListClick = onListClick,
        modifier = modifier,
    )
}
