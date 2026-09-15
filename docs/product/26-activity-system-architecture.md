# 动态系统架构与隐私基线

> 编号：`ACT-1.0`  
> 日期：2026-09-15  
> 状态：Approved；适用于 R1 iOS 首发和 Android 第二阶段。

## 1. 核心判断

当前 `activities` 同时承担“用户行为历史”和“社区动态”，导致收藏、在看、想看、进度同步等私域操作被公开展示。后续采用双层模型：

```text
业务事实：collections / ratings / reviews / comments / lists
动态投影：activities，只保存适合出现在动态流的事件
```

业务事实是权威源，动态是可撤回、可重建的展示投影。

## 2. R1 可见性规则

| 行为 | 默认是否进公开动态 | 说明 |
|---|---:|---|
| 添加/修改/删除收藏 | 否 | 只保留片库事实 |
| 修改观看进度 | 否 | 不产生社区内容 |
| Bangumi 同步 | 否 | 不能泄露用户片库 |
| Anime 评分 | 否 | 可在个人页展示，未来允许用户选择公开 |
| Anime 评价 | 是，须公开且通过审核 | 用户主动发布 |
| 作品评论 | 否，默认只在作品讨论区 | 用户主动发布，但不自动污染全局动态 |
| 公开片单 | 是 | 片单可单独设置公开性 |
| 关注片单 | 否 | 属于用户关系行为 |

公开流只能查询 `visibility = public` 且源内容仍为 published/active 的动态。关注流还要验证关注关系；不得用“登录即可看见全部”代替关系过滤。

## 3. 生命周期与删除语义

用户删除评价、评论或片单时，源内容软删除，关联动态进入 `withdrawn`，不做物理删除。用户删除一条自己可见的动态时，默认是撤回动态投影，不自动删除片库或评分事实；界面必须明确显示“撤回动态”与“删除内容”的差异。

旧版本无可靠来源关联的 `collected/commented/rated` 动态按迁移规则设为 private，不再出现在公共流中。

## 4. 目标字段和偏好

在当前字段基础上逐步增加：

```text
event_type, object_type, object_id,
feed_visibility, lifecycle_state,
event_key, group_key, revision, payload,
withdrawn_at, deleted_at, edited_at
```

用户偏好至少包括：是否分享评价、是否分享评分、是否分享片单、评论是否进入全局动态。R1 默认关闭收藏/进度/评分分享，后续通过设置显式打开。

## 5. R1 API

```text
GET   /api/v1/me/activities?scope=all|published|private|withdrawn
DELETE /api/v1/me/activities/{id}       # 撤回动态投影
GET   /api/v1/me/activity-preferences
PATCH /api/v1/me/activity-preferences
```

现有公共 feed 保留 `public/following/popular`，但查询必须排除私域行为和失效源内容。

## 6. 多轮审核结论

### Round 1：隐私

收藏、进度、同步和评分默认不进入公共动态；源内容 visibility 与活动 visibility 必须同步。通过。

### Round 2：删除和恢复

撤回动态不等于删除业务事实；源内容删除/审核隐藏会让动态自动消失；恢复源内容可恢复公开投影。通过。

### Round 3：幂等和性能

使用 event_key/group_key 去重；feed 走索引和游标；popular 不在每行执行高成本子查询。R1 先完成可见性和撤回，聚合统计列后续补齐。通过。

### Round 4：客户端

动态页区分“社区动态”和“我的记录”；删除操作显示确认、进度、失败重试和刷新；个人页提供可见性设置入口。通过。

