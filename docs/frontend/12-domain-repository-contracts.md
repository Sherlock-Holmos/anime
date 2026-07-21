# CMP 领域模型与 Repository 完整契约

> 状态：V1.3 规范性、阻断性<br>
> 时间单位：领域层使用 `Instant`；日期使用 `LocalDate`；序列化格式为 ISO-8601 UTC。<br>
> 分数边界：仅承载 Bangumi 只读评分，不存在 Anime 自有评分写模型。

## 1. 标识和值对象

```kotlin
@JvmInline value class SubjectId(val value: Long) { init { require(value > 0) } }
@JvmInline value class EpisodeId(val value: Long) { init { require(value > 0) } }
@JvmInline value class CharacterId(val value: Long) { init { require(value > 0) } }
@JvmInline value class PersonId(val value: Long) { init { require(value > 0) } }
@JvmInline value class CommentId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class UserId(val value: String) { init { require(value.isNotBlank()) } }
@JvmInline value class MutationId(val value: String)
@JvmInline value class Cursor(val value: String)

sealed interface ImageRef {
    data class Remote(val url: String, val cacheKey: String) : ImageRef
    data class Resource(val path: String) : ImageRef
}
```

领域 ID 不包含来源前缀；当前 Subject/Episode/Character/Person 的 Long ID 对应 Bangumi ID。字符串 ID 由服务端或 Fixture 生成。URL 仅允许 `https`，Fixture 资源使用 `Resource`。

## 2. 条目模型

```kotlin
enum class SubjectType { Tv, Web, Ova, Movie, Other }
enum class AiringStatus { Announced, Airing, Finished, Unknown }

data class BangumiRating(
    val score: Double?,          // null 或 0.0..10.0；零票必须为 null
    val votes: Int,              // >= 0
    val distribution: Map<Int, Int>, // key 1..10，缺失按 0 展示
    val sourceUpdatedAt: Instant?,
)

data class SubjectSummary(
    val id: SubjectId,
    val title: String,
    val originalTitle: String?,
    val aliases: List<String>,
    val poster: ImageRef?,
    val year: Int?,
    val type: SubjectType,
    val airingStatus: AiringStatus,
    val rating: BangumiRating?,
    val collection: CollectionSnapshot?,
)

data class SubjectDetail(
    val summary: SubjectSummary,
    val summaryText: String?,
    val airDate: LocalDate?,
    val endDate: LocalDate?,
    val totalEpisodes: Int?,
    val tags: List<Tag>,
    val backdrop: ImageRef?,
    val sourceUrl: String,
    val dataUpdatedAt: Instant,
)

data class Tag(val name: String, val order: Int)
```

约束：`title` trim 后 1–200 字符；别名去重且最多 20 个；简介最多 10,000 字符；`year` 为 1900–当前年+3；总话数 null 或非负；评分分布数量总和允许与 votes 有上游口径差异，客户端不重算 score。

## 3. 章节、人物与关联

```kotlin
enum class EpisodeType { Main, Special, Opening, Ending, Other }
enum class EpisodeAirStatus { Unreleased, Aired, Delayed, Unknown }

data class Episode(
    val id: EpisodeId,
    val subjectId: SubjectId,
    val number: Double?,
    val sort: Int,
    val title: String?,
    val originalTitle: String?,
    val type: EpisodeType,
    val airDate: LocalDate?,
    val airStatus: EpisodeAirStatus,
)

data class CharacterCredit(
    val characterId: CharacterId,
    val name: String,
    val image: ImageRef?,
    val relation: String,
    val actors: List<PersonCredit>,
)

data class PersonCredit(
    val personId: PersonId,
    val name: String,
    val image: ImageRef?,
    val role: String?,
)

enum class RelationKind { Prequel, Sequel, SameWorld, Alternative, Character, Summary, Other }
data class SubjectRelation(
    val subject: SubjectSummary,
    val kind: RelationKind,
    val label: String,
)
```

章节排序以 `sort` 为唯一稳定顺序；number 只用于显示。进度只统计 `EpisodeType.Main`，未知总数时允许任意非负进度。

## 4. 收藏与同步

```kotlin
enum class CollectionStatus { Wish, Watching, Completed, OnHold, Dropped }
enum class SyncPhase { Synced, Pending, Syncing, Failed, Conflict }

data class SyncState(
    val phase: SyncPhase,
    val pendingCount: Int = 0,
    val lastAttemptAt: Instant? = null,
    val nextRetryAt: Instant? = null,
    val error: AppError? = null,
    val conflictId: String? = null,
)

data class CollectionSnapshot(
    val subjectId: SubjectId,
    val status: CollectionStatus,
    val watchedEpisodes: Int,
    val note: String?,
    val updatedAt: Instant,
    val sync: SyncState,
)

data class CollectionConflict(
    val id: String,
    val subjectId: SubjectId,
    val local: CollectionVersion,
    val remote: CollectionVersion,
    val detectedAt: Instant,
)

data class CollectionVersion(
    val status: CollectionStatus,
    val watchedEpisodes: Int,
    val updatedAt: Instant,
    val source: ChangeSource,
)

enum class ChangeSource { ThisDevice, AnimeServer, Bangumi }
enum class ConflictChoice { KeepLocal, UseRemote, Later }
```

进度必须 `>=0`；已知主线总话数时不得超过总数。状态和进度默认相互独立；只有用户在确认对话框明确选择“同时补全进度”时，`setStatus(..., completeProgress=true)` 才在同一事务将进度设为已知主线总话数。总话数未知时该参数返回 Validation。

## 5. 评论与会话

```kotlin
data class Comment(
    val id: CommentId,
    val subjectId: SubjectId,
    val parentId: CommentId?, // null 为顶级；非 null 时只允许一层
    val author: UserSummary,
    val body: String,
    val spoiler: Boolean,
    val createdAt: Instant,
    val editedAt: Instant?,
    val ownership: Ownership,
    val pending: Boolean,
)

data class UserSummary(val id: UserId, val displayName: String, val avatar: ImageRef?)
data class UserProfile(
    val summary: UserSummary,
    val collectionCounts: Map<CollectionStatus, Int>,
    val connectedProvider: Provider,
)

enum class Ownership { Self, Other }
enum class Provider { Bangumi, Anime }

sealed interface SessionState {
    data object Guest : SessionState
    data class Authenticated(val user: UserProfile, val expiresAt: Instant) : SessionState
    data class Expired(val lastUser: UserSummary?) : SessionState
}
```

评论 trim 后 1–300 Unicode code points；换行保留，连续空行显示最多两个；Fixture 和客户端只执行长度/空白校验，服务端负责治理。评论正文不进入日志、Route 或分析字段。

## 6. 搜索与分页

```kotlin
data class SearchRequest(
    val query: String,
    val types: Set<SubjectType> = emptySet(),
    val years: IntRange? = null,
    val airing: Set<AiringStatus> = emptySet(),
    val sort: SearchSort = SearchSort.Relevance,
    val cursor: Cursor? = null,
    val pageSize: Int,
)

enum class SearchSort { Relevance, Rating, Updated }

data class Page<T>(
    val items: List<T>,
    val nextCursor: Cursor?,
    val hasMore: Boolean,
)

data class SearchHistoryItem(val id: String, val query: String, val usedAt: Instant)
data class SearchSuggestion(val value: String)
```

查询 trim 后 1–100 code points；`pageSize` 只允许 `1..50`，由 `BuildProfile.searchPageSize` 传入（Demo=5，Dev/Prod=20），同一次分页链不得改变。年份范围 1900…当前年+3。排序必须稳定，等值时按 SubjectId 升序。Cursor 对 UI 不透明，不能解析或拼接。

## 6.1 Repository 支撑模型

```kotlin
data class DiscoveryFeed(val sections: List<DiscoverySection>, val generatedAt: Instant)
data class DiscoverySection(val id: String, val title: String, val subjects: List<SubjectSummary>)

data class SubjectCredits(
    val characters: List<CharacterCredit>,
    val persons: List<PersonCredit>,
)

enum class SubjectSection { Episodes, Credits, Relations }

data class CollectionItem(
    val subject: SubjectSummary,
    val collection: CollectionSnapshot,
)

data class SyncSummary(
    val pendingCount: Int,
    val failedCount: Int,
    val conflictCount: Int,
    val lastSuccessfulAt: Instant?,
)

enum class CommentSort { Newest, Oldest }
data class CommentDraft(
    val subjectId: SubjectId,
    val parentId: CommentId?,
    val text: String,
    val spoiler: Boolean,
    val updatedAt: Instant,
)

data class LoginRequest(val requestId: String, val pendingActionId: String?)
data class ExternalAuthRequest(val requestId: String, val authorizeUrl: String, val expiresAt: Instant)
data class AuthCallback(val requestId: String, val ticket: String)

enum class ThemePreference { System, Light, Dark }
enum class GlassPreference { Auto, On, Off }
enum class ReduceMotionPreference { FollowSystem, On, Off }

data class AppSettings(
    val theme: ThemePreference,
    val dynamicColor: Boolean,
    val glass: GlassPreference,
    val reduceMotion: ReduceMotionPreference,
    val diagnosticsConsent: Boolean,
)
```

`authorizeUrl` 必须由 Data 层验证为 HTTPS 后才构造；UI 只交给 ExternalBrowser，不解析 Query。评论 parent 为 null 表示顶级；非 null 时 Repository 必须确认父评论是顶级且属于同一 Subject。

## 7. 读取策略

```kotlin
enum class RefreshPolicy { IfMissing, IfStale, Force }
enum class FreshnessKind { Fresh, Stale, OfflineCache }
data class Freshness(val kind: FreshnessKind, val updatedAt: Instant)

data class ResourceState<T>(
    val value: T?,
    val freshness: Freshness?,
    val refreshing: Boolean,
    val error: AppError?,
)
```

Repository `observe*` 永不抛出业务异常；异常体现在 ResourceState 或 MutationResult。参数非法可同步抛 `IllegalArgumentException`，只表示编程错误。

TTL：发现分区 30 分钟；搜索结果 10 分钟；条目基础资料 24 小时；评分 6 小时；章节/人物/关联 24 小时；评论 2 分钟；用户收藏本地永不过期但联网后后台同步。强制刷新绕过 TTL，但不删除旧内容。

## 8. Repository 完整接口

```kotlin
interface CatalogRepository {
    fun observeDiscovery(): Flow<ResourceState<DiscoveryFeed>>
    fun observeSubject(id: SubjectId): Flow<ResourceState<SubjectDetail>>
    fun observeEpisodes(id: SubjectId): Flow<ResourceState<List<Episode>>>
    fun observeCredits(id: SubjectId): Flow<ResourceState<SubjectCredits>>
    fun observeRelations(id: SubjectId): Flow<ResourceState<List<SubjectRelation>>>
    suspend fun refreshDiscovery(policy: RefreshPolicy): Result<Unit>
    suspend fun refreshSubject(id: SubjectId, policy: RefreshPolicy): Result<Unit>
    suspend fun refreshSection(id: SubjectId, section: SubjectSection, policy: RefreshPolicy): Result<Unit>
}

interface SearchRepository {
    fun observeHistory(): Flow<List<SearchHistoryItem>>
    fun observeSuggestions(query: String): Flow<ResourceState<List<SearchSuggestion>>>
    suspend fun search(request: SearchRequest): Result<Page<SubjectSummary>>
    suspend fun saveHistory(query: String)
    suspend fun deleteHistory(id: String)
    suspend fun clearHistory()
}

interface CollectionRepository {
    fun observeCollections(status: CollectionStatus?): Flow<ResourceState<List<CollectionItem>>> // null=全部
    fun observeCollection(id: SubjectId): Flow<CollectionSnapshot?>
    fun observeSyncSummary(): Flow<SyncSummary>
    fun observeConflict(id: String): Flow<CollectionConflict?>
    suspend fun setStatus(id: SubjectId, status: CollectionStatus?, completeProgress: Boolean = false): MutationResult
    suspend fun setProgress(id: SubjectId, watched: Int): MutationResult
    suspend fun retry(mutationId: MutationId): MutationResult
    suspend fun resolveConflict(id: String, choice: ConflictChoice): MutationResult
    suspend fun requestSync(): Result<Unit>
}

interface CommentRepository {
    fun observeComments(id: SubjectId, sort: CommentSort): Flow<ResourceState<List<Comment>>>
    fun observeDraft(id: SubjectId): Flow<CommentDraft?>
    suspend fun loadNext(id: SubjectId, sort: CommentSort): Result<Page<Comment>>
    suspend fun saveDraft(id: SubjectId, parentId: CommentId?, text: String, spoiler: Boolean)
    suspend fun deleteDraft(id: SubjectId)
    suspend fun create(id: SubjectId, parentId: CommentId?, text: String, spoiler: Boolean): MutationResult
    suspend fun delete(id: CommentId): MutationResult
}

interface SessionRepository {
    fun observeSession(): Flow<SessionState>
    suspend fun beginLogin(request: LoginRequest): Result<ExternalAuthRequest>
    suspend fun completeLogin(callback: AuthCallback): Result<SessionState.Authenticated>
    suspend fun refresh(): Result<SessionState.Authenticated>
    suspend fun logout(): Result<Unit>
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setTheme(value: ThemePreference)
    suspend fun setDynamicColor(value: Boolean)
    suspend fun setGlass(value: GlassPreference)
    suspend fun setReduceMotion(value: ReduceMotionPreference)
    suspend fun setDiagnosticsConsent(value: Boolean)
    suspend fun clearPublicCache(): Result<Unit>
    suspend fun resetDemo(): Result<Unit>
}
```

## 9. Mutation 结果

```kotlin
sealed interface MutationResult {
    data class Accepted(val id: MutationId, val sync: SyncState) : MutationResult
    data class Rejected(val error: AppError.Validation) : MutationResult
    data class RequiresAuth(val pendingActionId: String) : MutationResult
    data class Failed(val error: AppError) : MutationResult
}
```

对收藏和进度，网络离线属于 Accepted/Pending，不属于 Failed；Failed 表示本地事务、序列化或不可恢复错误。对评论创建/删除，离线返回 `Failed(AppError.Offline)`，不创建公开占位，草稿由 CommentRepository 保留。相同 `clientMutationId` 重放必须返回等价结果。

## 10. 缓存与事务不变量

- 网络 DTO 先完整验证，再在单事务写主表、关系表和 freshness；解析失败不得污染旧缓存。
- 删除列表中缺失的关系必须限制在本次完整响应覆盖范围，分页响应不得误删。
- 收藏写入和 Pending Mutation 同事务；评论草稿单独持久化，服务端确认后才写入 Comment 表。
- 搜索历史最多 20 条，按标准化 query 去重，最新置顶。
- 注销清除 Token、用户资料、私有收藏镜像、草稿和 Pending；公开条目缓存保留。
- Demo 数据库文件与真实数据库物理隔离；Reset Demo 不能触碰真实库。

## 11. 并发与背压

- Subject/Discovery 同 key 刷新 SingleFlight；第二个调用等待同一 Deferred。
- 搜索建议使用 `debounce(300ms) + distinctUntilChanged + flatMapLatest`。
- 分页同一 request fingerprint 一次只允许一个 append；重复触底忽略。
- Repository Flow 使用数据库观察，不以无限 Replay 的 SharedFlow 保存大列表。
- Session refresh 全应用 SingleFlight；失败一次广播 Expired，不由多个请求弹多个登录页。
- Sync 每实体串行、跨实体最多 3 并发；429 尊重 Retry-After。

## 12. DTO 和数据库隔离

DTO 使用 `@Serializable` 且仅存在 `core:network`/data implementation；字段通过显式 Mapper 转为 Domain。SQLDelight 类型只存在 database/data implementation。Mapper 必须测试：缺字段、未知枚举、非法 URL、零票评分、超长文本、时间格式和未知关系类型。

未知上游枚举映射 `Other/Unknown` 并记录一次受限诊断，不能导致整个条目不可用。数值越界视为 Data Error，不进行静默截断，除非字段规则明确允许。

## 13. Repository 契约测试

Fake 与 Remote 实现共享测试套件，至少断言：首次无缓存、Fresh 不请求、Stale 后台刷新、离线有/无缓存、解析失败保留旧值、并发 SingleFlight、取消、分页去重、乐观写、重启恢复 Pending、幂等重放、登录过期、冲突三种选择、注销清私有数据、Demo Reset 隔离。
