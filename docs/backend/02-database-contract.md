# PostgreSQL 数据库实施契约

## 1. 可执行结构

`contracts/database/migrations/0001_initial.sql` 是初始 Schema 事实源。文档中的 ER 图用于阅读，不能替代 migration。后续 migration 只能向前追加，不得修改已经进入共享环境的文件。

## 2. 标识和时间

- 用户、会话、评论、同步任务使用 UUID，由应用层生成 UUIDv7；数据库不生成业务 ID。
- Subject、Episode 等 Bangumi 镜像使用内部 `BIGINT GENERATED ALWAYS AS IDENTITY`，`bgm_id` 是唯一外部键。
- 所有时间为 `TIMESTAMPTZ`，应用层统一以 UTC 读写。
- API 对所有 ID 使用字符串，服务端边界完成严格解析。

## 3. 数据所有权

| 数据 | 事实源 | 删除规则 |
|---|---|---|
| Subject/Episode/别名/评分 | Bangumi 镜像 | 不因用户删除而删除 |
| 用户身份与外部账号 | Anime + Bangumi 身份 | 注销时撤销 Token，用户行软删除 |
| 收藏与进度 | Anime 本地目标状态 | 用户注销后级联删除 |
| 评论 | Anime | 软删除，保留审计字段 |
| Outbox/冲突/同步运行 | Anime | 按保留策略清理 |

`raw_data` 只保存经过大小限制的 Bangumi 响应，不得包含 OAuth Token、Cookie 或请求 Header。

## 4. 事务不变量

- 收藏更新、`local_version` 递增、同步状态更新和 Outbox 入队必须处于同一事务。
- 评论创建与幂等记录必须处于同一事务。
- Refresh Token 轮换必须锁定旧 Token，插入新 Token并撤销旧 Token后一起提交。
- 同步 Worker 使用 `FOR UPDATE SKIP LOCKED` 领取 Outbox；租约到期可安全重领。
- 分页同步不得删除当前页面未覆盖的关系。
- Bangumi DTO 完整校验后才允许替换镜像；解析失败保留旧快照。

## 5. 约束规则

- 枚举值由 CHECK 约束限制，应用枚举与 SQL 值逐项对应；
- 计数、版本和观看进度不得为负；
- Rating score 为 null 或 `0..10`，零投票时 score 必须为 null；
- 评论为 1–300 Unicode code points；数据库限制字节/字符上限，精确 code point 校验由应用完成；
- 回复只允许一层，Repository 在事务内验证父评论为同 Subject 的顶级评论；
- URL 的 HTTPS 规则在输入 Mapper 校验，数据库只保存规范化结果。

## 6. Migration 策略

生产执行前必须备份并在同版本副本演练。大表加非空列使用“可空列 → 回填 → 校验 → NOT NULL”分阶段方式。回滚依靠前滚修复和数据库恢复，不维护可能丢数据的自动 down migration。
