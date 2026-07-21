# Demo Fixture 与数据契约总览

> 状态：V1.3 规范性导航页<br>
> 注意：本页不重复定义模型或接口，避免与实施合同形成第二套签名。

## 1. 唯一实施来源

| 内容 | 唯一规范 |
|---|---|
| 依赖、模块、BuildProfile、DataMode | `10-engineering-baseline.md` |
| 状态流、AppContainer、错误、缓存与同步运行时 | `11-runtime-architecture.md` |
| 领域模型、枚举、Repository 函数签名 | `12-domain-repository-contracts.md` |
| Fixture 文件、条目、搜索结果和 12 个场景 | `13-fixture-specification.md` |
| Feature 使用哪些状态和事件 | `features/` 对应契约 |
| 合同测试和需求编号 | `15-requirements-traceability.md` |

任何代码示例、旧探讨文档或 Wiki 历史版本与上述文件不一致时，均以上述文件为准。

## 2. 数据模式

数据模式固定为 Fixture 和 Remote：

- `demoDebug`：只允许 Fixture，任何真实网络请求使测试失败；
- `devDebug`：默认 Remote，诊断面板可切换 Fixture；切换会重建 AppContainer，不在运行中的 Repository 偷换数据源；
- `prodRelease`：只允许 Remote，编译产物不得包含 FixtureLoader、场景文件或诊断路由。

UI、Reducer、UI Model 和路由不得读取 DataMode。差异只存在于 AppContainer 的实现装配和 BuildProfile 的非业务参数中。

## 3. 数据边界

```text
Fixture JSON / Anime API / Bangumi Adapter
                ↓
        DTO + Source-specific validation
                ↓
        Local Entity / Transaction / TTL
                ↓
       Repository domain contract
                ↓
         UI Mapper → UiState → Screen
```

- Feature 不导入 DTO、SQLDelight 生成类型、Ktor、JSON 或平台 Context。
- Domain 不含 Android/Compose/数据库/网络注解。
- `SubjectId/EpisodeId/CharacterId/PersonId` 使用正 `Long`；服务端自有 ID 使用非空 String 值对象。
- Bangumi 评分只读，`score=null,votes=0` 表示无评分；不得转换为本站评分。
- 收藏使用 Wish、Watching、Completed、OnHold、Dropped，未收藏用 `null`。
- 评论正文 1–300 Unicode code points，只允许一层回复。

## 4. Fixture 原则

Fixture Schema 固定为 `anime.fixture/v1`，时钟为 `2026-07-19T08:00:00Z`，种子为 `20260719`。数据必须可再分发、确定、可离线，不引用真实用户资料或受限制海报。12 个条目和 12 个场景的内容与顺序完全由 `13-fixture-specification.md` 定义。

Fixture 的错误不是临时 Mock：慢加载、陈旧缓存、离线、429、401、写入重试和冲突都必须走与 Remote 相同的领域错误和状态转换。

## 5. 本地写入

- 收藏与进度采用 optimistic local write + Outbox；
- 同一条目的连续写入按领域合同折叠，不允许旧响应覆盖新意图；
- 评论首发不做离线乐观发布，失败保留草稿；
- 设置和 Demo 数据使用 SQLDelight，Token 只进入 SecureStorage；
- 重置 Demo 需要确认，只清除 Demo 用户数据并恢复 happy 场景。

## 6. Remote 替换门禁

Remote 接入时不得更改 Composable、UiState 或公开 Repository API。Fake 和 Remote 必须运行同一组 `CT-*` 合同测试；只有当缓存、新鲜度、错误、取消、分页、写入和冲突语义全部一致时才能切换 Dev 默认数据源。

允许 Remote 增加内部 DTO 字段或 API 适配；禁止将 HTTP 状态码、Bangumi 原始枚举、offset/cursor 字符串解释权或 token 生命周期泄漏到 Feature。

## 7. 完成条件

- `fixtureCheck` 验证 Schema、引用、枚举、摘要和 UTF-8；
- Demo 断网可完成发现、搜索、详情、收藏、进度、评论和设置路径；
- 12 个场景都能由诊断面板显式激活并由测试无动画复现；
- Fake/Remote 合同测试使用同一测试工厂；
- Prod Release 的依赖图和 APK 扫描证明不含 Fixture/Diagnostics；
- Wiki 同步检查为零差异。
