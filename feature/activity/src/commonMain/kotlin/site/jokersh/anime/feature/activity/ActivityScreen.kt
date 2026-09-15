package site.jokersh.anime.feature.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import site.jokersh.anime.core.designsystem.AnimeCopy
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.animeString
import site.jokersh.anime.data.comment.CommunityActivity
import site.jokersh.anime.data.comment.CommunityListSummary
import site.jokersh.anime.data.comment.CommunityNotification
import site.jokersh.anime.data.comment.CommunityRepository

private const val MODE_FEED = "feed"
private const val MODE_NOTIFICATIONS = "notifications"
private const val FEED_FOLLOWING = "following"
private const val FEED_POPULAR = "popular"
private const val FEED_ALL = "all"
private const val FEED_DISCUSSION = "discussion"

@Composable
public fun ActivityScreen(
    repository: CommunityRepository,
    initialFeed: String = "following",
    onSubjectClick: (Long) -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onUserClick: (String) -> Unit = {},
    onCreateList: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentUnderSystemBars: Boolean = false,
) {
    var selectedFeed by rememberSaveable { mutableStateOf(initialFeedLabel(initialFeed)) }
    var selectedMode by rememberSaveable { mutableStateOf(MODE_FEED) }
    var loading by rememberSaveable { mutableStateOf(true) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var feed by remember { mutableStateOf(emptyList<CommunityActivity>()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var lists by remember { mutableStateOf(emptyList<CommunityListSummary>()) }
    var notifications by remember { mutableStateOf(emptyList<CommunityNotification>()) }
    var reload by rememberSaveable { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(repository, selectedMode, selectedFeed, reload) {
        loading = true
        error = null
        if (selectedMode == MODE_NOTIFICATIONS) {
            repository.notifications().onSuccess { notifications = it }.onFailure { error = it.message }
        } else {
            repository
                .feedPage(feed = selectedFeedWire(selectedFeed), limit = 20)
                .onSuccess {
                    feed = it.items
                    nextCursor = it.nextCursor
                }.onFailure { error = it.message }
            repository.lists(5).onSuccess { lists = it }
        }
        loading = false
    }
    val visibleFeed = if (selectedFeed == FEED_DISCUSSION) feed.filter { it.kind == "commented" } else feed

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
                Column(
                    modifier = if (contentUnderSystemBars) Modifier.statusBarsPadding() else Modifier,
                ) {
                    if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                animeString(AnimeCopy.screenCommunity),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(animeString(AnimeCopy.rootActivity), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text(animeString(AnimeCopy.screenActivityDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ActivityModeFilters(selectedMode) { selectedMode = it }
                        if (selectedMode == MODE_FEED) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(FEED_FOLLOWING, FEED_POPULAR, FEED_ALL, FEED_DISCUSSION).forEach { label ->
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
                                            animeString(feedResource(label)),
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
                    } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                animeString(AnimeCopy.screenCommunity),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(animeString(AnimeCopy.rootActivity), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                            Text(animeString(AnimeCopy.screenActivityDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActivityModeFilters(selectedMode) { selectedMode = it }
                            if (selectedMode == MODE_FEED) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(FEED_FOLLOWING, FEED_POPULAR, FEED_ALL, FEED_DISCUSSION).forEach { label ->
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
                                                animeString(feedResource(label)),
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
                item {
                    EmptyPanel(
                        if (selectedMode ==
                            MODE_NOTIFICATIONS
                        ) {
                            animeString(AnimeCopy.activityNotificationsUnavailable)
                        } else {
                            animeString(AnimeCopy.activityFeedUnavailable)
                        },
                        error.orEmpty(),
                        animeString(AnimeCopy.actionRetry),
                    ) { reload++ }
                }
            } else if (selectedMode == MODE_NOTIFICATIONS) {
                item { NotificationPanel(notifications, repository, onSubjectClick, onReviewClick, onListClick, onCommentsClick) }
            } else if (visibleFeed.isEmpty()) {
                item {
                    EmptyPanel(
                        animeString(AnimeCopy.stateNoContent),
                        animeString(AnimeCopy.activityEmptyDescription),
                        null,
                        {},
                    )
                }
            } else if (wide) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1.55f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            visibleFeed.forEach { FeedCard(it, repository, onSubjectClick, onReviewClick, onListClick, onUserClick) }
                            if (nextCursor != null) {
                                AnimeSecondaryButton(if (loading) animeString(AnimeCopy.stateLoading) else animeString(AnimeCopy.actionLoadMore), onClick = {
                                    if (!loading && nextCursor != null) {
                                        scope.launch {
                                            loading = true
                                            repository
                                                .feedPage(feed = selectedFeedWire(selectedFeed), limit = 20, cursor = nextCursor)
                                                .onSuccess {
                                                    feed = (feed + it.items).distinctBy { item -> item.id }
                                                    nextCursor = it.nextCursor
                                                }.onFailure { error = it.message }
                                            loading = false
                                        }
                                    }
                                })
                            }
                        }
                        Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            DiscussionPanel(feed, onSubjectClick)
                            ListPanel(lists, onListClick, onCreateList)
                        }
                    }
                }
            } else {
                items(
                    visibleFeed.size,
                ) { FeedCard(visibleFeed[it], repository, onSubjectClick, onReviewClick, onListClick, onUserClick) }
                if (nextCursor != null) {
                    item { AnimeSecondaryButton(if (loading) animeString(AnimeCopy.stateLoading) else animeString(AnimeCopy.actionLoadMore), onClick = {
                        if (!loading && nextCursor != null) {
                            scope.launch {
                                loading = true
                                repository
                                    .feedPage(feed = selectedFeedWire(selectedFeed), limit = 20, cursor = nextCursor)
                                    .onSuccess {
                                        feed = (feed + it.items).distinctBy { item -> item.id }
                                        nextCursor = it.nextCursor
                                    }.onFailure { error = it.message }
                                loading = false
                            }
                        }
                    }) }
                }
                item { DiscussionPanel(feed, onSubjectClick) }
                item { ListPanel(lists, onListClick, onCreateList) }
            }
        }
    }
}

@Composable
private fun FeedCard(
    item: CommunityActivity,
    repository: CommunityRepository,
    onSubjectClick: (Long) -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
) {
    var following by remember(item.actorId) { mutableStateOf(false) }
    var followMessage by remember(item.actorId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val actionFailedLabel = animeString(AnimeCopy.activityActionFailed)
    LaunchedEffect(item.actorId) {
        item.actorId?.let { actorId -> repository.followUserStatus(actorId).onSuccess { following = it } }
    }
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
                    Text(
                        item.actorName,
                        fontWeight = FontWeight.SemiBold,
                        modifier = item.actorId?.let { Modifier.clickable { onUserClick(it) } } ?: Modifier,
                    )
                    Text(
                        actionLabel(item.kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.actorId?.let { actorId ->
                    AnimeSecondaryButton(
                        if (following) animeString(AnimeCopy.activityFollowing) else animeString(AnimeCopy.actionFollow),
                        onClick = {
                            scope.launch {
                                val result =
                                    if (following) {
                                        repository.unfollowUser(
                                            actorId,
                                        )
                                    } else {
                                        repository.followUser(actorId)
                                    }
                                result.onSuccess { following = it }.onFailure { followMessage = actionFailedLabel }
                            }
                        },
                    )
                }
                Text(
                    item.occurredAt.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            followMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
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
                        item.subjectId?.let { AnimeSecondaryButton(animeString(AnimeCopy.actionViewSubject), { onSubjectClick(it) }) }
                        item.reviewId?.let { AnimeSecondaryButton(animeString(AnimeCopy.actionViewReview), { onReviewClick(it) }) }
                        item.listId?.let { AnimeSecondaryButton(animeString(AnimeCopy.actionViewList), { onListClick(it) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityModeFilters(
    selected: String,
    onSelected: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(MODE_FEED to AnimeCopy.activityModeFeed, MODE_NOTIFICATIONS to AnimeCopy.activityModeNotifications).forEach { (mode, resource) ->
            Surface(
                onClick = { onSelected(mode) },
                shape = CircleShape,
                color =
                    if (mode ==
                        selected
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
                    },
                contentColor =
                    if (mode ==
                        selected
                    ) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            ) {
                Text(animeString(resource), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun NotificationPanel(
    notifications: List<CommunityNotification>,
    repository: CommunityRepository,
    onSubjectClick: (Long) -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    onCommentsClick: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    if (notifications.isEmpty()) {
        EmptyPanel(animeString(AnimeCopy.stateNoNotifications), animeString(AnimeCopy.activityNotificationsDescription), null, {})
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        notifications.forEach { notification ->
            val unread = notification.readAt == null
            Surface(
                onClick = {
                    if (unread) {
                        // Fire-and-forget is intentional: navigation should not wait for a best-effort read receipt.
                        scope.launch { repository.markNotificationRead(notification.id) }
                    }
                    val subjectId = notification.subjectId
                    val listId = notification.listId
                    val reviewId = notification.reviewId
                    when {
                        reviewId != null -> onReviewClick(reviewId)
                        notification.commentId != null && subjectId != null -> onCommentsClick(subjectId)
                        listId != null -> onListClick(listId)
                        subjectId != null -> onSubjectClick(subjectId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color =
                    if (unread) {
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = .08f,
                        )
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surface
                    },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment =
                        Alignment.Top,
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (unread) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(notificationLabel(notification), fontWeight = FontWeight.SemiBold)
                        notification.actorName?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text(
                            notification.createdAt.take(16).replace('T', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun notificationLabel(notification: CommunityNotification): String =
    when (notification.kind) {
        "comment_reply" -> animeString(AnimeCopy.notificationCommentReply)
        "user_follow" -> animeString(AnimeCopy.notificationUserFollow)
        "list_follow" -> animeString(AnimeCopy.notificationListFollow)
        else -> animeString(AnimeCopy.notificationDefault)
    }

@Composable
private fun actionLabel(kind: String): String =
    when (kind) {
        "rated" -> animeString(AnimeCopy.activityRecordedRating)
        "reviewed" -> animeString(AnimeCopy.activityPublishedReview)
        "commented" -> animeString(AnimeCopy.activityJoinedDiscussion)
        "collected" -> animeString(AnimeCopy.activityUpdatedCollection)
        "created_list" -> animeString(AnimeCopy.activityCreatedList)
        else -> animeString(AnimeCopy.activityUpdated)
    }

private fun initialFeedLabel(feed: String): String =
    when (feed) {
        "popular" -> FEED_POPULAR
        "public" -> FEED_ALL
        else -> FEED_FOLLOWING
    }

private fun selectedFeedWire(feed: String): String =
    when (feed) {
        FEED_POPULAR -> "popular"
        FEED_ALL, FEED_DISCUSSION -> "public"
        else -> "following"
    }

private fun feedResource(feed: String) = when (feed) {
    FEED_POPULAR -> AnimeCopy.activityFeedPopular
    FEED_ALL -> AnimeCopy.activityFeedAll
    FEED_DISCUSSION -> AnimeCopy.activityFeedDiscussion
    else -> AnimeCopy.activityFeedFollowing
}

@Composable private fun DiscussionPanel(
    feed: List<CommunityActivity>,
    onSubjectClick: (Long) -> Unit,
) {
    SidePanel(animeString(AnimeCopy.activityRecentDiscussions)) {
        val items =
            feed
                .filter {
                    it.kind ==
                        "commented"
                }.take(3)
        ; if (items.isEmpty()) {
            Text(animeString(AnimeCopy.activityNoDiscussions), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onCreateList: () -> Unit,
) {
    SidePanel(animeString(AnimeCopy.activityLatestLists)) {
        AnimeSecondaryButton(animeString(AnimeCopy.activityNewList), onCreateList)
        if (lists.isEmpty()) {
            Text(
                animeString(AnimeCopy.stateNoPublicLists),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            lists.take(3).forEach { item ->
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    animeString(AnimeCopy.activityUpdatedListItem, item.itemCount, item.ownerName),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                AnimeSecondaryButton(animeString(AnimeCopy.actionViewList), { onListClick(item.id) })
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
