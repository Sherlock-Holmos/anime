# Anime

面向动漫爱好者的移动端资料与社区应用，采用 Compose Multiplatform、Rust、PostgreSQL 与 Cloudflare Tunnel。

## 文档

- [产品愿景与功能架构 V2.0](docs/product/00-product-vision.md)
- [完整设计基线](docs/anime项目完整详细设计文档.md)
- [CMP 无歧义实施基线](docs/frontend/00-specification-index.md)
- [后端可执行契约基线](docs/backend/00-backend-specification-index.md)
- [文档维护与 Wiki 同步](docs/README.md)
- [在线 Wiki](https://git.jokersh.site/Holmes/anime/wiki)

主仓库中的总体设计、前后端规范、OpenAPI 和 PostgreSQL migration 共同构成项目事实源。在线 Wiki 由同步脚本自动生成，不直接人工维护。

## CMP 客户端工程

Android Application 位于 `app/android`，Windows Desktop Application 位于 `app/desktop`，WebAssembly Application 位于 `app/web`，共享 Compose UI 位于 `shared/app`；领域模型与 Repository 接口位于 `core/model` 和 `data/*`，确定性 Demo 数据位于 `fixtures/v1`。三端复用相同的导航、页面、设计系统与 Remote Repository；`demoDebug` 单独保留 Fixture，浏览器端对不支持的液态玻璃能力降级为半透明表面。

App Shell 已采用 iOS 26-inspired 的内容层与悬浮 Liquid Glass 功能层，不直接呈现 Material 3 默认顶部栏、底部栏、选中指示器或 Ripple；F1 组件目录已可演示海报卡、紧凑卡片、Bangumi 评分、按钮、状态面板与玻璃降级。Android 已通过统一适配层接入 Kyant Backdrop 2.0.0：API 26–30 使用半透明表面，API 31–32 使用实时模糊，API 33+ 使用受限液态折射。所有用户可见文案和根导航图标均来自 `commonMain` 资源。`AppContainer` 在 Android 壳层创建后注入共享 UI，Feature 模块之间由构建任务阻止直接依赖。

要求：JDK 17、Android SDK 37、Gradle Wrapper 9.5.0；构建 Web 时还需安装 Node.js 并加入 `PATH`，也可通过 `NODE_BINARY` 指定可执行文件。首次使用前在未提交的 `local.properties` 中配置 Android SDK 路径。

```powershell
.\gradlew.bat :app:android:assembleDemoDebug
.\gradlew.bat :app:android:assembleDevDebug
.\gradlew.bat :app:desktop:run
.\gradlew.bat :app:web:wasmJsBrowserDevelopmentRun
.\gradlew.bat :app:web:wasmJsBrowserDistribution
.\gradlew.bat :app:desktop:createDistributable
.\gradlew.bat :app:desktop:packageExe
.\gradlew.bat animeCheck
```

Demo APK 输出到 `app/android/build/outputs/apk/demo/debug/android-demo-debug.apk`，真实数据 Dev APK 输出到 `app/android/build/outputs/apk/dev/debug/android-dev-debug.apk`。Android 模拟器中的 Dev 版本默认访问宿主机 `http://10.0.2.2:8080`；真机或远程环境应使用 `-PANIME_API_BASE_URL=https://你的API域名` 构建。Android 已声明网络权限、接收 `anime://bangumi-auth` OAuth 回调，并使用 Android Keystore + AES-GCM 保护本地 Session。

Windows 便携应用输出到 `app/desktop/build/compose/binaries/main/app/Anime`，EXE 安装包输出到 `app/desktop/build/compose/binaries/main/exe/Anime-0.1.0.exe`。桌面端当前使用 Demo/Fixture 配置，已支持宽屏玻璃侧栏、1/2/3 列搜索结果、常用快捷键和窗口状态持久化；完整焦点遍历、多环境显示与 GPU 性能矩阵仍待验收，不应把“可以构建运行”误认为“已完成桌面产品化”。

Web 开发服务器由 `wasmJsBrowserDevelopmentRun` 启动；源码目录中的 `index.html` 不能通过 `file://` 直接运行（Wasm、字体和 Compose 资源必须经 HTTP 提供）。开发静态文件输出到 `app/web/build/dist/wasmJs/developmentExecutable`，生产静态文件输出到 `app/web/build/dist/wasmJs/productionExecutable`。Web 已接入真实目录、搜索、详情、社区和 Session Repository，本地开发默认访问 `http://127.0.0.1:8080`；部署时默认使用页面同源 API，也可通过 `?api=https%3A%2F%2Fapi.example.com` 写入浏览器配置。Web OAuth 可解析返回页面中的 `?code=...&state=...`；Bangumi 开发者后台必须把回调地址登记为实际 Web HTTPS 地址，不能继续使用只适用于原生客户端的 `anime://bangumi-auth`。

允许的应用变体：

| Variant | applicationId | 数据策略 | 诊断 |
|---|---|---|---:|
| `demoDebug` | `site.jokersh.anime.demo` | Fixture only | 开启 |
| `devDebug` | `site.jokersh.anime.dev` | Remote，可切 Fixture | 开启 |
| `prodRelease` | `site.jokersh.anime` | Remote only | 关闭 |

Android Dev/Prod 的 API 地址通过本机或 CI Gradle 属性 `ANIME_API_BASE_URL` 注入，不写入仓库；未注入时只有 Android Dev 使用模拟器宿主机默认值，Prod 仍要求显式配置 HTTPS 地址。

后端开发以 `docs/backend/00-backend-specification-index.md` 为入口；HTTP 字段和状态码以 `contracts/openapi/anime-v1.yaml` 为机器事实源，PostgreSQL 初始结构以 `contracts/database/migrations/0001_initial.sql` 为可执行事实源。运行 `python scripts/check_backend_contracts.py` 可在实现前检查文档、OpenAPI 引用、公开路径和核心表是否完整。
