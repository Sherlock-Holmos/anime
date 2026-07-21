# 收藏 Feature 契约

## 1. 路由与范围

路由 `AppRoute.Collection(filter: CollectionStatus? = null)`。`null` 表示全部分组。匿名时显示登录引导；登录后展示分组、筛选、进度更新和同步状态。首发不支持自定义列表、批量编辑和拖拽排序。

## 2. Contract

```kotlin
data class CollectionUiState(
    val session: SessionState,
    val selectedFilter: CollectionStatus?,
    val content: AsyncContent<List<CollectionItemUi>>,
    val pendingCount: Int,
    val conflictCount: Int,
)

sealed interface CollectionIntent {
    data object Entered : CollectionIntent
    data object LoginClicked : CollectionIntent
    data class FilterSelected(val value: CollectionStatus?) : CollectionIntent
    data class SubjectClicked(val id: SubjectId) : CollectionIntent
    data class IncrementProgress(val id: SubjectId) : CollectionIntent
    data class SetProgress(val id: SubjectId, val value: Int) : CollectionIntent
    data class ResolveConflict(val id: SubjectId, val resolution: ConflictChoice) : CollectionIntent
    data object Retry : CollectionIntent
}
```

## 3. 排序和筛选

默认分组顺序为 Watching、Wish、OnHold、Completed、Dropped；组内按 `updatedAt` 降序，再按 subjectId 升序保证稳定。选择筛选后只显示该状态，空组不显示。筛选值写入 SavedState，进程重建后恢复。

## 4. 写入和冲突

点击 `+1` 立即更新本地进度和 Pending 标记，800ms 合并窗口内的连续点击只形成一个 Outbox 操作。同步成功移除 Pending；可重试失败保留新值并显示错误图标；不可重试失败回滚。

冲突对话框必须同时显示“本机：状态/进度/时间”和“云端：状态/进度/时间”。选择保留本机会以远端版本为 base 重新提交；选择采用云端会覆盖本地并清除 Outbox；关闭对话框不解决冲突。

## 5. 布局

筛选为可横向滚动的 FilterChip 行，高至少 48dp。手机列表卡片高至少 120dp，海报 72×101dp；宽屏为两列。同步摘要位于列表顶部，仅 pendingCount 或 conflictCount 非零时显示。

匿名空态包含标题、解释和“登录”主按钮，不将其称为错误。已登录且无收藏显示“还没有收藏”，提供跳到发现页的按钮。

## 6. 标签和验收

`collection.signedOut`、`collection.login`、`collection.filters`、`collection.filter.{status}`、`collection.list`、`collection.item.{id}`、`collection.progress.{id}`、`collection.pending.{id}`、`collection.conflict.{id}`、`collection.conflict.keepLocal`、`collection.conflict.useRemote`。

- FE-COL-001：Fixture 五条收藏按规定分组排序。
- FE-COL-002：匿名页面不会请求用户收藏接口。
- FE-COL-003：连续三次 +1 形成一个最终写操作且 UI 不回跳。
- FE-SYNC-001：write-retry 最终从 Pending 进入 Synced。
- FE-SYNC-002：collection-conflict 两种解决路径均可确定复现。

每个进度按钮语义包含条目名和当前进度，例如“星海邮差，增加一集，当前 3/12”。
