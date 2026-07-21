# 评论 Feature 契约

## 1. 路由与边界

路由 `AppRoute.Comments(subjectId: Long, sort: CommentSort = CommentSort.Newest)`。评论为 Anime 自有社区数据；与 Bangumi 评论完全隔离。支持查看、分页、发一条顶级评论、回复一层、折叠剧透和删除自己的评论。不支持点赞、编辑、无限嵌套、图片和富文本。

## 2. Contract

```kotlin
data class CommentUiState(
    val subject: SubjectHeaderUi?,
    val comments: AsyncContent<List<CommentThreadUi>>,
    val composer: ComposerState,
    val nextCursor: String?,
    val isLoadingMore: Boolean,
)

sealed interface CommentIntent {
    data object Entered : CommentIntent
    data class SpoilerToggled(val commentId: CommentId) : CommentIntent
    data class ReplyClicked(val commentId: CommentId) : CommentIntent
    data class DraftChanged(val value: String) : CommentIntent
    data class SpoilerChanged(val value: Boolean) : CommentIntent
    data object Submit : CommentIntent
    data class DeleteClicked(val commentId: CommentId) : CommentIntent
    data object DeleteConfirmed : CommentIntent
    data object LoadNextPage : CommentIntent
    data object Retry : CommentIntent
}
```

## 3. 内容与权限

评论正文纯文本，NFC 后长度 1..300；只含空白不可提交。换行按一个字符计，Unicode code point 计数而非 UTF-16 单元。回复对象必须仍存在且属于同一 subject。匿名可看，提交/删除通过 AuthGate。仅作者能删除；删除后显示 tombstone，保留回复结构。

剧透默认折叠，只显示“此评论包含剧透”和“显示”；展开状态仅保存在当前页面 SavedState。TalkBack 不得在展开前读到正文。

## 4. 提交语义

Submit 成功前按钮禁用并显示进度，防止重复。成功后清空草稿，将服务端确认评论插入列表顶部并聚焦；离线或可重试错误保留草稿。首发不对评论做离线乐观发布，避免产生用户误解。

删除必须二次确认。删除成功更新为 tombstone；失败保留原评论并提示。401 走登录门禁，403 显示“无权删除”，404 将本地项刷新为最新状态。

## 5. 布局与键盘

评论卡片左右 16dp，顶部/底部 12dp；头像 40dp；回复相对父项缩进 24dp，但正文可用宽度不得小于 240dp，窄屏时取消额外缩进并用左边框表示层级。

编辑器为底部 Sheet：多行输入 120–240dp、字数计数、剧透开关、取消和发布。IME 打开时 Sheet 随键盘上移；草稿在旋转和进程重建后恢复，提交成功后才清除。

## 6. 标签和验收

`comments.list`、`comments.item.{id}`、`comments.spoiler.{id}`、`comments.reply.{id}`、`comments.delete.{id}`、`comments.composer`、`comments.input`、`comments.spoilerSwitch`、`comments.submit`、`comments.loadMore`。

- FE-COM-001：1008 首屏和第二页无重复、顺序稳定。
- FE-COM-002：剧透正文展开前不在可访问语义中。
- FE-COM-003：匿名提交登录后保留草稿和回复目标，只提交一次。
- FE-COM-004：离线提交失败保留全部草稿内容。
- FE-COM-005：只能删除自己的评论，确认与失败路径完整。
