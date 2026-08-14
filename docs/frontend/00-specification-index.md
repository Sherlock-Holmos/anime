# Anime CMP 前端实施规范索引

> 基线编号：`FES-2.1`<br>
> 生效日期：2026-08-12<br>
> 适用范围：Compose Multiplatform 的 Desktop 与 Android 客户端<br>
> 规范状态：Approved

## 1. 目标

本组文档不是概念方案，而是实现合同。任何 Agent 在开始任务前，必须先定位需求编号、页面契约、领域接口、Fixture 场景和验收用例；如果实现与文档冲突，应先提交文档变更，禁止在代码中形成隐含的新规则。

## 2. 文档优先级

发生冲突时按以下顺序处理：

1. `product/00-product-vision.md` 产品愿景与领域边界；
2. 已批准的 ADR/决策记录；
3. 本索引和 `10`–`20` 实施规范；
4. `features/` 下的逐 Feature 契约；
5. `01`–`09` 产品、视觉、页面和测试规范；
6. 总体设计文档；
7. 名称中含“探讨”的研究材料。

同一优先级以基线编号较新的内容为准。仍无法判断时不得自行猜测，应登记 `OPEN-*` 决策项并暂停受影响的接口合并。

## 3. 开发入口

| 要解决的问题 | 必读文档 |
|---|---|
| 工程怎么建、依赖用什么版本 | `10-engineering-baseline.md` |
| 状态流、导航、登录门禁、错误如何实现 | `11-runtime-architecture.md` |
| 模型、Repository、缓存与写操作语义 | `12-domain-repository-contracts.md` |
| Demo 数据和所有异常如何复现 | `13-fixture-specification.md` |
| Compose 组件、尺寸、图标、测试标签 | `14-compose-implementation-spec.md` |
| 一个需求由谁实现、如何验收 | `15-requirements-traceability.md` |
| 为什么采用当前方案、什么不能擅自改变 | `16-decision-register.md` |
| Desktop/macOS 与 Mobile/iOS 的视觉原则 | `19-apple-design-baseline.md` |
| 颜色、间距、圆角、栅格和响应式数值 | `20-color-and-layout-specification.md` |
| 某个页面的完整状态机 | `features/` 对应文件 |

## 4. 术语

| 术语 | 唯一定义 |
|---|---|
| Subject | 动画条目；Android 首发阶段不扩展为书籍、游戏等其他类型 |
| Anime 评分 | 用户在 Anime 内提交的 1–10 分个人评分及其社区聚合 |
| Bangumi 评分 | Bangumi 返回的只读外部评分和人数，必须显示来源 |
| Collection | 当前用户对 Subject 的收藏状态及观看进度 |
| Fixture | 可版本化、确定性的本地演示数据和故障脚本 |
| Demo | 仅使用 Fixture，不访问真实网络、不依赖后端的可把玩版本 |
| Dev | 可在 Fixture 与真实后端之间切换的开发版本 |
| Prod | 只接入经过批准的真实服务，不暴露调试入口的发布版本 |
| Effect | 一次性 UI 行为，例如跳转、Snackbar、启动登录；不得承载持久状态 |
| Fresh | 数据仍在 TTL 内；允许直接展示 |
| Stale | 可展示但需要后台刷新；不得等同于错误 |

## 5. 无歧义实施规则

- 所有时间通过 `AppClock`，ID 通过 `IdGenerator`，调度器通过 `AppDispatchers`；测试禁止读取系统时间或随机数。
- 所有列表必须声明稳定 `key` 和 `contentType`。
- 所有交互元素必须使用本规范中的测试标签；文本不是自动化定位器。
- Loading、Empty、Content、Refreshing、Recoverable Error、Blocking Error、Offline、Auth Required 必须逐一有状态和测试。
- UI 不得直接依赖 DTO、数据库实体或网络客户端。
- UI 不得自行拼接后端 URL、解释 HTTP 状态码或写缓存。
- Anime 评分与 Bangumi 评分在命名、视觉和数据源上严格分离；不得混算或用外部评分冒充本站评分。
- 变更领域接口、路由参数、Fixture Schema、视觉 Token 或验收条件，必须先修改文档和决策记录。

## 6. 完成定义

一个前端任务只有同时满足以下条件才算完成：

- 需求编号、实现文件和测试编号可以互相追踪；
- Demo 场景通过，且不需要真实网络；
- Loading/Empty/Error/Offline/长文本/无图/大字体至少各有一个验证；
- TalkBack 顺序、触控尺寸、颜色对比和减少动态效果策略通过检查；
- `animeCheck`、单元测试、截图测试和 Android Debug 构建通过；
- 没有新增未登记的 `TODO`、magic number、裸字符串或未固定版本依赖；
- 对应文档和 Wiki 同步检查为零差异。

## 7. 变更流程

1. 在需求追踪表中定位或新增 `FE-*`；
2. 如涉及取舍，在决策记录新增 `FED-*`；
3. 先修改规范、Fixture 和验收条件；
4. 再修改实现和测试；
5. 执行 `python scripts/sync_wiki.py --check`；
6. 合并主仓库后，由同步流程发布 Wiki。

`FES-2.1` 以产品愿景中的 Foundation → Social Core → Discovery 分期推进；未进入当前阶段的能力不得以不可用入口污染 UI。`ADS-1.0` 与 `ACL-1.0` 自本版本起作为 UI 视觉变更的强制评审基线。
