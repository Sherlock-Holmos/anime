package site.jokersh.anime.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs

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
    val backdrop = LocalAnimeBackdrop.current
    if (backdrop == null) {
        StaticTabBarFallback(
            labels = labels,
            selectedIndex = safeSelectedIndex,
            onSelected = onSelected,
            modifier = modifier,
            icon = icon,
        )
        return
    }

    val selectedIndexState = rememberUpdatedState(safeSelectedIndex)
    val selectedIndexProvider = remember { { selectedIndexState.value } }
    LiquidBottomTabs(
        selectedTabIndex = selectedIndexProvider,
        onTabSelected = onSelected,
        backdrop = backdrop,
        tabsCount = labels.size,
        modifier =
            modifier.shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(AnimeRadius.round),
                clip = false,
            ),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == safeSelectedIndex
            LiquidBottomTab(
                onClick = { onSelected(index) },
                modifier = Modifier.semantics { this.selected = selected },
            ) {
                TabContent(
                    label = label,
                    selected = selected,
                    icon = { tint -> icon(index, selected, tint) },
                )
            }
        }
    }
}

@Composable
private fun StaticTabBarFallback(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier,
    icon: @Composable (index: Int, selected: Boolean, tint: Color) -> Unit,
) {
    AnimeGlassPanel(
        role = GlassRole.BottomBar,
        modifier = modifier,
        tierOverride = GlassTier.Translucent,
        shape = RoundedCornerShape(AnimeRadius.round),
        contentPadding = PaddingValues(AnimeSpacing.xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                LiquidBottomTab(
                    onClick = { onSelected(index) },
                    modifier = Modifier.semantics { this.selected = selected },
                ) {
                    TabContent(
                        label = label,
                        selected = selected,
                        icon = { tint -> icon(index, selected, tint) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    label: String,
    selected: Boolean,
    icon: @Composable (Color) -> Unit,
) {
    val foreground =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Column(
        modifier = Modifier.heightIn(min = 56.dp).padding(vertical = AnimeSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
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
