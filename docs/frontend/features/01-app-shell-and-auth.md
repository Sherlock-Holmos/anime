# App Shell 与登录门禁契约

## 1. 范围

负责应用启动、四个根导航栈、全局 Snackbar、登录门禁、离线横幅和系统返回。登录协议本身由后端阶段实现；Demo 使用匿名/演示用户切换器。

## 2. 状态和事件

```kotlin
data class AppShellUiState(
    val boot: BootState,
    val selectedRoot: RootDestination,
    val isOffline: Boolean,
    val snackbar: SnackbarMessage?,
    val authDialog: AuthDialogState?,
)

sealed interface AppShellIntent {
    data class SelectRoot(val destination: RootDestination) : AppShellIntent
    data object Back : AppShellIntent
    data class ProtectedActionRequested(val action: PendingAuthAction) : AppShellIntent
    data class AuthCompleted(val session: SessionState.Authenticated) : AppShellIntent
    data object AuthDismissed : AppShellIntent
    data class SnackbarConsumed(val id: String) : AppShellIntent
}
```

`BootState` 仅为 `Loading`、`Ready(session)`、`BlockingError(error)`。Boot 完成前不显示底部导航；失败页只提供重试和诊断信息复制。

## 3. 导航规则

- 根目的地固定为 Discover、Search、Collection、Profile，各持有独立 back stack。
- 再次点击当前根目的地：若不在根页面，pop 到根；已在根时滚动到顶部并按 TTL 决定是否刷新。
- Android Back：先关闭对话框/Sheet，再 pop 当前栈；当前栈在根且不是 Discover 时切回 Discover；Discover 根再次返回才交给系统退出。
- Deep link 路由先完成 Boot，再入栈；同一路由参数完全一致时只保留最顶一个实例。
- Collection/Profile 根页允许匿名进入，但显示登录引导，不自动弹登录。

## 4. 登录门禁

受保护动作只有：修改收藏、修改进度、发评论、删自己的评论。匿名触发时：保存一个 `PendingAuthAction` → 展示登录 Sheet → 登录成功后重新校验目标存在和参数合法 → 只重放一次。取消登录即丢弃动作。

写操作收到 401 时清除本地 Session，但保留触发该请求的动作；不得无限重放。新的受保护动作会替换旧 Pending Action，并提示用户。

## 5. 布局

- 手机使用底部 NavigationBar，高 80dp（含安全区之外的内容高按 Material 计算），四个等宽入口。
- 宽度 ≥ 600dp 使用 NavigationRail，宽 80dp；内容最大宽 1200dp 居中。
- 离线横幅位于顶部栏下方，高度自适应且不覆盖内容；恢复在线后保留 2 秒再淡出。
- 全局 Snackbar 位于导航组件之上，最多排队 3 条，相同 messageKey 在 3 秒内合并。

## 6. 测试标签与验收

`shell.root`、`shell.nav.discover`、`shell.nav.search`、`shell.nav.collection`、`shell.nav.profile`、`shell.offlineBanner`、`shell.authSheet`、`shell.snackbar`、`shell.blockingError`。

- FE-NAV-001：四个栈之间切换后，各自的路由和滚动位置保持。
- FE-NAV-002：系统返回严格符合关闭浮层 → 当前栈 → Discover → 退出顺序。
- FE-AUTH-001：匿名浏览不被强制登录。
- FE-AUTH-002：受保护动作在一次登录成功后准确重放一次。
- FE-OFF-001：离线缓存可用时内容不被全屏错误替换。

TalkBack 先读页面标题和离线状态，再读内容；底部入口使用“发现，标签 1/4”一类状态描述，选中状态不可只依靠颜色。
