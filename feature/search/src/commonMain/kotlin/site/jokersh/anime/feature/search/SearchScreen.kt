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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import anime.core.designsystem.generated.resources.Res as DesignRes
import anime.core.designsystem.generated.resources.copy_action_retry
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
import site.jokersh.anime.core.designsystem.localizedMetadata
import site.jokersh.anime.core.designsystem.localizedRating
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.feature.search.generated.resources.Res
import site.jokersh.anime.feature.search.generated.resources.*

@Composable
public fun SearchScreen(
    state: SearchUiState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentUnderSystemBars: Boolean = false,
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(if (contentUnderSystemBars) Modifier else Modifier.statusBarsPadding())
                    .testTag("search.list"),
            contentPadding =
                PaddingValues(
                    start = AnimeSpacing.lg,
                    top = AnimeSpacing.lg,
                    end = AnimeSpacing.lg,
                    bottom = 132.dp,
                ),
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
                    contentUnderSystemBars = contentUnderSystemBars,
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
                            title = Res.string.search_loading_title,
                            message = Res.string.search_loading_message,
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
                            title = Res.string.search_empty_title,
                            message = Res.string.search_empty_message,
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
                            title = Res.string.search_unavailable_title,
                            message = Res.string.search_unavailable_message,
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
                Res.string.search_filter_tv to (SubjectType.Tv in types),
                Res.string.search_filter_web to (SubjectType.Web in types),
                Res.string.search_filter_movie to (SubjectType.Movie in types),
                Res.string.search_filter_recent_years to (years != null),
                Res.string.search_filter_airing to (AiringStatus.Airing in airing),
                Res.string.search_filter_finished to (AiringStatus.Finished in airing),
                Res.string.search_filter_rating to (sort == SearchSort.Rating),
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
                AnimePrimaryButton(label = stringResource(chip.first), onClick = action)
            } else {
                AnimeSecondaryButton(label = stringResource(chip.first), onClick = action)
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
        title = Res.string.search_trending_title,
        subtitle = Res.string.search_trending_description,
        modifier = modifier,
    ) {
        if (queries.isEmpty()) {
            Text(stringResource(Res.string.search_trending_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        title = Res.string.search_recommendation_title,
        subtitle = if (personalized) Res.string.search_recommendation_personalized else Res.string.search_recommendation_default,
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
                    Text(stringResource(Res.string.search_recommendation_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    subject.localizedRating() ?: subject.localizedMetadata(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SearchDiscoveryPanel(
    title: StringResource,
    subtitle: StringResource,
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
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(subtitle),
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
    contentUnderSystemBars: Boolean,
) {
    Column(
        modifier = if (contentUnderSystemBars) Modifier.statusBarsPadding() else Modifier,
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
    ) {
        val clearAccessibility = stringResource(Res.string.search_clear_accessibility)
        Text(
            text = stringResource(Res.string.search_brand),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.search_title),
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
                                    text = stringResource(Res.string.search_prompt),
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
                    label = stringResource(Res.string.search_clear),
                    onClick = onClear,
                    modifier =
                        Modifier
                            .testTag("search.clear")
                            .semantics { contentDescription = clearAccessibility },
                )
            }
            PillButton(
                label = stringResource(Res.string.search_action),
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
                Text(stringResource(Res.string.search_recent_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (items.isNotEmpty()) {
                    AnimeSecondaryButton(
                        label = stringResource(Res.string.search_recent_clear_all),
                        onClick = onClearAll,
                        modifier = Modifier.testTag("search.recent.clearAll"),
                    )
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = stringResource(Res.string.search_recent_empty),
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
    val deleteAccessibility = stringResource(Res.string.search_recent_delete_accessibility, item.query)
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
                label = stringResource(Res.string.search_recent_delete),
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = deleteAccessibility },
            )
        }
    }
}

@Composable
private fun SearchSuggestionRow(
    suggestion: SearchSuggestionUi,
    onClick: () -> Unit,
) {
    val suggestionAccessibility = stringResource(Res.string.search_suggestion_accessibility, suggestion.value)
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = suggestionAccessibility
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
        text = stringResource(Res.string.search_result_summary, query, count),
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
                    Text(stringResource(DesignRes.string.copy_action_retry), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimeSecondaryButton(label = stringResource(DesignRes.string.copy_action_retry), onClick = onRetry)
                }
            }
        }

        hasMore -> {
            AnimePrimaryButton(
                label = stringResource(Res.string.search_load_more),
                onClick = onLoadMore,
                modifier = Modifier.fillMaxWidth().testTag("search.loadMore"),
            )
        }
    }
}

@Composable
private fun SearchStatePanel(
    kind: StatePaneKind,
    title: StringResource,
    message: StringResource,
) {
    site.jokersh.anime.core.designsystem.AnimeStatePane(
        state = StatePaneModel(kind = kind, title = stringResource(title), message = stringResource(message)),
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
