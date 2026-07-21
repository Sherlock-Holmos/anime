# 发现 Feature 契约

## 1. 路由和职责

路由 `AppRoute.Discover` 无参数。负责展示发现分区、继续观看、刷新和进入条目详情；不负责搜索、收藏写操作或播放。

## 2. Contract

```kotlin
data class DiscoverUiState(
    val content: AsyncContent<DiscoverContent>,
    val isRefreshing: Boolean,
    val isOffline: Boolean,
    val lastUpdatedLabel: String?,
)

data class DiscoverContent(val sections: List<DiscoverSectionUi>)

sealed interface DiscoverIntent {
    data object Entered : DiscoverIntent
    data object Refresh : DiscoverIntent
    data class SubjectClicked(val subjectId: SubjectId) : DiscoverIntent
    data class SectionMoreClicked(val sectionId: String) : DiscoverIntent
    data object Retry : DiscoverIntent
}
```

Effect 只有 `NavigateToSubject(id)`、`NavigateToSection(id)` 和 `ShowMessage(key)`。

## 3. 状态转换

| 当前状态 + 事件 | 结果 |
|---|---|
| 初次进入 + 无缓存 | Loading，调用 `observeDiscover(false)` |
| 初次进入 + 有缓存 | 立即 Content；陈旧时后台刷新 |
| Content + Refresh | 保留列表、`isRefreshing=true`，调用 forceRefresh |
| Loading + 失败 | Blocking Error，可重试 |
| Content + 刷新失败 | 保留 Content，显示 Snackbar；不得全屏替换 |
| Content + 数据为空 | Empty，提供“重新加载” |

重复 Entered 不重复订阅。刷新期间再次 Refresh 被忽略。页面销毁只取消页面级任务，不取消 Repository 共享刷新。

## 4. Compose 结构

`DiscoverRoute → DiscoverScreen → LazyColumn`。顶部 20dp 间距；分区标题左右 16dp，标题与内容 12dp；分区间 28dp。横向列表 `LazyRow` 左右 content padding 16dp，item spacing 12dp，卡片手机宽 132dp、宽屏 156dp。继续观看卡片宽 264dp、高 112dp。

Skeleton 与真实内容共用尺寸：标题 96×20dp、5 张 132×198dp 卡片。首屏预加载只允许当前可见项加后续 2 项。

列表 key：`section:{sectionId}`；横向项 key：`subject:{subjectId}`；contentType 为 `section-header`、`poster-card`、`progress-card`。

## 5. 内容规则

- Section 空时整体隐藏；所有 Section 均空才显示页面 Empty。
- 继续观看只在登录且有 `CollectionStatus.Watching` 收藏时出现，永远排第一。
- Bangumi 评分为 null 时隐藏评分行，不显示 0 分或“暂无评分”占位。
- 标题最多 2 行；语种混排不手动插入空格或截断字符。
- 图片失败显示由 subjectId 生成的色块和标题首字符；不无限重试。

## 6. 标签和验收

`discover.list`、`discover.refresh`、`discover.section.{id}`、`discover.subject.{id}`、`discover.loading`、`discover.empty`、`discover.error`。

- FE-DIS-001：happy 场景分区、顺序、评分和跳转均与 Fixture 一致。
- FE-DIS-002：cold-slow 首帧显示等尺寸 Skeleton，无明显布局跳动。
- FE-DIS-003：refresh-slow 保留旧内容且只显示一个刷新进度。
- FE-DIS-004：无海报、无评分、长标题均不破坏卡片对齐。

TalkBack 卡片描述顺序：标题、播出状态、进度（若有）、Bangumi 分数与人数（若有）；海报作为装饰图不重复朗读。
