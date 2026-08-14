package site.jokersh.anime.feature.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimePosterArtwork
import site.jokersh.anime.core.designsystem.AnimePosterCard
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSectionHeader
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.feature.discover.generated.resources.Res
import site.jokersh.anime.feature.discover.generated.resources.discover_brand
import site.jokersh.anime.feature.discover.generated.resources.discover_empty_action
import site.jokersh.anime.feature.discover.generated.resources.discover_empty_body
import site.jokersh.anime.feature.discover.generated.resources.discover_empty_title
import site.jokersh.anime.feature.discover.generated.resources.discover_error_action
import site.jokersh.anime.feature.discover.generated.resources.discover_error_data
import site.jokersh.anime.feature.discover.generated.resources.discover_error_offline
import site.jokersh.anime.feature.discover.generated.resources.discover_error_rate_limited
import site.jokersh.anime.feature.discover.generated.resources.discover_error_service
import site.jokersh.anime.feature.discover.generated.resources.discover_error_unknown
import site.jokersh.anime.feature.discover.generated.resources.discover_hero_eyebrow
import site.jokersh.anime.feature.discover.generated.resources.discover_offline
import site.jokersh.anime.feature.discover.generated.resources.discover_refresh
import site.jokersh.anime.feature.discover.generated.resources.discover_see_all
import site.jokersh.anime.feature.discover.generated.resources.discover_title

@Composable
public fun DiscoverScreen(
    state: DiscoverUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSubjectClick: (SubjectId) -> Unit,
    onSeeAll: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val desktopLayout = maxWidth >= 560.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("discover.list"),
            contentPadding =
                PaddingValues(
                    top = AnimeSpacing.lg,
                    bottom = if (desktopLayout) AnimeSpacing.giant else 132.dp,
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    if (desktopLayout) AnimeSpacing.xl else AnimeSpacing.xxl,
                ),
        ) {
            item(key = "header", contentType = "header") {
                DiscoverHeader(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    desktopLayout = desktopLayout,
                )
            }
            if (state.isOffline) {
                item(key = "offline", contentType = "status-banner") {
                    OfflineBanner(lastUpdatedLabel = state.lastUpdatedLabel)
                }
            }
            when (val content = state.content) {
                AsyncContent.Initial,
                AsyncContent.Loading,
                -> {
                    item(key = "loading", contentType = "loading") { DiscoverLoading() }
                }

                AsyncContent.Empty -> {
                    item(key = "empty", contentType = "empty") {
                        DiscoverStatusPanel(
                            title = stringResource(Res.string.discover_empty_title),
                            body = stringResource(Res.string.discover_empty_body),
                            action = stringResource(Res.string.discover_empty_action),
                            onAction = onRetry,
                            tag = "discover.empty",
                        )
                    }
                }

                is AsyncContent.Failure -> {
                    item(key = "error", contentType = "error") {
                        DiscoverStatusPanel(
                            title = errorTitle(content.error),
                            body = stringResource(Res.string.discover_error_unknown),
                            action = stringResource(Res.string.discover_error_action),
                            onAction = onRetry,
                            tag = "discover.error",
                        )
                    }
                }

                is AsyncContent.Content -> {
                    item(key = "hero", contentType = "hero") {
                        DiscoverHeroCarousel(
                            subjects = content.value.heroes,
                            onSubjectClick = onSubjectClick,
                            desktopLayout = desktopLayout,
                        )
                    }
                    items(
                        items = content.value.sections,
                        key = { section -> "section:${section.id}" },
                        contentType = { "section" },
                    ) { section ->
                        DiscoverSection(
                            section = section,
                            onSubjectClick = onSubjectClick,
                            onSeeAll = { onSeeAll(section.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverHeader(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    desktopLayout: Boolean,
) {
    Column(
        modifier = Modifier.padding(horizontal = AnimeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.discover_brand),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.discover_title),
                style =
                    if (desktopLayout) {
                        MaterialTheme.typography.headlineLarge
                    } else {
                        MaterialTheme.typography.displayLarge
                    },
                fontWeight = FontWeight.Bold,
            )
            AnimePrimaryButton(
                label = stringResource(Res.string.discover_refresh),
                onClick = onRefresh,
                loading = isRefreshing,
                enabled = !isRefreshing,
                modifier = Modifier.testTag("discover.refresh"),
            )
        }
    }
}

@Composable
private fun OfflineBanner(lastUpdatedLabel: String?) {
    AnimeGlassPanel(
        role = GlassRole.FloatingPanel,
        modifier = Modifier.padding(horizontal = AnimeSpacing.lg).fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.round),
        contentPadding = PaddingValues(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.md),
    ) {
        Text(
            text =
                if (lastUpdatedLabel == null) {
                    stringResource(Res.string.discover_offline)
                } else {
                    "${stringResource(Res.string.discover_offline)} · $lastUpdatedLabel"
                },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiscoverHeroCarousel(
    subjects: List<SubjectCardUi>,
    onSubjectClick: (SubjectId) -> Unit,
    desktopLayout: Boolean,
) {
    val pagerState = rememberPagerState(pageCount = subjects::size)
    val scope = rememberCoroutineScope()
    val carouselHeight = if (desktopLayout) 216.dp else 250.dp
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress, subjects.size) {
        if (subjects.size > 1 && !pagerState.isScrollInProgress) {
            delay(6_000)
            pagerState.animateScrollToPage(
                if (pagerState.currentPage == subjects.lastIndex) 0 else pagerState.currentPage + 1,
            )
        }
    }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(carouselHeight)
                .testTag("discover.hero.carousel"),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> subjects[page].id.value },
        ) { page ->
            val subject = subjects[page]
            DiscoverHero(
                subject = subject,
                onClick = { onSubjectClick(subject.id) },
                desktopLayout = desktopLayout,
            )
        }
        if (subjects.size > 1) {
            CarouselArrow(
                label = "上一部推荐",
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            if (pagerState.currentPage == 0) subjects.lastIndex else pagerState.currentPage - 1,
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = AnimeSpacing.xl),
            )
            CarouselArrow(
                label = "下一部推荐",
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            if (pagerState.currentPage == subjects.lastIndex) 0 else pagerState.currentPage + 1,
                        )
                    }
                },
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = AnimeSpacing.xl)
                        .rotate(180f),
            )
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AnimeSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
            ) {
                subjects.indices.forEach { page ->
                    val selected = pagerState.currentPage == page
                    Box(
                        Modifier
                            .width(if (selected) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color.White else Color.White.copy(alpha = 0.42f),
                            ).clickable {
                                scope.launch { pagerState.animateScrollToPage(page) }
                            }.semantics {
                                role = Role.Button
                                contentDescription = "第 ${page + 1} 部推荐"
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselArrow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .size(42.dp)
                .semantics { contentDescription = label },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        shadowElevation = 8.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimeBackIcon(
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DiscoverHero(
    subject: SubjectCardUi,
    onClick: () -> Unit,
    desktopLayout: Boolean,
) {
    val gradients =
        listOf(
            listOf(Color(0xFF0A4B9F), Color(0xFF4A49A3), Color(0xFF8E4E9E)),
            listOf(Color(0xFF006B5E), Color(0xFF257A75), Color(0xFF5A5BAA)),
            listOf(Color(0xFF71411F), Color(0xFF8A4960), Color(0xFF604C9D)),
            listOf(Color(0xFF1C4F74), Color(0xFF3D6294), Color(0xFF725394)),
            listOf(Color(0xFF51407E), Color(0xFF77528C), Color(0xFFA35D79)),
        )
    Box(
        modifier =
            Modifier
                .padding(horizontal = AnimeSpacing.lg)
                .fillMaxWidth()
                .height(if (desktopLayout) 216.dp else 250.dp)
                .clip(RoundedCornerShape(AnimeRadius.panel))
                .background(
                    Brush.linearGradient(
                        gradients[(subject.id.value % gradients.size).toInt()],
                    ),
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = subject.accessibilityLabel
                }.clickable(onClick = onClick),
    ) {
        AnimePosterArtwork(
            poster = subject.poster,
            title = subject.title,
            id = subject.id,
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }.blur(18.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF071018).copy(alpha = 0.94f),
                            Color(0xFF071018).copy(alpha = 0.7f),
                            Color(0xFF071018).copy(alpha = 0.32f),
                        ),
                    ),
                ),
        )
        AnimePosterArtwork(
            poster = subject.poster,
            title = subject.title,
            id = subject.id,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        top = AnimeSpacing.md,
                        end = if (desktopLayout) 72.dp else 58.dp,
                        bottom = AnimeSpacing.md,
                    ).width(if (desktopLayout) 122.dp else 96.dp)
                    .fillMaxHeight()
                    .shadow(12.dp, RoundedCornerShape(AnimeRadius.card))
                    .clip(RoundedCornerShape(AnimeRadius.card)),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth(if (desktopLayout) 0.7f else 0.72f)
                    .padding(start = 72.dp, end = AnimeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.discover_hero_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subject.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subject.originalTitle ?: subject.metadata,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subject.rating?.let { rating ->
                Surface(
                    shape = RoundedCornerShape(AnimeRadius.round),
                    color = Color.Black.copy(alpha = 0.36f),
                ) {
                    Text(
                        text = rating,
                        modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.xs),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverSection(
    section: DiscoverSectionUi,
    onSubjectClick: (SubjectId) -> Unit,
    onSeeAll: () -> Unit,
) {
    Column(
        modifier = Modifier.testTag("discover.section.${section.id}"),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
    ) {
        AnimeSectionHeader(
            title = section.title,
            description = section.description,
            actionLabel = stringResource(Res.string.discover_see_all),
            onAction = onSeeAll,
            modifier = Modifier.padding(horizontal = AnimeSpacing.lg),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = AnimeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            items(
                items = section.subjects,
                key = { subject -> "subject:${subject.id.value}" },
                contentType = { if (section.id == "continue") "progress-card" else "poster-card" },
            ) { subject ->
                if (section.id == "continue") {
                    ContinueWatchingCard(
                        subject = subject,
                        onClick = { onSubjectClick(subject.id) },
                        modifier = Modifier.testTag("discover.subject.${subject.id.value}"),
                    )
                } else {
                    AnimePosterCard(
                        model = subject,
                        onClick = { onSubjectClick(subject.id) },
                        modifier = Modifier.testTag("discover.subject.${subject.id.value}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    subject: SubjectCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .width(264.dp)
                .height(112.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = subject.accessibilityLabel
                },
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(AnimeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(62.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AnimeRadius.control))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f),
                                ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = subject.title.take(1),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
            ) {
                Text(
                    text = subject.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subject.collectionLabel ?: subject.metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DiscoverLoading() {
    Column(
        modifier = Modifier.testTag("discover.loading"),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxl),
    ) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
                Box(
                    Modifier
                        .padding(horizontal = AnimeSpacing.lg)
                        .width(96.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(AnimeRadius.round))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = AnimeSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                ) {
                    items(count = 5, key = { index -> "skeleton:$index" }, contentType = { "poster-card" }) {
                        Box(
                            Modifier
                                .width(132.dp)
                                .height(198.dp)
                                .clip(RoundedCornerShape(AnimeRadius.card))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverStatusPanel(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    tag: String,
) {
    AnimeGlassPanel(
        role = GlassRole.FloatingPanel,
        modifier = Modifier.padding(horizontal = AnimeSpacing.lg).fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(AnimeSpacing.xl),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimePrimaryButton(label = action, onClick = onAction)
        }
    }
}

@Composable
private fun errorTitle(error: DiscoverErrorUi): String =
    stringResource(
        when (error) {
            DiscoverErrorUi.Offline -> Res.string.discover_error_offline
            DiscoverErrorUi.RateLimited -> Res.string.discover_error_rate_limited
            DiscoverErrorUi.ServiceUnavailable -> Res.string.discover_error_service
            DiscoverErrorUi.InvalidData -> Res.string.discover_error_data
            DiscoverErrorUi.Unknown -> Res.string.discover_error_unknown
        },
    )
