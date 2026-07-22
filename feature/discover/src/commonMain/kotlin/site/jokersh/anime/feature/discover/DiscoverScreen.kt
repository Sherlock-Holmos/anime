package site.jokersh.anime.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimePosterCard
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSectionHeader
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.feature.discover.generated.resources.Res
import site.jokersh.anime.feature.discover.generated.resources.discover_airing
import site.jokersh.anime.feature.discover.generated.resources.discover_airing_description
import site.jokersh.anime.feature.discover.generated.resources.discover_brand
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_accessibility
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_eyebrow
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_rating
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_summary
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_title
import site.jokersh.anime.feature.discover.generated.resources.discover_see_all
import site.jokersh.anime.feature.discover.generated.resources.discover_title
import site.jokersh.anime.feature.discover.generated.resources.discover_today
import site.jokersh.anime.feature.discover.generated.resources.discover_top_rated
import site.jokersh.anime.feature.discover.generated.resources.discover_top_rated_description

@Immutable
public data class DiscoverSectionUi(
    val id: String,
    val title: String,
    val description: String,
    val subjects: List<SubjectCardUi>,
)

@Immutable
public data class DiscoverUiState(
    val sections: List<DiscoverSectionUi>,
)

@Composable
public fun DiscoverScreen(
    state: DiscoverUiState,
    onSubjectClick: (SubjectId) -> Unit,
    onSeeAll: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = AnimeSpacing.lg, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxl),
    ) {
        item(key = "header") { DiscoverHeader() }
        item(key = "hero") { DiscoverHero(onClick = { onSubjectClick(SubjectId(1001)) }) }
        items(state.sections, key = { it.id }) { section ->
            DiscoverSection(
                section = section,
                onSubjectClick = onSubjectClick,
                onSeeAll = { onSeeAll(section.id) },
            )
        }
    }
}

@Composable
public fun rememberDiscoverFixtureState(): DiscoverUiState {
    val airingTitle = stringResource(Res.string.discover_airing)
    val topRatedTitle = stringResource(Res.string.discover_top_rated)
    return DiscoverUiState(
        sections =
            listOf(
                DiscoverSectionUi(
                    id = "airing",
                    title = airingTitle,
                    description = stringResource(Res.string.discover_airing_description),
                    subjects =
                        listOf(
                            subject(1001, "星海邮差", "2026 · TV · 连载中", "8.6"),
                            subject(1006, "纸月亮计划", "2026 · TV · 更新至 8 集", "8.2"),
                            subject(1008, "红茶侦探社", "2026 · TV · 连载中", "8.8"),
                        ),
                ),
                DiscoverSectionUi(
                    id = "top-rated",
                    title = topRatedTitle,
                    description = stringResource(Res.string.discover_top_rated_description),
                    subjects =
                        listOf(
                            subject(1002, "雨城备忘录", "2025 · TV · 全 12 集", "9.1"),
                            subject(1009, "夏末天文台", "2025 · TV · 全 12 集", "9.0"),
                            subject(1011, "风经过旧书店", "2026 · Web · 连载中", "8.1"),
                        ),
                ),
            ),
    )
}

@Composable
private fun DiscoverHeader() {
    Column(
        modifier = Modifier.padding(horizontal = AnimeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.discover_brand),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.discover_title),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Text(
                    text = stringResource(Res.string.discover_today),
                    modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.sm),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiscoverHero(onClick: () -> Unit) {
    val accessibilityLabel = stringResource(Res.string.discover_hero_accessibility)
    Box(
        modifier =
            Modifier
                .padding(horizontal = AnimeSpacing.lg)
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(AnimeRadius.panel))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF7776E8),
                            Color(0xFFB28FD9),
                            Color(0xFFF4B8C8),
                        ),
                    ),
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = accessibilityLabel
                }.clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(190.dp)
                .offset(x = 210.dp, y = (-42).dp)
                .background(Color.White.copy(alpha = 0.16f), CircleShape),
        )
        Box(
            Modifier
                .size(120.dp)
                .offset(x = (-24).dp, y = 158.dp)
                .background(Color(0xFF5A5FC7).copy(alpha = 0.22f), CircleShape),
        )
        AnimeGlassPanel(
            role = GlassRole.FloatingPanel,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(AnimeSpacing.md),
            shape = RoundedCornerShape(AnimeRadius.card),
            contentPadding = PaddingValues(AnimeSpacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                Text(
                    text = stringResource(Res.string.discover_hero_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.discover_hero_title),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(Res.string.discover_hero_rating),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(Res.string.discover_hero_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DiscoverSection(
    section: DiscoverSectionUi,
    onSubjectClick: (SubjectId) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        AnimeSectionHeader(
            title = section.title,
            description = section.description,
            actionLabel = stringResource(Res.string.discover_see_all),
            onAction = onSeeAll,
            modifier = Modifier.padding(horizontal = AnimeSpacing.lg),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = AnimeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            items(section.subjects, key = { it.id.value }) { subject ->
                AnimePosterCard(
                    model = subject,
                    onClick = { onSubjectClick(subject.id) },
                )
            }
        }
    }
}

private fun subject(
    id: Long,
    title: String,
    metadata: String,
    score: String,
): SubjectCardUi =
    SubjectCardUi(
        id = SubjectId(id),
        title = title,
        originalTitle = null,
        poster = null,
        metadata = metadata,
        rating = "$score  Bangumi",
        collectionLabel = null,
        accessibilityLabel = "$title，$metadata，Bangumi 评分 $score 分",
    )
