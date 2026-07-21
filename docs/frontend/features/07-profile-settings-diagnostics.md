# 个人、设置与诊断 Feature 契约

## 1. 路由和职责

路由为 `AppRoute.Profile`、`AppRoute.Settings`、`AppRoute.Diagnostics`。Profile 展示会话和入口；Settings 管理本地外观/行为；Diagnostics 仅 Demo/Dev 可见，展示构建和同步诊断，不泄露令牌或正文隐私。

## 2. Profile

匿名状态展示产品说明和登录按钮；登录状态展示头像、显示名、收藏统计、待同步数量、设置和退出。退出前若有 Pending 写操作，确认对话框明确说明本地队列是否保留：默认保留并在同一账户再次登录后恢复，其他账户不可见。

事件：`LoginClicked`、`SettingsClicked`、`DiagnosticsClicked`、`LogoutClicked`、`LogoutConfirmed`。标签：`profile.signedOut`、`profile.login`、`profile.account`、`profile.pendingCount`、`profile.settings`、`profile.diagnostics`、`profile.logout`。

## 3. Settings

固定设置项：

| key | 类型/默认 | 规则 |
|---|---|---|
| theme | System/Light/Dark，默认 System | 即时应用并持久化 |
| dynamicColor | Boolean，默认 true | Android 支持时生效，否则禁用并解释 |
| glassEffects | Auto/On/Off，默认 Auto | Auto 按性能和减少透明度策略决定 |
| reduceMotion | FollowSystem/On/Off，默认 FollowSystem | On 关闭共享元素和弹性过冲 |
| spoilerReveal | TAP，固定 | 首发不可改为自动展开 |
| diagnosticsConsent | Boolean，默认 false | 只控制未来诊断上传；首发无上传实现 |

设置行高至少 64dp，整行可点击；Switch 与行共享同一个语义动作，避免双重焦点。标签使用 `settings.{key}`。

## 4. Diagnostics

只展示：应用版本、Git SHA、BuildProfile、Fixture 版本/场景、数据库 Schema、当前登录状态（匿名/用户 ID 哈希）、网络状态、缓存统计、Outbox 计数、最近 50 条脱敏日志。不得展示 access token、Cookie、完整用户 ID、评论草稿或请求正文。

动作固定为复制诊断摘要、重置 Fixture（Demo）、清空图片缓存、导出脱敏日志。重置 Fixture 必须二次确认；清空缓存不删除收藏或草稿。

标签：`diagnostics.build`、`diagnostics.fixtureScenario`、`diagnostics.outbox`、`diagnostics.copy`、`diagnostics.resetFixture`、`diagnostics.clearImageCache`、`diagnostics.exportLogs`。

## 5. 验收

- FE-PRO-001：匿名和登录两种 Profile 不发生数据串用。
- FE-PRO-002：有待同步数据退出时出现准确确认，重新登录同账户可恢复。
- FE-SET-001：主题、动态色、玻璃和减少动态设置在重启后保持。
- FE-SET-002：减少动态开启时没有必须依赖动画才能理解的信息。
- FE-DIA-001：Prod 无 Diagnostics 路由、入口和 Fixture 依赖。
- FE-DIA-002：corrupted-fixture 的诊断摘要指出文件和引用错误，但不崩溃。
- FE-DIA-003：所有导出内容通过 secret scanner，不包含认证信息。
