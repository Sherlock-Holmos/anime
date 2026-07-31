# CMP Android 实现日志

本文记录已经进入代码库并通过验证的前端实现增量。它补充规范文档，不替代 `features/*` 中的契约。

## 2026-07-31 Windows Desktop 体验适配

已落地：

- `>= 840 dp` 使用左侧悬浮玻璃导航，窄屏继续使用 Kyant 官方可拖动 `LiquidBottomTabs`。
- 搜索结果按窗口宽度切换 1/2/3 列，状态、标题和分页行保持全宽。
- 增加 `Ctrl+1..4`、`Ctrl+K`、`Escape`、`Alt+Left` 窗口级快捷键映射。
- 增加 `840 x 640 dp` 最小窗口约束以及尺寸、位置、最大化状态持久化。
- 远程桌面默认将玻璃能力降为 `Translucent`，并提供开发诊断覆盖参数。
- 增加初始根页面和搜索词启动参数，便于确定性桌面视觉验收。
- 补充 `kotlinx-coroutines-swing`，修复 Desktop 缺少 Main dispatcher 导致启动后异常对话框的问题。
- 紧凑条目卡片改为具备明确 `contentColor` 的 Material Surface，保证深浅主题文字对比一致。

已验证：

- `:core:designsystem:desktopTest`：通过。
- `:shared:app:desktopTest`：通过。
- `:app:desktop:compileKotlin`：通过。
- 发现页、搜索页和双列搜索结果已做 Windows 窗口截图检查。

仍未完成：

- 完整键盘焦点遍历与桌面 UI 自动化。
- 多屏、系统缩放、高对比度和多类 GPU 性能矩阵。
- Desktop Remote 数据模式、安装签名与升级卸载验收。

## 2026-07-31 Windows Desktop 工程基线

已落地：

- 所有共享 KMP 模块新增 `desktop` JVM 17 变体。
- 新增 `app/desktop` Compose Desktop 壳，复用 `shared/app`、Navigation 3、Fixture 与 Design System。
- Kyant Backdrop 和 Shapes 直接使用官方 Desktop 变体；Liquid Bottom Tabs 保持上游源码不变。
- Desktop Glass actual 支持 Blur 与 Liquid lens，Vendor `awaitFrame` 使用 Compose frame clock。
- 配置便携目录、Uber JAR、EXE 和 MSI 发行任务。

已验证：

- `:app:desktop:compileKotlin`：通过。
- `:app:desktop:createDistributable`：通过。
- `:app:desktop:packageUberJarForCurrentOS`：通过。
- `:app:desktop:packageExe`：通过。

仍未完成：

- 完整键鼠焦点与桌面无障碍适配。
- Desktop Remote 数据模式。
- 桌面截图、性能和安装升级验收。

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
