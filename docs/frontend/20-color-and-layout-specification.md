# Anime 配色与布局规范

> 基线编号：`ACL-1.0`
> 生效日期：2026-08-12
> 依赖规范：`19-apple-design-baseline.md`
> 规范状态：Approved

## 1. 使用规则

本文件是颜色、间距、尺寸和响应式布局的唯一视觉数值基线。页面只消费语义 Token，不直接使用十六进制颜色，不在 Feature 内创建新的圆角、边框透明度或页面边距常量。

当本文档与代码不一致时：已批准的文档表示目标，`core:designsystem` 表示当前运行事实。两者必须在同一个变更中重新对齐。

## 2. 核心配色

### 2.1 Material 语义色

| Token | Light | Dark | 使用范围 |
|---|---|---|---|
| `primary` | `#007AFF` | `#0A84FF` | 主动作、链接、选中态 |
| `onPrimary` | `#FFFFFF` | `#FFFFFF` | 主色容器上的文字和图标 |
| `secondary` | `#00A7C4` | `#64D2FF` | 同步、信息型状态 |
| `onSecondary` | `#FFFFFF` | `#001E28` | 次色容器前景 |
| `tertiary` | `#AF52DE` | `#BF5AF2` | 收藏、个性化的少量强调 |
| `background` | `#F2F2F7` | `#0C0C0E` | 页面与窗口底色 |
| `onBackground` | `#1D1D1F` | `#F5F5F7` | 页面主文字 |
| `surface` | `#FFFFFF` | `#1C1C1E` | 卡片、菜单、内容容器 |
| `onSurface` | `#1D1D1F` | `#F5F5F7` | 表面主文字 |
| `surfaceVariant` | `#E9E9EE` | `#2C2C2E` | 输入、筛选、次级容器 |
| `onSurfaceVariant` | `#6E6E73` | `#A1A1A6` | 说明、元数据、未选中项 |
| `outline` | `#B8B8BD` | `#545458` | 必需的控件边界与焦点 |
| `outlineVariant` | `#D8D8DC` | `#363638` | 分隔线与弱边界 |
| `error` | `#FF3B30` | `#FF453A` | 错误和破坏性动作 |
| `scrim` | `#000000` 45% | `#000000` 60% | 模态遮罩 |

### 2.2 Anime 扩展语义色

| Token | Light | Dark | 使用范围 |
|---|---|---|---|
| `accent` | `#AF52DE` | `#BF5AF2` | 品牌强调、收藏态 |
| `success` | `#34C759` | `#30D158` | 已同步、成功、在线 |
| `warning` | `#FF9500` | `#FF9F0A` | 旧数据、等待、风险提示 |
| `info` | `#007AFF` | `#64D2FF` | 提示、外部来源、帮助 |

成功、警告和错误色只用于状态，不作为大面积装饰背景。状态必须同时包含文字或图标。

## 3. 表面配方

| 角色 | Light 配方 | Dark 配方 | 边界 |
|---|---|---|---|
| 页面 Canvas | `background` 100% | `background` 100% | 无 |
| 普通 Card | `surface` 92%–100% | `surface` 88%–96% | 可选 `outlineVariant` 20%–35% |
| 次级 Group | `surfaceVariant` 55%–75% | `surfaceVariant` 45%–65% | 默认无 |
| Sidebar | `surface` 78%–88% | `surface` 72%–82% | 内容侧单条 `outlineVariant` 25% |
| Top/Bottom Bar | `surface` 80%–90% | `surface` 76%–88% | 单条 `outlineVariant` 20%–30% |
| Popover/Dialog | `surface` 92%–98% | `surface` 90%–96% | `outlineVariant` 35%–50% |
| Hero Text Plate | `surface` 72%–86% | `surface` 70%–84% | 低对比高光边 |

### 3.1 边框纪律

- 业务边框宽度统一为 `1dp`。
- 普通内容卡优先无边框；只有与背景无法区分时才使用弱边界。
- 同一容器不得同时使用明显边框和明显阴影。
- 分隔线使用 `outlineVariant`，透明度 Light `25%–40%`、Dark `30%–45%`。
- Sidebar 分隔线不得使用 `outline` 全不透明色。

### 3.2 阴影纪律

- 普通 Card：`0dp`。
- Hover/Pressed：最多形成 `1dp` 的高度感，但布局尺寸不改变。
- Popover：柔和环境阴影，禁止硬黑边。
- Dialog：由 Scrim 建立层级，不依赖浓重阴影。

## 4. 内容图像与动态色

- 海报和剧照使用原始色彩，不添加全局品牌滤镜。
- Hero 可提取图片主色，但饱和度应降低，混入表面色的比例不超过 `12%`。
- Hero 文本区域必须覆盖稳定 Scrim 或材质，不能直接依赖图片本身的明暗。
- 图片缺失时使用 `surfaceVariant` 与作品类型图标；禁止使用随机彩色渐变冒充封面。
- 列表海报比例固定 `2:3`，Backdrop 固定 `16:9`，头像固定 `1:1`。

## 5. 间距系统

| Token | 值 | 典型用途 |
|---|---:|---|
| `xxs` | 2dp | 极细视觉校正 |
| `xs` | 4dp | 图标与微型标签 |
| `sm` | 8dp | 紧凑元素、标签组 |
| `md` | 12dp | 控件内部、紧凑卡片 |
| `lg` | 16dp | 手机页面边距、普通卡片 |
| `xl` | 20dp | 内容卡内部、标题组 |
| `xxl` | 24dp | Desktop 页面边距、区块间距 |
| `xxxl` | 32dp | 大区块、宽屏边距 |
| `huge` | 40dp | 页面章节分隔 |
| `giant` | 48dp | 大标题与主体分隔 |
| `massive` | 64dp | 页面底部和沉浸区留白 |

### 5.1 组合规则

- 同一层级间距必须来自相邻 Token，不使用 `13dp`、`18dp` 等临时值。
- 组件内部间距小于组件之间间距；组件之间间距小于区块之间间距。
- 手机卡片内边距通常 `16–20dp`；Desktop 通常 `20–24dp`。
- 标题与说明间距 `4–8dp`；说明与主体间距 `16–24dp`。

## 6. 圆角与尺寸

### 6.1 圆角

| Token | 值 | 用途 |
|---|---:|---|
| `chip` | 10dp | 标签、状态、小型选择 |
| `control` | 14dp | 输入框、按钮、Segment |
| `card` | 18dp | 普通卡片、搜索结果 |
| `panel` | 24dp | Hero、大面板、Dialog |
| `round` | 999dp | 头像、圆形按钮、确需胶囊的控件 |

子容器圆角应小于父容器圆角。页面内同级卡片使用同一圆角，不允许 12、16、20dp 混排。

### 6.2 核心尺寸

| Token | 值 | 用途 |
|---|---:|---|
| `touch` | 48dp | 最小交互命中区域 |
| `iconSm` | 18dp | 元数据、小型控制 |
| `icon` | 24dp | 标准图标 |
| `iconLg` | 32dp | 空状态、头像内图标 |
| `posterCompact` | 80×112dp | 紧凑横向条目 |
| `poster` | 132×198dp | 标准海报卡 |
| `readingMax` | 720dp | 长正文最大宽度 |
| `contentMax` | 1320dp | 页面内容最大宽度 |

## 7. 排版表

| 语义 | 字号 / 行高 | 字重 | 使用场景 |
|---|---|---:|---|
| Display Large | 34 / 41sp | 700 | Mobile 根页面大标题 |
| Headline Large | 28 / 34sp | 700 | Desktop 页面标题、紧凑标题 |
| Title Large | 20 / 28sp | 650 | 区块、详情标题 |
| Title Medium | 17 / 24sp | 600 | 卡片标题、列表主文案 |
| Body Large | 16 / 24sp | 400 | 正文、输入内容 |
| Body Medium | 14 / 20sp | 400 | 说明、辅助内容 |
| Label Large | 14 / 20sp | 600 | 按钮、Segment、Tab |
| Label Small | 12 / 16sp | 500 | 来源、时间、人数 |

主文字使用 `onBackground/onSurface`，说明使用 `onSurfaceVariant`。禁用态通过前景透明度表达，但文字对比仍需可辨认。

## 8. 响应式断点

| 宽度 | 模式 | 根导航 | 页面布局 |
|---|---|---|---|
| `< 700dp` | Compact | 底部 Tab Bar | 单列，16dp 边距 |
| `700–1099dp` | Desktop Regular | 220dp Sidebar | 单列或受限双栏，24dp 边距 |
| `1100–1599dp` | Desktop Wide | 220dp Sidebar | 主辅双栏，24–32dp 边距 |
| `>= 1600dp` | Desktop Large | 220dp Sidebar | 内容居中，最大 1320dp |

断点由可用内容宽度决定，不由设备名称决定。横屏手机如果高度不足，仍优先使用 Compact 的阅读与触控规则。

## 9. 栅格与页面边距

### 9.1 Compact

- 页面水平边距：`16dp`。
- 卡片间距：`12–16dp`。
- 区块间距：`32–40dp`。
- 单列为默认；海报轨道允许水平滚动。
- 双列海报仅在单卡宽度不低于 `132dp` 时启用。

### 9.2 Desktop Regular/Wide

- Sidebar：`220dp`。
- Sidebar 到内容的视觉间隔：由内容边距 `24–32dp` 提供。
- 内容最大宽度：`1320dp`，在可用区域中水平居中。
- 标准网格列间距：`16–20dp`。
- 主辅布局推荐比例：`2:1` 或 `minmax(0, 1fr) + 360–440dp`。
- 长正文无论窗口多宽都限制在 `720dp`。

## 10. 组件布局规范

### 10.1 Button

- 高度不低于 `44dp`，命中区域不低于 `48dp`。
- 水平内边距 `16–20dp`。
- 按压只改变材质、颜色或轻微缩放，不改变测量宽高。
- 独立主动作使用填充主色；次动作使用弱表面或文字样式。

### 10.2 Search Field

- 圆角 `14dp`，背景使用 `surfaceVariant` 或弱 `surface`。
- 输入区、清除、搜索动作属于一个控制组。
- 默认不使用独立粗边框；Focused 时使用主色弱焦点环。
- Mobile 允许搜索动作由键盘 IME 承担，减少额外按钮。

### 10.3 Card

- 普通内容卡圆角 `18dp`，内边距 `16–24dp`。
- 同一屏同级 Card 表面一致。
- 可点击 Card 的反馈必须裁切在圆角内。
- 信息可由简单列表分隔表达时，不新增 Card。

### 10.4 Poster Card

- 标准宽高 `132×198dp`，下方信息区高度由最多两行标题和一行元数据决定。
- 海报与信息区共享外层圆角；图片只裁切顶部圆角。
- 横向轨道在 Desktop 支持滚轮横移、拖动和方向键；Mobile 支持触摸惯性滚动。

### 10.5 Segmented Control

- 外层使用 `surfaceVariant`，选中块使用 `surface` 或低饱和 `primary`。
- 2–5 个互斥选项；更多选项改用菜单或 Sheet。
- 所有 Segment 等宽或按内容稳定测量，选中时不得引起布局跳动。

## 11. 页面布局配方

| 页面 | Compact | Desktop |
|---|---|---|
| 发现 | 大标题、Hero、横向轨道 | 标题工具栏、Hero、横向轨道/网格 |
| 资料库 | 搜索框、筛选 Sheet、单列结果 | 搜索工具组、侧向筛选或双列结果 |
| 动态 | 单列 Feed | 居中 Feed + 可选趋势辅助栏 |
| 我的 | 身份卡、统计横滑、设置分组 | 身份横幅、统计三列、内容双栏 |
| 收藏 | 筛选横滑、单列/双列 | 筛选工具栏、自适应网格 |
| 详情 | 纵向 Hero、动作、正文 | 海报信息双栏、正文 + 辅助栏 |
| 登录 | 全屏/Sheet 单列步骤 | 居中双区面板或独立账号窗口 |

## 12. 动效数值

| Token | 时长 | 用途 |
|---|---:|---|
| `instant` | 0ms | 减少动态效果、同步状态切换 |
| `fast` | 120ms | 按压、Hover、图标切换 |
| `standard` | 220ms | 展开、内容替换、Segment |
| `emphasized` | 360ms | Sheet、Hero、页面空间过渡 |
| `debounceSearch` | 300ms | 搜索输入防抖，不是视觉动画 |

页面转场位移不超过 `24dp`。列表刷新不得重复播放瀑布入场动画。

## 13. Light/Dark 一致性

Dark 模式不是把所有颜色改为纯黑：

- Canvas 使用 `#0C0C0E`，Card 使用 `#1C1C1E`，次级容器使用 `#2C2C2E`。
- 通过相邻灰阶建立深度，不使用白色全透明边框。
- 图片亮度不自动降低；必要时只在文字覆盖区域增加 Scrim。
- Light 与 Dark 保持相同布局、圆角和信息顺序。

## 14. 开发与验收约束

1. 新视觉数值先进入 `AnimeTheme.kt` 或 `AnimeTokens.kt`。
2. 组件只消费语义 Token，Feature 只组合组件。
3. 每个共享组件至少验证 Light、Dark、Compact、Desktop、200% 字体。
4. 截图回归覆盖 390dp Mobile、1024dp Desktop 和 1440dp Desktop。
5. 设计评审先检查对齐、留白和信息层级，再检查材质与动效。

### 14.1 快速检查

- [ ] 页面是否只使用一个明确的内容起始线？
- [ ] 是否存在可以用留白替代的边框？
- [ ] 主动作是否唯一且位置稳定？
- [ ] 是否误用纯黑、纯白或高饱和渐变？
- [ ] Desktop 是否限制内容最大宽度？
- [ ] Mobile 是否保留安全区和 48dp 触控目标？
- [ ] Hover/Pressed 是否没有矩形泄漏和尺寸变化？
- [ ] 图片缺失时是否使用真实占位而非伪造封面？
