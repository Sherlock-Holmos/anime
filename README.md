# Anime

面向动漫爱好者的移动端资料与社区应用，采用 Compose Multiplatform、Rust、PostgreSQL 与 Cloudflare Tunnel。

## 文档

- [完整设计基线](docs/anime项目完整详细设计文档.md)
- [CMP Android 无歧义实施基线](docs/frontend/00-specification-index.md)
- [文档维护与 Wiki 同步](docs/README.md)
- [在线 Wiki](https://git.jokersh.site/Holmes/anime/wiki)

主仓库 `docs/` 中的总体设计与前端规范分册共同构成项目设计事实源。在线 Wiki 由同步脚本自动生成，不直接人工维护。

## CMP Android 工程

F0 工程基线已经完成并通过真机冷启动验收，当前进入可并行推进的 F1 Design System 与 F2 契约/Fixture 阶段。Android Application 位于 `app/android`，共享 Compose UI 位于 `shared/app`；领域模型与 Repository 接口位于 `core/model` 和 `data/*`，确定性 Demo 数据位于 `fixtures/v1`。业务页面仍需在对应 Feature 契约下逐步实现。

App Shell 已采用 iOS 26-inspired 的内容层与悬浮 Liquid Glass 功能层，不直接呈现 Material 3 默认顶部栏、底部栏、选中指示器或 Ripple；F1 组件目录已可演示海报卡、紧凑卡片、Bangumi 评分、按钮、状态面板与玻璃降级。Android 已通过统一适配层接入 Kyant Backdrop 2.0.0：API 26–30 使用半透明表面，API 31–32 使用实时模糊，API 33+ 使用受限液态折射。所有用户可见文案和根导航图标均来自 `commonMain` 资源。`AppContainer` 在 Android 壳层创建后注入共享 UI，Feature 模块之间由构建任务阻止直接依赖。

要求：JDK 17、Android SDK 37、Gradle Wrapper 9.5.0。首次使用前在未提交的 `local.properties` 中配置 Android SDK 路径。

```powershell
.\gradlew.bat :app:android:assembleDemoDebug
.\gradlew.bat animeCheck
```

Demo APK 输出到 `app/android/build/outputs/apk/demo/debug/android-demo-debug.apk`。`animeCheck` 当前覆盖 ktlint、KMP host tests、Android unit tests、Lint、SQLDelight 迁移任务、模块依赖边界和 Fixture 完整性；截图、设备和性能测试会在对应 F1/F3/F10 阶段填充既有稳定任务。

允许的应用变体：

| Variant | applicationId | 数据策略 | 诊断 |
|---|---|---|---:|
| `demoDebug` | `site.jokersh.anime.demo` | Fixture only | 开启 |
| `devDebug` | `site.jokersh.anime.dev` | Remote，可切 Fixture | 开启 |
| `prodRelease` | `site.jokersh.anime` | Remote only | 关闭 |

Dev/Prod 的 API 地址通过本机或 CI Gradle 属性 `ANIME_API_BASE_URL` 注入，不写入仓库。
