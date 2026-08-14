@file:Suppress("ktlint:standard:no-wildcard-imports")

package site.jokersh.anime.feature.comment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.model.*
import site.jokersh.anime.data.comment.CommentRepository

@Composable
public fun CommentsScreen(
    subjectId: Long,
    initialSort: CommentSort,
    repository: CommentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val id = remember(subjectId) { SubjectId(subjectId) }
    var sort by remember { mutableStateOf(initialSort) }
    val state by repository.observeComments(id, sort).collectAsState(ResourceState(null, null, false, null))
    val draft by repository.observeDraft(id).collectAsState(null)
    var text by remember(draft) { mutableStateOf(draft?.text.orEmpty()) }
    var spoiler by remember(draft) { mutableStateOf(draft?.spoiler ?: false) }
    var parentId by remember(draft) { mutableStateOf(draft?.parentId) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id, sort) { repository.loadNext(id, sort) }
    LaunchedEffect(text, spoiler, parentId) {
        if (text.isNotEmpty() || parentId != null) repository.saveDraft(id, parentId, text, spoiler)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 20.dp, 24.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) { AnimeBackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp)) }
                }
                Column(Modifier.weight(1f)) {
                    Text("评价与讨论", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("围绕作品展开讨论；剧透内容默认收起。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SortButton("最新", sort == CommentSort.Newest) { sort = CommentSort.Newest }
                SortButton("最早", sort == CommentSort.Oldest) { sort = CommentSort.Oldest }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f)),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    parentId?.let { Text("正在回复 · ${it.value.take(8)}", color = MaterialTheme.colorScheme.primary) }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(300) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (text.isBlank()) {
                                    Text(
                                        "写下你的想法（最多 300 字）",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Switch(checked = spoiler, onCheckedChange = { spoiler = it })
                        Text("包含剧透", Modifier.weight(1f))
                        if (parentId != null) AnimeSecondaryButton("取消回复", { parentId = null })
                        AnimePrimaryButton("发布", {
                            scope.launch {
                                when (repository.create(id, parentId, text, spoiler)) {
                                    is MutationResult.Accepted -> {
                                        text = ""
                                        spoiler = false
                                        parentId = null
                                        message =
                                            "已发布"
                                    }

                                    is MutationResult.Rejected -> {
                                        message = "内容需要 1–300 个字符"
                                    }

                                    else -> {
                                        message = "发布失败，草稿已保留"
                                    }
                                }
                            }
                        })
                    }
                    message?.let {
                        Text(
                            it,
                            color =
                                if (it ==
                                    "已发布"
                                ) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                        )
                    }
                }
            }
        }
        if (state.value.isNullOrEmpty() && state.refreshing) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        } else if (state.value.isNullOrEmpty()) {
            item {
                Text(
                    "还没有讨论，来写下第一条记录。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 48.dp),
                )
            }
        } else {
            items(state.value.orEmpty(), key = { it.id.value }) { comment ->
                CommentCard(comment, onReply = { parentId = comment.id }, onDelete = {
                    scope.launch { repository.delete(comment.id) }
                })
            }
            item {
                AnimeSecondaryButton(if (state.refreshing) "加载中" else "加载更多", {
                    if (!state.refreshing) scope.launch { repository.loadNext(id, sort) }
                })
            }
        }
    }
}

@Composable
private fun SortButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(
                    alpha = .16f,
                )
            } else {
                MaterialTheme.colorScheme.surface
            },
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CommentCard(
    comment: Comment,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    var revealSpoiler by remember(comment.id) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.author.displayName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    comment.createdAt
                        .toString()
                        .take(16)
                        .replace('T', ' '),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            comment.parentId?.let {
                Text(
                    "回复 ${it.value.take(8)}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (comment.spoiler && !revealSpoiler) {
                AnimeSecondaryButton("显示剧透内容", { revealSpoiler = true })
            } else {
                Text(comment.body, style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimeSecondaryButton("回复", onReply)
                if (comment.ownership == Ownership.Self) AnimeSecondaryButton("删除", onDelete)
            }
        }
    }
}
