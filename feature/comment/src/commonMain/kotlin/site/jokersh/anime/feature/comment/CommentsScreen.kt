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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimeCopy
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.animeString
import site.jokersh.anime.core.model.*
import site.jokersh.anime.data.comment.CommentRepository

@Composable
public fun CommentsScreen(
    subjectId: Long,
    initialSort: CommentSort,
    repository: CommentRepository,
    canWrite: Boolean,
    onLoginClick: () -> Unit,
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
    var reportTarget by remember { mutableStateOf<Comment?>(null) }
    var editTarget by remember { mutableStateOf<Comment?>(null) }
    var editBody by remember { mutableStateOf("") }
    var editSpoiler by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf<String?>(null) }
    var reportDetails by remember { mutableStateOf("") }
    var hasMore by remember(id, sort) { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val publishedMessage = animeString(AnimeCopy.commentPublished)
    val lengthErrorMessage = animeString(AnimeCopy.commentLengthError)
    val publishFailedMessage = animeString(AnimeCopy.commentPublishFailed)
    val reportSubmittedMessage = animeString(AnimeCopy.commentReportSubmitted)
    val reportFailedMessage = animeString(AnimeCopy.commentReportFailed)
    val updatedMessage = animeString(AnimeCopy.commentUpdated)
    val updateFailedMessage = animeString(AnimeCopy.commentUpdateFailed)

    LaunchedEffect(id, sort) {
        repository.loadNext(id, sort).onSuccess { page -> hasMore = page.hasMore }
    }
    LaunchedEffect(text, spoiler, parentId) {
        delay(DRAFT_SAVE_DELAY_MILLIS)
        if (text.isNotBlank() || parentId != null) {
            repository.saveDraft(id, parentId, text, spoiler)
        } else {
            repository.deleteDraft(id)
        }
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
                    Text(animeString(AnimeCopy.screenComment), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(animeString(AnimeCopy.screenCommentDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SortButton(animeString(AnimeCopy.commentSortNewest), sort == CommentSort.Newest) { sort = CommentSort.Newest }
                SortButton(animeString(AnimeCopy.commentSortOldest), sort == CommentSort.Oldest) { sort = CommentSort.Oldest }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .3f)),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    parentId?.let { Text(animeString(AnimeCopy.commentReplying, it.value.take(8)), color = MaterialTheme.colorScheme.primary) }
                    if (!canWrite) {
                        Text(animeString(AnimeCopy.commentLogin), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(300) },
                        enabled = canWrite,
                        readOnly = !canWrite,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (text.isBlank()) {
                                    Text(
                                        animeString(AnimeCopy.commentPlaceholder),
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
                        Switch(enabled = canWrite, checked = spoiler, onCheckedChange = { spoiler = it })
                        Text(animeString(AnimeCopy.commentSpoiler), Modifier.weight(1f))
                        if (canWrite) {
                            if (parentId != null) AnimeSecondaryButton(animeString(AnimeCopy.actionCancelReply), { parentId = null })
                            AnimePrimaryButton(animeString(AnimeCopy.actionPublish), {
                                scope.launch {
                                    when (repository.create(id, parentId, text, spoiler)) {
                                        is MutationResult.Accepted -> {
                                            text = ""
                                            spoiler = false
                                            parentId = null
                                            message =
                                                publishedMessage
                                        }

                                        is MutationResult.Rejected -> {
                                            message = lengthErrorMessage
                                        }

                                        else -> {
                                            message = publishFailedMessage
                                        }
                                    }
                                }
                            })
                        } else {
                            AnimePrimaryButton(animeString(AnimeCopy.commentLoginAction), onLoginClick)
                        }
                    }
                    message?.let {
                        Text(
                            it,
                            color =
                                if (it == publishedMessage) {
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
        } else if (state.value.isNullOrEmpty() && state.error != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(animeString(AnimeCopy.commentLoadFailed), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimeSecondaryButton(
                        label = animeString(AnimeCopy.actionRetry),
                        onClick = {
                            scope.launch {
                                repository.loadNext(id, sort).onSuccess { page -> hasMore = page.hasMore }
                            }
                        },
                    )
                }
            }
        } else if (state.value.isNullOrEmpty()) {
            item {
                Text(
                    animeString(AnimeCopy.commentEmpty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 48.dp),
                )
            }
        } else {
            items(state.value.orEmpty(), key = { it.id.value }) { comment ->
                CommentCard(
                    comment,
                    canWrite = canWrite,
                    onLoginClick = onLoginClick,
                    onReply = { parentId = comment.id },
                    onDelete = { scope.launch { repository.delete(comment.id) } },
                    onEdit = {
                        editTarget = comment
                        editBody = comment.body
                        editSpoiler = comment.spoiler
                    },
                    onReport = {
                        reportTarget = comment
                        reportReason = null
                        reportDetails = ""
                    },
                )
            }
            if (hasMore) {
                item {
                    AnimeSecondaryButton(if (state.refreshing) animeString(AnimeCopy.stateLoading) else animeString(AnimeCopy.actionLoadMore), {
                        if (!state.refreshing) {
                            scope.launch {
                                repository.loadNext(id, sort).onSuccess { page -> hasMore = page.hasMore }
                            }
                        }
                    })
                }
            }
        }
    }

    reportTarget?.let { comment ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text(animeString(AnimeCopy.commentReportTitle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(animeString(AnimeCopy.commentReportDescription))
                    listOf(
                        "spam" to animeString(AnimeCopy.commentReportSpam),
                        "harassment" to animeString(AnimeCopy.commentReportHarassment),
                        "spoiler" to animeString(AnimeCopy.commentReportSpoiler),
                        "illegal" to animeString(AnimeCopy.commentReportIllegal),
                        "other" to animeString(AnimeCopy.commentReportOther),
                    ).forEach { (code, label) ->
                        TextButton(
                            onClick = { reportReason = code },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (reportReason == code) "✓ $label" else label,
                                color =
                                    if (reportReason == code) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }
                    if (reportReason != null) {
                        BasicTextField(
                            value = reportDetails,
                            onValueChange = { reportDetails = it.take(300) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                ) {
                                    Box(Modifier.padding(12.dp)) {
                                        if (reportDetails.isBlank()) {
                                            Text(
                                                animeString(AnimeCopy.commentReportDetails),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        inner()
                                    }
                                }
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { reportTarget = null }) { Text(animeString(AnimeCopy.actionCancel)) } },
            dismissButton = {
                TextButton(
                    enabled = reportReason != null,
                    onClick = {
                        val reason = reportReason ?: return@TextButton
                        reportTarget = null
                        reportReason = null
                        val details = reportDetails.trim().takeIf(String::isNotBlank)
                        reportDetails = ""
                        scope.launch {
                            message =
                                if (repository.report(comment.id, reason, details) is MutationResult.Accepted) {
                                    reportSubmittedMessage
                                } else {
                                    reportFailedMessage
                                }
                        }
                    },
                ) { Text(animeString(AnimeCopy.commentSubmitReport)) }
            },
        )
    }
    editTarget?.let { comment ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(animeString(AnimeCopy.commentEditTitle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editBody,
                        onValueChange = { editBody = it.take(300) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        label = { Text(animeString(AnimeCopy.commentContent)) },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = editSpoiler, onCheckedChange = { editSpoiler = it })
                        Text(animeString(AnimeCopy.commentSpoiler))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.update(comment.id, editBody, editSpoiler)
                            .onSuccess { editTarget = null; message = updatedMessage }
                            .onFailure { message = updateFailedMessage }
                    }
                }) { Text(animeString(AnimeCopy.actionSave)) }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text(animeString(AnimeCopy.actionCancel)) } },
        )
    }
}

private const val DRAFT_SAVE_DELAY_MILLIS: Long = 350

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
    canWrite: Boolean,
    onLoginClick: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onReport: () -> Unit,
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
                    animeString(AnimeCopy.commentReplyTo, it.value.take(8)),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (comment.spoiler && !revealSpoiler) {
                AnimeSecondaryButton(animeString(AnimeCopy.commentRevealSpoiler), { revealSpoiler = true })
            } else {
                Text(comment.body, style = MaterialTheme.typography.bodyLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AnimeSecondaryButton(animeString(AnimeCopy.actionReply), if (canWrite) onReply else onLoginClick)
                if (comment.ownership == Ownership.Self) AnimeSecondaryButton(animeString(AnimeCopy.actionEdit), if (canWrite) onEdit else onLoginClick)
                if (comment.ownership == Ownership.Self) AnimeSecondaryButton(animeString(AnimeCopy.commentDelete), if (canWrite) onDelete else onLoginClick)
                if (comment.ownership == Ownership.Other) AnimeSecondaryButton(animeString(AnimeCopy.actionReport), if (canWrite) onReport else onLoginClick)
            }
        }
    }
}
