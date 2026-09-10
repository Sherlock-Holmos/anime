package site.jokersh.anime.feature.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.animeColors
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.UserCollectionSummary
import site.jokersh.anime.data.session.SessionRepository

@Composable
public fun CollectionScreen(
    sessionState: SessionState,
    sessionRepository: SessionRepository,
    onSubjectClick: (SubjectId) -> Unit,
    onDiscoverClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by rememberSaveable { mutableStateOf<CollectionStatus?>(CollectionStatus.Watching) }
    val profile = (sessionState as? SessionState.Authenticated)?.user
    val cachedCollection = profile?.collections.orEmpty()
    var collection by remember(
        selectedFilter,
        profile?.summary?.id,
    ) { mutableStateOf(emptyList<UserCollectionSummary>()) }
    var nextCursor by remember(selectedFilter, profile?.summary?.id) { mutableStateOf<String?>(null) }
    var loading by remember(selectedFilter, profile?.summary?.id) { mutableStateOf(false) }
    var initialized by remember(selectedFilter, profile?.summary?.id) { mutableStateOf(false) }
    var message by remember(selectedFilter, profile?.summary?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(selectedFilter, profile?.summary?.id) {
        if (profile == null) return@LaunchedEffect
        loading = true
        sessionRepository
            .collectionPage(selectedFilter)
            .onSuccess { page ->
                collection = page.items
                nextCursor = page.nextCursor
                initialized = true
            }.onFailure {
                collection =
                    if (selectedFilter ==
                        null
                    ) {
                        cachedCollection
                    } else {
                        cachedCollection.filter { item -> item.status == selectedFilter }
                    }
                message = "当前显示本机缓存"
                initialized = true
            }
        loading = false
    }
    val visibleItems =
        if (initialized) {
            collection
        } else if (selectedFilter ==
            null
        ) {
            cachedCollection
        } else {
            cachedCollection.filter { it.status == selectedFilter }
        }
    val overviewItems = (cachedCollection + collection).distinctBy { it.subjectId }

    BoxWithConstraints(modifier.fillMaxSize()) {
        // The root navigation rail consumes part of the desktop window. Keep the
        // default 1180 dp window single-column and only fan out on genuinely wide canvases.
        val columns = if (maxWidth >= 760.dp) 2 else 1
        val horizontalPadding = if (maxWidth >= 600.dp) 32.dp else 16.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = horizontalPadding,
                    top = 20.dp,
                    end = horizontalPadding,
                    bottom = 112.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionHeader(itemCount = visibleItems.size, onBack = onBack)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionOverview(
                    collection = overviewItems,
                    syncedAt = profile?.syncedAt?.toString(),
                    restoring = sessionState is SessionState.Restoring,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionFilters(
                    selected = selectedFilter,
                    onSelected = { selectedFilter = it },
                )
            }
            if (sessionState is SessionState.Restoring && profile == null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimeGlassPanel(
                        role = GlassRole.FloatingPanel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AnimeRadius.panel),
                    ) {
                        Text("正在恢复片库…", style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else if (visibleItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyCollection(onDiscoverClick = onDiscoverClick)
                }
            } else {
                items(visibleItems, key = { it.subjectId.value }) { item ->
                    CollectionCard(
                        item = item,
                        onClick = { onSubjectClick(item.subjectId) },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (nextCursor != null) {
                            AnimePrimaryButton(if (loading) "加载中" else "加载更多", {
                                if (!loading) {
                                    scope.launch {
                                        loading = true
                                        sessionRepository
                                            .collectionPage(selectedFilter, nextCursor)
                                            .onSuccess { page ->
                                                collection =
                                                    (collection + page.items).distinctBy { it.subjectId }
                                                ; nextCursor =
                                                    page.nextCursor
                                            }.onFailure { message = "加载失败，请稍后重试" }
                                        loading = false
                                    }
                                }
                            }, enabled = !loading)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionHeader(
    itemCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 1_200.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            site.jokersh.anime.core.designsystem
                .AnimeSecondaryButton(label = "返回", onClick = onBack)
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                Text(
                    text = "个人片库",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(text = "收藏", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "把想看的、正在追的和看过的作品收在一处。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        ) {
            Text(
                text = "$itemCount 部作品",
                modifier = Modifier.padding(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.sm),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun CollectionOverview(
    collection: List<UserCollectionSummary>,
    syncedAt: String?,
    restoring: Boolean,
) {
    val watching = collection.count { it.status == CollectionStatus.Watching }
    val completed = collection.count { it.status == CollectionStatus.Completed }
    AnimeGlassPanel(
        role = GlassRole.StaticHero,
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(AnimeSpacing.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OverviewMetric(value = watching.toString(), label = "正在追")
            OverviewMetric(value = completed.toString(), label = "已看完")
            OverviewMetric(value = collection.size.toString(), label = "全部收藏")
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (restoring) {
                        "正在恢复"
                    } else if (syncedAt == null) {
                        "等待登录"
                    } else {
                        "已同步"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.animeColors.success,
                )
                Text(
                    syncedAt?.let { "Bangumi · ${it.replace('T', ' ').take(16)}" } ?: "登录后同步 Bangumi 片库",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OverviewMetric(
    value: String,
    label: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxs)) {
        Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CollectionFilters(
    selected: CollectionStatus?,
    onSelected: (CollectionStatus?) -> Unit,
) {
    val filters =
        listOf(
            null to "全部",
            CollectionStatus.Watching to "在看",
            CollectionStatus.Wish to "想看",
            CollectionStatus.Completed to "看过",
            CollectionStatus.OnHold to "搁置",
        )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
    ) {
        filters.forEach { (status, label) ->
            val active = selected == status
            Surface(
                onClick = { onSelected(status) },
                modifier = Modifier.heightIn(min = AnimeSize.touch),
                shape = CircleShape,
                color =
                    if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.72f,
                        )
                    },
                contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border =
                    BorderStroke(
                        AnimeSize.border,
                        MaterialTheme.colorScheme.outline.copy(alpha = if (active) 0f else 0.24f),
                    ),
            ) {
                Box(Modifier.padding(horizontal = AnimeSpacing.lg), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CollectionCard(
    item: UserCollectionSummary,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 178.dp),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        tonalElevation = AnimeSpacing.xxs,
    ) {
        Row(
            modifier = Modifier.padding(AnimeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(100.dp)
                        .height(150.dp)
                        .clip(RoundedCornerShape(AnimeRadius.control))
                        .background(Brush.linearGradient(listOf(Color(0xFF3978D4), Color(0xFF8B5CF6)))),
                contentAlignment = Alignment.Center,
            ) {
                if (item.posterUrl != null) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = "${item.title} 海报",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        item.title.take(1),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).height(150.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = item.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        StatusBadge(item.status.label)
                    }
                    Text(
                        text =
                            buildString {
                                append(item.airDate?.take(4) ?: "年份未知")
                                if (item.score > 0.0) append(" · ${item.score} Bangumi")
                                if (item.userRating > 0) append(" · 我的评分 ${item.userRating}")
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                if (item.status ==
                                    CollectionStatus.Watching
                                ) {
                                    if (item.totalEpisodes > 0) {
                                        "已看 ${item.episodeProgress} / ${item.totalEpisodes} 集"
                                    } else {
                                        "已看 ${item.episodeProgress} 集"
                                    }
                                } else {
                                    item.comment.ifBlank { item.status.detail }
                                },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    if (item.status == CollectionStatus.Watching && item.totalEpisodes > 0) {
                        LinearProgressIndicator(
                            progress = {
                                item.episodeProgress
                                    .toFloat()
                                    .div(item.totalEpisodes)
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.xs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyCollection(onDiscoverClick: () -> Unit) {
    AnimeGlassPanel(
        role = GlassRole.FloatingPanel,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(AnimeSpacing.xxxl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            Text("这个分类还是空的", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("从发现页挑一部作品加入收藏。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimePrimaryButton(label = "去发现", onClick = onDiscoverClick)
        }
    }
}

private val CollectionStatus.label: String
    get() =
        when (this) {
            CollectionStatus.Wish -> "想看"
            CollectionStatus.Watching -> "在看"
            CollectionStatus.Completed -> "看过"
            CollectionStatus.OnHold -> "搁置"
            CollectionStatus.Dropped -> "抛弃"
        }

private val CollectionStatus.detail: String
    get() =
        when (this) {
            CollectionStatus.Wish -> "等待开播"
            CollectionStatus.Completed -> "已全部看完"
            CollectionStatus.OnHold -> "暂时搁置"
            CollectionStatus.Dropped -> "已停止观看"
            CollectionStatus.Watching -> "正在观看"
        }
