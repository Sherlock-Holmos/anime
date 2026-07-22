package site.jokersh.anime.feature.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeRatingBadge
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.AnimeStatePane
import site.jokersh.anime.core.designsystem.BangumiRatingUi
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.StatePaneKind
import site.jokersh.anime.core.designsystem.StatePaneModel
import site.jokersh.anime.feature.subject.generated.resources.Res
import site.jokersh.anime.feature.subject.generated.resources.subject_back
import site.jokersh.anime.feature.subject.generated.resources.subject_bangumi
import site.jokersh.anime.feature.subject.generated.resources.subject_episode_unknown
import site.jokersh.anime.feature.subject.generated.resources.subject_episodes
import site.jokersh.anime.feature.subject.generated.resources.subject_error
import site.jokersh.anime.feature.subject.generated.resources.subject_error_description
import site.jokersh.anime.feature.subject.generated.resources.subject_information
import site.jokersh.anime.feature.subject.generated.resources.subject_loading
import site.jokersh.anime.feature.subject.generated.resources.subject_loading_description
import site.jokersh.anime.feature.subject.generated.resources.subject_no_rating
import site.jokersh.anime.feature.subject.generated.resources.subject_rating_accessibility
import site.jokersh.anime.feature.subject.generated.resources.subject_retry
import site.jokersh.anime.feature.subject.generated.resources.subject_source
import site.jokersh.anime.feature.subject.generated.resources.subject_status
import site.jokersh.anime.feature.subject.generated.resources.subject_summary
import site.jokersh.anime.feature.subject.generated.resources.subject_votes

@Composable
public fun SubjectScreen(
    state: SubjectUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.content != null -> {
                SubjectContent(state.content, onBack)
            }

            state.loading -> {
                SubjectState(
                    title = stringResource(Res.string.subject_loading),
                    message = stringResource(Res.string.subject_loading_description),
                    kind = StatePaneKind.Loading,
                    onBack = onBack,
                )
            }

            else -> {
                SubjectState(
                    title = stringResource(Res.string.subject_error),
                    message = stringResource(Res.string.subject_error_description),
                    kind = StatePaneKind.Error,
                    actionLabel = stringResource(Res.string.subject_retry),
                    onAction = onRetry,
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun SubjectContent(
    content: SubjectContentUi,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("subject.screen.${content.id}")
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = AnimeSpacing.lg)
                .padding(top = AnimeSpacing.md, bottom = AnimeSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xl),
    ) {
        BackButton(onBack)
        Row(
            modifier = Modifier.fillMaxWidth().testTag("subject.hero"),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(112.dp)
                        .height(158.dp)
                        .clip(RoundedCornerShape(AnimeRadius.card))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f),
                                ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = content.title.take(1),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
            ) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                content.originalTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = content.metadata,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (content.score != null) {
            AnimeRatingBadge(
                rating = content.ratingUi(),
                modifier = Modifier.testTag("subject.rating"),
            )
        }
        TagRow(content.tags)
        DetailSection(stringResource(Res.string.subject_summary)) {
            Text(
                text = content.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DetailSection(stringResource(Res.string.subject_information)) {
            InformationRow(
                stringResource(Res.string.subject_episodes),
                content.episodeCount?.toString()
                    ?: stringResource(Res.string.subject_episode_unknown),
            )
            InformationRow(stringResource(Res.string.subject_status), content.status)
            InformationRow(
                stringResource(Res.string.subject_source),
                stringResource(Res.string.subject_bangumi),
            )
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    val label = stringResource(Res.string.subject_back)
    AnimeGlassPanel(
        role = GlassRole.TopBar,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier =
            Modifier
                .size(48.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                }.clickable(onClick = onBack),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimeBackIcon(
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TagRow(tags: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
        tags.take(3).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(AnimeRadius.round),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.sm),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        AnimeGlassPanel(
            role = GlassRole.FloatingPanel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) { content() }
        }
    }
}

@Composable
private fun InformationRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(AnimeSpacing.lg))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SubjectContentUi.ratingUi(): BangumiRatingUi {
    val source = stringResource(Res.string.subject_bangumi)
    val votesLabel = stringResource(Res.string.subject_votes, votes)
    val empty = stringResource(Res.string.subject_no_rating)
    return BangumiRatingUi(
        score = score,
        votesLabel = if (score == null) null else votesLabel,
        updatedAtLabel = null,
        sourceLabel = source,
        emptyLabel = empty,
        accessibilityLabel =
            if (score == null) {
                "$source，$empty"
            } else {
                stringResource(Res.string.subject_rating_accessibility, score, votes)
            },
    )
}

@Composable
private fun SubjectState(
    title: String,
    message: String,
    kind: StatePaneKind,
    onBack: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(if (kind == StatePaneKind.Error) "subject.error" else "subject.loading")
                .statusBarsPadding()
                .padding(AnimeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xl),
    ) {
        BackButton(onBack)
        AnimeStatePane(
            state = StatePaneModel(kind, title, message, actionLabel),
            onAction = onAction,
        )
    }
}
