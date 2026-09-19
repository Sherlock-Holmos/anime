# iOS/KMP 质量审计记录

## 当前可执行测试入口

| 范围 | 测试框架 | 命令 | 本次处理 |
| --- | --- | --- | --- |
| KMP common | `kotlin.test` | `./gradlew :shared:app:allTests` | 新增共享会话、OAuth 凭据和 TokenStore 契约测试 |
| KMP iOS Native | `kotlin.test` | `./gradlew :shared:app:iosSimulatorArm64Test` | 新增不依赖网络的 OAuth URL Bridge 测试 |
| SwiftUI host | XCTest / XCUITest | 当前没有 test target | 不新增不可执行的 Swift 测试 |

## 已覆盖

- OAuth 授权地址必须使用 HTTPS。
- 共享 TokenStore 的 access token、refresh token、过期时间读写和清理。
- Fixture session 的不完整 OAuth callback 拒绝、登录完成和 logout 生命周期。
- `IosBridge.handleOpenUrl` 对 scheme/host、code、state 的校验。
- OAuth callback 的覆盖、消费前 pending 状态和清理行为。

## 当前无法稳定落地的测试

### NativeAppModel 状态边界

`NativeAppModel` 位于 iOS SwiftUI 宿主，当前 Xcode 工程只有 `iosApp` application target，没有 XCTest target。直接新增 Swift 测试文件不会被任何命令执行，因此暂不添加伪覆盖。

验收要求：

1. 创建 `iosAppTests` XCTest target。
2. 为 `NativeAppModel` 注入可替换的 facade，而不是在测试中访问真实网络。
3. 验证启动、游客、登录、logout、刷新失败、请求竞态和错误状态清理。

### 片库筛选与点击

筛选状态和 `NavigationLink` 目前直接写在 SwiftUI 页面中，缺少可独立调用的纯函数和 XCTest target。

验收要求：

1. 将筛选输入、筛选结果和选中状态提取为可测试模型。
2. 验证在看、想看、看过、搁置、弃番互斥切换。
3. 验证卡片内容点击只导航到详情，状态按钮点击不触发详情导航。
4. 验证重复 ID、异步刷新和空结果不会扩大点击区域或覆盖旧状态。

### 双入口路由

当前路由选择封装在 `IosBridge` 私有 `ensureApiRoute()` 中，依赖真实 HTTP 请求，无法在现有测试框架中注入假响应。

验收要求：

1. 抽出纯路由决策函数：国内选择直连，其他地区选择 Cloudflare，探测失败回退 Cloudflare。
2. 为 domestic、foreign、unknown、malformed response、timeout 分别建立测试。
3. 验证所有 Repository 在路由变更后使用同一个 base URL。

### Keychain 生命周期

真正的 `IosKeychainSessionTokenStore` 是 `iosMain` 私有实现，依赖 Apple Keychain 和 `NSUserDefaults`，不能由当前 Windows 环境执行。

验收要求：

1. 在 macOS iOS test target 中使用独立 Keychain access group 或测试前缀。
2. 验证新格式记录、旧格式迁移、损坏记录清理、过期 token、refresh token 和 logout 清理。
3. 测试完成后删除测试账户和测试 Keychain 项。

### Bridge 错误映射

Bridge 的 `AppError` 到 Swift completion error 文本映射目前是私有扩展，且公共 API 使用 `(String?, String?)`，无法在不修改生产代码的情况下直接注入和断言完整错误类型。

验收要求：

1. 为错误映射提供稳定的测试入口或 typed error contract。
2. 覆盖 Unauthorized、Timeout、Offline、NotFound、Validation、Server、Upstream 和 Unknown。
3. 验证错误回调不会携带过期 snapshot，也不会在成功回调中残留旧错误。

## 交付门槛

- common tests 全部通过。
- iOS Native tests 在 macOS Simulator 上通过。
- SwiftUI XCTest/XCUITest target 建立后，补齐上面的五类验收项。
- iOS CI 同时执行 KMP iOS Native tests、Swift unit tests 和关键 XCUITest 流程；仅构建 App 不视为测试通过。
