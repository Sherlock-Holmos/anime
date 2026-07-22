# CMP Android 需求追踪与验收矩阵

> 规则：每项需求必须有一个 Owner Feature、一个 Fixture 场景和至少一个自动化测试 ID。实现合并时将“实现位置”替换为实际文件路径，但不得更改需求 ID。

## 0. 当前实现基线

| 范围 | 实现位置 | 已验证 | 状态 |
|---|---|---|---|
| 语义 Token 与主题 | `core/designsystem/.../AnimeTokens.kt`、`AnimeTheme.kt` | `animeKtlintCheck`、Android 编译 | 已落地 |
| 公共作品卡、评分、按钮和状态面板 | `core/designsystem/.../AnimeComponents.kt` | Android 编译、Demo Catalog | F1 进行中 |
| Glass 能力与确定性降级 | `core/designsystem/.../AnimeGlass.kt`、`PlatformGlass.kt`、`PlatformGlass.android.kt` | `GlassTierTest`、`PlatformGlassTest`、Android 编译 | Kyant Backdrop 2.0.0 Android Adapter 已落地；动态省电/低内存/用户设置能力注入及四级截图仍属于 F1 |
| iOS 26-inspired App Shell 与根导航 | `core/vendor/kyant-liquid-tabs`、`core/designsystem/.../AnimeNavigation.kt`、`PlatformGlass.android.kt`、`shared/app/.../AnimeApp.kt`、`composeResources/drawable/ic_*.xml` | Vendor 与上游文本一致性检查、`animeCheck`、`assembleDemoDebug`、API 36 冷启动/点击/拖动截图 | 根底栏复用固定上游提交的官方 `LiquidBottomTabs`；Anime 仅保留语义和导航适配，并以稳定 selected-index Provider 保持官方动画状态。API 36 已验证点击时液态选中层跟随、从“搜索”拖至“收藏”完成形变/吸附并切换页面；200% 字体及四级 Glass 黄金图仍属于 F1 |
| 发现页 Repository 纵向切片 | `data/catalog/.../FixtureCatalogRepository.kt`、`feature/discover/.../DiscoverRoute.kt`、`DiscoverViewModel.kt`、`DiscoverReducer.kt`、`DiscoverUiMapper.kt`、`DiscoverScreen.kt`、`shared/app/.../AppContainer.kt` | `FixtureCatalogRepositoryContractTest`、`DiscoverReducerTest`、`animeCheck`、`assembleDemoDebug` | Demo 已移除页面内静态 Fixture，改由正式 `CatalogRepository` 状态流驱动；首次骨架、保留内容刷新、空/离线/阻断错误 UI、4 分区与继续观看横卡已落地。Fixture 当前为领域记录映射，JSON DTO 直读/SQLDelight、场景面板、Snackbar、详情导航和 Compose UI/截图测试仍属于 F2/F4 |
| 领域模型与错误/资源状态 | `core/model/...` | `DomainValidationTest`、Android 编译 | 已落地 |
| 六组 Repository 合同 | `data/catalog`、`data/collection`、`data/comment`、`data/session`、`data/settings` | Android 编译、各模块 `allTests` | 接口与可复用 CT 测试基类已落地；Remote 复用和完整异常/并发语义未完成 |
| Fixture v1 | `fixtures/v1`、`core/testing/.../FixtureLoader.kt`、`data/catalog/.../FixtureCatalogRepository.kt` | `fixtureCheck`、`FixtureLoaderTest`、`FixtureCatalogRepositoryContractTest` | 数据、固定时钟/种子、12 场景、哈希及原子场景切换已落地；发现目录的确定性领域映射已接入 Demo，完整 JSON DTO 映射、场景行为和 SQLDelight 装载未完成 |
| F1 可视目录 | `shared/app/.../DesignSystemCatalog.kt` | `assembleDemoDebug` | 已接入 Demo Shell |

本表中的“进行中/未完成”是强制交付边界，不能因接口或静态数据已经存在而宣称对应阶段完成。

## 1. 产品与导航

| 需求 | 验收结果 | Owner | 场景 | 测试 ID |
|---|---|---|---|---|
| FE-NAV-001 | 四根栈保存路由和滚动状态 | App Shell | happy | UI-NAV-001 |
| FE-NAV-002 | Back 顺序唯一且符合平台习惯 | App Shell | happy | UI-NAV-002 |
| FE-AUTH-001 | 匿名可浏览，不被强制登录 | App Shell | happy | UI-AUTH-001 |
| FE-AUTH-002 | 登录后受保护动作只重放一次 | App Shell | unauthorized | UI-AUTH-002 |
| FE-DIS-001..004 | 发现内容、骨架、刷新和边界正确 | Discover | happy/cold-slow/refresh-slow | UI-DIS-001..004 |
| FE-SEA-001..005 | 搜索并发、结果、空态、分页、历史正确 | Search | happy/empty-search | VM-SEA-001..005 |
| FE-SUB-001..005 | 详情模块、缺失数据、离线和评分语义正确 | Subject | happy/offline-cached | UI-SUB-001..005 |
| FE-COL-001..003 | 收藏分组、匿名态和合并写入正确 | Collection | happy/write-retry | UI-COL-001..003 |
| FE-COM-001..005 | 评论分页、剧透、登录、草稿和权限正确 | Comment | happy/offline-empty/unauthorized | UI-COM-001..005 |
| FE-PRO-001..002 | Profile 隔离账户并保护待同步数据 | Profile | happy/write-retry | UI-PRO-001..002 |
| FE-SET-001..002 | 设置持久化且减少动态可用 | Settings | happy | UI-SET-001..002 |
| FE-DIA-001..003 | 诊断隔离、可解释且无秘密信息 | Diagnostics | corrupted-fixture | BUILD-DIA-001, UI-DIA-002..003 |

## 2. 横切需求

| 需求 | 验收结果 | 责任模块 | 场景 | 测试 ID |
|---|---|---|---|---|
| FE-STA-001 | 冷启动有同尺寸 Skeleton | Design System | cold-slow | SHOT-STA-001 |
| FE-STA-002 | 后台刷新不清空已有内容 | Runtime | refresh-slow | VM-STA-002 |
| FE-OFF-001 | 离线有缓存继续可读 | Data/Runtime | offline-cached | CT-OFF-001 |
| FE-OFF-002 | 离线无缓存显示阻断错误和重试 | Runtime | offline-empty | UI-OFF-002 |
| FE-SYNC-001 | 可重试写入最终同步且不丢意图 | Collection Data | write-retry | CT-SYNC-001 |
| FE-SYNC-002 | 收藏冲突两种解决策略可复现 | Collection | collection-conflict | UI-SYNC-002 |
| FE-A11Y-001 | 全部点击目标 ≥48dp | Design System | happy | SEM-A11Y-001 |
| FE-A11Y-002 | fontScale 2.0 无关键裁切 | All Features | happy | SHOT-A11Y-002 |
| FE-A11Y-003 | 状态不只用颜色表达 | Design System | all | SEM-A11Y-003 |
| FE-PERF-001 | 冷启动满足 P50/P95 预算 | Benchmark | happy | MACRO-PERF-001 |
| FE-PERF-002 | 关键列表帧超时率 <5% | Benchmark | happy | MACRO-PERF-002 |
| FE-SEC-001 | Prod 不包含 Fixture/Diagnostics | Build Logic | n/a | BUILD-SEC-001 |
| FE-SEC-002 | 导出和日志不含认证秘密 | Core Logging | all | UNIT-SEC-002 |

## 3. 数据合同测试

| 接口 | 成功 | 缓存/离线 | 错误 | 并发/一致性 |
|---|---|---|---|---|
| CatalogRepository | CT-CAT-001 | CT-CAT-002 | CT-CAT-003 | CT-CAT-004 |
| SearchRepository | CT-SEA-001 | CT-SEA-002 | CT-SEA-003 | CT-SEA-004 generation/cursor |
| CollectionRepository | CT-COL-001 | CT-COL-002 | CT-COL-003 | CT-COL-004 optimistic/conflict |
| CommentRepository | CT-COM-001 | CT-COM-002 | CT-COM-003 | CT-COM-004 duplicate submit |
| SessionRepository | CT-AUTH-001 | CT-AUTH-002 | CT-AUTH-003 | CT-AUTH-004 account isolation |
| SettingsRepository | CT-SET-001 | CT-SET-002 | CT-SET-003 | CT-SET-004 atomic update |

每个合同测试必须对 Fixture 与真实实现运行同一测试套件；真实后端尚未接入时使用 in-memory fake 证明接口语义，不能删除测试。

## 4. 截图清单

固定黄金图 ID：`SHOT-DIS-CONTENT-LIGHT`、`SHOT-DIS-LOADING-DARK`、`SHOT-SEA-EMPTY`、`SHOT-SUB-FULL`、`SHOT-SUB-MISSING`、`SHOT-COL-SIGNED-OUT`、`SHOT-COL-CONFLICT`、`SHOT-COM-SPOILER`、`SHOT-SET-200FONT`。每张图同时记录基线编号、Fixture 版本、设备、主题和玻璃质量。

## 5. 合并门禁

| 变更类型 | 最低必过 |
|---|---|
| Domain/Repository | 单元测试 + 对应 CT + fixtureCheck |
| Reducer/ViewModel | 单元测试 + 对应 VM |
| 公共组件/Token | 组件测试 + 全部受影响 SHOT + A11Y |
| 页面布局 | 对应 UI + SHOT + 360/600/1200dp Preview |
| 导航/登录 | 全部 NAV/AUTH 回归 |
| 构建配置 | animeCheck + demoDebug + prodRelease 编译 + SEC |
| 性能/玻璃 | Macrobenchmark + Liquid/Blur/Translucent/None 截图 |

任何 `FE-*` 没有测试映射、任何测试没有需求来源，均视为追踪断裂并阻止合并。

## 6. 多 Agent 交付模板

每个任务描述必须包含：需求 ID、允许修改的模块、前置依赖、输入/输出合同、Fixture 场景、测试 ID、不在范围内的事项。交付说明必须列出实际实现文件、测试结果、规范偏差（应为无）和新增决策。Agent 不得跨模块复制模型来绕过依赖。
