# 客户端语言系统设计与开发说明

## 1. 目标与范围

客户端首期支持四种语言：

| 显示名称 | 稳定值 | BCP-47 标签 | 说明 |
| --- | --- | --- | --- |
| 跟随系统 | `System` | 由平台决定 | 默认值；不修改操作系统全局语言 |
| 简体中文 | `SimplifiedChinese` | `zh-Hans` | 应用固定使用简体中文 |
| 繁体中文 | `TraditionalChinese` | `zh-TW` | 使用繁体资源；当前 Compose Resources 插件使用 `values-zh-rTW` |
| English | `English` | `en` | 应用固定使用英语 |
| 日本語 | `Japanese` | `ja` | 应用固定使用日语 |

语言切换是应用级设置，必须在重启后保持。语言枚举和持久化属于共享层，页面资源和平台 Locale 应用属于各平台 UI 层。

## 2. 架构决策

```text
                    ┌──────────────────────────┐
                    │ PersistentSettingsStore  │
                    │ anime.settings.language  │
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │ KMP LanguagePreference   │
                    │ System / zh-Hans / ...   │
                    └───────┬─────────┬─────────┘
                            │         │
             ┌──────────────▼───┐ ┌──▼────────────────┐
             │ CMP resources    │ │ Native iOS SwiftUI │
             │ AppLocale bridge │ │ Locale environment │
             └──────┬───────────┘ └──────────┬─────────┘
                    │                        │
       ┌────────────▼───────────┐   ┌────────▼─────────┐
       │ Android / Desktop / Web │   │ .lproj resources  │
       │ app-scoped locale       │   │ Base/en/zh-Hant/ja│
       └─────────────────────────┘   └──────────────────┘
```

共享设置层只保存一个语言来源，避免 iOS 原生模型、KMP 设置仓库和平台系统设置互相覆盖。`System` 只代表“使用平台当前语言”，不能把用户选择写入 `AppleLanguages` 或浏览器全局设置。

## 3. 各平台实现

### iOS

- 原生 SwiftUI 根容器使用 `NativeLanguagePreference` 与 KMP 枚举保持同名 raw value。
- `ContentView` 通过 `.environment(\.locale, ...)` 将应用语言注入整个 SwiftUI 树。
- 本地化资源位于 `app/iosApp/iosApp/{Base,en,zh-Hant,ja}.lproj/Localizable.strings`。
- 使用 `LocalizedStringKey`，使 Tab、设置项和系统组件随 Locale 更新。
- `System` 根据当前设备首选语言映射到四个受支持语言；不改变系统全局语言。

### Android

- 启动时从现有 `anime_settings` SharedPreferences 读取语言，避免首帧先显示错误语言。
- 语言改变后更新 `Configuration` 并重建 Activity，确保 Android framework 与 Compose Resources 同步。
- Compose 运行时通过 `AppLocale` 提供新的 `LocalConfiguration`。

### Desktop

- `AppLocale` 在应用进程内更新 JVM Locale，并通过 CompositionLocal 触发资源重新选择。
- `System` 保留进程启动时的系统 Locale。

### Web

- `AppLocale` 设置应用作用域的 `window.__customLocale`。
- Web 入口在加载 Compose 脚本前代理 `navigator.languages`，仅影响本应用资源解析，不修改浏览器设置。

## 4. 资源约定

默认 `values/strings.xml` 与 `Base.lproj` 使用简体中文作为产品默认文案。新增文案必须同时补充 `values-en`、`values-ja`、`values-zh-rTW` 和 iOS 四套资源；带参数的字符串必须保持参数顺序和数量一致。

禁止在页面中把用户可见文本拼接成不可翻译的长字符串。日期、数量、百分比和错误提示应使用平台 Locale 格式化器或带参数资源。

## 5. 当前交付内容

- 共享 `LanguagePreference`、设置读写、重置和契约测试。
- iOS 原生语言选择、持久化、Locale 注入和首批根导航/设置资源。
- Android 启动恢复、切换重建和 Compose Locale 适配。
- Desktop/Web 的共享 Locale bridge。
- Discover、Subject、根应用现有 Compose 资源的英语、日语、繁体中文版本。

当前资源迁移已建立完整机制，但历史 feature 中仍有部分直接写死的业务文案，后续应按页面逐批迁移到资源文件。未迁移的文案会显示默认简体中文，这是可控的 fallback，不会导致页面空白或崩溃。

## 6. 验收标准

1. 四种语言均可从“我的 → 设置/外观”选择。
2. 杀进程并重新启动后，选择仍然保留。
3. iOS 切换语言不改变系统“设置 → 通用 → 语言与地区”。
4. Android 切换后 Activity 重建，首屏与后续页面语言一致。
5. Discover、作品详情、根 Tab 与设置页面不存在缺失资源 key。
6. 长文本、参数字符串、深色模式、动态字体和 VoiceOver 不因语言切换破坏布局。
7. CI 至少执行 settings contract tests、四个平台共享层编译和 iOS Xcode archive/build。
