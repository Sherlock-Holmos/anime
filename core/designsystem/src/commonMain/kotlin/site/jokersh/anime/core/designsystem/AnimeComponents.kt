package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
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
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = AnimeSize.touch),
        enabled = enabled && !loading,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(AnimeSize.iconSm),
                strokeWidth = AnimeSize.border,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(AnimeSpacing.sm))
        }
        Text(label, maxLines = 2)
    }
}

@Composable
public fun AnimeSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = AnimeSize.touch),
        enabled = enabled,
    ) {
        Text(label, maxLines = 2)
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
        modifier =
            modifier
                .width(size.width)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = model.accessibilityLabel
                }.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(AnimeRadius.card),
    ) {
        Poster(model = model, width = size.width, height = size.height)
        Column(
            modifier = Modifier.padding(AnimeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
        ) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = model.rating ?: model.metadata,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        color = MaterialTheme.colorScheme.surface,
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
    val remoteModel = (model.poster as? ImageRef.Remote)?.url
    Box(
        modifier =
            Modifier
                .width(width)
                .height(height)
                .background(model.placeholderColor()),
        contentAlignment = Alignment.Center,
    ) {
        if (remoteModel != null) {
            AsyncImage(
                model = remoteModel,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(width).height(height),
            )
        } else {
            Text(
                text = model.title.take(1),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun SubjectCardUi.placeholderColor(): Color {
    val colors =
        listOf(
            Color(0xFFDDE1FF),
            Color(0xFFD7F2EF),
            Color(0xFFFFDCE8),
            Color(0xFFFFE3C2),
        )
    return colors[(id.value % colors.size).toInt()]
}
