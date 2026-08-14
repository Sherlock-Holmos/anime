package site.jokersh.anime.feature.subject

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimePosterArtwork
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.model.CharacterCredit
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectCredits
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectRelation
import site.jokersh.anime.core.model.SubjectSection
import site.jokersh.anime.data.catalog.CatalogRepository

@Composable
public fun EpisodesRoute(
    subjectId: Long,
    repository: CatalogRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = SubjectId(subjectId)
    val state by repository.observeEpisodes(id).collectAsState(ResourceState<List<Episode>>(null, null, false, null))
    LaunchedEffect(repository, subjectId) {
        repository.refreshSection(id, SubjectSection.Episodes, RefreshPolicy.IfStale)
    }
    SubjectSectionScaffold<Episode>(
        title = "分集",
        state = state,
        emptyMessage = "暂无分集资料",
        onBack = onBack,
        onRetry = { repository.refreshSection(id, SubjectSection.Episodes, RefreshPolicy.Force) },
        modifier = modifier,
    ) { episode -> EpisodeRow(episode) }
}

@Composable
public fun CharactersRoute(
    subjectId: Long,
    repository: CatalogRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = SubjectId(subjectId)
    val state by repository.observeCredits(id).collectAsState(ResourceState<SubjectCredits>(null, null, false, null))
    LaunchedEffect(repository, subjectId) {
        repository.refreshSection(id, SubjectSection.Credits, RefreshPolicy.IfStale)
    }
    val characters = state.value?.characters
    SubjectSectionScaffold<CharacterCredit>(
        title = "角色与声优",
        state = ResourceState(characters, state.freshness, state.refreshing, state.error),
        emptyMessage = "暂无角色资料",
        onBack = onBack,
        onRetry = { repository.refreshSection(id, SubjectSection.Credits, RefreshPolicy.Force) },
        modifier = modifier,
    ) { character -> CharacterRow(character) }
}

@Composable
public fun RelationsRoute(
    subjectId: Long,
    repository: CatalogRepository,
    onBack: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = SubjectId(subjectId)
    val state by repository
        .observeRelations(
            id,
        ).collectAsState(ResourceState<List<SubjectRelation>>(null, null, false, null))
    LaunchedEffect(repository, subjectId) {
        repository.refreshSection(id, SubjectSection.Relations, RefreshPolicy.IfStale)
    }
    SubjectSectionScaffold<SubjectRelation>(
        title = "关联作品",
        state = state,
        emptyMessage = "暂无关联作品",
        onBack = onBack,
        onRetry = { repository.refreshSection(id, SubjectSection.Relations, RefreshPolicy.Force) },
        modifier = modifier,
    ) { relation -> RelationRow(relation) { onSubjectClick(relation.subject.id.value) } }
}

@Composable
private fun <T> SubjectSectionScaffold(
    title: String,
    state: ResourceState<List<T>>,
    emptyMessage: String,
    onBack: () -> Unit,
    onRetry: suspend () -> Unit,
    modifier: Modifier,
    row: @Composable (T) -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().widthIn(max = AnimeSize.contentMax).padding(AnimeSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(AnimeRadius.round),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(Modifier.padding(AnimeSpacing.md), contentAlignment = Alignment.Center) {
                    AnimeBackIcon(color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        when {
            state.value == null && state.refreshing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            state.value == null && state.error != null -> {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                ) {
                    Text("暂时无法加载$title", style = MaterialTheme.typography.titleLarge)
                    RetryEffectButton(onRetry)
                }
            }

            state.value.isNullOrEmpty() -> {
                Box(Modifier.fillMaxWidth().padding(AnimeSpacing.huge), contentAlignment = Alignment.Center) {
                    Text(emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                    items(state.value.orEmpty()) { item -> row(item) }
                }
            }
        }
    }
}

@Composable
private fun RetryEffectButton(onRetry: suspend () -> Unit) {
    var retrySignal by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    LaunchedEffect(retrySignal) { if (retrySignal > 0) onRetry() }
    AnimeSecondaryButton("重试", { retrySignal += 1 })
}

@Composable
private fun EpisodeRow(episode: Episode) {
    SectionRow {
        Text(
            episode.number?.let {
                "第 ${it.toReadableNumber()} 话"
            } ?: "特别篇",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            episode.title?.takeIf(String::isNotBlank) ?: episode.originalTitle.orEmpty().ifBlank {
                "标题待补充"
            },
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            listOfNotNull(episode.airDate?.toString(), episode.airStatus.name).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CharacterRow(character: CharacterCredit) {
    SectionRow {
        Text(character.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(character.relation, color = MaterialTheme.colorScheme.primary)
        if (character.actors.isNotEmpty()) {
            Text(
                "声优：${character.actors.joinToString { it.name }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RelationRow(
    relation: SubjectRelation,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Row(
            Modifier.padding(AnimeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimePosterArtwork(
                poster = relation.subject.poster,
                title = relation.subject.title,
                id = relation.subject.id,
                modifier = Modifier.width(64.dp).height(88.dp).clip(RoundedCornerShape(AnimeRadius.control)),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                Text(
                    relation.subject.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(relation.label.ifBlank { relation.kind.name }, color = MaterialTheme.colorScheme.primary)
                relation.subject.originalTitle?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionRow(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            Modifier.padding(AnimeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
            content = content,
        )
    }
}

private fun Double.toReadableNumber(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
