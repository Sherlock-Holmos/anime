# Anime API v1 实施契约

## 1. 机器契约

`contracts/openapi/anime-v1.yaml` 是 API 字段、必填性、枚举、状态码和安全要求的唯一机器可读事实源。本文件解释不能仅靠 Schema 表达的运行时规则。

## 2. 通用规则

- Base path 为 `/api/v1`，生产环境只允许 HTTPS。
- JSON 字段统一 `snake_case`；未知响应字段必须被客户端忽略。
- Subject、Episode、Character 和 Person 的 API `id` 在 V1 对应 Bangumi 稳定 ID；PostgreSQL 内部主键永不暴露。
- 所有响应携带 `X-Request-Id`；成功体使用 `{data, meta}`，错误体使用 `{error}`。
- `meta.next_cursor` 为不透明、带版本和签名的游标。查询条件、排序或用户改变后旧游标失效并返回 `400 CURSOR_INVALID`。
- 列表 `limit` 默认 20、范围 1–50；服务端可以降低但不能扩大客户端请求值。
- Bearer access token 只放在 Authorization Header。刷新令牌只出现在 `/auth/refresh` 请求体，并在每次刷新后轮换。
- 写请求的 `Idempotency-Key` 为 16–128 个可打印 ASCII 字符；同一用户、路由和 Key 在 24 小时内必须返回等价结果。相同 Key 携带不同正文返回 `409 IDEMPOTENCY_CONFLICT`。
- 登录前接口使用 request/ticket/token-family 的服务端摘要作为幂等 scope，数据库不得保存明文 Token。
- 资源更新使用 `expected_version` 乐观锁；版本不一致返回 `409 VERSION_CONFLICT`。

## 3. DTO 边界

API DTO 不等同于 PostgreSQL 行，也不直接进入 Compose：

```text
Bangumi DTO -> 校验/规范化 -> Domain -> PostgreSQL
PostgreSQL -> Domain -> API Response DTO
API Response DTO -> CMP Mapper -> CMP Domain
```

枚举的线上值固定为 OpenAPI 中的小写 `snake_case`。未知上游枚举只能映射为既有 `unknown/other`，不得把 Bangumi 原始整数透传为 Anime API 枚举。

## 4. 认证与 OAuth

1. App 调用 `/auth/bangumi/start`，携带 HTTPS 回调地址和可选 pending action；
2. 服务端保存一次性 state，返回 Bangumi HTTPS 授权 URL；
3. HTTPS 回调由服务端交换 Bangumi code，并生成只能使用一次、短期有效的 App ticket；
4. App 调用 `/auth/bangumi/callback` 交换 ticket，获得 Anime access/refresh token pair；
5. Access token 默认 15 分钟；Refresh token 默认 30 天并按 family 轮换；
6. 检测到旧 refresh token 重放时撤销整个 family。

OAuth code、Bangumi Token 和 Anime access token不得进入 URL Query、日志、埋点或错误详情。

## 5. 缓存和条件请求

公开详情响应可携带 `ETag` 与 `Cache-Control`。Bangumi 镜像过期但仍可用时，返回旧数据并设置 `data_freshness=stale`；后台刷新不阻塞响应。没有本地快照且上游失败时才返回 `502 UPSTREAM_ERROR`。

## 6. 错误分类

- `VALIDATION_ERROR`：字段格式或范围错误，`fields` 使用 JSON Pointer 风格键；
- `UNAUTHORIZED` / `TOKEN_EXPIRED`：缺少或失效的 Anime 会话；
- `FORBIDDEN`：已认证但不允许执行；
- `NOT_FOUND`：资源不存在或不可见；
- `VERSION_CONFLICT`：乐观锁失败；
- `RATE_LIMITED`：必须携带 `Retry-After`；
- `UPSTREAM_ERROR`：Bangumi 失败且没有可用本地结果；
- `SERVICE_UNAVAILABLE`：Anime 核心依赖不可用。

服务端不得把 SQL、堆栈、Bangumi Token 或内部 URL放入 `message`、`fields`。
