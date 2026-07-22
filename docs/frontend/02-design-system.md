# CMP UI 设计系统

> 视觉方向：**Luminous Archive / 流光档案馆**<br>
> 设计语言：**iOS 26-inspired Liquid Glass**，Android 是优先运行平台而不是 Material 视觉目标<br>
> 核心气质：内容优先、安静克制、轻盈通透；液态玻璃用于悬浮导航与关键控件，不覆盖每张卡片

## 1. 设计原则

1. **作品是主角**：封面和剧照承担主要色彩，界面底色保持克制。
2. **层级可感知**：通过间距、排版、色调和少量玻璃建立深度，不依赖浓重阴影。
3. **状态可理解**：加载、离线、旧数据、同步和错误都有一致且非侵入式的表达。
4. **Apple 式视觉同源**：Android 与未来 iOS 共享 iOS 26-inspired 的层级、圆角、留白、动效和 Liquid Glass 导航；仅系统返回、窗口边距、键盘与触感遵循运行平台习惯。
5. **性能也是设计**：滚动列表不使用逐卡实时模糊；低性能降级不改变信息和可操作性。

Material 3 只可作为 Compose 底层实现与无障碍语义来源，不得直接使用其默认 `TopAppBar`、`NavigationBar`、选中指示器、Elevation 或 Ripple 决定成品外观。

## 2. 颜色 Token

页面只能消费语义颜色，不直接引用十六进制值。动态取色仅可影响 `heroTint`，不可改变正文对比度。

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `background` | `#F7F7FB` | `#0C0D13` | 页面底色 |
| `surface` | `#FFFFFF` | `#151720` | 普通卡片、菜单 |
| `surfaceVariant` | `#EEF0F7` | `#20232E` | 次级容器、输入框 |
| `onSurface` | `#191A22` | `#E7E8F1` | 主文字 |
| `onSurfaceVariant` | `#5D6070` | `#C5C6D2` | 次文字 |
| `primary` | `#5B5CE2` | `#B8B8FF` | 主要动作与选中态 |
| `onPrimary` | `#FFFFFF` | `#171750` | 主按钮文字 |
| `secondary` | `#007F80` | `#77DAD7` | 信息与同步状态 |
| `accent` | `#C93669` | `#FFAFCC` | 少量强调、收藏态 |
| `success` | `#16865B` | `#6DDBA6` | 成功 |
| `warning` | `#A76500` | `#FFB95C` | 旧数据、待同步 |
| `error` | `#BA1A1A` | `#FFB4AB` | 错误与破坏性动作 |
| `outline` | `#CBCDD9` | `#8E909F` | 边框与分隔线 |
| `scrim` | `#00000073` | `#00000099` | 模态遮罩 |

评分数字沿用 `onSurface`，不可用“金色”暗示本站背书；分布条使用 `primary` 单色阶。收藏状态使用文字/图标与颜色共同表达。

## 3. 排版 Token

默认字体使用系统无衬线字体栈；Android 为 Roboto/Noto Sans CJK，iOS 为 SF Pro/PingFang。数字评分启用等宽数字特性。

| Style | 字号 / 行高 | 字重 | 用途 |
|---|---|---:|---|
| `display` | 34sp / 41sp | 700 | iPhone 式大标题、沉浸式标题 |
| `headline` | 28sp / 34sp | 700 | 紧凑页面标题 |
| `titleLarge` | 20sp / 28sp | 650 | 区块与详情标题 |
| `titleMedium` | 17sp / 24sp | 600 | 卡片标题 |
| `bodyLarge` | 16sp / 24sp | 400 | 正文、输入 |
| `bodyMedium` | 14sp / 20sp | 400 | 列表说明 |
| `labelLarge` | 14sp / 20sp | 600 | 按钮、Tab |
| `labelSmall` | 12sp / 16sp | 500 | 元数据、来源 |

- 中文和日文正文不使用全大写或额外字间距。
- 用户字体缩放 100%–200% 时不得裁切；空间不足时内容换行，操作按钮转入下一行。
- 两行标题超出后省略；详情页主标题不省略，由页面自然增长。

## 4. 尺寸与形状

| 类别 | Token / 值 |
|---|---|
| 间距 | `space4/8/12/16/20/24/32/40/48/64` dp |
| 圆角 | `radius8/12/16/20/28/full` |
| 最小触控区域 | `48 × 48dp` |
| 页面水平边距 | 手机 16dp；平板 24–32dp |
| 内容最大宽度 | 列表 840dp；正文 720dp |
| 海报比例 | `2:3`，默认 120×180dp |
| 背景剧照比例 | `16:9` |
| 头像比例 | `1:1` |
| 细边框 | 1dp；高密度屏保持可见，不使用 0.5dp 业务常量 |

列表密度：标准卡高度由内容决定且不低于 116dp；紧凑条目不低于 80dp。页面中同级卡片圆角必须一致。

## 5. 层级与表面

| Level | 表面 | 阴影/边框 | 示例 |
|---|---|---|---|
| 0 | `background` | 无 | 页面背景 |
| 1 | `surface` | 1dp outline 20% 或 1–2dp tonal elevation | 普通内容卡 |
| 2 | `surfaceVariant` / 轻玻璃 | 细高光边 | 筛选、浮动工具条 |
| 3 | Liquid Glass | 悬浮柔影 + 高光边，不与屏幕边缘粘连 | 底部导航、工具组、弹层 |
| Modal | 不透明或稳定玻璃 | scrim + 明确焦点 | 对话框、Sheet |

同一屏幕最多两个持续存在的复杂玻璃区域。内容列表卡默认 Level 1，不使用实时 Blur 或 Lens。

## 6. 图标与图像

- 使用项目自有的 iOS-inspired 圆润图标体系，默认 22dp；不得直接复制仅获准用于 Apple 平台的 SF Symbols，也不得使用 Material 默认图标作为成品资产。
- 根导航图标保持统一视觉盒和线宽；选中态允许填充或提高字重，未选中使用轮廓，始终配单词标签。
- 图片加载按容器尺寸解码；列表缩略图不请求原图。
- 图片占位由 `surfaceVariant`、作品类型图标和可访问名称构成，禁止用随机渐变造成跳变。
- 海报裁切使用 `ContentScale.Crop`；人物头像允许中心裁切；截图类图片提供全图查看入口。

## 7. 响应式断点

| 宽度 | 布局 |
|---|---|
| `< 600dp` | 单列、悬浮 Liquid Glass Tab Bar、全宽详情区块；内容延伸到底栏下方 |
| `600–839dp` | Navigation Rail；发现页两列；详情主从双栏 |
| `>= 840dp` | Rail/Drawer；内容居中限宽；详情海报与信息 1:2 分栏 |

断点只改变布局，不改变功能、路由或阅读顺序。横屏手机优先保证正文与操作可见，不强制套用平板双栏。

## 8. Token 实现约束

- Token 统一声明于 `core:designsystem`，组件和 Feature 不得复制颜色、圆角、动效时长。
- Design Token 使用稳定语义名称；视觉调整不应导致业务组件 API 变化。
- 所有 Token 提供 Light/Dark Preview；组件 Preview 必须覆盖 200% 字体和长标题。
- 新增 Token 需说明不可由现有 Token 表达的原因；临时数值不得进入 Feature 主分支。

## 9. 设计依据

- Apple Human Interface Guidelines — Tab bars：<https://developer.apple.com/design/human-interface-guidelines/tab-bars>
- Apple Human Interface Guidelines — Materials：<https://developer.apple.com/design/human-interface-guidelines/materials>
- WWDC25 — Meet Liquid Glass：<https://developer.apple.com/videos/play/wwdc2025/219/>
- Apple Design Resources License：<https://developer.apple.com/support/downloads/terms/apple-design-resources/Apple-Design-Resources-License-20230621-English.pdf>
