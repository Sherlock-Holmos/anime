# UI 状态矩阵与状态转换规范

## 1. 通用状态模型

页面状态区分首次加载与后台刷新，保留最近成功数据，禁止用单一 `loading: Boolean` 覆盖全部内容。

```kotlin
sealed interface LoadState<out T> {
    data object Initial : LoadState<Nothing>
    data class Loading<T>(val previous: T? = null) : LoadState<T>
    data class Content<T>(
        val data: T,
        val freshness: Freshness,
        val refreshing: Boolean = false,
        val partialErrors: Set<SectionError> = emptySet(),
    ) : LoadState<T>
    data class Empty(val reason: EmptyReason) : LoadState<Nothing>
    data class Failure<T>(val error: UiError, val previous: T? = null) : LoadState<T>
}
```

`Freshness` 至少包含 `Fresh`、`Stale(updatedAt)`、`OfflineCache(updatedAt)`；同步状态独立为 `Synced`、`Pending`、`Syncing`、`Failed`、`Conflict`，不可与内容加载状态混为一个枚举。

## 2. 展示优先规则

1. 有可用内容时始终优先展示内容，加载/离线/局部失败以非阻断形式附加。
2. 首次加载且没有内容时才展示全屏 Skeleton。
3. 请求成功但集合为空显示 Empty；请求失败且无缓存才显示 FullPageError。
4. 认证过期不清空公开数据；仅受保护区域显示登录恢复操作。
5. 待同步、同步失败和冲突只影响用户数据操作，不遮挡作品资料。

全局顶部最多显示一个 Banner，优先级为：`Conflict > AuthExpired > SyncFailed > Offline > Stale > SyncPending`。低优先级状态折叠进状态详情，不堆叠。

## 3. 页面矩阵

| 页面 | Initial Loading | Empty | Offline + Cache | Offline 无 Cache | Partial Error | Auth Required | Sync Conflict |
|---|---|---|---|---|---|---|---|
| 发现 | 分区 Skeleton | 去搜索 | 内容 + 离线 Banner | 全页离线 + 重试 | 区块内重试 | 不适用 | 不阻断，状态入口提示 |
| 搜索建议 | 输入框立即可用 | 热门/历史 | 历史可用 | 告知需联网搜索 | 建议区静默失败 | 不适用 | 不适用 |
| 搜索结果 | 结果骨架 | 清除筛选 | 缓存结果 + 时间 | 保留查询，全页提示 | 分页尾重试 | 不适用 | 不适用 |
| 详情 | Hero/Section 骨架 | 条目不存在 | 缓存详情 + 时间 | 全页重试 | 每 Section 独立 | 收藏操作触发登录 | 收藏区显示处理入口 |
| 收藏 | 骨架 | 按 Tab 引导 | 本地收藏可操作 | 本地收藏可操作 | 单项同步状态 | 访客价值页 | 冲突项置顶 |
| 短评 | 列表骨架 | 写第一条短评 | 已缓存只读 | 无缓存提示 | 分页尾重试 | 发布触发登录 | 不适用 |
| 我的 | 本地会话骨架 | 不适用 | 资料 + 离线 | 访客/本地资料 | 同步模块错误 | 访客版 | 高优先级入口 |

## 4. 事件与转换

| 当前状态 | 事件 | 下一状态 | UI 反馈 |
|---|---|---|---|
| Initial | `Load` | Loading | 结构化 Skeleton |
| Loading | `Success(data)` | Content/Fresh | 内容淡入，不整页闪烁 |
| Loading | `Success(empty)` | Empty | 原因 + 下一步 |
| Loading | `Fail(error)` | Failure | 错误分类 + 重试 |
| Content | `Refresh` | Content(refreshing) | 保留内容，顶部进度 |
| Content(refreshing) | `Fail` | Content + partial error | Snackbar/Inline，不清空 |
| Content | `NetworkLost` | Content/OfflineCache | Banner + 更新时间 |
| Content | `Mutate` | Content + Pending | 乐观更新、待同步图标 |
| Pending | `SyncSuccess` | Synced | 状态图标消失/短反馈 |
| Pending | `SyncFail` | Failed | 保留本地值、可重试 |
| Pending | `Conflict` | Conflict | 比较与选择入口 |
| Authenticated | `TokenExpired` | AuthExpired | 暂停受保护同步，不清公开数据 |

## 5. 错误分类与重试

- `NetworkUnavailable`：显示离线说明；网络恢复自动重试一次，用户操作仍可手动重试。
- `Timeout`：保留内容并给局部重试；连续失败不进行无限自动重试。
- `Unauthorized`：单飞刷新会话；失败后进入 AuthExpired 并保留返回意图。
- `NotFound`：详情显示条目不存在，提供返回发现/搜索。
- `RateLimited(retryAfter)`：显示可理解的稍后再试时间，不启用快速连点。
- `Server`/`Upstream`：区分 Anime 服务和 Bangumi 数据暂不可用，但不暴露堆栈。
- `Parse`：记录诊断 ID，用户侧显示数据格式异常；不得把错误数据当 0 或空列表。

重试操作必须幂等；局部请求只重试对应 Section。自动重试使用指数退避与抖动，在页面离开、应用后台或网络不可用时暂停。

## 6. 冲突处理

收藏状态/进度冲突 Sheet 同时展示“此设备”“Bangumi/云端”、各自更新时间和来源，默认不自动覆盖。操作为“保留此设备”“使用云端”“稍后处理”；稍后处理保持本地可见值和 Conflict 标记。Fixture 场景必须能固定复现完整流程。

## 7. 恢复与持久化

- 旋转、窗口变化：保留全部 UI State。
- 进程重建：保留路由参数、查询筛选、滚动键、草稿和待同步操作。
- 冷启动：先恢复本地内容，再进行后台刷新；不得等待网络显示空白页。
- 用户主动退出：清除令牌和私有缓存，但保留公开缓存与外观设置；Demo 重置单独执行。
