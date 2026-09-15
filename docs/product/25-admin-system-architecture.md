# 管理员系统架构与审核基线

> 编号：`ADM-1.0`  
> 日期：2026-09-15  
> 状态：Approved；本文件只冻结 R1 管理 MVP，不把未来角色体系伪装成已完成能力。

## 1. 产品目标

管理员系统要解决的是内容治理、用户安全和运营可观测性，不是单纯展示数量。R1 管理端必须让管理员能够：

- 看见待处理举报及其上下文；
- 对评论和评价执行审核、隐藏、恢复或删除；
- 看到操作结果、失败原因和刷新状态；
- 追踪谁在什么时间对什么内容做了什么操作；
- 按权限显示入口，服务端再次校验权限；
- 管理自己的头像和资料，不把 Bangumi 外链当成唯一资料源。

## 2. R1 权限边界

保留现有 `users.role = maintainer` 作为兼容角色，映射为 R1 管理员。权限由服务端判定，客户端只使用返回的 permissions 渲染界面。

| 权限 | R1 | 说明 |
|---|---:|---|
| `admin.dashboard.read` | ✓ | 概览和待处理数量 |
| `admin.report.read` | ✓ | 举报队列、详情 |
| `admin.report.resolve` | ✓ | 处理、驳回、重新打开 |
| `admin.comment.read` | ✓ | 评论审核队列 |
| `admin.comment.moderate` | ✓ | 发布、隐藏、删除、恢复 |
| `admin.review.read` | ✓ | 评价列表和详情 |
| `admin.review.moderate` | ✓ | 评价审核 |
| `admin.audit.read` | ✓ | 管理操作日志 |
| `admin.user.suspend` | R2 | 暂停/恢复账号，避免首发误操作 |
| `admin.role.assign` | R2 | 角色管理必须单独评审 |

普通用户访问这些接口必须得到 403；客户端不能用“按钮隐藏”代替服务端鉴权。

## 3. 页面与状态

### 3.1 管理概览

概览卡片必须是可点击的入口，而不是静态数字：待处理举报、待审评论、待审评价、活跃用户、最近管理操作。加载、空状态、错误、刷新中都要有明确反馈。

### 3.2 举报中心

筛选：状态、原因、内容类型、时间、处理人。每项展示举报人、被举报内容、作者、作品、原因、创建时间和当前状态。

状态机：

```text
open -> claimed -> resolved
open -> dismissed
claimed -> open
resolved/dismissed -> open（仅允许重新打开，并记录原因）
```

处理举报必须在一个事务内完成：更新举报状态、执行内容动作、写入 moderation action、写入不可变 audit log。请求携带 `reason`、`expected_version` 和幂等键，重复请求不得重复产生副作用。

### 3.3 内容审核

首发支持评论和评价；动态只审核主动发布的评价/评论，不把收藏、进度、同步事件作为社区内容进入队列。

### 3.4 审计日志

日志至少记录 actor、action、target_type、target_id、request_id、时间、原因、变更前后摘要和结果。日志只允许追加，不提供编辑或物理删除 UI。

## 4. API 契约冻结

兼容保留现有 `/admin/overview`、`/admin/comments`，新增：

```text
GET  /api/v1/admin/me
GET  /api/v1/admin/dashboard
GET  /api/v1/admin/reports
POST /api/v1/admin/reports/{id}/claim
POST /api/v1/admin/reports/{id}/resolve
POST /api/v1/admin/reports/{id}/dismiss
POST /api/v1/admin/reports/{id}/reopen
GET  /api/v1/admin/audit-logs
```

所有管理写接口统一返回变更后的资源状态；失败返回可读错误，不允许客户端静默吞掉 4xx/5xx。

## 5. 多轮审核结论

### Round 1：领域边界

- 评论/评价是 Anime 自有内容；
- 举报是治理对象，不等于内容本身；
- 收藏和进度是用户私域事实，不进入公开审核队列；
- 客户端只是权限和状态的展示层。

结论：通过。

### Round 2：数据与并发

- 举报处理与内容状态、操作日志必须同事务；
- 状态变更携带 expected version；
- 幂等键避免重复操作；
- 软删除/隐藏优先，源记录保留以支撑审计。

结论：通过；R1 先覆盖评论举报，评价审核沿用同一模型逐步接入。

### Round 3：安全

- 服务端按权限拒绝；
- 不接受客户端传入的 actor、role 或 resolved_by；
- CORS、频控和管理员会话策略在生产配置中收紧；
- 所有管理写操作必须留痕。

结论：通过；角色分层和 2FA 延后 R2，不阻塞 R1 治理闭环。

### Round 4：客户端

- 按钮必须有 loading、成功、失败和刷新反馈；
- 数字卡片可以跳转到对应列表；
- 权限不足显示不可用状态而非空白页面；
- 举报枚举必须与服务端完全一致。

结论：通过。

