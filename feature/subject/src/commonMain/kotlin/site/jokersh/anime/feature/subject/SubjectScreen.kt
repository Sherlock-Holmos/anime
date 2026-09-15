package site.jokersh.anime.feature.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimePosterArtwork
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeRatingBadge
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.AnimeStatePane
import site.jokersh.anime.core.designsystem.BangumiRatingUi
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.StatePaneKind
import site.jokersh.anime.core.designsystem.StatePaneModel
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.SubjectType
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
import site.jokersh.anime.feature.subject.generated.resources.*

@Composable
public fun SubjectScreen(
    state: SubjectUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCollect: () -> Unit,
    onEpisodesClick: () -> Unit,
    onCharactersClick: () -> Unit,
    onRelationsClick: () -> Unit,
    onRatingClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.content != null -> {
                SubjectContent(
                    state,
                    state.content,
                    onBack,
                    onCollect,
                    onEpisodesClick,
                    onCharactersClick,
                    onRelationsClick,
                    onRatingClick,
                    onCommentsClick,
                    onReviewsClick,
                    onReviewClick,
                    onListClick,
                )
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
    state: SubjectUiState,
    content: SubjectContentUi,
    onBack: () -> Unit,
    onCollect: () -> Unit,
    onEpisodesClick: () -> Unit,
    onCharactersClick: () -> Unit,
    onRelationsClick: () -> Unit,
    onRatingClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        val compact = maxWidth < 520.dp
        Column(
            modifier =
                Modifier
                    .widthIn(max = AnimeSize.contentMax)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .testTag("subject.screen.${content.id}")
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = if (compact) AnimeSpacing.lg else AnimeSpacing.xxl)
                    .padding(top = AnimeSpacing.lg, bottom = AnimeSpacing.huge),
            verticalArrangement = Arrangement.spacedBy(if (compact) AnimeSpacing.xl else AnimeSpacing.xxl),
        ) {
            SubjectHero(
                state = state,
                content = content,
                wide = wide,
                compact = compact,
                onBack = onBack,
                onCollect = onCollect,
            )
            RatingOverview(content, compact)
            Surface(
                shape = RoundedCornerShape(AnimeRadius.round),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            ) {
                Text(
                    content.dataStatusLabel(),
                    modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.xs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TagRow(content.tags)
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.xl),
                    verticalAlignment = Alignment.Top,
                ) {
                    DetailSection(
                        title = stringResource(Res.string.subject_summary),
                        modifier = Modifier.weight(1.45f),
                    ) {
                        SummaryText(content.summary)
                    }
                    DetailSection(
                        title = stringResource(Res.string.subject_information),
                        modifier = Modifier.weight(1f),
                    ) {
                        SubjectInformation(content, onEpisodesClick, onCharactersClick, onRelationsClick)
                    }
                }
            } else {
                DetailSection(stringResource(Res.string.subject_summary)) {
                    SummaryText(content.summary)
                }
                DetailSection(stringResource(Res.string.subject_information)) {
                    SubjectInformation(content, onEpisodesClick, onCharactersClick, onRelationsClick)
                }
            }
            CommunityPreview(state, onRatingClick, onCommentsClick, onReviewsClick, onReviewClick, onListClick, wide)
        }
    }
}

@Composable
private fun SubjectHero(
    state: SubjectUiState,
    content: SubjectContentUi,
    wide: Boolean,
    compact: Boolean,
    onBack: () -> Unit,
    onCollect: () -> Unit,
) {
    val heroHeight = when {
        wide -> 326.dp
        compact -> 360.dp
        else -> 342.dp
    }
    AnimeGlassPanel(
        role = GlassRole.StaticHero,
        modifier = Modifier.fillMaxWidth().testTag("subject.hero"),
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            AnimePosterArtwork(
                poster = content.poster,
                title = content.title,
                id = SubjectId(content.id),
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.16f; scaleY = 1.16f }.blur(26.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF08111F).copy(alpha = 0.28f),
                            Color(0xFF08111F).copy(alpha = 0.5f),
                            Color(0xFF08111F).copy(alpha = 0.96f),
                        ),
                    ),
                ),
            )
            BackButton(
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(AnimeSpacing.lg),
                darkContent = true,
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(AnimeSpacing.xl),
                horizontalArrangement = Arrangement.spacedBy(if (compact) AnimeSpacing.md else AnimeSpacing.xl),
                verticalAlignment = Alignment.Bottom,
            ) {
                AnimePosterArtwork(
                    poster = content.poster,
                    title = content.title,
                    id = SubjectId(content.id),
                    modifier =
                        Modifier
                            .width(if (wide) 150.dp else 108.dp)
                            .height(if (wide) 216.dp else 156.dp)
                            .clip(RoundedCornerShape(AnimeRadius.control)),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                ) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (compact) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    content.originalTitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = content.metadataLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.84f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        content.score?.let { score ->
                            Surface(shape = RoundedCornerShape(AnimeRadius.round), color = Color.White.copy(alpha = 0.18f)) {
                                Text(
                                    text = stringResource(Res.string.subject_score, score),
                                    modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.xs),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(AnimeRadius.round), color = Color.White.copy(alpha = 0.14f)) {
                            Text(
                                text = content.statusLabel(),
                                modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.xs),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    AnimePrimaryButton(
                        label = state.collectionStatus.collectionLabel(),
                        onClick = onCollect,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.actionMessage?.let { message ->
                        Text(
                            text = message,
                            color = Color.White.copy(alpha = 0.86f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun String?.collectionLabel(): String =
    stringResource(
        when (this) {
            "watching" -> Res.string.subject_collection_watching
            "completed" -> Res.string.subject_collection_completed
            "onhold" -> Res.string.subject_collection_on_hold
            "dropped" -> Res.string.subject_collection_dropped
            else -> Res.string.subject_collection_wish
        },
    )

@Composable
private fun SubjectContentUi.metadataLabel(): String =
    stringResource(
        Res.string.subject_metadata,
        year?.toString() ?: "—",
        stringResource(type.resource()),
        stringResource(airingStatus.resource()),
    )

@Composable
private fun SubjectContentUi.statusLabel(): String = stringResource(airingStatus.resource())

@Composable
private fun SubjectContentUi.dataStatusLabel(): String =
    stringResource(
        when (freshness) {
            FreshnessKind.OfflineCache -> Res.string.subject_fresh_offline
            FreshnessKind.Stale -> Res.string.subject_fresh_stale
            else -> Res.string.subject_fresh_remote
        },
        dataUpdatedAt,
    )

private fun SubjectType.resource(): StringResource =
    when (this) {
        SubjectType.Tv -> Res.string.subject_type_tv
        SubjectType.Web -> Res.string.subject_type_web
        SubjectType.Ova -> Res.string.subject_type_ova
        SubjectType.Movie -> Res.string.subject_type_movie
        SubjectType.Other -> Res.string.subject_type_other
    }

private fun AiringStatus.resource(): StringResource =
    when (this) {
        AiringStatus.Announced -> Res.string.subject_airing_announced
        AiringStatus.Airing -> Res.string.subject_airing_airing
        AiringStatus.Finished -> Res.string.subject_airing_finished
        AiringStatus.Unknown -> Res.string.subject_airing_unknown
    }

@Composable
private fun RatingOverview(
    content: SubjectContentUi,
    compact: Boolean,
) {
    val distributionParts = mutableListOf<String>()
    for (entry in content.ratingDistribution.entries.sortedByDescending { it.key }.take(3)) {
        distributionParts += stringResource(Res.string.subject_rating_distribution, entry.key, entry.value)
    }
    val distribution = distributionParts.joinToString(" / ")
    val source = stringResource(Res.string.subject_bangumi_rating)
    val detail =
        if (content.score == null) {
            stringResource(Res.string.subject_no_rating)
        } else {
            stringResource(
                Res.string.subject_rating_detail,
                content.votes,
                distribution,
            )
        }
    val rating: @Composable (Modifier) -> Unit = { modifier ->
        RatingSourceCard(
            source = source,
            score = content.score ?: "—",
            detail = detail,
            highlighted = true,
            modifier = modifier,
        )
    }
    if (compact) {
        rating(Modifier.fillMaxWidth())
    } else {
        rating(Modifier.fillMaxWidth())
    }
}

@Composable
private fun RatingSourceCard(
    source: String,
    score: String,
    detail: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AnimeRadius.card),
        color =
            if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.1f,
                )
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            },
        border =
            androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f),
            ),
    ) {
        Row(Modifier.padding(AnimeSpacing.lg), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    source,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                score,
                style = MaterialTheme.typography.headlineMedium,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CommunityPreview(
    state: SubjectUiState,
    onRatingClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    wide: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        Text(stringResource(Res.string.subject_community), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        val reviews: @Composable (Modifier) -> Unit = { modifier ->
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(AnimeRadius.card),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Column(Modifier.padding(AnimeSpacing.xl), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                    Text(
                        stringResource(Res.string.subject_review_discussion),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val review = state.reviews.firstOrNull()
                    val comment = state.comments.firstOrNull()
                    when {
                        state.communityError != null -> {
                            Text(stringResource(Res.string.subject_community_unavailable), color = MaterialTheme.colorScheme.error)
                            Text(
                                stringResource(Res.string.subject_community_unavailable_message),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        state.communityLoading -> {
                            Text(stringResource(Res.string.subject_community_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        review !=
                            null -> {
                            Text(
                                review.title ?: stringResource(Res.string.subject_latest_review),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                review.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            AnimeSecondaryButton(stringResource(Res.string.subject_read_review), { onReviewClick(review.id) })
                        }

                        comment !=
                            null -> {
                            Text(stringResource(Res.string.subject_comment_title, comment.authorName), fontWeight = FontWeight.Bold)
                            Text(comment.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        else -> {
                            Text(stringResource(Res.string.subject_no_reviews_action), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                        AnimeSecondaryButton(stringResource(Res.string.subject_all_reviews), onReviewsClick)
                        AnimeSecondaryButton(stringResource(Res.string.subject_write_review), onRatingClick)
                        AnimeSecondaryButton(stringResource(Res.string.subject_join_discussion), onCommentsClick)
                    }
                }
            }
        }
        val lists: @Composable (Modifier) -> Unit = { modifier ->
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(AnimeRadius.card),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Column(Modifier.padding(AnimeSpacing.xl), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                    Text(
                        stringResource(Res.string.subject_community_lists),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    val list = state.lists.firstOrNull()
                    if (list ==
                        null
                    ) {
                        Text(stringResource(Res.string.subject_no_public_lists), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimeSecondaryButton(stringResource(Res.string.subject_create_list), { onListClick("new") })
                    } else {
                        Text(list.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(Res.string.subject_list_summary, list.itemCount, list.ownerName),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AnimeSecondaryButton(stringResource(Res.string.subject_view_list), { onListClick(list.id) })
                    }
                }
            }
        }
        if (wide) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                reviews(Modifier.weight(1.2f))
                lists(Modifier.weight(0.8f))
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
                reviews(Modifier.fillMaxWidth())
                lists(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SummaryText(summary: String) {
    Text(
        text = summary,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SubjectInformation(
    content: SubjectContentUi,
    onEpisodesClick: () -> Unit,
    onCharactersClick: () -> Unit,
    onRelationsClick: () -> Unit,
) {
    InformationRow(
        stringResource(Res.string.subject_episodes),
        content.episodeCount?.toString() ?: stringResource(Res.string.subject_episode_unknown),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    InformationRow(stringResource(Res.string.subject_status), content.statusLabel())
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    InformationRow(
        stringResource(Res.string.subject_source),
        stringResource(Res.string.subject_bangumi),
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
    ) {
        AnimeSecondaryButton(stringResource(Res.string.subject_type_episodes), onEpisodesClick)
        AnimeSecondaryButton(stringResource(Res.string.subject_type_characters), onCharactersClick)
        AnimeSecondaryButton(stringResource(Res.string.subject_type_relations), onRelationsClick)
    }
}

@Composable
private fun BackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    darkContent: Boolean = false,
) {
    val label = stringResource(Res.string.subject_back)
    Surface(
        onClick = onBack,
        shape = CircleShape,
        color = if (darkContent) Color.Black.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier =
            Modifier
                .then(modifier)
                .size(48.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimeBackIcon(
                color = if (darkContent) Color.White else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TagRow(tags: List<String>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
    ) {
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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(AnimeRadius.round))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AnimeRadius.card),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 1.dp,
        ) {
            Box(Modifier.padding(AnimeSpacing.xl)) {
                Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) { content() }
            }
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
