# Bangumi 适配与双向同步契约

## 1. 适配器边界

后端只能通过 `CatalogSource` 和 `UserLibrarySource` 访问 Bangumi。Application Service 不引用 Bangumi URL、整数枚举或原始 DTO。禁止抓取 HTML、伪造官方客户端或从客户端直传 Bangumi Token。

## 2. 元数据映射

- Bangumi subject ID 映射 `subjects.bgm_id`；
- 原始名、中文名和 aliases 规范化后分别保存；
- Bangumi 评分只映射 `score/votes/distribution/source_updated_at`；
- 缺少评分返回 null，不能使用 0；
- 未知媒体类型映射 `other`，未知放送状态映射 `unknown`；
- 原始响应可以进入 `raw_data`，但 Domain 和 API 不依赖其未声明字段。

## 3. 请求治理

- 所有请求携带项目可识别的 User-Agent；
- 全局并发、每用户并发和请求速率由配置限制；
- 429 尊重 `Retry-After`，没有该 Header 时使用带抖动的指数退避；
- 只对网络错误、429 和明确的 5xx 重试；4xx 参数或权限错误不自动重试；
- 日志只记录 route class、状态码、耗时、request id，不记录 Token 或完整用户正文。

## 4. 收藏与进度同步

本地更新先落库并写 Outbox，API 返回 `pending`，不等待 Bangumi。Worker 推送成功后更新 `last_synced_local_version` 和远端快照。拉取远端时：

- 本地没有未同步字段：接受远端；
- 本地和远端修改不同字段：字段级合并；
- 同一字段双方都变更：写入 `sync_conflicts`，状态为 `conflict`；
- 用户选择 keep_local 后产生新版本和 Outbox；
- 用户选择 use_remote 后以远端值覆盖本地并清除对应 dirty field；
- later 不修改数据，只保留冲突。

同一 Outbox `target_version` 重试必须幂等；旧版本任务不得覆盖更新版本。

## 5. 可观测性

每次批量同步写 `sync_runs`，记录成功、失败、429、游标和脱敏错误摘要。指标至少包括请求延迟、上游错误率、Outbox 堆积、最老任务年龄、冲突数和授权失效数。
