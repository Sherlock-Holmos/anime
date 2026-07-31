# 后端测试与交付基线

## 1. 测试分层

| 层级 | 必测内容 |
|---|---|
| Domain | 校验、状态机、冲突合并、Cursor fingerprint |
| Repository | PostgreSQL 事务、约束、并发、幂等 |
| API contract | OpenAPI 请求/响应、状态码、认证和错误体 |
| Bangumi adapter | 固定录制响应、未知字段、429、超时和授权失效 |
| Integration | Axum + 临时 PostgreSQL，从 migration 空库启动 |
| End-to-end | OAuth ticket、收藏 pending→synced、冲突解决、评论权限 |

测试不得访问真实 Bangumi 账号。外部响应使用脱敏 Fixture；时间、ID、随机退避均注入可控端口。

## 2. 合并门禁

- OpenAPI 可解析，所有 `$ref` 可解析，operationId 唯一；
- SQL migration 禁止 BOM、禁止 `DROP DATABASE/SCHEMA`，空库事务执行成功；
- API 实现不得返回 OpenAPI 未声明的成功或业务错误结构；
- Rust format、Clippy warnings-as-errors、单元与集成测试通过；
- CMP Remote Repository 通过与 Fixture 相同的契约测试；
- 文档和 Wiki 同步无差异。

## 3. 交付阶段

1. B0：Rust workspace、配置、日志、健康检查、migration runner；
2. B1：公开 Subject/Search/Home API 与 Bangumi 只读镜像；
3. B2：OAuth、Anime 会话、Token 轮换；
4. B3：收藏/进度事务、Outbox 和双向同步；
5. B4：评论、举报和基础治理；
6. B5：冲突处理、运维指标、备份恢复和性能验收。

## 4. 完成定义

一个接口只有在 OpenAPI、Handler、Application Service、Repository、migration、契约测试、日志/指标和 CMP Remote 映射全部完成后才算完成。只返回 Fixture、只实现 Happy Path 或只在 Swagger UI 中可调用都不能标记为完成。
