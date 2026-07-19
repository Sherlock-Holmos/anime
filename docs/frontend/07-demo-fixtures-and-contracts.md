# Demo Fixture、领域模型与 Repository 契约

## 1. 数据模式

首个 Android 成果使用同一套 UI 和领域层，通过注入切换数据实现：

```kotlin
enum class DataMode { Fixture, Remote }

enum class DemoScenario {
    Normal,
    SlowNetwork,
    Empty,
    OfflineWithCache,
    OfflineWithoutCache,
    PartialFailure,
    StaleData,
    LoggedOut,
    AuthExpired,
    SyncPending,
    SyncFailure,
    SyncConflict,
}
```

建议构建配置为 `demoDebug`、`devDebug`、`prodRelease`。若 CMP 工程建立时不使用 Android Flavor，也必须提供等价的编译期 `BuildProfile`，确保诊断入口不会进入生产包。

## 2. 领域模型最小集

```kotlin
@JvmInline value class SubjectId(val value: Long)
@JvmInline value class UserId(val value: String)

data class SubjectSummary(
    val id: SubjectId,
    val title: String,
    val originalTitle: String?,
    val poster: ImageRef?,
    val year: Int?,
    val type: SubjectType,
    val rating: BangumiRating?,
    val collection: CollectionSnapshot?,
)

data class BangumiRating(
    val score: Double?,
    val votes: Int,
    val distribution: Map<Int, Int>,
    val updatedAt: Instant?,
)

data class SubjectDetail(
    val summary: SubjectSummary,
    val summaryText: String?,
    val airDate: LocalDate?,
    val totalEpisodes: Int?,
    val tags: List<String>,
    val episodes: List<Episode>,
    val characters: List<CharacterCredit>,
    val persons: List<PersonCredit>,
    val relations: List<SubjectRelation>,
)

enum class CollectionStatus { Wish, Watching, Completed, OnHold, Dropped }
data class CollectionSnapshot(
    val status: CollectionStatus,
    val watchedEpisodes: Int,
    val sync: SyncState,
    val updatedAt: Instant,
)
```

还需定义 `Episode`、`CharacterCredit`、`PersonCredit`、`SubjectRelation`、`Comment`、`UserProfile`、`Page<T>`、`UiError`。DTO、数据库实体和领域模型分离，Feature 不直接依赖 Bangumi DTO。

## 3. Repository 接口

```kotlin
interface CatalogRepository {
    fun observeDiscovery(): Flow<LoadState<DiscoveryFeed>>
    fun observeSubject(id: SubjectId): Flow<LoadState<SubjectDetail>>
    suspend fun refreshDiscovery(force: Boolean = false)
    suspend fun refreshSubject(id: SubjectId, force: Boolean = false)
}

interface SearchRepository {
    fun observeHistory(): Flow<List<SearchHistoryItem>>
    fun search(request: SearchRequest): Flow<Page<SubjectSummary>>
    suspend fun clearHistory()
}

interface CollectionRepository {
    fun observeCollections(status: CollectionStatus?): Flow<List<SubjectSummary>>
    suspend fun setStatus(id: SubjectId, status: CollectionStatus?): MutationResult
    suspend fun setProgress(id: SubjectId, watched: Int): MutationResult
    fun observeSyncState(): Flow<SyncSummary>
    suspend fun resolveConflict(id: SubjectId, choice: ConflictChoice)
}

interface CommentRepository {
    fun comments(subjectId: SubjectId, cursor: String?): Flow<Page<Comment>>
    suspend fun create(subjectId: SubjectId, text: String, spoiler: Boolean): Comment
    suspend fun delete(commentId: String)
}

interface SessionRepository {
    fun observeSession(): Flow<SessionState>
    suspend fun beginLogin(returnTo: AppRoute)
    suspend fun logout()
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
```

接口表达产品能力，不照抄 HTTP Endpoint。写操作返回本地接受结果与同步状态，禁止让页面等待远端成功后才更新。

## 4. Fixture 数据规范

Fixture 文件建议位于 `shared/src/commonMain/composeResources/files/fixtures/v1/`，版本化且只包含可再分发的测试素材或明确许可的远程 URL。每个数据文件带 `schemaVersion`，时间固定为相对 Demo 时钟，禁止调用系统当前时间导致截图漂移。

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-07-19T08:00:00Z",
  "subjects": [
    {
      "id": 1001,
      "title": "极长标题示例：用于验证两行截断与大字体布局",
      "originalTitle": "レイアウト検証用の長い原題",
      "year": 2026,
      "type": "TV",
      "rating": {
        "score": 8.2,
        "votes": 12345,
        "distribution": { "10": 1200, "9": 2800, "8": 4300 }
      }
    }
  ]
}
```

Fixture 集必须覆盖：正常完整数据、无海报、无评分、零票、超长中/日/英文标题、未知章节总数、已完结、未开播、空简介、人物缺图、300 字短评、剧透、缓存过期和非法远程图片。

## 5. 确定性场景控制器

`DemoScenarioController` 通过 `StateFlow<DemoScenario>` 驱动 Fake Repository。每个场景有固定延迟与结果：Normal 120ms、SlowNetwork 2500ms；错误类型和失败 Section 固定。测试不得依赖随机数、真实网络或墙上时钟。

切换场景时：取消旧请求 → 保留/清除缓存按场景定义 → 发出新状态。`OfflineWithCache` 保留固定旧数据，`PartialFailure` 固定让评分或人物 Section 失败，`SyncConflict` 生成明确的本地/远程两个版本。

## 6. Demo 本地写入

- 收藏、进度、短评和设置写入独立 Demo SQLDelight 数据库，应用重启后保留。
- “重置 Demo”仅清除 Demo 数据库并重新导入 Fixture，需确认且不影响真实账户数据。
- Fake 写操作采用与未来 Remote 相同的乐观更新和 Outbox 语义。
- 可用 Ktor `MockEngine` 验证 DTO/HTTP 层，但页面测试优先直接注入 Fake Repository，避免把演示和传输协议耦合。

## 7. Remote 接入约束

后端开发开始后，Remote Repository 必须通过现有契约和同一套契约测试。允许新增字段，不允许为了 HTTP 结构把分页、错误码或 DTO 暴露给 UI。Bangumi 密钥和 OAuth 只存在受控适配层；客户端展示 `source = Bangumi`，但不直接假定第三方响应永远稳定。

## 8. 契约验收

- Fake 与 Remote 对 `Normal/Empty/Offline/Stale/Unauthorized/Conflict` 运行同一套行为测试。
- 同一 Fixture 输入产生稳定排序、稳定 ID 和稳定截图。
- Repository 流在取消订阅后停止无用工作；新搜索取消旧查询。
- 领域层不存在 Android 类型、Compose 类型、JSON 注解或数据库注解。
- 页面只依赖接口和 UI Model 映射器，切换 `DataMode` 无需修改 Composable。
