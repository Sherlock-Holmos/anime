package site.jokersh.anime.feature.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimeCopy
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.animeString
import site.jokersh.anime.data.comment.CommunityListDetail
import site.jokersh.anime.data.comment.CommunityListSummary
import site.jokersh.anime.data.comment.CommunityRepository
import site.jokersh.anime.data.comment.CommunityReview

@Composable
public fun SubjectReviewsScreen(
    repository: CommunityRepository,
    subjectId: Long,
    currentUserId: String?,
    onBack: () -> Unit,
    onReviewClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var reviews by remember(subjectId) { mutableStateOf(emptyList<CommunityReview>()) }
    var nextCursor by remember(subjectId) { mutableStateOf<String?>(null) }
    var oldestFirst by rememberSaveable(subjectId) { mutableStateOf(false) }
    var loading by remember(subjectId) { mutableStateOf(false) }
    var initialized by remember(subjectId) { mutableStateOf(false) }
    var error by rememberSaveable(subjectId) { mutableStateOf<String?>(null) }
    var loadSignal by remember(subjectId) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(subjectId, oldestFirst, loadSignal) {
        if (loading) return@LaunchedEffect
        loading = true
        error = null
        val cursor = if (loadSignal == 0) null else nextCursor
        repository
            .reviewPage(subjectId, 20, cursor, oldestFirst)
            .onSuccess { page ->
                reviews = if (cursor == null) page.items else (reviews + page.items).distinctBy { it.id }
                nextCursor = page.nextCursor
                initialized = true
            }.onFailure { error = readableError(it) }
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 20.dp, 24.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PageBack(onBack)
                Column(Modifier.weight(1f)) {
                    Text(animeString(AnimeCopy.screenReview), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(animeString(AnimeCopy.screenReviewDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SelectChip(animeString(AnimeCopy.commentSortNewest), !oldestFirst) {
                    if (oldestFirst) {
                        oldestFirst = false
                        reviews = emptyList()
                        nextCursor =
                            null
                        loadSignal = 0
                    }
                }
                SelectChip(animeString(AnimeCopy.commentSortOldest), oldestFirst) {
                    if (!oldestFirst) {
                        oldestFirst = true
                        reviews = emptyList()
                        nextCursor =
                            null
                        loadSignal = 0
                    }
                }
            }
        }
        if (!initialized && loading) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        } else if (error != null && reviews.isEmpty()) {
            item { StatePanel(animeString(AnimeCopy.communityReviewLoadFailed), error.orEmpty()) }
        } else if (reviews.isEmpty()) {
            item { StatePanel(animeString(AnimeCopy.stateNoReviews), animeString(AnimeCopy.communityReviewEmptyDescription)) }
        } else {
            items(reviews, key = { it.id }) { review ->
                ReviewListCard(
                    review = review,
                    owned = review.owned || currentUserId == review.authorId,
                    repository = repository,
                    onOpen = { onReviewClick(review.id) },
                    onDelete = {
                        scope.launch {
                            repository
                                .deleteReview(review.id)
                                .onSuccess { reviews = reviews.filterNot { it.id == review.id } }
                                .onFailure { error = readableError(it) }
                        }
                    },
                )
            }
            if (nextCursor != null) {
                item { AnimeSecondaryButton(if (loading) animeString(AnimeCopy.stateLoading) else animeString(AnimeCopy.actionLoadMore), { if (!loading) loadSignal++ }) }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
private fun ReviewListCard(
    review: CommunityReview,
    owned: Boolean,
    repository: CommunityRepository,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    var spoilerVisible by remember(review.id) { mutableStateOf(!review.spoiler) }
    var liked by remember(review.id) { mutableStateOf(false) }
    var bookmarked by remember(review.id) { mutableStateOf(false) }
    var likeCount by remember(review.id) { mutableStateOf(review.likeCount) }
    var bookmarkCount by remember(review.id) { mutableStateOf(review.bookmarkCount) }
    val scope = rememberCoroutineScope()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    review.title ?: animeString(AnimeCopy.communityReviewDefaultTitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    review.createdAt.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (spoilerVisible) {
                Text(
                    review.body,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(animeString(AnimeCopy.communitySpoilerCollapsed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (review.spoiler) {
                    AnimeSecondaryButton(if (spoilerVisible) animeString(AnimeCopy.communityHideSpoiler) else animeString(AnimeCopy.communityShowSpoiler), {
                        spoilerVisible =
                            !spoilerVisible
                    })
                }
                AnimeSecondaryButton(animeString(AnimeCopy.actionViewDetails), onOpen)
                if (owned) AnimeSecondaryButton(animeString(AnimeCopy.actionDelete), onDelete)
                AnimeSecondaryButton(if (liked) animeString(AnimeCopy.communityLiked) else animeString(AnimeCopy.communityLike), onClick = {
                    scope.launch {
                        repository.reactReview(review.id, "like", !liked).onSuccess {
                            liked = it.active
                            likeCount = it.likeCount
                            bookmarkCount = it.bookmarkCount
                        }
                    }
                })
                AnimeSecondaryButton(if (bookmarked) animeString(AnimeCopy.communityBookmarked) else animeString(AnimeCopy.communityBookmark), onClick = {
                    scope.launch {
                        repository.reactReview(review.id, "bookmark", !bookmarked).onSuccess {
                            bookmarked = it.active
                            likeCount = it.likeCount
                            bookmarkCount = it.bookmarkCount
                        }
                    }
                })
                Text(animeString(AnimeCopy.communityReactions, likeCount, bookmarkCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
public fun RatingEditorScreen(
    repository: CommunityRepository,
    subjectId: Long,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var score by rememberSaveable { mutableStateOf(8) }
    var tag by rememberSaveable { mutableStateOf("rewatch") }
    var visibility by rememberSaveable { mutableStateOf("public") }
    var kind by rememberSaveable { mutableStateOf("short") }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var spoiler by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saveSignal by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(saveSignal) {
        if (saveSignal == 0) return@LaunchedEffect
        saving = true
        error = null
        val result =
            repository.saveRating(subjectId, score, setOf(tag), visibility).fold(
                onSuccess = {
                    when {
                        body.isBlank() -> {
                            Result.success(Unit)
                        }

                        kind == "comment" -> {
                            repository.createComment(subjectId, body.trim(), spoiler).map { Unit }
                        }

                        else -> {
                            repository
                                .createReview(
                                    subjectId,
                                    kind,
                                    title.trim().takeIf {
                                        kind == "long"
                                    },
                                    body.trim(),
                                    spoiler,
                                    visibility,
                                ).map { Unit }
                        }
                    }
                },
                onFailure = { Result.failure(it) },
            )
        saving = false
        result.onSuccess { onSaved() }.onFailure { error = readableError(it) }
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 20.dp, 16.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { PageBack(onBack) }
        item {
            Column(Modifier.widthIn(max = 820.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    animeString(AnimeCopy.communityRatingRecord),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(animeString(AnimeCopy.communityRatingTitle), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(animeString(AnimeCopy.communityRatingDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ContentPanel(Modifier.widthIn(max = 820.dp)) {
                Text(animeString(AnimeCopy.communityMyRating, score.toDouble()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (1..10).forEach { value -> SelectChip(value.toString(), score == value) { score = value } }
                }
                Text(animeString(AnimeCopy.communityTags), fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "rewatch" to animeString(AnimeCopy.communityTagRewatch),
                        "art" to animeString(AnimeCopy.communityTagArt),
                        "atmosphere" to animeString(AnimeCopy.communityTagAtmosphere),
                        "slow" to animeString(AnimeCopy.communityTagSlow),
                    ).forEach { (value, label) ->
                        SelectChip(
                            label,
                            tag == value,
                        ) { tag = value }
                    }
                }
                Text(animeString(AnimeCopy.communityExtraContent), fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "short" to animeString(AnimeCopy.communityKindReview),
                        "comment" to animeString(AnimeCopy.communityKindComment),
                    ).forEach { (value, label) ->
                        SelectChip(
                            label,
                            kind == value,
                        ) { kind = value }
                    }
                }
                if (kind == "long") EditorField(title, { title = it }, animeString(AnimeCopy.communityLongTitle), singleLine = true)
                EditorField(
                    body,
                    { body = it },
                    when (kind) {
                        "long" -> animeString(AnimeCopy.communityLongBody)
                        "comment" -> animeString(AnimeCopy.communityCommentBody)
                        else -> animeString(AnimeCopy.communityReviewBody)
                    },
                    singleLine = false,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = spoiler, onCheckedChange = { spoiler = it })
                    Text(animeString(AnimeCopy.commentSpoiler))
                }
                Text(animeString(AnimeCopy.communityVisibility), fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "public" to animeString(AnimeCopy.communityPublic),
                        "followers" to animeString(AnimeCopy.communityFollowers),
                        "private" to animeString(AnimeCopy.communityPrivate),
                    ).forEach { (value, label) ->
                        SelectChip(
                            label,
                            visibility == value,
                        ) { visibility = value }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimePrimaryButton(if (saving) animeString(AnimeCopy.communitySaving) else animeString(AnimeCopy.actionSave), { if (!saving) saveSignal++ })
                    AnimeSecondaryButton(animeString(AnimeCopy.actionCancel), onBack)
                    if (saving) CircularProgressIndicator(Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
public fun ReviewDetailScreen(
    repository: CommunityRepository,
    reviewId: String,
    onBack: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var review by remember { mutableStateOf<CommunityReview?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var liked by remember(reviewId) { mutableStateOf(false) }
    var bookmarked by remember(reviewId) { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var mutationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val reviewUpdatedMessage = animeString(AnimeCopy.communityReviewUpdated)
    LaunchedEffect(repository, reviewId) {
        repository.review(reviewId).onSuccess { review = it }.onFailure {
            error =
                readableError(it)
        }
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(32.dp, 24.dp, 32.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { PageBack(onBack) }
        item {
            when {
                error != null -> {
                    StatePanel(animeString(AnimeCopy.communityReviewLoadError), error.orEmpty())
                }

                review == null -> {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    ReviewArticle(
                        review = review!!,
                        onSubjectClick = onSubjectClick,
                        liked = liked,
                        bookmarked = bookmarked,
                        onLike = {
                            scope.launch {
                                repository.reactReview(review!!.id, "like", !liked)
                                    .onSuccess { reaction ->
                                        liked = reaction.active
                                        review = review!!.copy(likeCount = reaction.likeCount, bookmarkCount = reaction.bookmarkCount)
                                    }.onFailure { mutationMessage = readableError(it) }
                            }
                        },
                        onBookmark = {
                            scope.launch {
                                repository.reactReview(review!!.id, "bookmark", !bookmarked)
                                    .onSuccess { reaction ->
                                        bookmarked = reaction.active
                                        review = review!!.copy(likeCount = reaction.likeCount, bookmarkCount = reaction.bookmarkCount)
                                    }.onFailure { mutationMessage = readableError(it) }
                            }
                        },
                        onEdit = { editing = true },
                    )
                }
            }
        }
    }
    if (editing && review != null) {
        var title by remember(review!!.id) { mutableStateOf(review!!.title.orEmpty()) }
        var body by remember(review!!.id) { mutableStateOf(review!!.body) }
        var spoiler by remember(review!!.id) { mutableStateOf(review!!.spoiler) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(animeString(AnimeCopy.communityEditReview)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text(animeString(AnimeCopy.communityTitle)) }, singleLine = true)
                    OutlinedTextField(body, { body = it.take(500) }, label = { Text(animeString(AnimeCopy.communityBody)) }, minLines = 5)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(spoiler, { spoiler = it })
                        Text(animeString(AnimeCopy.commentSpoiler))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.updateReview(review!!.id, title.trim().ifBlank { null }, body.trim(), spoiler, review!!.visibility)
                            .onSuccess { review = it; editing = false; mutationMessage = reviewUpdatedMessage }
                            .onFailure { mutationMessage = readableError(it) }
                    }
                }) { Text(animeString(AnimeCopy.actionSave)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(animeString(AnimeCopy.actionCancel)) } },
        )
    }
    mutationMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp)) }
}

@Composable
public fun CuratedListScreen(
    repository: CommunityRepository,
    listId: String,
    onBack: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (listId == "new") {
        CreateListScreen(repository, onBack, modifier)
        return
    }
    var detail by remember { mutableStateOf<CommunityListDetail?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var following by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var deleteConfirm by rememberSaveable { mutableStateOf(false) }
    var mutationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listUpdatedMessage = animeString(AnimeCopy.communityListUpdated)
    LaunchedEffect(repository, listId) {
        repository.list(listId).onSuccess {
            detail = it
            following = it.summary.following
        }.onFailure {
            error =
                readableError(it)
        }
    }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(32.dp, 24.dp, 32.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { PageBack(onBack) }
        item {
            when {
                error != null -> {
                    StatePanel(animeString(AnimeCopy.communityListLoadError), error.orEmpty())
                }

                detail == null -> {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(animeString(AnimeCopy.communityFeaturedList), color = MaterialTheme.colorScheme.primary)
                        Text(
                            detail!!.summary.title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            animeString(AnimeCopy.communityOwnerItemSummary, detail!!.summary.ownerName, detail!!.summary.itemCount),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(detail!!.summary.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!detail!!.summary.owned) {
                                AnimeSecondaryButton(
                                    if (following) animeString(AnimeCopy.communityFollowedList) else animeString(AnimeCopy.communityFollowList),
                                    onClick = {
                                        scope.launch {
                                            val result =
                                                if (following) repository.unfollowList(listId) else repository.followList(listId)
                                            result.onSuccess { following = it }.onFailure { mutationMessage = readableError(it) }
                                        }
                                    },
                                )
                            } else {
                                AnimeSecondaryButton(animeString(AnimeCopy.actionEdit), { editing = true })
                                AnimeSecondaryButton(animeString(AnimeCopy.actionDelete), { deleteConfirm = true })
                            }
                            Text(
                                animeString(AnimeCopy.communityFollowerCount, detail!!.summary.followerCount),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        detail?.items?.let { items ->
            items(items.size) { index ->
                val item = items[index]
                Surface(
                    onClick = {
                        onSubjectClick(item.subjectId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            18.dp,
                        ),
                    color = MaterialTheme.colorScheme.surface,
                    border =
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f),
                        ),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.posterUrl !=
                            null
                        ) {
                            AsyncImage(
                                item.posterUrl,
                                null,
                                Modifier.width(72.dp).height(100.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        ; Column(Modifier.weight(1f)) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.primary)
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            item.note?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            item.score?.toString() ?: "—",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
    if (editing && detail != null) {
        var title by remember(detail!!.summary.title) { mutableStateOf(detail!!.summary.title) }
        var description by remember(detail!!.summary.description) { mutableStateOf(detail!!.summary.description) }
        var subjectIds by remember(detail!!.items) { mutableStateOf(detail!!.items.joinToString(",") { it.subjectId.toString() }) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(animeString(AnimeCopy.communityEditList)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text(animeString(AnimeCopy.communityTitle)) }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text(animeString(AnimeCopy.communityListDescription)) }, minLines = 3)
                    OutlinedTextField(subjectIds, { subjectIds = it }, label = { Text(animeString(AnimeCopy.communitySubjectIds)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val ids = subjectIds.split(',', '，', ' ').mapNotNull { it.trim().toLongOrNull() }
                        repository
                            .updateList(listId, title.trim(), description.trim(), null, ids)
                            .onSuccess { updated ->
                                detail = detail!!.copy(summary = updated)
                                editing = false
                                mutationMessage = listUpdatedMessage
                            }.onFailure { mutationMessage = readableError(it) }
                    }
                }) { Text(animeString(AnimeCopy.actionSave)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(animeString(AnimeCopy.actionCancel)) } },
        )
    }
    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text(animeString(AnimeCopy.communityDeleteListTitle)) },
            text = { Text(animeString(AnimeCopy.communityDeleteListMessage)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deleteList(listId).onSuccess { onBack() }.onFailure { mutationMessage = readableError(it) }
                        deleteConfirm = false
                    }
                }) { Text(animeString(AnimeCopy.actionDelete)) }
            },
            dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text(animeString(AnimeCopy.actionCancel)) } },
        )
    }
    mutationMessage?.let { message ->
        if (detail != null) Text(message, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp))
    }
}

@Composable
public fun UserProfileScreen(
    repository: CommunityRepository,
    userId: String,
    onBack: () -> Unit,
    onReviewClick: (String) -> Unit,
    onListClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var profile by remember(userId) { mutableStateOf<site.jokersh.anime.data.comment.CommunityUserProfile?>(null) }
    var reviews by remember(userId) { mutableStateOf(emptyList<CommunityReview>()) }
    var lists by remember(userId) { mutableStateOf(emptyList<CommunityListSummary>()) }
    var following by remember(userId) { mutableStateOf(false) }
    var loading by remember(userId) { mutableStateOf(true) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, userId) {
        loading = true
        error = null
        repository.userProfile(userId).onSuccess {
            profile = it
            following = it.following
        }.onFailure { error = readableError(it) }
        repository.userReviews(userId, limit = 20).onSuccess { reviews = it.items }
        repository.userLists(userId, limit = 20).onSuccess { lists = it }
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 20.dp, 24.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PageBack(onBack) }
        item {
            when {
                loading && profile == null -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && profile == null -> StatePanel(animeString(AnimeCopy.communityUserLoadError), error.orEmpty())
                profile != null -> {
                    val value = profile!!
                    ContentPanel(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(Modifier.size(56.dp), CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .16f)) {
                                if (value.avatarUrl != null) {
                                    AsyncImage(value.avatarUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(value.displayName.take(1), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(value.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text(animeString(AnimeCopy.communityJoinedAt, value.createdAt.take(10)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            AnimeSecondaryButton(if (following) animeString(AnimeCopy.activityFollowing) else animeString(AnimeCopy.actionFollow), onClick = {
                                scope.launch {
                                    val result = if (following) repository.unfollowUser(userId) else repository.followUser(userId)
                                    result.onSuccess { following = it }.onFailure { error = readableError(it) }
                                }
                            })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Text(animeString(AnimeCopy.communityRatingCount, value.ratingCount))
                            Text(animeString(AnimeCopy.communityReviewCount, value.reviewCount))
                            Text(animeString(AnimeCopy.communityListCount, value.listCount))
                            Text(animeString(AnimeCopy.communityFansCount, value.followerCount))
                        }
                    }
                }
            }
        }
        item { Text(animeString(AnimeCopy.screenReview), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (reviews.isEmpty()) {
            item { Text(animeString(AnimeCopy.communityPublicReviews), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(reviews, key = { it.id }) { review ->
                Surface(
                    onClick = { onReviewClick(review.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(review.title ?: animeString(AnimeCopy.communityReviewDefaultTitle), fontWeight = FontWeight.SemiBold)
                        Text(review.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text(animeString(AnimeCopy.communityReviewLikeBookmark, review.likeCount, 0) + " · " + review.createdAt.take(16).replace('T', ' '), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { Text(animeString(AnimeCopy.communityFeaturedList), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (lists.isEmpty()) {
            item { Text(animeString(AnimeCopy.communityPublicLists), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(lists, key = { it.id }) { list ->
                Surface(
                    onClick = { onListClick(list.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(list.title, fontWeight = FontWeight.SemiBold)
                        Text(list.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(animeString(AnimeCopy.communityListItemSummary, list.itemCount, list.followerCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateListScreen(
    repository: CommunityRepository,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var ids by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var createdListTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var signal by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(signal) {
        if (signal == 0) return@LaunchedEffect
        val parsed = ids.split(',', '，', ' ').mapNotNull { it.trim().toLongOrNull() }
        repository
            .createList(title.trim(), description.trim(), parsed)
            .onSuccess {
                createdListTitle = it.title
                message = null
            }
            .onFailure { message = readableError(it) }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(32.dp, 24.dp, 32.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { PageBack(onBack) }
        item { Text(animeString(AnimeCopy.communityCreateList), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold) }
        item {
            ContentPanel(Modifier.widthIn(max = 760.dp)) {
                EditorField(title, { title = it }, animeString(AnimeCopy.communityListTitleHint), true)
                EditorField(description, { description = it }, animeString(AnimeCopy.communityListDescription), false)
                EditorField(ids, { ids = it }, animeString(AnimeCopy.communityBangumiIdsHint), true)
                createdListTitle?.let {
                    Text(animeString(AnimeCopy.communityListCreated, it), color = MaterialTheme.colorScheme.primary)
                }
                message?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                AnimePrimaryButton(animeString(AnimeCopy.actionCreate), { signal++ })
            }
        }
    }
}

@Composable
private fun ReviewArticle(
    review: CommunityReview,
    onSubjectClick: (Long) -> Unit,
    liked: Boolean,
    bookmarked: Boolean,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onEdit: () -> Unit,
) {
    ContentPanel(Modifier.widthIn(max = 900.dp)) {
        Text(review.title ?: animeString(AnimeCopy.communityReviewDefaultTitle), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            animeString(AnimeCopy.communityReviewAuthor, review.createdAt.take(16).replace('T', ' ')),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(review.body, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimeSecondaryButton(animeString(AnimeCopy.actionViewSubject), { onSubjectClick(review.subjectId) })
            AnimeSecondaryButton(if (liked) animeString(AnimeCopy.communityLiked) else animeString(AnimeCopy.communityLike), onLike)
            AnimeSecondaryButton(if (bookmarked) animeString(AnimeCopy.communityBookmarked) else animeString(AnimeCopy.communityBookmark), onBookmark)
            if (review.owned) AnimeSecondaryButton(animeString(AnimeCopy.actionEdit), onEdit)
            Text(
                animeString(AnimeCopy.communityReactions, review.likeCount, review.bookmarkCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun ContentPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f)),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
    }
}

@Composable
private fun SelectChip(
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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
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
private fun EditorField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f)),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(if (singleLine) 52.dp else 132.dp).padding(16.dp),
            singleLine = singleLine,
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box {
                    if (value.isBlank()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun PageBack(onBack: () -> Unit) {
    Surface(
        onClick = onBack,
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) { AnimeBackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(22.dp)) }
    }
}

@Composable
private fun StatePanel(
    title: String,
    detail: String,
) {
    ContentPanel {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun readableError(error: Throwable): String =
    error.message?.substringAfter("\"message\":\"")?.substringBefore('"') ?: error.message.orEmpty()
