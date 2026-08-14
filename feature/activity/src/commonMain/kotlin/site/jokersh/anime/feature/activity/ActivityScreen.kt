package site.jokersh.anime.feature.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.data.comment.CommunityActivity
import site.jokersh.anime.data.comment.CommunityListSummary
import site.jokersh.anime.data.comment.CommunityRepository

@Composable
public fun ActivityScreen(
    repository: CommunityRepository,
    onSubjectClick: (Long) -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFeed by rememberSaveable { mutableStateOf("全站") }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var feed by remember { mutableStateOf(emptyList<CommunityActivity>()) }
    var lists by remember { mutableStateOf(emptyList<CommunityListSummary>()) }
    var reload by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(repository, selectedFeed, reload) {
        loading = true
        error = null
        repository.feed().onSuccess { feed = it }.onFailure { error = it.message }
        repository.lists(5).onSuccess { lists = it }
        loading = false
    }
    val visibleFeed = if (selectedFeed == "讨论") feed.filter { it.kind == "commented" } else feed

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 1040.dp
        val compact = maxWidth < 600.dp
        val horizontalPadding = if (compact) 16.dp else 32.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = horizontalPadding,
                    top = 20.dp,
                    end = horizontalPadding,
                    bottom = 112.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "社区",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("动态", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text("来自 Anime 社区的真实评分、评价、片单与讨论。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("全站", "讨论").forEach { label ->
                                Surface(
                                    onClick = { selectedFeed = label },
                                    shape = CircleShape,
                                    color =
                                        if (label ==
                                            selectedFeed
                                        ) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        },
                                ) {
                                    Text(
                                        label,
                                        Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                        color =
                                            if (label ==
                                                selectedFeed
                                            ) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "社区",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("动态", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text("来自 Anime 社区的真实评分、评价、片单与讨论。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("全站", "讨论").forEach { label ->
                                Surface(
                                    onClick = { selectedFeed = label },
                                    shape = CircleShape,
                                    color =
                                        if (label ==
                                            selectedFeed
                                        ) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        },
                                ) {
                                    Text(
                                        label,
                                        Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                                        color =
                                            if (label ==
                                                selectedFeed
                                            ) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (loading) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            } else if (error != null) {
                item { EmptyPanel("暂时无法加载社区动态", error.orEmpty(), "重试") { reload++ } }
            } else if (visibleFeed.isEmpty()) {
                item { EmptyPanel("这里还没有内容", "完成一次评分、评价或讨论后，动态会出现在这里。", null, {}) }
            } else if (wide) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            visibleFeed.forEach { FeedCard(it, onSubjectClick, onReviewClick, onListClick) }
                        }
                        Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DiscussionPanel(feed, onSubjectClick)
                            ListPanel(lists, onListClick)
                        }
                    }
                }
            } else {
                items(visibleFeed.size) { FeedCard(visibleFeed[it], onSubjectClick, onReviewClick, onListClick) }
                item { DiscussionPanel(feed, onSubjectClick) }
                item { ListPanel(lists, onListClick) }
            }
        }
    }
}

@Composable
private fun FeedCard(
    item: CommunityActivity,
    onSubjectClick: (Long) -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    Modifier.size(42.dp),
                    CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                ) {
                    if (item.actorAvatarUrl !=
                        null
                    ) {
                        AsyncImage(
                            item.actorAvatarUrl,
                            null,
                            Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                item.actorName.take(1),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(item.actorName, fontWeight = FontWeight.SemiBold)
                    Text(
                        actionLabel(item.kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    item.occurredAt.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                item.posterUrl?.let {
                    AsyncImage(
                        it,
                        null,
                        Modifier.width(72.dp).height(100.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.subjectTitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        item.summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.subjectId?.let { AnimeSecondaryButton("查看作品", { onSubjectClick(it) }) }
                        item.reviewId?.let { AnimeSecondaryButton("阅读评价", { onReviewClick(it) }) }
                        item.listId?.let { AnimeSecondaryButton("查看片单", { onListClick(it) }) }
                    }
                }
            }
        }
    }
}

private fun actionLabel(kind: String): String =
    when (kind) {
        "rated" -> "记录了评分"
        "reviewed" -> "发表了评价"
        "commented" -> "参与了讨论"
        "collected" -> "更新了收藏"
        "created_list" -> "创建了片单"
        else -> "更新了动态"
    }

@Composable private fun DiscussionPanel(
    feed: List<CommunityActivity>,
    onSubjectClick: (Long) -> Unit,
) {
    SidePanel("最近讨论") {
        val items =
            feed
                .filter {
                    it.kind ==
                        "commented"
                }.take(3)
        ; if (items.isEmpty()) {
            Text("还没有作品讨论", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEachIndexed { index, item ->
                Surface(
                    onClick = { item.subjectId?.let(onSubjectClick) },
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    Text(item.subjectTitle ?: item.summary, Modifier.fillMaxWidth().padding(vertical = 6.dp))
                }
                ; if (index <
                    items.lastIndex
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                }
            }
        }
    }
}

@Composable private fun ListPanel(
    lists: List<CommunityListSummary>,
    onListClick: (String) -> Unit,
) {
    SidePanel("最新片单") {
        if (lists.isEmpty()) {
            Text(
                "还没有公开片单",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            lists.take(3).forEach { item ->
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.itemCount} 部作品 · ${item.ownerName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                AnimeSecondaryButton("查看片单", { onListClick(item.id) })
            }
        }
    }
}

@Composable private fun SidePanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable private fun EmptyPanel(
    title: String,
    detail: String,
    action: String?,
    onAction: () -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action?.let { AnimeSecondaryButton(it, onAction) }
        }
    }
}
