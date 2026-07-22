package site.jokersh.anime.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import site.jokersh.anime.feature.discover.generated.resources.discover_brand
import site.jokersh.anime.feature.discover.generated.resources.discover_empty_action
import site.jokersh.anime.feature.discover.generated.resources.discover_empty_body
import site.jokersh.anime.feature.discover.generated.resources.discover_empty_title
import site.jokersh.anime.feature.discover.generated.resources.discover_error_action
import site.jokersh.anime.feature.discover.generated.resources.discover_error_data
import site.jokersh.anime.feature.discover.generated.resources.discover_error_offline
import site.jokersh.anime.feature.discover.generated.resources.discover_error_rate_limited
import site.jokersh.anime.feature.discover.generated.resources.discover_error_service
import site.jokersh.anime.feature.discover.generated.resources.discover_error_unknown
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_eyebrow
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_summary
import site.jokersh.anime.feature.discover.generated.resources.discover_offline
import site.jokersh.anime.feature.discover.generated.resources.discover_refresh
import site.jokersh.anime.feature.discover.generated.resources.discover_see_all
import site.jokersh.anime.feature.discover.generated.resources.discover_title

@Composable
public fun DiscoverScreen(
    state: DiscoverUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSubjectClick: (SubjectId) -> Unit,
    onSeeAll: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("discover.list"),
        contentPadding = PaddingValues(top = AnimeSpacing.lg, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxl),
    ) {
        item(key = "header", contentType = "header") {
            DiscoverHeader(isRefreshing = state.isRefreshing, onRefresh = onRefresh)
        }
        if (state.isOffline) {
            item(key = "offline", contentType = "status-banner") {
                OfflineBanner(lastUpdatedLabel = state.lastUpdatedLabel)
            }
        }
        when (val content = state.content) {
            AsyncContent.Initial,
            AsyncContent.Loading,
            -> {
                item(key = "loading", contentType = "loading") { DiscoverLoading() }
            }

            AsyncContent.Empty -> {
                item(key = "empty", contentType = "empty") {
                    DiscoverStatusPanel(
                        title = stringResource(Res.string.discover_empty_title),
                        body = stringResource(Res.string.discover_empty_body),
                        action = stringResource(Res.string.discover_empty_action),
                        onAction = onRetry,
                        tag = "discover.empty",
                    )
                }
            }

            is AsyncContent.Failure -> {
                item(key = "error", contentType = "error") {
                    DiscoverStatusPanel(
                        title = errorTitle(content.error),
                        body = stringResource(Res.string.discover_error_unknown),
                        action = stringResource(Res.string.discover_error_action),
                        onAction = onRetry,
                        tag = "discover.error",
                    )
                }
            }

            is AsyncContent.Content -> {
                item(key = "hero", contentType = "hero") {
                    DiscoverHero(
                        subject = content.value.hero,
                        onClick = { onSubjectClick(content.value.hero.id) },
                    )
                }
                items(
                    items = content.value.sections,
                    key = { section -> "section:${section.id}" },
                    contentType = { "section" },
                ) { section ->
                    DiscoverSection(
                        section = section,
                        onSubjectClick = onSubjectClick,
                        onSeeAll = { onSeeAll(section.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
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
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isRefreshing, onClick = onRefresh)
                        .testTag("discover.refresh"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = AnimeSpacing.md)) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = stringResource(Res.string.discover_refresh),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(lastUpdatedLabel: String?) {
    AnimeGlassPanel(
        role = GlassRole.FloatingPanel,
        modifier = Modifier.padding(horizontal = AnimeSpacing.lg).fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.round),
        contentPadding = PaddingValues(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.md),
    ) {
        Text(
            text =
                if (lastUpdatedLabel == null) {
                    stringResource(Res.string.discover_offline)
                } else {
                    "${stringResource(Res.string.discover_offline)} · $lastUpdatedLabel"
                },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiscoverHero(
    subject: SubjectCardUi,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .padding(horizontal = AnimeSpacing.lg)
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(AnimeRadius.panel))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7776E8), Color(0xFFB28FD9), Color(0xFFF4B8C8)),
                    ),
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = subject.accessibilityLabel
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
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(AnimeSpacing.md),
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
                        text = subject.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subject.rating?.let { rating ->
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
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
    Column(
        modifier = Modifier.testTag("discover.section.${section.id}"),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
    ) {
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
            items(
                items = section.subjects,
                key = { subject -> "subject:${subject.id.value}" },
                contentType = { if (section.id == "continue") "progress-card" else "poster-card" },
            ) { subject ->
                if (section.id == "continue") {
                    ContinueWatchingCard(
                        subject = subject,
                        onClick = { onSubjectClick(subject.id) },
                        modifier = Modifier.testTag("discover.subject.${subject.id.value}"),
                    )
                } else {
                    AnimePosterCard(
                        model = subject,
                        onClick = { onSubjectClick(subject.id) },
                        modifier = Modifier.testTag("discover.subject.${subject.id.value}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    subject: SubjectCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width(264.dp)
                .height(112.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = subject.accessibilityLabel
                }.clickable(onClick = onClick),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(AnimeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(62.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AnimeRadius.control))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f),
                                ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subject.title.take(1),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
            ) {
                Text(
                    text = subject.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subject.collectionLabel ?: subject.metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DiscoverLoading() {
    Column(
        modifier = Modifier.testTag("discover.loading"),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxl),
    ) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
                Box(
                    Modifier
                        .padding(horizontal = AnimeSpacing.lg)
                        .width(96.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(AnimeRadius.round))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AnimeSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                ) {
                    items(count = 5, key = { index -> "skeleton:$index" }, contentType = { "poster-card" }) {
                        Box(
                            Modifier
                                .width(132.dp)
                                .height(198.dp)
                                .clip(RoundedCornerShape(AnimeRadius.card))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverStatusPanel(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    tag: String,
) {
    AnimeGlassPanel(
        role = GlassRole.FloatingPanel,
        modifier = Modifier.padding(horizontal = AnimeSpacing.lg).fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(AnimeSpacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).clip(CircleShape).clickable(onClick = onAction),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = AnimeSpacing.xl)) {
                    Text(action, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun errorTitle(error: DiscoverErrorUi): String =
    stringResource(
        when (error) {
            DiscoverErrorUi.Offline -> Res.string.discover_error_offline
            DiscoverErrorUi.RateLimited -> Res.string.discover_error_rate_limited
            DiscoverErrorUi.ServiceUnavailable -> Res.string.discover_error_service
            DiscoverErrorUi.InvalidData -> Res.string.discover_error_data
            DiscoverErrorUi.Unknown -> Res.string.discover_error_unknown
        },
    )
