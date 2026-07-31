# Anime 后端实施规范索引

> 基线编号：`BES-1.0`<br>
> 状态：Approved<br>
> 适用范围：Rust 模块化单体、Anime API v1、PostgreSQL 与 Bangumi 适配器

## 1. 规范事实源

后端实现同时受以下三类文件约束：

1. 本分册规定模块边界、事务、同步、测试和交付规则；
2. `contracts/openapi/anime-v1.yaml` 是 HTTP 请求与响应的唯一机器可读契约；
3. `contracts/database/migrations/0001_initial.sql` 是 PostgreSQL 初始结构的可执行事实源。

总体产品边界仍以 `docs/anime项目完整详细设计文档.md` 为准。字段或接口发生冲突时，已批准 ADR 优先，其次是本分册与机器契约，再其次是总体设计。不得只修改 Wiki。

## 2. 开发入口

| 问题 | 必读文件 |
|---|---|
| API 字段、状态码、分页、幂等 | `01-api-contract.md`、OpenAPI |
| 表、索引、约束、事务边界 | `02-database-contract.md`、SQL migration |
| Bangumi 映射、限流、重试、冲突 | `03-bangumi-and-sync.md` |
| 测试、阶段和完成定义 | `04-testing-and-delivery.md` |
| CMP 领域语义 | `../frontend/12-domain-repository-contracts.md` |

## 3. 固定技术边界

- Rust + Axum + Tokio，模块化单体，单一 API 二进制；
- PostgreSQL 是事实源，Redis 只允许缓存、限流和短租约；
- Bangumi OAuth 是唯一用户登录方式，Bangumi Token 只保存在服务端；
- Anime 不建设自有评分，API 仅返回 Bangumi 只读评分快照；
- 所有公开 API 位于 `/api/v1`，ID 以字符串传输，时间使用 RFC 3339 UTC；
- 公开列表使用不透明 Cursor；写接口使用 `Idempotency-Key` 或明确的资源幂等语义；
- Handler 不包含 SQL、Bangumi URL 或冲突策略；Application Service 不返回 Axum Response；
- 任何 OpenAPI 破坏性变化必须新增 API 版本或先完成兼容迁移。

## 4. 契约变更流程

1. 先修改 OpenAPI、migration 或本分册；
2. 更新客户端 Domain/Repository 映射和 Fixture；
3. 增加服务端、客户端契约测试；
4. 运行 `python scripts/check_backend_contracts.py`；
5. 运行 `animeCheck` 与 Wiki 同步检查；
6. 在同一个提交或 PR 中评审规范和实现。

## 5. 当前边界

`BES-1.0` 冻结首版公开资料、OAuth 会话、收藏/进度、短评和同步冲突契约。后台管理 UI、Anime 自有评分、私信、通知、图片上传和独立搜索服务不在本基线。
