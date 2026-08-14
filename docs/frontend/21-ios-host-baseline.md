# Anime iOS 宿主与交付基线

> 基线编号：`IOS-1.0`  
> 生效日期：2026-08-14  
> 状态：源码已实现，macOS 发布验证待执行

## 1. 交付边界

`app/iosApp` 是薄 SwiftUI 宿主，页面、导航、状态机、Repository 和领域模型继续由 `shared/app` 与 `commonMain` 提供。共享框架名固定为 `AnimeShared`，目标固定为 `iosArm64` 和 `iosSimulatorArm64`，最低系统版本为 iOS 17。

Windows 上“完成”仅指依赖可解析、iOS metadata 可编译、公共跨端质量门通过。以下门禁只能在 macOS 完成：Kotlin/Native framework 链接、Swift/Xcode 编译、代码签名、模拟器启动、真机 Keychain/授权回调验证和 App Store Archive。

## 2. 平台能力

| 能力 | iOS 实现 | 安全约束 |
|---|---|---|
| HTTP | Ktor Darwin，生产地址 `https://api.jokersh.site` | 禁止 ATS 明文放行 |
| Anime 会话 | Keychain 保存 access/refresh token | token 不进入 `NSUserDefaults` 或日志 |
| 非敏感缓存 | `NSUserDefaults` | 只保存设置、草稿、离线队列和缓存 JSON |
| Bangumi 绑定 | `ASWebAuthenticationSession` 应用内安全授权页 | 客户端不收集 Bangumi 密码；访问由 Anime 服务端代理 |
| 回调 | `anime://bangumi-auth` | 必须同时校验 code 与 state，交换操作由后端完成 |
| UI | Compose Multiplatform + iOS 安全区 | 平台玻璃不支持时使用半透明降级，不阻断内容 |

## 3. Xcode 宿主

- `AnimeIosApp.swift` 负责 SwiftUI 生命周期和 URL 回调；
- `ContentView.swift` 嵌入 `IosBridge.mainViewController`；
- Xcode Build Phase 调用 `:shared:app:embedAndSignAppleFrameworkForXcode`；
- `PrivacyInfo.xcprivacy`、URL Scheme 和 Keychain Group 必须随宿主提交；Universal Link 只有在 AASA 与 Apple entitlement 同时配置后才启用；
- App Icon 使用 1024×1024 原始资产，由 Asset Catalog 生成设备尺寸。

## 4. macOS 验收命令

```sh
./gradlew :shared:app:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project app/iosApp/iosApp.xcodeproj \
  -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build
```

随后必须人工验证：冷启动、登录/注册、应用内 Bangumi 授权、授权取消、回调恢复、Token 重启恢复、退出登录、发现/搜索/动态/收藏/详情、离线缓存、深浅色、动态字体、VoiceOver 和减少动态效果。

## 5. 发布阻塞条件

缺少 Apple Development Team、macOS Runner 或至少一台真机时，代码可以合并但不得标记为 App Store 可发布。若后续启用 Universal Link，还必须先部署 AASA 并增加 Associated Domains entitlement。所有这些外部条件完成后，才将本文状态升级为 `Verified`。
