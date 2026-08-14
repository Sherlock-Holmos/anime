# CMP Windows Desktop 工程基线

本文规定 Anime Windows 客户端的当前实现边界、构建入口和验收标准。Desktop 是当前 UI 交付与宽屏体验的主验收平台，业务和领域能力继续与 Android 共用，不复制一套独立实现。

## 1. 平台与模块边界

| 项目 | 约束 |
|---|---|
| UI 技术 | Compose Multiplatform Desktop |
| 运行目标 | Windows x64，JVM 17 字节码 |
| 应用壳 | `app/desktop` |
| 共享 UI | `shared/app/commonMain` |
| 平台实现 | 各共享模块的 `desktopMain` |
| 数据模式 | 当前固定 `FixtureOnly` |
| 发行格式 | 便携目录、Windows EXE；MSI 配置已保留 |

`app/desktop` 是平台 composition root：可以依赖 `shared/app`，并仅为创建平台存储、网络客户端与
Repository 实现而依赖 `core:*`、`data:*`；不得直接依赖 `feature:*` 或承载业务规则。Feature 之间仍禁止
直接依赖，所有桌面业务行为必须复用现有领域模型、Repository 契约、Navigation 3 路由和 Fixture。

## 2. 应用入口

桌面入口为：

```text
app/desktop/src/main/kotlin/site/jokersh/anime/desktop/Main.kt
```

默认窗口尺寸为 `1180 x 820 dp`，最小尺寸为 `840 x 640 dp`，标题为 `Anime`。窗口尺寸、位置与最大化状态通过 Java Preferences 持久化。当前 BuildProfile：

| 字段 | 值 |
|---|---|
| environment | `Demo` |
| dataModePolicy | `FixtureOnly` |
| diagnosticsEnabled | `true` |
| searchPageSize | `10` |

桌面端接入真实后端前，禁止在入口中硬编码 API Token、Bangumi Token 或生产地址。

桌面入口支持仅用于开发验收的启动参数：

| 环境变量 | JVM System Property | 用途 |
|---|---|---|
| `ANIME_DESKTOP_INITIAL_ROOT` | `anime.desktop.initialRoot` | 指定初始根页面：`discover/library/activity/profile`；兼容旧的 `search/timeline` 别名 |
| `ANIME_DESKTOP_INITIAL_QUERY` | `anime.desktop.initialQuery` | 进入搜索后自动提交查询 |
| `ANIME_DESKTOP_GLASS_TIER` | `anime.desktop.glassTier` | 强制玻璃等级：`none/translucent/blur/liquid` |

这些参数不得参与正常用户状态持久化或生产业务分支。

## 3. Desktop KMP 变体

`anime.kmp.library` 为所有共享模块声明名为 `desktop` 的 JVM target，并统一：

- JVM target 17；
- Kotlin language/API 2.4；
- warnings as errors；
- `desktopMain` / `desktopTest` 平台源码集。

新增共享模块时必须使用该约定插件，保证 Android 与 Desktop 至少都能解析其变体。平台能力通过 `expect/actual` 补齐，不允许从 `commonMain` 引用 AWT、Swing 或 Android API。

## 4. Kyant Backdrop 与 Liquid Tabs

Windows 端直接解析 Kyant Backdrop 2.0.0 和 Shapes 1.2.0 的官方 `desktop` JVM 变体。项目只在 `core/designsystem/desktopMain` 中提供平台适配：

- `AnimeBackdropHost` 使用 `rememberLayerBackdrop` 与 `layerBackdrop`；
- Blur 使用 Backdrop `drawPlainBackdrop + blur`；
- Liquid 使用 Backdrop `drawBackdrop + blur + lens`；
- 根底栏继续复用固定上游提交的 `LiquidBottomTabs` 源码；
- `awaitFrame` 在 Desktop 使用 Compose frame clock。

不得为 Windows 重新手写液态玻璃采样、拖拽动画或底栏选中物理。若 Backdrop Desktop 在特定 GPU/驱动异常，只能通过设计系统能力分级降级，不能修改 Vendor 源码掩盖问题。

Windows 本地会话默认启用 `Liquid`；远程桌面会话默认降级到 `Translucent`，也可以通过上述玻璃等级参数做诊断。宽屏侧边栏复用相同的 Backdrop 能力与项目设计系统，但不是对 Kyant `LiquidBottomTabs` 的重写；窄屏根导航仍直接使用上游可拖动液态底栏。

## 5. 构建与产物

开发运行：

```powershell
.\gradlew.bat :app:desktop:run
```

编译验证：

```powershell
.\gradlew.bat :app:desktop:compileKotlin
```

便携应用：

```powershell
.\gradlew.bat :app:desktop:createDistributable
```

输出：

```text
app/desktop/build/compose/binaries/main/app/Anime/
```

Windows EXE 安装包：

```powershell
.\gradlew.bat :app:desktop:packageExe
```

输出：

```text
app/desktop/build/compose/binaries/main/exe/Anime-0.1.0.exe
```

可执行 Uber JAR：

```powershell
.\gradlew.bat :app:desktop:packageUberJarForCurrentOS
```

输出：

```text
app/desktop/build/compose/jars/Anime-windows-x64-0.1.0.jar
```

## 6. 当前完成定义

Windows 工程接入完成必须同时满足：

1. `:app:desktop:compileKotlin` 成功；
2. `:app:desktop:createDistributable` 成功；
3. `:app:desktop:packageExe` 成功；
4. `animeCheck` 与 Android `assembleDemoDebug` 不回归；
5. 便携版 `Anime.exe` 可启动并保持进程存活；
6. 发现、搜索、详情和四根导航复用共享实现；
7. 底栏拖拽与液态效果仍来自 Kyant 上游组件。

## 7. 桌面交互与响应式规则

- `700 dp` 以下使用 Kyant `LiquidBottomTabs`；`700 dp` 及以上使用 196dp 桌面侧栏。
- 搜索结果在 `< 720 dp`、`720–1199 dp`、`>= 1200 dp` 分别使用 1、2、3 列。
- `Ctrl+1/2/3/4` 切换发现、资料库、动态、我的，`Ctrl+K` 进入资料库搜索入口。
- `Escape` 与 `Alt+Left` 请求返回上一层；无可返回页面时保持当前根页面。
- Desktop UI 协程必须引入 `kotlinx-coroutines-swing`，确保 `Dispatchers.Main` 由 Swing dispatcher 提供。仅检查进程存活不能替代窗口无异常对话框的视觉验收。

## 8. 已知未完成项

当前完成的是 Windows 工程和发行基线，不代表桌面产品体验已经完成。后续至少需要：

- 完整的 Tab 焦点顺序、焦点可见性与 Enter/Space 激活验收；
- Windows 深浅色、缩放比例、多屏和高对比度验证；
- Backdrop 在集显、独显和远程桌面环境的性能与降级矩阵测试；
- 桌面截图基线与 UI 自动化；
- 应用图标、签名、版本升级和卸载验收；
- Remote 数据模式所需的 JVM 网络引擎、SQLDelight Desktop driver 与安全存储。

这些项目应在真实后端接入前逐项关闭，避免 Desktop 壳先行造成“多端已经产品化”的歧义。
