# CMP Demo Fixture 规范

> Fixture Schema：`anime.fixture/v1`<br>
> 固定时钟：`2026-07-19T08:00:00Z`<br>
> 随机种子：`20260719`

## 1. 目的和硬约束

Fixture 是 Demo、截图测试和验收测试的共同数据源。相同版本、场景、查询和设备配置必须产生相同结果。

- Demo 构建不得发出网络请求；发现网络调用立即测试失败。
- Fixture 内容使用虚构名称、合成色块图，不依赖受版权约束的海报。
- ID、排序、分页、时间、延迟和错误均来自文件，不允许运行时随机生成。
- Schema 不兼容变更必须新建 `v2`，不得静默改变 `v1` 语义。
- 文件编码统一为 UTF-8、LF，不使用 BOM、双向控制符或易混淆 Unicode 标点。

## 2. 目录和文件职责

```text
fixtures/v1/
├── manifest.json      # 版本、时钟、种子、默认场景和文件摘要
├── subjects.json      # 条目、话数、角色、制作人员和关联条目
├── discovery.json     # 发现页分区和稳定顺序
├── search.json        # 建议词、规范化规则和预期命中
├── collections.json   # 初始收藏、进度和待同步写操作
├── comments.json      # 评论树、折叠与权限样例
├── users.json         # 匿名用户和已登录演示用户
└── scenarios.json     # 延迟、失败、离线、缓存和冲突脚本
```

`manifest.json` 中列出的文件必须全部存在；加载器启动时验证 Schema、引用完整性和 SHA-256。验证失败时 Demo 必须进入明确的 Blocking Error，不得降级为部分数据。

## 3. 条目目录

固定包含 12 个虚构条目，所有列表都引用以下 ID：

| ID | 标题 | 用于覆盖的边界 |
|---:|---|---|
| 1001 | 星海邮差 | 标准连载、评分 8.6、已收藏 |
| 1002 | 雨城备忘录 | 完结、长简介、多标签 |
| 1003 | 玻璃庭院 | 无海报、无评分、未开播 |
| 1004 | 十二点的电车 | 超长标题、单集动画 |
| 1005 | 北岸信号 | 评分人数为 1、进度为 0 |
| 1006 | 纸月亮计划 | 24 集、看到一半、待同步 |
| 1007 | 无声航线 | 没有简介、没有角色数据 |
| 1008 | 红茶侦探社 | 多季关联、评论很多 |
| 1009 | 夏末天文台 | 已看完、评论含剧透 |
| 1010 | 零号花园 | 收藏冲突样例 |
| 1011 | 风经过旧书店 | 中英日混排标题 |
| 1012 | 最后一片云 | 超长制作人员名单和 48 集 |

统一规则：`subjectId` 为正整数 JSON number，与 `SubjectId(Long)` 一致；评分范围 `0.0..10.0`，人数非负；无评分时 `score=null,votes=0`；话数未知使用 `null` 而不是 `0`；图片使用 `ImageRef.Resource("poster/{id}")`，由 FixtureImageProvider 按 ID 生成确定性色块。

## 4. 发现页固定分区

| sectionId | 标题 | Subject 顺序 | 展示方式 |
|---|---|---|---|
| airing | 正在热播 | 1001, 1006, 1008, 1011, 1012 | 横向卡片 |
| top-rated | 高分精选 | 1002, 1009, 1001, 1008 | 横向紧凑卡片 |
| upcoming | 即将开播 | 1003, 1004 | 横向卡片 |
| continue | 继续观看 | 1006, 1001 | 进度卡片，仅登录态 |

刷新成功后 `generatedAt` 前进 60 秒但内容和顺序不变；测试通过 Fixture Clock 推进，不修改系统时钟。

## 5. 搜索合同

- 预处理只做 Unicode NFC、首尾空白裁剪和英文字母小写转换；不做拼音或模糊分词。
- 空查询展示最近搜索：`星海`、`侦探`、`garden`。
- `星海` → 1001；`侦探` → 1008；`garden` → 1003, 1010；`月` → 1006；`全部` → 1001..1012；`不存在` → 空结果。
- 建议词最多 8 条，结果页每页 5 条；第二页请求只能由首屏接近末尾触发一次。
- 搜索历史最多 10 条，去重后最新置顶；Demo 重置后恢复默认三条。

## 6. 收藏和评论初始状态

登录用户 `user-demo`：

| Subject | 收藏状态 | 进度 | 同步状态 |
|---:|---|---:|---|
| 1001 | Watching | 3/12 | Synced |
| 1002 | Wish | 0/12 | Synced |
| 1006 | Watching | 12/24 | Pending |
| 1009 | Completed | 12/12 | Synced |
| 1010 | OnHold | 4/12 | Conflict |

评论固定 18 条：1008 有 10 条用于分页，1009 有 5 条且其中 2 条标记剧透，1001 有 3 条；嵌套只允许一层回复。`comment-demo-locked` 不属于当前用户且不可删除；`comment-demo-own` 可删除。Demo 新评论 ID 依次为 `local-comment-0001`、`0002`。

## 7. 场景脚本

| scenarioId | 网络/缓存 | 固定行为 | 必须出现的 UI |
|---|---|---|---|
| happy | 在线/新鲜 | 读 120ms，写 180ms，全部成功 | 正常内容 |
| cold-slow | 在线/无缓存 | 首读 1600ms | 骨架屏后内容 |
| refresh-slow | 在线/陈旧 | 旧数据立即返回，刷新 1800ms | 内容上刷新指示 |
| empty-search | 在线/新鲜 | 所有非空搜索返回空 | 空结果建议 |
| offline-cached | 离线/陈旧 | 网络拒绝，缓存可读 | 离线提示和旧内容 |
| offline-empty | 离线/无缓存 | 网络拒绝 | 阻断错误和重试 |
| server-error | 在线/无缓存 | 读请求返回 Server | 服务异常和重试 |
| rate-limited | 在线/无缓存 | 首次返回 429，retryAfter=30s | 倒计时，不自动风暴重试 |
| unauthorized | 在线/新鲜 | 第一次受保护写操作返回 401 | 登录门禁并保留动作 |
| write-retry | 在线/新鲜 | 写先入队，前两次失败、第三次成功 | Pending → Synced |
| collection-conflict | 在线/新鲜 | 1010 返回远端较新版本 | 冲突对话框 |
| corrupted-fixture | 不适用 | 引用缺失 | 诊断型 Blocking Error |

延迟由虚拟调度器执行；单元测试可以 `advanceUntilIdle`，截图测试默认跳过动画并直接落到指定状态。

## 8. 加载器接口

```kotlin
interface FixtureLoader {
    val manifest: FixtureManifest
    suspend fun activate(scenarioId: String)
    suspend fun reset()
    fun currentScenario(): StateFlow<FixtureScenario>
}

interface FixtureImageProvider {
    fun model(uri: String): Any
}
```

`activate` 是原子操作：场景不存在时保持原场景并返回领域错误。`reset` 清理运行期搜索历史、本地评论、收藏修改和请求计数，再恢复 `happy`。Prod 源集不得依赖 FixtureLoader。

## 9. 引用和完整性校验

- discovery、collection、comment 中的 `subjectId` 必须在 subjects 中存在。
- `episode.current <= episode.total`；total 为 null 时只验证 current 非负。
- 评论 parent 必须存在且不能形成环；深度不得超过 1。
- 所有枚举值必须匹配领域文档，未知值使校验失败。
- 每个场景必须声明 `readDelayMs`、`writeDelayMs`、`network`、`failurePlan`。
- 所有 ID 在同类型集合内唯一，数组顺序具有业务意义。

固定校验任务为 `fixtureCheck`，并由根任务 `animeCheck` 调用。CI 必须验证 JSON 可解析、Schema 版本正确、引用完整、文件摘要匹配和 Demo 无真实网络能力。

## 10. 场景到验收用例

| 场景 | 必测需求 |
|---|---|
| happy | FE-DIS-001、FE-SEA-002、FE-SUB-001、FE-COL-001 |
| cold-slow / refresh-slow | FE-STA-001、FE-STA-002 |
| empty-search | FE-SEA-003 |
| offline-cached / offline-empty | FE-OFF-001、FE-OFF-002 |
| unauthorized | FE-AUTH-002 |
| write-retry | FE-SYNC-001 |
| collection-conflict | FE-SYNC-002 |
| corrupted-fixture | FE-DIA-002 |

新增界面状态时，若不能由现有场景稳定复现，该状态不得合并，必须先扩展 Fixture Schema 或场景。
