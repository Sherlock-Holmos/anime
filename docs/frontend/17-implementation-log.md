# CMP Android 实现日志

本文记录已经进入代码库并通过验证的前端实现增量。它补充规范文档，不替代 `features/*` 中的契约。

## 2026-09-10 iOS 体验修复与评分范围收敛

- iOS 根容器改用 Kyant Backdrop 的 `rememberLayerBackdrop` / `layerBackdrop`，底栏、按钮和 Toggle 继续复用上游组件；首帧以同尺寸降级态显示，并在屏外预热交互组件后再开启液态渲染。
- 应用内玻璃开关、减少动态设置现在会作用到实际渲染；关闭玻璃或减少动态时不创建 Backdrop 管线，改用稳定 Surface/Switch，避免冷启动竞争主线程。
- 根 Tab Bar 移除紧凑宽度上限，补齐底部安全区；重复点击当前 Tab 会回到该 Tab 根页面。
- 搜索路由保留类型、年份、播出状态和排序，结果页首次加载不再重复导航或重复请求。
- 作品详情当前只展示 Bangumi 只读评分；Anime 社区评分编辑入口暂缓到后续独立阶段，社区内容失败不会覆盖资料状态，并提供全局轻提示。
- 收藏页默认进入“在看”，统计使用当前远程结果；恢复会话期间不再误显示空收藏。
- 无海报占位统一为中性色，避免按 ID 产生与作品内容无关的随机色块。
- Navigation 3 统一覆盖前进、返回和预测性返回手势：子页面使用 24dp 内的方向性横移；根 Tab 切换保持瞬时切换，避免 Backdrop 交接时整页闪烁；开启“减少动态”时所有转场同样为瞬时切换。
- 根 Tab 转场 metadata 与动画 lambda 保持稳定 identity，避免页面内容刷新时误触发整页动画。
- 底栏点击同时经过直接点击和上游选中同步回调时，适配层按当前 selected index 去重，避免一次点击触发两次根导航。
- 搜索结果页改用 `replaceTop` 更新筛选/查询，避免连续搜索不断堆叠路由并重复播放页面转场。

验证说明：本次修改已通过 `git diff --check` 与目标文件 ktlint；本机 Gradle 构建仍受 `build-logic` 的 Kotlin DSL 插件缓存/网络限制，跨平台编译需在可访问插件源的环境重新执行。

## 2026-08-13 P0 完整交付收口

- 资料链路完成服务端首页缓存、别名搜索、多条件稳定分页、详情与 Bangumi 评分分布/新鲜度展示。
- 认证链路完成 OAuth 身份映射、头像代理、短访问令牌、旋转刷新令牌、当前设备退出和三端安全存储。
- 收藏链路完成五状态幂等写入、进度边界、状态筛选/服务端游标分页、双向同步、冲突列表/解决和真实 pending/failed 计数。
- 收藏 Outbox 会在应用启动和认证恢复后自动排空；评分 Outbox 同步在这两个时机重试。
- 评分链路完成 Anime 1–10 分幂等写入、持久 Outbox 和独立社区聚合，不与 Bangumi 分数混算。
- 短评链路完成 1–500 字发布、最新/最早游标分页、本人软删除、敏感词和服务端频率限制。
- OpenAPI 补齐 Anime 社区评分、个人评分和短评列表/详情/创建/删除端点，合同门禁由 25 条路由扩展为 29 条。
- 后端同步冲突的 `keep_local` 会真实回写 Bangumi，`use_remote` 更新本地；解决前会重新检查并发修改。

本地验证：

- 后端 `cargo clippy --all-targets -- -D warnings` 和 `cargo test` 通过（13 通过、1 忽略；2 个真实网络 E2E 按设计忽略）。
- 前端 `animeCheck` 通过，覆盖 ktlint、合同、模块图、Fixture、Android Lint 与 Desktop/Android/Wasm 编译测试。
- `core:navigation:wasmJsBrowserTest` 因上游 Navigation3/Skiko 测试运行时不解析 `skiko.mjs` 而定向禁用；Wasm 生产编译与浏览器分发仍由交付门禁验证。

## 2026-08-10 真实目录闭环 P1-A

- 后端补齐 `GET /api/v1/home`，以服务端主题镜像生成“最近同步 / 高分动画”真实分区。
- 后端补齐 `GET /api/v1/search/subjects`：优先经服务端统一代理查询 Bangumi，结果规范化写入 PostgreSQL；上游失败时降级到本地镜像搜索。
- 搜索响应支持 `next_offset` 游标，Desktop 新增 `RemoteSearchRepository` 并取代 Dev 运行时的 Fixture 搜索注入。
- 发现页切换到 `/home` 多分区响应，不再把服务端目录包装成单个 Fixture 风格分区。
- 用户收藏同步完成后，后台按 20 条一批渐进补齐浅层主题；请求限速执行，不阻塞 `/me` 返回。
- 搜索命中的主题与收藏、发现、详情共用 Bangumi ID、Anime 海报 URL 和同一服务端缓存记录。

已验证：

- 后端 11 个单元测试通过，严格 Clippy 通过。
- `:data:catalog:desktopTest`、`:shared:app:desktopTest` 与 `:app:desktop:compileKotlin` 通过。
- 本地真实 E2E 返回 2 个发现分区；“大理寺”搜索返回真实条目“大理寺日志”。

P1 后续：剧集/人物/关联资料补齐、统一 stale-while-revalidate 状态提示，以及 Remote Repository 的可注入传输层合同测试。

## 2026-08-10 真实目录与媒体缓存 P0

- Desktop Dev 组合根新增 `RemoteCatalogRepository`，发现与作品详情改读 Anime API；Demo 继续使用 Fixture。
- 公开 Subject ID 统一为 Bangumi ID，收藏条目可直接打开真实详情。
- 后端浅层收藏主题在首次打开时通过官方 API 补齐简介、话数、标签和评分。
- 收藏与主题 DTO 不再向 Desktop 暴露 Bangumi CDN 直链，统一使用 Anime 海报端点。
- 新增服务端海报允许列表、8 MiB 限制、类型校验、本地持久缓存和浏览器缓存头。
- 本阶段仍未替换 `SearchRepository`，搜索真实化属于 P1。

已验证：

- 后端 11 个单元测试通过，1 个真实网络测试按设计忽略。
- `:data:catalog:desktopTest` 与 `:app:desktop:compileKotlin` 通过。
- 本地 PostgreSQL migration 成功；真实 Bangumi 主题详情补齐与 JPEG 海报缓存端到端通过。

## 2026-08-04 Social Core 第二阶段

- 一级导航完成“发现 / 资料库 / 动态 / 我的”迁移，并保持四根独立返回栈。
- 新增 `feature:activity` 与 `feature:community`，Fixture 下可浏览关注动态、长评和片单。
- 作品详情新增 Anime/Bangumi 双评分来源卡、评分编辑入口、热门评价与关联片单。
- “我的”新增评分、评价、片单个人档案入口；收藏归入个人空间。
- 新增 `RatingEditor`、`Review`、`CuratedList` 类型安全路由和恢复序列化测试。

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
