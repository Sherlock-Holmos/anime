package site.jokersh.anime.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
public fun SearchScreen(
    state: SearchUiState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding().testTag("search.list"),
        contentPadding = PaddingValues(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
    ) {
        item(key = "header", contentType = "header") {
            SearchHeader(
                query = state.query,
                onQueryChanged = { onIntent(SearchIntent.QueryChanged(it)) },
                onSubmit = { onIntent(SearchIntent.Submit(state.query)) },
                onClear = { onIntent(SearchIntent.ClearQuery) },
            )
        }

        when (state.mode) {
            SearchMode.Idle -> {
                item(key = "recent", contentType = "recent") {
                    RecentSearches(
                        items = state.recentQueries,
                        onClick = { query -> onIntent(SearchIntent.RecentClicked(query)) },
                        onRemove = { id -> onIntent(SearchIntent.RemoveRecent(id)) },
                        onClearAll = { onIntent(SearchIntent.ClearAllRecent) },
                    )
                }
            }

            SearchMode.Suggesting -> {
                items(
                    items = state.suggestions,
                    key = { suggestion -> "suggestion:${suggestion.value}" },
                    contentType = { "suggestion" },
                ) { suggestion ->
                    SearchSuggestionRow(
                        suggestion = suggestion,
                        onClick = { onIntent(SearchIntent.SuggestionClicked(suggestion.value)) },
                    )
                }
            }

            SearchMode.Loading -> {
                item(key = "loading", contentType = "state") {
                    SearchStatePanel(
                        kind = StatePaneKind.Loading,
                        title = "正在搜索",
                        message = "从本地演示数据中匹配 Bangumi 条目",
                    )
                }
            }

            SearchMode.Results -> {
                item(key = "summary", contentType = "summary") {
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
                item(key = "more", contentType = "pagination") {
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
                item(key = "empty", contentType = "state") {
                    SearchStatePanel(
                        kind = StatePaneKind.Empty,
                        title = "没有找到结果",
                        message = "换一个作品名、别名或 Bangumi ID 试试。",
                    )
                }
            }

            SearchMode.BlockingError -> {
                item(key = "error", contentType = "state") {
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
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
        )
        AnimeGlassPanel(
            role = GlassRole.FloatingPanel,
            shape = RoundedCornerShape(AnimeRadius.round),
            contentPadding = PaddingValues(AnimeSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.weight(1f).testTag("search.input"),
                    singleLine = true,
                    placeholder = { Text("作品名、别名、Bangumi ID") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                )
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AnimeRadius.control))
                .clickable(onClick = onClick)
                .padding(vertical = AnimeSpacing.sm),
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

@Composable
private fun SearchSuggestionRow(
    suggestion: SearchSuggestionUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "搜索 ${suggestion.value}"
                }.clip(RoundedCornerShape(AnimeRadius.card))
                .clickable(onClick = onClick)
                .testTag("search.suggestion.${suggestion.value}"),
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
    Surface(
        modifier =
            modifier
                .defaultMinSize(minWidth = 54.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = AnimeSpacing.md)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}
