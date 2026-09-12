package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.catalog.components.LiquidButton
import com.kyant.backdrop.catalog.components.LiquidToggle
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.SubjectId

@Immutable
public data class SubjectCardUi(
    public val id: SubjectId,
    public val title: String,
    public val originalTitle: String?,
    public val poster: ImageRef?,
    public val metadata: String,
    public val rating: String?,
    public val collectionLabel: String?,
    public val accessibilityLabel: String,
)

public enum class PosterCardSize(
    public val width: Dp,
    public val height: Dp,
) {
    Standard(AnimeSize.posterWidth, AnimeSize.posterHeight),
    Compact(AnimeSize.posterCompactWidth, AnimeSize.posterCompactHeight),
}

@Immutable
public data class BangumiRatingUi(
    public val score: String?,
    public val votesLabel: String?,
    public val updatedAtLabel: String?,
    public val sourceLabel: String,
    public val emptyLabel: String,
    public val accessibilityLabel: String,
)

public enum class StatePaneKind { Loading, Empty, Error }

@Immutable
public data class StatePaneModel(
    public val kind: StatePaneKind,
    public val title: String,
    public val message: String,
    public val actionLabel: String? = null,
)

@Composable
public fun AnimePrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interactive = enabled && !loading
    val buttonModifier = modifier.heightIn(min = 48.dp).semantics { if (!interactive) disabled() }
    val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                maxLines = 2,
                color =
                    MaterialTheme.colorScheme.onPrimary.copy(
                        alpha = if (loading) 0f else 1f,
                    ),
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AnimeSize.iconSm),
                    strokeWidth = AnimeSize.border,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
    if (LocalAnimeLiquidGlassEnabled.current && !LocalAnimeReduceMotion.current) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val canvasBackdrop = rememberCanvasBackdrop { drawRect(backgroundColor) }
        val backdrop = LocalAnimeBackdrop.current ?: canvasBackdrop
        LiquidButton(
            onClick = if (interactive) onClick else ({}),
            backdrop = backdrop,
            modifier = buttonModifier,
            isInteractive = interactive,
            tint = MaterialTheme.colorScheme.primary,
            content = { content() },
        )
    } else {
        Surface(
            onClick = if (interactive) onClick else ({}),
            enabled = interactive,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { content() }
    }
}

@Composable
public fun AnimeLiquidToggle(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalAnimeLiquidGlassEnabled.current || LocalAnimeReduceMotion.current) {
        Switch(
            checked = selected,
            onCheckedChange = onSelectedChange,
            modifier = modifier,
        )
        return
    }
    val backgroundColor = MaterialTheme.colorScheme.background
    val canvasBackdrop = rememberCanvasBackdrop { drawRect(backgroundColor) }
    val backdrop = LocalAnimeBackdrop.current ?: canvasBackdrop

    LiquidToggle(
        selected = { selected },
        onSelect = onSelectedChange,
        backdrop = backdrop,
        modifier = modifier,
    )
}

@Composable
public fun AnimeSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val buttonModifier = modifier.heightIn(min = 48.dp).semantics { if (!enabled) disabled() }
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            maxLines = 2,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (LocalAnimeLiquidGlassEnabled.current && !LocalAnimeReduceMotion.current) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val canvasBackdrop = rememberCanvasBackdrop { drawRect(backgroundColor) }
        val backdrop = LocalAnimeBackdrop.current ?: canvasBackdrop
        LiquidButton(
            onClick = if (enabled) onClick else ({}),
            backdrop = backdrop,
            modifier = buttonModifier,
            isInteractive = enabled,
            surfaceColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            content = { content() },
        )
    } else {
        Surface(
            onClick = if (enabled) onClick else ({}),
            enabled = enabled,
            modifier = buttonModifier,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
        ) { content() }
    }
}

@Composable
public fun AnimeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            AnimeSecondaryButton(label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
public fun AnimePosterCard(
    model: SubjectCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: PosterCardSize = PosterCardSize.Standard,
) {
    Card(
        onClick = onClick,
        modifier =
            modifier
                .width(size.width)
                // Keep every card in a horizontal shelf the same height. Without this,
                // one-line titles and two-line titles produce visibly uneven shelves.
                .height(size.height + 82.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = model.accessibilityLabel
                },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 1.dp),
        shape = RoundedCornerShape(AnimeRadius.card),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Poster(model = model, width = size.width, height = size.height)
        Column(
            modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.sm),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = model.rating ?: model.metadata,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                model.collectionLabel?.let { label ->
                    Surface(
                        shape = RoundedCornerShape(AnimeRadius.round),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = AnimeSpacing.sm, vertical = AnimeSpacing.xxs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
public fun AnimeCompactSubjectCard(
    model: SubjectCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = AnimeSize.posterCompactHeight)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = model.accessibilityLabel
                },
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(AnimeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            Poster(model, AnimeSize.posterCompactWidth, AnimeSize.posterCompactHeight)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
            ) {
                Text(model.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                model.originalTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(model.metadata, style = MaterialTheme.typography.labelSmall)
                model.rating?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
public fun AnimeRatingBadge(
    rating: BangumiRatingUi,
    modifier: Modifier = Modifier,
    role: GlassRole = GlassRole.FloatingPanel,
) {
    AnimeGlassPanel(
        role = role,
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = rating.accessibilityLabel
            },
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(AnimeSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = rating.score ?: "—",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(AnimeSpacing.sm))
            Column {
                Text(rating.sourceLabel, style = MaterialTheme.typography.labelLarge)
                Text(
                    rating.votesLabel ?: rating.emptyLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
public fun AnimeStatePane(
    state: StatePaneModel,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(AnimeSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
    ) {
        if (state.kind == StatePaneKind.Loading) {
            CircularProgressIndicator(modifier = Modifier.size(AnimeSize.iconLg))
        }
        Text(state.title, style = MaterialTheme.typography.titleLarge)
        Text(
            state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.actionLabel != null && onAction != null) {
            AnimePrimaryButton(label = state.actionLabel, onClick = onAction)
        }
    }
}

@Composable
private fun Poster(
    model: SubjectCardUi,
    width: Dp,
    height: Dp,
) {
    AnimePosterArtwork(
        poster = model.poster,
        title = model.title,
        id = model.id,
        modifier = Modifier.width(width).height(height),
    )
}

@Composable
public fun AnimePosterArtwork(
    poster: ImageRef?,
    title: String,
    id: SubjectId,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val remoteModel = (poster as? ImageRef.Remote)?.url
    Box(
        modifier = modifier.background(posterPlaceholderColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (remoteModel != null) {
            AsyncImage(
                model = remoteModel,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = title.take(1),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun posterPlaceholderColor(): Color = Color(0xFF293241)
