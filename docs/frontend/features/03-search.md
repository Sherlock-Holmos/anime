# 搜索 Feature 契约

## 1. 路由与职责

路由 `AppRoute.Search(query: String?)`。负责输入、最近搜索、建议、结果分页和筛选的 UI；查询解释和数据源在 SearchRepository。

## 2. Contract

```kotlin
data class SearchUiState(
    val query: String,
    val draftFilters: SearchFilters,
    val appliedFilters: SearchFilters,
    val mode: SearchMode,
    val suggestions: List<SearchSuggestionUi>,
    val recentQueries: List<String>,
    val results: List<SubjectCardUi>,
    val nextCursor: String?,
    val isLoadingMore: Boolean,
    val loadMoreError: AppError?,
)

sealed interface SearchIntent {
    data class QueryChanged(val value: String) : SearchIntent
    data class Submit(val value: String) : SearchIntent
    data class SuggestionClicked(val value: String) : SearchIntent
    data class RecentClicked(val value: String) : SearchIntent
    data class RemoveRecent(val value: String) : SearchIntent
    data object ClearAllRecent : SearchIntent
    data class FilterDraftChanged(val value: SearchFilters) : SearchIntent
    data object ApplyFilters : SearchIntent
    data object ResetFilters : SearchIntent
    data object ClearQuery : SearchIntent
    data object LoadNextPage : SearchIntent
    data object RetryNextPage : SearchIntent
    data class SubjectClicked(val id: SubjectId) : SearchIntent
}
```

`SearchMode` 固定为 `Idle`、`Suggesting`、`Loading`、`Results`、`Empty`、`BlockingError`。结果存在时分页错误不得切换为 BlockingError。

## 3. 输入和并发

- 输入变化去抖 300ms；短于 2 个字符不请求建议，但中文/日文单字符允许请求。
- 每次 QueryChanged 增加 requestGeneration；旧 generation 返回结果必须丢弃。
- Submit 使用规范化后的非空查询，立即取消建议任务、写入历史并发起第一页。
- 筛选固定为动画类型、首播年份和播出状态；打开 Sheet 复制 applied 到 draft，只有 Apply 才发起新搜索，关闭 Sheet 不改变结果。
- Reset 清空 draft；在 Sheet 中 Apply 后形成 `SearchRequest(query, filters)` 的新 generation，分页 cursor 同时失效。
- LoadNextPage 只在 cursor 非空、未加载且最后 4 项进入可见区时触发。
- 同一 cursor 最多并发一次；返回重复 Subject 时按 ID 去重，保留首次顺序。
- ClearQuery 回到 Idle，显示最近搜索并清空结果；不清空历史。

## 4. 布局

搜索框手机距边 16dp、高 56dp；宽屏最大 720dp。Idle 显示“最近搜索”Chips；Suggesting 使用单列建议列表；Results 手机单列紧凑卡片，≥600dp 两列、≥1000dp 三列，列间 16dp。筛选入口显示已应用条件数量；Sheet 中“重置”在左、“应用”在右，主按钮固定于底部。

结果卡片高至少 128dp，海报 80×112dp，文本区间距 8dp。分页加载器占 64dp；分页错误为行内组件，包含“重试”，不得用全局全屏错误。

## 5. 键盘和无障碍

进入 Search 根页不自动弹键盘；通过导航参数带 query 时自动提交但仍不强制聚焦。IME Search 等同 Submit。返回键先关闭键盘，再遵循 Shell 返回规则。

清除按钮 contentDescription 为“清空搜索内容”；删除历史为“删除最近搜索：{query}”。搜索结果总数更新通过 polite live region 宣布一次。

## 6. 标签和验收

`search.input`、`search.clear`、`search.submit`、`search.recent.clearAll`、`search.recent.{normalized}`、`search.suggestion.{index}`、`search.filters`、`search.filters.reset`、`search.filters.apply`、`search.results`、`search.subject.{id}`、`search.loadMore`、`search.loadMoreError`、`search.empty`。

- FE-SEA-001：快速输入时只显示最后 generation 的建议。
- FE-SEA-002：`garden` 的结果严格为 1003、1010。
- FE-SEA-003：`不存在` 进入 Empty，展示修改关键词建议且不伪造推荐。
- FE-SEA-004：分页请求去重，错误重试不清空第一页。
- FE-SEA-005：搜索历史去重、置顶、最多 10 条且可逐项或全部删除。
- FE-SEA-006：筛选只有应用后生效，改变筛选会取消旧请求并重置分页。
