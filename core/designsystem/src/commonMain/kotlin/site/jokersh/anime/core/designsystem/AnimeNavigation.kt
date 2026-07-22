package site.jokersh.anime.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
public fun AnimeLiquidTabBar(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (index: Int, selected: Boolean, tint: Color) -> Unit,
) {
    if (labels.isEmpty()) return

    val safeSelectedIndex = selectedIndex.coerceIn(labels.indices)
    val shape = RoundedCornerShape(AnimeRadius.round)
    AnimeGlassPanel(
        role = GlassRole.BottomBar,
        modifier =
            modifier.shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
            ),
        tierOverride = GlassTier.Liquid,
        shape = shape,
        contentPadding = PaddingValues(AnimeSpacing.xs),
    ) {
        LiquidTabTrack(
            labels = labels,
            selectedIndex = safeSelectedIndex,
            onSelected = onSelected,
            icon = icon,
        )
    }
}

@Composable
private fun LiquidTabTrack(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    icon: @Composable (index: Int, selected: Boolean, tint: Color) -> Unit,
) {
    val shape = RoundedCornerShape(AnimeRadius.round)
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(62.dp),
    ) {
        val density = LocalDensity.current
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val tabWidth = maxWidth / labels.size
        val tabWidthPx = with(density) { tabWidth.toPx() }
        var dragging by remember { mutableStateOf(false) }
        var targetPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
        val animatedPosition by
            animateFloatAsState(
                targetValue = targetPosition,
                animationSpec =
                    spring(
                        dampingRatio = if (dragging) 0.86f else 0.68f,
                        stiffness = if (dragging) 620f else 420f,
                    ),
            )
        val interactionProgress by
            animateFloatAsState(
                targetValue = if (dragging) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
            )

        LaunchedEffect(selectedIndex, dragging) {
            if (!dragging) targetPosition = selectedIndex.toFloat()
        }

        val dragState =
            rememberDraggableState { delta ->
                val direction = if (isRtl) -1f else 1f
                targetPosition =
                    (targetPosition + delta / tabWidthPx * direction)
                        .coerceIn(0f, labels.lastIndex.toFloat())
            }
        val activeIndex = resolveDraggedTab(animatedPosition, labels.size)

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        onDragStarted = {
                            dragging = true
                            targetPosition = animatedPosition
                        },
                        onDragStopped = {
                            val targetIndex = resolveDraggedTab(targetPosition, labels.size)
                            targetPosition = targetIndex.toFloat()
                            dragging = false
                            onSelected(targetIndex)
                        },
                    ),
        ) {
            val physicalPosition =
                if (isRtl) labels.lastIndex - animatedPosition else animatedPosition
            Box(
                modifier =
                    Modifier
                        .offset {
                            IntOffset(
                                x = (physicalPosition * tabWidthPx).roundToInt(),
                                y = 0,
                            )
                        }.width(tabWidth)
                        .fillMaxHeight()
                        .padding(AnimeSpacing.xxs)
                        .graphicsLayer {
                            val expansion = 1f + interactionProgress * 0.12f
                            scaleX = expansion
                            scaleY = 1f - interactionProgress * 0.04f
                        }.clip(shape)
                        .platformLiquidSelectionEffect(
                            shape = shape,
                            tint = MaterialTheme.colorScheme.primary,
                            interactionProgress = interactionProgress,
                        ),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                labels.forEachIndexed { index, label ->
                    LiquidTabItem(
                        label = label,
                        selected = index == activeIndex,
                        onClick = {
                            targetPosition = index.toFloat()
                            onSelected(index)
                        },
                        icon = { tint -> icon(index, index == activeIndex, tint) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.LiquidTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f)
    val foreground by
        animateColorAsState(
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

    Box(
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = 58.dp)
                .padding(horizontal = AnimeSpacing.xxs)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.semantics(mergeDescendants = true) { this.selected = selected }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = AnimeSpacing.xs),
        ) {
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon(foreground)
            }
            Text(
                text = label,
                color = foreground,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

internal fun resolveDraggedTab(
    position: Float,
    tabCount: Int,
): Int {
    require(tabCount > 0)
    return position.roundToInt().coerceIn(0, tabCount - 1)
}
