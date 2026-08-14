# CMP Android 决策记录

> 状态定义：Approved 可实施；Deferred 首发不得实现；Superseded 仅保留历史。变更 Approved 决策必须新增替代决策，禁止直接改写理由。

## 1. 已批准决策

| ID | 决策 | 理由 | 影响 |
|---|---|---|---|
| FED-001 | Android 首发，但业务/UI 放在 CMP commonMain | 快速获得可玩成果，同时保留后续多端能力 | 平台能力必须经 Port |
| FED-002 | Kotlin 2.4.10 + CMP 1.11.1 + AGP 9.3.0 固定版本 | 形成可重复构建，避免 Agent 自行升级 | 升级单独 PR/决策 |
| FED-003 | 使用 Navigation 3，四根栈 | 类型安全并保持各根状态 | 不引入 Decompose/Voyager |
| FED-004 | UDF + ViewModel + Reducer | 状态转换可测试、适合并行协作 | 不引入第三方 MVI 框架 |
| FED-005 | 手工 AppContainer 构造注入 | 依赖图当前可控，减少生成/平台复杂度 | 禁止 Service Locator/全局单例 |
| FED-006 | Ktor + SQLDelight + Coil | CMP 成熟组合、职责清楚 | DTO/Entity 不进入 UI |
| FED-007 | Local-first Flow + TTL + Outbox | 离线可用并允许后端稍后开发 | 写操作必须显式同步状态 |
| FED-008 | Bangumi 评分只读展示 | 外部评分只作为资料参考 | 不允许客户端改写或与 Anime 评分混算 |
| FED-009 | Fixture 是 Demo 与验收共同数据源 | 后端缺席仍可完整开发和复现 | Demo 禁止真实网络 |
| FED-010 | Backdrop 只经 AnimeGlassPanel 使用 | 保留高质量视觉且可快速降级 | Feature 禁止直接依赖库 |
| FED-011 | Roborazzi 黄金图 + Compose UI/语义测试 | 视觉和人性化设计可回归 | 黄金图变更必须审阅原因 |
| FED-012 | ktlint + Android Lint，不在首版启用 Detekt | 采用稳定、兼容的门禁 | 未来引入需给出规则和基线 |
| FED-013 | minSdk 26、compile/target 37 | 兼顾现代能力与覆盖；依赖下限更低 | 修改需统计目标用户影响 |
| FED-014 | 文档在主仓库为唯一源，Wiki 单向生成 | 防止双向编辑漂移 | Wiki 禁止手工改受管页 |
| FED-015 | 评论是 Anime 自有数据，不聚合 Bangumi 评论 | 权限与社区规则可控 | 数据模型和来源必须隔离 |
| FED-016 | 中文简体为首发唯一完整语言 | 控制首发验收变量，同时保留资源化 | 禁止硬编码用户文案 |
| FED-017 | 普通设置持久化使用 SQLDelight，不额外引入 DataStore | 保持 CMP 存储统一并允许设置与缓存事务化迁移 | Token 仍只能进入 SecureStorage |
| FED-018 | 成品采用 iOS 26-inspired Liquid Glass 设计语言，Android 不以 Material 3 为视觉目标 | 用户要求 Android 优先但跨端视觉统一、强调高质量液态玻璃与内容沉浸 | Material 只作底层；导航、顶部层、图标、动效和表面必须由 Anime Design System 定义 |
| FED-019 | 根底栏固定复用 Kyant 官方 `LiquidBottomTabs` 示例源码 | 官方 Maven 库明确不包含高层组件；隔离 Vendor 可保留 CMP 官方交互并避免 Anime 自行实现 | 固定 `bebb11a9...`；Vendor 文件不做定制，携带 Apache-2.0 License，更新必须整体替换和真机回归 |
| FED-020 | Maven Central 受 Cloudflare 403 阻断时使用带 Content Filter 的阿里云 Central 镜像 | Kotlin 编译插件和 CMP AndroidX 属于可重复构建必需依赖，当前开发网络无法直接取得；镜像内容与 Central 坐标一致 | 镜像只允许 `org.jetbrains.kotlin*`、`org.jetbrains.androidx*`，仍保留固定版本与依赖校验；不得借此引入额外仓库或扩大依赖面 |
| FED-021 | 产品升级为“资料库 + 兴趣档案 + 社区发现”的二次元豆瓣模型 | 单纯资料浏览无法形成记录、表达与再发现闭环 | 正式建设 Anime 评分、评价、片单、关注和动态领域 |
| FED-022 | 一级导航固定为发现、资料库、动态、我的 | 一级入口分别承担内容发现、主动检索、关系流和个人归档 | 收藏不再占用一级入口，归入“我的” |
| FED-023 | Anime 评分与 Bangumi 评分并存但严格隔离 | 同时保留本站社区价值与外部资料参考 | UI 必须显示来源；模型、接口与聚合均不可复用同一字段 |
| FED-024 | 社区能力采用模块化单体领域边界 | 当前规模不需要微服务，但需要防止评论、关系和 Feed 相互污染 | Rating、Review、CuratedList、Social、Activity/Feed 分模型和仓储演进 |
| FED-025 | Remote 模式的 Bangumi 元数据与图片统一经 Anime 后端镜像和缓存 | 客户端网络可能无法访问 Bangumi；直链会造成海报失败、延迟与多端行为不一致 | 客户端禁止直连 Bangumi API/CDN；公开 Subject ID 统一为 Bangumi ID；Fixture 只允许 Demo/测试使用 |

## 2. 性能降级判定

玻璃偏好默认 AUTO，但不是开放选择。基准测试若任一关键场景连续 3 次运行中有 2 次超过 `FE-PERF-002`，该设备档位从 Liquid 逐级降为 Blur、Translucent、None，直到达标。省电模式最高 Translucent；系统减少透明度直接 None；reduceMotion=ON 禁用形变但不强制关闭静态透明。降级不得改变信息层级、点击范围或对比度。

## 3. Deferred

| ID | 能力 | 首发处理 |
|---|---|---|
| FED-D02 | 视频播放/下载 | 剧集只展示元数据 |
| FED-D03 | 私信 | 不创建空接口；关注与动态已由 FED-021 转为 Approved |
| FED-D04 | 评论点赞、编辑、图片、深层回复 | 不出现在 UI |
| FED-D05 | 推送通知 | 不请求通知权限 |
| FED-D06 | Desktop/iOS 发布 | 代码保持可移植，但不作为首发验收 |
| FED-D07 | 多语言完整翻译 | 资源化，只有 zh-CN 作为门禁 |
| FED-D08 | 用户自定义收藏夹与批量操作 | 不实现隐藏入口 |

Deferred 项只有在新增 `FE-*`、Feature 契约、隐私/权限说明和验收矩阵后才能转为 Approved。

## 4. 明确否决的替代方案

- Wiki 与本地文档双向编辑：无法可靠合并，否决。
- 先写真实 API 再补 Demo：会让 UI 被后端进度阻塞，否决。
- 由 Composable 直接请求数据：生命周期和测试不可控，否决。
- 每个 Feature 自选导航/状态管理/DI：形成平行架构，否决。
- 用索引作为 Lazy key、用可见文本作为测试定位：不稳定，否决。
- 所有错误都用全屏错误：破坏已有内容和恢复体验，否决。

## 5. 开放项

`FES-2.0` 发布时没有阻塞 UI Fixture 开发的开放项。开发中新增开放项格式为 `OPEN-YYYY-NNN`，必须包含：问题、可选方案、默认临时行为、影响模块、决策截止点和 Owner。只有默认行为不会改变公共合同或用户数据时，才允许带着开放项继续实现。
