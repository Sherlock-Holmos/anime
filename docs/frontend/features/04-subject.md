# 条目详情 Feature 契约

## 1. 路由与页面结构

路由 `AppRoute.Subject(subjectId: Long)`；ID `<= 0` 直接进入 Not Found，不发请求。详情包含 Hero、标题与元信息、Bangumi 只读评分、简介、观看进度、剧集、角色、制作人员、关联条目和评论入口。

## 2. Contract

```kotlin
data class SubjectUiState(
    val subjectId: SubjectId,
    val detail: AsyncContent<SubjectDetailUi>,
    val collection: CollectionUiState,
    val expandedSynopsis: Boolean,
    val selectedEpisode: EpisodeId?,
    val isOffline: Boolean,
)

sealed interface SubjectIntent {
    data object Entered : SubjectIntent
    data object Retry : SubjectIntent
    data class CollectionClicked(val target: CollectionStatus) : SubjectIntent
    data class ProgressChanged(val episode: Int) : SubjectIntent
    data object SynopsisToggled : SubjectIntent
    data class EpisodeClicked(val id: EpisodeId) : SubjectIntent
    data class PersonClicked(val id: PersonId) : SubjectIntent
    data class RelatedClicked(val id: SubjectId) : SubjectIntent
    data object CommentsClicked : SubjectIntent
}
```

## 3. 数据与状态

详情和收藏是两个独立数据流：详情成功不等待收藏；匿名收藏状态为 `SignedOut`。详情刷新失败时保留旧详情。收藏写操作采用 optimistic UI，并显示 `Pending`；最终失败回滚到 lastConfirmed，冲突进入 Conflict Dialog。

Subject 不存在映射为 Not Found；权限错误不适用于只读详情；Bangumi 字段缺失不构成页面错误。任何子区块为空时隐藏该区块，不显示无意义标题。

## 4. 布局

手机 Hero 高 280dp，背景图裁切并叠加渐变；海报 112×158dp，距边 16dp。宽屏采用双栏，左栏 280dp，右栏最大 720dp。标题使用 DisplaySmall/TitleLarge 响应式切换。

评分卡显示 `8.6`、`/ 10`、`1,234 人评分`和“Bangumi”来源标签；无评分时整个评分卡隐藏。不得出现星级输入、可点击评分或 Anime 排名。

简介默认 4 行，溢出才显示“展开”；展开后按钮为“收起”。剧集使用流式网格，单元最小 48×48dp；角色和关联条目使用横向列表。区块垂直间距 32dp。

## 5. 收藏和进度

- 收藏状态选择固定：Wish（想看）、Watching（在看）、Completed（看过）、OnHold（搁置）、Dropped（抛弃）和未收藏（`null`）。
- 选“未收藏”等同删除收藏，但必须允许撤销 Snackbar 5 秒；撤销创建新的写操作。
- 当前进度未满时选择 Completed，询问是否“同时补全进度”；默认主按钮为“仅改变状态”，确认补全时传 `completeProgress=true`。
- 进度范围 `0..totalEpisodes`；总集数未知时最大值为已发布集数，未知则只允许 `+1`。
- 将进度设为总集数时询问是否同步改为 Completed；默认按钮为“仅更新进度”。
- Pending 时允许继续修改，新值折叠为同一 subject 的最后写入；不得并发乱序覆盖。

## 6. 标签与验收

`subject.screen.{id}`、`subject.hero`、`subject.rating`、`subject.synopsis.toggle`、`subject.collection`、`subject.progress`、`subject.episode.{id}`、`subject.characters`、`subject.staff`、`subject.related.{id}`、`subject.comments`、`subject.error`、`subject.notFound`。

- FE-SUB-001：1001 全部模块、顺序和跳转正确。
- FE-SUB-002：1003 无海报无评分时不出现空评分卡或布局断层。
- FE-SUB-003：1007 缺少简介/角色时对应区块完全隐藏。
- FE-SUB-004：离线缓存详情可读，收藏写入 Outbox 并显示 Pending。
- FE-SUB-005：Bangumi 评分在语义树中明确为只读来源数据。

图片作为装饰；页面语义标题为条目标题。评分朗读“Bangumi 评分 8.6 分，1234 人评分”。

## 7. 当前实现边界（2026-07-22）

- 第一条详情纵向切片已接通：`SubjectRoute → SubjectViewModel → CatalogRepository.observeSubject`，页面参数只使用路由中的正数 `subjectId`，不传递领域对象。
- Demo Repository 可为发现页的 12 个作品返回确定性 `SubjectDetail`；1001 等记录提供简介、话数、标签、Bangumi 来源链接和只读评分，合同测试覆盖“发现条目可打开详情”。
- 当前页面已实现独立加载/错误/重试、返回、112×158dp 海报占位、标题元信息、只读评分、标签、简介和作品信息；详情是沉浸式子页，底栏自动隐藏。
- 无评分时评分卡完全不进入布局；返回按钮使用 Design System 自有圆端图标和本地化语义。收藏/进度、剧集、人物、关联、评论、Section 独立降级、离线缓存与完整 FE-SUB-001..005 测试仍属于后续 F6，不得把本切片标记为完整详情。
