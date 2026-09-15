package site.jokersh.anime.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import anime.core.designsystem.generated.resources.Res as DesignRes
import anime.core.designsystem.generated.resources.copy_action_return_discover
import site.jokersh.anime.core.designsystem.AnimePosterCard
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.data.catalog.CatalogRepository
import site.jokersh.anime.feature.discover.generated.resources.Res
import site.jokersh.anime.feature.discover.generated.resources.discover_all_subjects
import site.jokersh.anime.feature.discover.generated.resources.discover_subject_count

@Composable
public fun DiscoverSectionRoute(
    sectionId: String,
    repository: CatalogRepository,
    onBack: () -> Unit,
    onSubjectClick: (SubjectId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resource by repository.observeDiscovery().collectAsState(
        initial =
            site.jokersh.anime.core.model
                .ResourceState(null, null, false, null),
    )
    LaunchedEffect(repository) { repository.refreshDiscovery(RefreshPolicy.IfMissing) }
    val section = resource.value?.sections?.firstOrNull { it.id == sectionId }
    Column(
        modifier.fillMaxSize().padding(AnimeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
    ) {
        AnimeSecondaryButton(label = stringResource(DesignRes.string.copy_action_return_discover), onClick = onBack)
        Text(section?.title ?: stringResource(Res.string.discover_all_subjects), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.discover_subject_count, section?.subjects?.size ?: 0), color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AnimeSpacing.giant),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            items(section?.subjects.orEmpty(), key = { it.id.value }) { subject ->
                val ui = DiscoverUiMapper.subject(subject)
                AnimePosterCard(model = ui, onClick = { onSubjectClick(ui.id) })
            }
        }
    }
}
