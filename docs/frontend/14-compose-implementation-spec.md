# Compose 实施规范

## 1. 规范出口

所有 Feature 只能使用 `core:designsystem` 暴露的 Token 和公共组件。Feature 中出现未命名的颜色、字号、圆角、阴影、动画时长或重复组件即为审查失败。Material 组件允许作为底层实现与语义工具，但成品不得直接呈现 Material 默认导航栏、顶部栏、选中指示器、Ripple、Elevation 或形状。

## 2. Token 的 Kotlin 命名

```kotlin
object AnimeSpacing {
    val xxs = 2.dp; val xs = 4.dp; val sm = 8.dp; val md = 12.dp
    val lg = 16.dp; val xl = 20.dp; val xxl = 24.dp; val xxxl = 32.dp
}

object AnimeRadius {
    val chip = 10.dp; val control = 14.dp; val card = 20.dp
    val panel = 28.dp; val round = 999.dp
}

object AnimeSize {
    val touch = 48.dp; val iconSm = 18.dp; val icon = 24.dp; val iconLg = 32.dp
    val posterCompactWidth = 80.dp; val posterCompactHeight = 112.dp
    val posterWidth = 132.dp; val posterHeight = 198.dp
    val contentMax = 1200.dp; val readingMax = 720.dp
}

object AnimeMotion {
    const val instant = 0; const val fast = 120; const val standard = 220
    const val emphasized = 360; const val debounceSearch = 300
}
```

颜色和排版的具体值以 `02-design-system.md` 为源；代码名称固定为 `AnimeColorScheme` 和 `AnimeTypography`。语义色只允许 `success`、`warning`、`error`、`info`、`scrim`，不得用 `Color.Red` 表达错误。

## 3. 响应式断点

| Window width | 布局 | 外边距 | 列数 |
|---|---|---:|---:|
| `< 600dp` Compact | Bottom navigation | 16dp | 1 |
| `600..839dp` Medium | Navigation rail | 24dp | 2 |
| `>= 840dp` Expanded | Rail + 居中内容 | 32dp | 3，详情双栏 |

断点只由 `WindowSizeClass` 映射，Feature 不读取物理设备类型或方向。可折叠设备的 hinge 作为避让区，不自行创建第四断点。

## 4. 公共组件 API

```kotlin
@Composable fun AnimeApp(...)
@Composable fun AnimeScaffold(title: String?, navigationIcon: ..., actions: ..., content: ...)
@Composable fun AnimeLiquidTabBar(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, icon: ...)
@Composable fun AnimePosterCard(model: SubjectCardUi, size: PosterCardSize, onClick: () -> Unit)
@Composable fun AnimeCompactSubjectCard(model: SubjectCardUi, onClick: () -> Unit)
@Composable fun AnimeRatingBadge(rating: BangumiRatingUi)
@Composable fun AnimeCollectionButton(state: CollectionButtonState, onIntent: ...)
@Composable fun AnimeProgressControl(current: Int, total: Int?, enabled: Boolean, onChange: ...)
@Composable fun AnimeSectionHeader(title: String, actionLabel: String?, onAction: (() -> Unit)?)
@Composable fun AnimeStatePane(state: StatePaneModel, onRetry: (() -> Unit)?)
@Composable fun AnimeOfflineBanner(...)
@Composable fun AnimeGlassPanel(role: GlassRole, tierOverride: GlassTier? = null, modifier: Modifier, content: ...)
```

公共 API 参数使用 UI Model 和语义枚举，不暴露 Repository、DTO、NavController、Painter 实现或平台 Context。组件回调以用户意图命名，禁止组件内部导航或写数据。

## 5. 图标和文案

只使用项目自有且许可允许 Android 发布的 iOS-inspired 矢量资产，保持 22dp 视觉盒、圆端线条和统一线宽；不得直接复制 SF Symbols，也不得以 Material Symbols 作为成品图标来源。语义映射固定为 Discover、Search、Collection、Profile、Back、More、Retry、Offline、Pending、Conflict、Spoiler，具体资产名由 Design System 统一维护。

图标按钮必须同时有可本地化 contentDescription；纯装饰图标传 null。生产文案全部放在 `commonMain` 资源中，key 格式 `feature_element_state`，不允许在 Composable 中写用户可见裸字符串。首发资源必须包含 `zh-CN`，缺少其他语言时回退 `zh-CN`。

## 6. Lazy 列表纪律

- `key` 使用领域 ID，不使用 index；静态占位项使用稳定的 `skeleton:{slot}`。
- `contentType` 区分 header、poster、compact、progress、comment、footer。
- 禁止在 item Composable 中发起 Repository 请求；图片加载除外且必须由 Coil 管理。
- 滚动触发分页基于 `snapshotFlow` + distinct，不能在每次重组直接调用。
- UI Model 必须为 immutable；列表使用持久不可变集合或在 Mapper 边界复制。
- 大列表不得使用 `Column.verticalScroll`。

## 7. 图片

Coil 配置统一在 AppContainer：内存缓存 25% 可用内存上限，磁盘缓存按构建配置；请求尺寸必须与显示尺寸相符。海报 `ContentScale.Crop`，人物头像 Crop，背景 Hero Crop。占位、错误和无图使用同一比例，避免布局跳动。

图片 URL 不进入 contentDescription；海报随卡片整体点击时设为装饰。Prod 禁止明文 HTTP。Fixture URI 仅 Demo/Dev ImageLoader 能解析。

## 8. 玻璃、降级和动态效果

`AnimeGlassPanel` 是 Compose 平台（Android/Desktop/Web）普通玻璃表面的统一入口；iOS 26+ 根底栏由 SwiftUI 宿主提供官方 Liquid Glass。Android/Desktop/Web 的根底栏由 `AnimeLiquidTabBar` 通过隔离的 Vendor 模块调用 Kyant 官方 `LiquidBottomTabs`。Feature 仍不得直接依赖第三方 API 或 Vendor 模块。

- AUTO 偏好由能力适配器解析为 `Liquid`、`Blur`、`Translucent` 或 `None`；Feature 不判断设备等级。
- `Liquid`：模糊、受限折射和高光；Compact 根 Tab Bar 是每屏主要 Liquid 区域，存在时 StaticHero 降为 Blur，避免玻璃嵌套与争抢层级。
- `AnimeLiquidTabBar` 只在 Android/Desktop/Web 桌面与移动目标桥接官方 `LiquidBottomTabs` 的 `selectedTabIndex`、`onTabSelected`、Backdrop、图标和标签；iOS 26+ 由 `ContentView.swift` 的 SwiftUI `GlassEffectContainer` 与 `.buttonStyle(.glass(.regular))` 提供根导航。拖动、速度形变、吸附、折射、高光与阴影完全由对应平台实现负责；Anime 代码禁止复制 Kyant 的算法。
- `Blur`：实时背景模糊；每屏最多 2 处，不进入 Lazy item。
- `Translucent`：半透明 surface + 1dp outline，无实时模糊。
- `None`：不透明 surface；对比度必须独立达标。
- 滚动列表中的每个卡片禁止单独实时取样；只允许顶部栏、导航栏、Sheet 等有限容器。

动画使用 `AnimeMotion`。减少动态时，位移/缩放改为 0–120ms 淡入淡出，禁止弹簧过冲和持续视差。动画结束不是业务状态转换的前置条件。

## 9. Insets、系统栏和键盘

根布局消费系统栏 Insets；手机内容层延伸到悬浮 Tab Bar 下方，并额外保留可滚动到底的内容尾部空间。Tab Bar 自身消费 navigation bar safe inset，页面不得重复消费。系统栏颜色透明，图标明暗随主题。IME Insets 由包含输入框的页面处理；底部 Sheet 必须保持主要按钮可见。横屏和手势导航下触控区域不得进入不可点击安全区。

## 10. 可访问性

- 最小触控 48×48dp；正文对比度 ≥4.5:1，大字 ≥3:1。
- 支持系统字体 200%，不得裁切关键文本或用横向滚动承载正文。
- 卡片合并语义；内部独立操作才拆分焦点。
- 错误、Pending、选中和评分来源不能只靠颜色表达。
- 动态更新使用适当 live region，但分页加载完成不得抢焦点。
- UI 测试必须启用一次 TalkBack 语义树快照检查。

## 11. Preview 与截图矩阵

每个 Screen 提供纯函数 Preview 参数，不创建真实 ViewModel。最低矩阵：

| 维度 | 值 |
|---|---|
| 主题 | Light、Dark、Dynamic Light |
| 宽度 | 360×800、600×960、1200×800 dp |
| 字体 | 1.0、1.3、2.0 |
| 状态 | Loading、Empty、Content、Error、Offline Content |
| 玻璃 | Liquid、Blur、Translucent、None |

黄金截图设备固定为 360×800dp、density 1、fontScale 1、Light/Dark；容差每像素 0.2%、全图差异 0.05%。动画、时钟和图片均使用 Fixture 固定值。

## 12. 性能预算

- Release 配置冷启动至首个可交互画面 P50 ≤ 1.2s、P95 ≤ 2.0s（基准设备另在 benchmark 模块锁定）。
- 关键滚动 Macrobenchmark 帧超时率 < 5%，P95 frame time < 24ms。
- 首屏不超过 2 个并行页面数据请求和 6 个图片请求。
- 重组计数不是单独验收指标；以帧时间、分配和可见行为为准。
- 性能不达标时先降级玻璃与图片质量，不删除可访问性和状态反馈。

## 13. 固定测试标签规则

格式 `{feature}.{element}[.{stableId}]`，只使用 ASCII 小写、数字、连字符和点。动态 ID 必须是领域 ID，不得包含用户文案。标签集中在各 Feature 的 `TestTags.kt`，规范已经列出的标签不得改名；迁移时至少保留一个版本别名。
