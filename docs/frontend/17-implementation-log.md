# CMP Android 实现日志

本文记录已经进入代码库并通过验证的前端实现增量。它补充规范文档，不替代 `features/*` 中的契约。

## 2026-07-28 Search 纵向切片

已落地：

- `feature/search` 增加 `SearchRoute`、`SearchViewModel`、`SearchScreen`、`SearchUiMapper` 和页面状态契约。
- `data/catalog` 增加 `FixtureSearchRepository`，支持确定性搜索、分页、建议、历史去重置顶、历史删除和清空。
- `shared/app` 将 `AppRoute.Search` 作为搜索入口，将 `AppRoute.SearchResults(SearchRouteRequest)` 作为结果页路由。
- 搜索结果卡片可进入 `AppRoute.Subject(origin = Search)`，继续复用详情页面。
- Demo 搜索支持历史、建议、加载、结果、空态、阻断错误、分页加载、分页失败行内重试、Bangumi 只读评分展示和 subject id 精确搜索。

已验证：

- `CT-SEA-006`：Fixture 搜索分页为稳定顺序，跨页不重复。
- `CT-SEA-007`：使用 Bangumi subject id 可以精确命中条目。
- `./gradlew.bat animeCheck assembleDemoDebug`：通过。

仍未完成：

- 筛选 Sheet。
- 300ms debounce 与 generation 丢弃旧结果。
- 宽屏 2/3 列结果网格。
- Search Compose UI 测试和截图基线。
- 真实 Bangumi/后端搜索接入。
