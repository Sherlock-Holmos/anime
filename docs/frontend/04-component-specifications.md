# CMP 组件详细规范

## 1. 统一组件契约

每个公共组件必须明确：用途、结构、参数、状态、Token、语义、性能限制和 Preview。公共组件保持无业务 Repository 依赖，接收不可变 UI Model 与事件回调。

```kotlin
@Immutable
data class ComponentUiState<T>(
    val value: T?,
    val loading: Boolean = false,
    val enabled: Boolean = true,
    val errorMessage: String? = null,
)
```

禁止在 Design System 组件内启动网络请求、读取导航器或持有 ViewModel。

## 2. 基础组件

| 组件 | 结构与变体 | 必须覆盖状态 |
|---|---|---|
| `AnimeGlassSurface` | `role`, `preferredTier`, shape, content | Tier 四级、降级、深浅色 |
| `AnimeTopBar` | 内容层大标题 + 最多 2 个独立悬浮玻璃动作；禁止全宽 Material 矩形栏 | 默认、滚动折叠、离线标记 |
| `AnimeLiquidTabBar` | 4 个带文字目的地、圆角悬浮 Liquid Glass 容器；内容延伸到其下方 | 选中、未选中、Badge、200% 字体、四级 Glass 降级 |
| `PrimaryButton` | 文字、可选 leading icon | normal/pressed/disabled/loading |
| `SecondaryButton` | outline/tonal | normal/pressed/disabled |
| `AnimeIconButton` | 48dp 点击区、24dp 图标 | normal/selected/disabled/loading |
| `AnimeSearchField` | 搜索、清除、提交 | idle/focused/input/error/disabled |
| `FilterChipRow` | 单/多选 Chip、横向滚动 | selected/unselected/disabled |
| `AnimeTabs` | 固定或可滚动 | selected/focus/overflow |
| `SectionHeader` | 标题、说明、可选“查看全部” | 无动作/有动作 |

Loading 按钮保持原宽度，文字替换为 18dp 进度指示；不得因状态变化造成布局跳动。

### 2.1 `AnimeLiquidTabBar`

- Compact 宽度下距左右安全边 12–16dp，距底部安全区 8dp；主体高度 64dp，圆角为 full，不与屏幕左右/底边粘连。
- 四个目的地等宽，图标 22dp、标签 11–12sp、点击区至少 48dp；选中态使用小范围 accent tint、图标字重和文字共同表达，不采用 Material 3 宽胶囊指示器。
- Tab Bar 固定在功能层，页面内容必须绘制和滚动到其下方；Backdrop 采样实际内容层，不能只采样静态页面底色。
- API 33+ 首选 `Liquid`，API 31–32 降为 `Blur`，API 26–30 降为 `Translucent`；降级不改变尺寸、位置和点击语义。
- 整条 Tab Bar 都是横向拖动命中区：拖动时选中胶囊连续跟手、横向拉伸并使用 Backdrop 对内容层做真实折射；松手按最近目的地吸附并且只提交一次根导航。点击目的地仍可直接跳转，普通根导航不触发系统 Ripple。
- 选中胶囊是容器内唯一的交互式第二采样层，仅允许作用于当前约一个 Tab 宽的局部区域；静止时使用低强度 tint、高光和折射，拖动时才提高形变与色散，不能退化为 Material 3 静态宽胶囊。
- 拖动过程中只改变预览选中态，不提前替换页面；松手吸附完成时调用 `onSelected`。外部 `selectedIndex` 改变时胶囊使用弹簧移动到目标。TalkBack/键盘仍按四个独立 `Role.Tab` 操作，不要求执行拖动手势。

## 3. 条目展示组件

### 3.1 `SubjectCard`

变体：`CompactRow`、`StandardPoster`、`Hero`。参数至少包含 `id`、标题、原名、海报、年份/类型、收藏状态、可选 Bangumi 评分和点击回调。

```kotlin
@Composable
fun SubjectCard(
    model: SubjectCardUiModel,
    variant: SubjectCardVariant,
    onClick: (SubjectId) -> Unit,
    onCollectionClick: ((SubjectId) -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```

- `CompactRow`：80–112dp 高，海报 2:3、标题最多两行、元数据一行。
- `StandardPoster`：网格海报，上方 2:3 图片，下方标题两行和评分一行。
- `Hero`：仅发现首屏一个，16:9 图像上叠静态玻璃信息区。
- 海报加载失败保留标题和点击；Skeleton 与最终尺寸完全相同。
- 卡片整体可点击，内部收藏按钮使用独立语义，不嵌套重复点击区域。

### 3.2 `BangumiRatingBlock`

结构：大号分数、`Bangumi` 来源、参与人数、0–10 分布条和更新时间。无评分时显示“暂无 Bangumi 评分”，不显示 0.0。

```kotlin
data class BangumiRatingUiModel(
    val score: String?,
    val votesLabel: String?,
    val distribution: List<RatingBucketUiModel>,
    val updatedAtLabel: String?,
)
```

该组件没有点击打分、星级输入或提交回调。来源标签不可被折叠隐藏。

### 3.3 状态辅助组件

- `CollectionStatusChip`：想看/在看/看过/搁置/抛弃，含图标和文字。
- `ProgressEditor`：减一、当前进度、加一、直接输入；边界为 0…总章节数，未知总数时只限制非负。
- `EpisodeRow`：章节序号、标题、播出状态、观看状态；整行最小 56dp。
- `RelationCard`、`CharacterCard`、`PersonCard`：统一图片比例和缺图策略。

## 4. 社区组件

### `CommentCard`

包含头像、昵称、时间、正文、剧透状态、自己的操作菜单和治理占位。正文默认完整展示；剧透内容先遮罩，点击后仅本会话展开。删除后保留撤销 Snackbar，服务端接入后遵循幂等删除。

### `CommentComposer`

- 0–300 字，实时显示剩余字数；空白不可提交。
- 支持“包含剧透”开关；提交期间禁用重复发送但保留草稿。
- 未登录点击输入区进入登录，成功返回后恢复草稿与焦点。
- Fixture Demo 写入本地数据库并显示“仅保存在演示数据中”。

## 5. 页面状态组件

| 组件 | 用途 | 行为 |
|---|---|---|
| `ContentSkeleton` | 首次加载 | 结构与内容一致，Reduce Motion 时静态 |
| `EmptyState` | 合法空结果 | 图标、原因、一个主要动作 |
| `FullPageError` | 无任何可展示内容 | 简短原因、重试、可选诊断详情 |
| `InlineError` | 局部模块失败 | 不遮挡其他内容，模块内重试 |
| `OfflineBanner` | 离线 | 可关闭；有缓存时不阻断 |
| `StaleDataBadge` | 旧数据 | 显示最近更新时间，可手动刷新 |
| `SyncStatusBanner` | 待同步/失败/冲突 | 点击进入状态详情或冲突处理 |
| `AnimeSnackbar` | 短暂反馈 | 最多一个动作，不承担永久错误说明 |

Banner 优先级见《UI 状态矩阵》。页面不得同时堆叠三个 Banner。

## 6. 组件验收模板

每个组件合并前必须具备：

- Light/Dark、中文长标题、无图、加载、错误、禁用 Preview；交互组件另含按压/选中状态。
- 100% 与 200% 字体截图；TalkBack 可读名称和角色断言。
- 触控区、颜色对比和键盘/方向键焦点检查。
- Lazy 列表组件无独立实时 Blur、无限动画和 Composition 内对象抖动。
- UI Model 为稳定不可变类型，回调命名为用户意图，不暴露底层 DTO。
