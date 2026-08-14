package site.jokersh.anime.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import site.jokersh.anime.core.designsystem.AnimeCompactSubjectCard
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.StatePaneKind
import site.jokersh.anime.core.designsystem.StatePaneModel
import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SubjectType

@Composable
public fun SearchScreen(
    state: SearchUiState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns =
            when {
                maxWidth >= 1_200.dp -> 3
                maxWidth >= 580.dp -> 2
                else -> 1
            }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().statusBarsPadding().testTag("search.list"),
            contentPadding = PaddingValues(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            item(
                key = "header",
                contentType = "header",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                SearchHeader(
                    query = state.query,
                    onQueryChanged = { onIntent(SearchIntent.QueryChanged(it)) },
                    onSubmit = { onIntent(SearchIntent.Submit(state.query)) },
                    onClear = { onIntent(SearchIntent.ClearQuery) },
                )
            }

            item(
                key = "filters",
                contentType = "filters",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                SearchFilters(
                    types = state.types,
                    years = state.years,
                    airing = state.airing,
                    sort = state.sort,
                    onIntent = onIntent,
                )
            }

            when (state.mode) {
                SearchMode.Idle -> {
                    item(
                        key = "recent",
                        contentType = "recent",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        RecentSearches(
                            items = state.recentQueries,
                            onClick = { query -> onIntent(SearchIntent.RecentClicked(query)) },
                            onRemove = { id -> onIntent(SearchIntent.RemoveRecent(id)) },
                            onClearAll = { onIntent(SearchIntent.ClearAllRecent) },
                        )
                    }
                    item(
                        key = "discovery",
                        contentType = "discovery",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SearchDiscovery(
                            trending = state.trending,
                            recommendations = state.recommendations,
                            personalized = state.recommendationsPersonalized,
                            loading = state.discoveryLoading,
                            onSearch = { query ->
                                onIntent(SearchIntent.QueryChanged(query))
                                onIntent(SearchIntent.Submit(query))
                            },
                        )
                    }
                }

                SearchMode.Suggesting -> {
                    items(
                        items = state.suggestions,
                        key = { suggestion -> "suggestion:${suggestion.value}" },
                        contentType = { "suggestion" },
                        span = { GridItemSpan(maxLineSpan) },
                    ) { suggestion ->
                        SearchSuggestionRow(
                            suggestion = suggestion,
                            onClick = { onIntent(SearchIntent.SuggestionClicked(suggestion.value)) },
                        )
                    }
                }

                SearchMode.Loading -> {
                    item(
                        key = "loading",
                        contentType = "state",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SearchStatePanel(
                            kind = StatePaneKind.Loading,
                            title = "正在搜索",
                            message = "从本地演示数据中匹配 Bangumi 条目",
                        )
                    }
                }

                SearchMode.Results -> {
                    item(
                        key = "summary",
                        contentType = "summary",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        ResultsSummary(query = state.resultQuery.orEmpty(), count = state.results.size)
                    }
                    items(
                        items = state.results,
                        key = { subject -> "subject:${subject.id.value}" },
                        contentType = { "subject" },
                    ) { subject ->
                        AnimeCompactSubjectCard(
                            model = subject,
                            onClick = { onIntent(SearchIntent.SubjectClicked(subject.id)) },
                            modifier = Modifier.testTag("search.subject.${subject.id.value}"),
                        )
                    }
                    item(
                        key = "more",
                        contentType = "pagination",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SearchPagination(
                            hasMore = state.nextCursor != null,
                            loading = state.isLoadingMore,
                            error = state.loadMoreError != null,
                            onLoadMore = { onIntent(SearchIntent.LoadNextPage) },
                            onRetry = { onIntent(SearchIntent.RetryNextPage) },
                        )
                    }
                }

                SearchMode.Empty -> {
                    item(
                        key = "empty",
                        contentType = "state",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SearchStatePanel(
                            kind = StatePaneKind.Empty,
                            title = "没有找到结果",
                            message = "换一个作品名、别名或 Bangumi ID 试试。",
                        )
                    }
                }

                SearchMode.BlockingError -> {
                    item(
                        key = "error",
                        contentType = "state",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        SearchStatePanel(
                            kind = StatePaneKind.Error,
                            title = "搜索暂时不可用",
                            message = "稍后重试，或先回到发现页浏览演示条目。",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilters(
    types: Set<SubjectType>,
    years: IntRange?,
    airing: Set<AiringStatus>,
    sort: SearchSort,
    onIntent: (SearchIntent) -> Unit,
) {
    val chips =
        remember(types, years, airing, sort) {
            listOf(
                "TV" to (SubjectType.Tv in types),
                "Web" to (SubjectType.Web in types),
                "剧场版" to (SubjectType.Movie in types),
                "近五年" to (years != null),
                "连载中" to (AiringStatus.Airing in airing),
                "已完结" to (AiringStatus.Finished in airing),
                "高分优先" to (sort == SearchSort.Rating),
            )
        }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("search.filters"),
        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
    ) {
        chips.forEachIndexed { index, chip ->
            val action = {
                when (index) {
                    0 -> {
                        onIntent(SearchIntent.ToggleType(SubjectType.Tv))
                    }

                    1 -> {
                        onIntent(SearchIntent.ToggleType(SubjectType.Web))
                    }

                    2 -> {
                        onIntent(SearchIntent.ToggleType(SubjectType.Movie))
                    }

                    3 -> {
                        onIntent(SearchIntent.ToggleRecentYears)
                    }

                    4 -> {
                        onIntent(SearchIntent.ToggleAiring(AiringStatus.Airing))
                    }

                    5 -> {
                        onIntent(SearchIntent.ToggleAiring(AiringStatus.Finished))
                    }

                    else -> {
                        onIntent(
                            SearchIntent.SortChanged(if (chip.second) SearchSort.Relevance else SearchSort.Rating),
                        )
                    }
                }
            }
            if (chip.second) {
                AnimePrimaryButton(label = chip.first, onClick = action)
            } else {
                AnimeSecondaryButton(label = chip.first, onClick = action)
            }
        }
    }
}

@Composable
private fun SearchDiscovery(
    trending: List<String>,
    recommendations: List<SubjectCardUi>,
    personalized: Boolean,
    loading: Boolean,
    onSearch: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wide = maxWidth >= 860.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
                verticalAlignment = Alignment.Top,
            ) {
                TrendingSearches(trending, onSearch, Modifier.weight(0.82f))
                SearchPicks(recommendations, personalized, loading, onSearch, Modifier.weight(1.18f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg)) {
                TrendingSearches(trending, onSearch)
                SearchPicks(recommendations, personalized, loading, onSearch)
            }
        }
    }
}

@Composable
private fun TrendingSearches(
    queries: List<String>,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchDiscoveryPanel(
        title = "热门搜索",
        subtitle = "从大家最近关注的作品开始。",
        modifier = modifier,
    ) {
        if (queries.isEmpty()) {
            Text("还没有足够的搜索热度数据。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        queries.forEachIndexed { index, query ->
            Surface(
                onClick = { onSearch(query) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AnimeRadius.control),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(query, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SearchPicks(
    recommendations: List<SubjectCardUi>,
    personalized: Boolean,
    loading: Boolean,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchDiscoveryPanel(
        title = "不妨试试",
        subtitle = if (personalized) "根据你的收藏兴趣生成。" else "来自真实高分与热度数据。",
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            when {
                loading -> {
                    CircularProgressIndicator()
                }

                recommendations.isEmpty() -> {
                    Text("推荐数据正在积累。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                else -> {
                    recommendations.take(3).forEach { subject ->
                        SearchPick(
                            subject = subject,
                            modifier = Modifier.weight(1f),
                            onClick = { onSearch(subject.title) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPick(
    subject: SubjectCardUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(112.dp),
                contentAlignment = Alignment.Center,
            ) {
                site.jokersh.anime.core.designsystem.AnimePosterArtwork(
                    poster = subject.poster,
                    title = subject.title,
                    id = subject.id,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.padding(AnimeSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxs),
            ) {
                Text(
                    subject.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subject.rating ?: subject.metadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchDiscoveryPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.panel),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(AnimeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        Text(
            text = "Anime",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "搜索",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                    ),
                tonalElevation = 0.dp,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .padding(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.md)
                            .testTag("search.input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isBlank()) {
                                Text(
                                    text = "作品名、别名、Bangumi ID",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            if (query.isNotBlank()) {
                PillButton(
                    label = "清除",
                    onClick = onClear,
                    modifier =
                        Modifier
                            .testTag("search.clear")
                            .semantics { contentDescription = "清空搜索内容" },
                )
            }
            PillButton(
                label = "搜索",
                onClick = onSubmit,
                modifier = Modifier.testTag("search.submit"),
            )
        }
    }
}

@Composable
private fun RecentSearches(
    items: List<SearchHistoryUi>,
    onClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    AnimeGlassPanel(
        role = GlassRole.FloatingPanel,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(AnimeSpacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("最近搜索", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (items.isNotEmpty()) {
                    AnimeSecondaryButton(
                        label = "全部清除",
                        onClick = onClearAll,
                        modifier = Modifier.testTag("search.recent.clearAll"),
                    )
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = "搜索过的作品会留在这里，方便你继续探索。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { item ->
                    RecentRow(
                        item = item,
                        onClick = { onClick(item.query) },
                        onRemove = { onRemove(item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRow(
    item: SearchHistoryUi,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.control),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(vertical = AnimeSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.query,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PillButton(
                label = "删除",
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = "删除最近搜索：${item.query}" },
            )
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    suggestion: SearchSuggestionUi,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "搜索 ${suggestion.value}"
                }.testTag("search.suggestion.${suggestion.value}"),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
    ) {
        Box(modifier = Modifier.padding(AnimeSpacing.md), contentAlignment = Alignment.CenterStart) {
            Text(suggestion.value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ResultsSummary(
    query: String,
    count: Int,
) {
    Text(
        text = "“$query” 的结果 · 已显示 $count 个",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("search.results"),
    )
}

@Composable
private fun SearchPagination(
    hasMore: Boolean,
    loading: Boolean,
    error: Boolean,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        loading -> {
            Box(Modifier.fillMaxWidth().padding(AnimeSpacing.lg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        error -> {
            AnimeGlassPanel(
                role = GlassRole.FloatingPanel,
                modifier = Modifier.fillMaxWidth().testTag("search.loadMoreError"),
                shape = RoundedCornerShape(AnimeRadius.card),
                contentPadding = PaddingValues(AnimeSpacing.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("下一页加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimeSecondaryButton(label = "重试", onClick = onRetry)
                }
            }
        }

        hasMore -> {
            AnimePrimaryButton(
                label = "加载更多",
                onClick = onLoadMore,
                modifier = Modifier.fillMaxWidth().testTag("search.loadMore"),
            )
        }
    }
}

@Composable
private fun SearchStatePanel(
    kind: StatePaneKind,
    title: String,
    message: String,
) {
    site.jokersh.anime.core.designsystem.AnimeStatePane(
        state = StatePaneModel(kind = kind, title = title, message = message),
        modifier = Modifier.testTag("search.${kind.name.lowercase()}"),
    )
}

@Composable
private fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimeSecondaryButton(
        label = label,
        onClick = onClick,
        modifier = modifier,
    )
}
