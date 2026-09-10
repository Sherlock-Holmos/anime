# CMP 前端产品范围与导航规范 V2.0

> 状态：V2.0 规范性文档<br>
> 适用范围：Android 优先的 Compose Multiplatform 客户端；iOS 后续复用<br>
> 产品事实源：`docs/product/00-product-vision.md`<br>
> 目标：以 Fixture 先验证资料、档案、评价、片单和动态闭环，再接入真实后端

## 1. 本阶段交付边界

前端首个可玩版本必须在没有 Anime 后端的情况下完整运行。列表、搜索、详情、收藏进度、短评和登录状态均由可切换 Fixture Repository 驱动；Repository 接口稳定后再替换为 Remote 实现，页面不得感知数据来源。

| 能力 | Fixture Demo | 接入后端后 |
|---|---|---|
| 发现、搜索、条目详情 | 本地确定性数据 | Anime API / Bangumi 镜像 |
| Bangumi 评分 | 只读样例数据，明确来源 | 只读展示 Bangumi 数据 |
| 收藏与观看进度 | 写入本地 Demo 数据库，可重置 | 本地优先并同步服务端 |
| 短评 | 本地发布、删除自己的短评 | 服务端发布与治理 |
| 登录 | 场景开关模拟 | OAuth / Anime 会话 |
| 弱网、离线、冲突 | Demo 控制台一键复现 | 由真实网络与同步状态触发 |
| Anime 评分 | 首发阶段不实现 | 后续独立阶段评审通过后再接入 |
| 长评与片单 | 本地确定性内容与编辑预览 | 服务端发布、版本和治理 |
| 关注与动态 | 本地关注流 Fixture | 服务端关系图与 Feed 游标 |

Bangumi 评分为首发阶段唯一评分来源，只读且必须标注外部来源。Anime 个人评分和社区评分属于后续独立阶段，启用前不得在首发页面出现入口或占位卡片。

## 2. 信息架构

根导航固定为四项，避免把低频功能提升到一级入口：

1. **发现**：当季、趋势、热门评价、精选片单和关注动态摘要。
2. **资料库**：搜索、筛选、榜单、标签、人物、公司和系列关系。
3. **动态**：关注流、热门评价、片单更新和社区互动。
4. **我的**：收藏、进度、评分、评价、片单、统计、账号和设置。

```mermaid
flowchart TD
    App[App Shell] --> Discover[发现]
    App --> Library[资料库]
    App --> Activity[动态]
    App --> Profile[我的]
    Discover --> Subject[条目详情]
    Library --> Results[搜索与筛选结果]
    Results --> Subject
    Profile --> Collection[收藏与进度]
    Collection --> Subject
    Activity --> Review[评价详情]
    Activity --> List[片单详情]
    Subject --> Episodes[章节列表]
    Subject --> Cast[角色与人物]
    Subject --> Relations[关联条目]
    Subject --> Comments[短评]
    Comments --> Composer[发布短评]
    Profile --> Ratings[我的评分]
    Profile --> Reviews[我的评价]
    Profile --> Lists[我的片单]
    Profile --> Settings[设置]
    Profile --> Diagnostics[Demo/诊断]
```

## 3. 路由表

| Route | 参数 | 登录要求 | 入口 | 返回行为 |
|---|---|---:|---|---|
| `splash` | 无 | 否 | 冷启动 | 自动进入目标根页 |
| `discover` | 无 | 否 | 根导航 | 再次点击回顶 |
| `library` | `query?` | 否 | 根导航/Deep Link | 保留查询与筛选 |
| `search/results` | `query`, `filters?` | 否 | 搜索页 | 回到编辑态搜索页 |
| `subject/{id}` | `id` | 否 | 任意条目卡/Deep Link | 回到来源页及原滚动位置 |
| `subject/{id}/episodes` | `id` | 否 | 详情 | 回详情 |
| `subject/{id}/characters` | `id` | 否 | 详情 | 回详情 |
| `subject/{id}/relations` | `id` | 否 | 详情 | 回详情 |
| `subject/{id}/comments` | `id`, `sort?` | 否 | 详情 | 回详情 |
| `subject/{id}/comment/new` | `id` | 是 | 短评页 | 成功后回短评并定位新项 |
| `activity` | `feed?` | 否 | 根导航 | 保留动态游标与滚动位置 |
| `collection` | `status?` | 是* | 我的 | 游客显示登录引导，不强跳 |
| `profile` | 无 | 否 | 根导航 | 游客显示访客版 |
| `login` | `returnTo` | 否 | 受保护操作 | 成功后回到原操作位置 |
| `settings` | 无 | 否 | 我的 | 回我的 |
| `diagnostics` | 无 | 否 | Demo/Debug 构建 | 回设置 |

`collection` 页面本身可由游客访问，以解释价值；具体收藏写操作才触发登录。登录取消时必须返回原页面，且不得丢失输入、滚动位置和待执行意图。

## 4. 导航行为

- 四个根页各自保留独立返回栈与滚动位置；切换 Tab 不重建页面状态。
- Android 系统返回键先关闭弹层/键盘，再弹出子路由，最后在根页退出应用。
- Deep Link 只接受已验证的 HTTPS `/subjects/{id}`；非法、非正数或缺失 ID 进入可恢复错误页，Prod 不注册自定义 `anime://` Scheme。
- 页面进程恢复只保存轻量参数、筛选和草稿，不序列化大对象；内容从 Repository 恢复。
- 底部导航在详情等沉浸式子页隐藏；返回根页后恢复。大屏可替换为 Navigation Rail，但路由不变。
- 受保护操作通过 `AuthGate` 包装，不在各页面复制登录判断。
- 收藏、评分、评价和片单是个人档案能力，不得重新提升为独立根导航。

## 5. 内容与交互原则

- 首屏先呈现内容，不用宣传横幅挤占主要信息；英雄区最多一个。
- 标题优先显示中文名，原名作为次要信息；缺失中文名时回退原名。
- 所有来源数据在详情页可追溯；Bangumi 评分不可伪装为本站评分。
- 核心操作在单手拇指热区：收藏、进度、搜索、Tab；破坏性操作需二次确认。
- 长标题最多两行，列表布局不因日文假名、拉丁别名或 200% 字体缩放而遮挡操作。
- 空状态必须告诉用户原因和下一步；错误状态保留已成功加载的内容。

## 6. 首个可玩版本验收

安装后，测试者可以在不联网的情况下完成：发现作品 → 在资料库搜索 → 查看详情及 Bangumi 只读评分 → 收藏并更新进度 → 发布评价或加入片单 → 在动态中看到对应活动 → 回到个人主页查看兴趣档案。全部路径必须可用系统返回键闭环。
