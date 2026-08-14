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
import site.jokersh.anime.core.designsystem.AnimePosterCard
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.data.catalog.CatalogRepository

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
        AnimeSecondaryButton(label = "返回发现", onClick = onBack)
        Text(section?.title ?: "全部作品", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("${section?.subjects?.size ?: 0} 部作品", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
