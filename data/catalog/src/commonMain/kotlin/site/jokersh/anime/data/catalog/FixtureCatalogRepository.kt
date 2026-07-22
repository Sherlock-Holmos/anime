package site.jokersh.anime.data.catalog

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.BangumiRating
import site.jokersh.anime.core.model.CollectionSnapshot
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.DiscoverySection
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.Freshness
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.ResourceKind
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectCredits
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectRelation
import site.jokersh.anime.core.model.SubjectSection
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.core.model.SyncPhase
import site.jokersh.anime.core.model.SyncState
import site.jokersh.anime.core.model.Tag
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Deterministic catalog implementation for the demo build.
 *
 * Records mirror fixtures/v1. Feature code only observes the domain contract, so replacing this
 * implementation with the SQLDelight-backed remote repository does not change UI code.
 */
public class FixtureCatalogRepository(
    private val readDelayMs: Long = 120,
) : CatalogRepository {
    private val refreshMutex: Mutex = Mutex()
    private val mutableDiscovery: MutableStateFlow<ResourceState<DiscoveryFeed>> =
        MutableStateFlow(
            ResourceState(
                value = null,
                freshness = null,
                refreshing = false,
                error = null,
            ),
        )
    private var generatedAt: Instant = FIXTURE_NOW

    init {
        require(readDelayMs >= 0) { "readDelayMs must be non-negative" }
    }

    override fun observeDiscovery(): Flow<ResourceState<DiscoveryFeed>> = mutableDiscovery.asStateFlow()

    override fun observeSubject(id: SubjectId): Flow<ResourceState<SubjectDetail>> =
        MutableStateFlow(
            fixtureSubjects[id.value]
                ?.let(::subjectResource)
                ?: notFound(ResourceKind.Subject),
        )

    override fun observeEpisodes(id: SubjectId): Flow<ResourceState<List<Episode>>> = MutableStateFlow(emptyResource())

    override fun observeCredits(id: SubjectId): Flow<ResourceState<SubjectCredits>> =
        MutableStateFlow(
            ResourceState(
                value = SubjectCredits(emptyList(), emptyList()),
                freshness = fresh(),
                refreshing = false,
                error = null,
            ),
        )

    override fun observeRelations(id: SubjectId): Flow<ResourceState<List<SubjectRelation>>> =
        MutableStateFlow(emptyResource())

    override suspend fun refreshDiscovery(policy: RefreshPolicy): Result<Unit> =
        runCatching {
            refreshMutex.withLock {
                val current = mutableDiscovery.value
                val shouldRefresh =
                    when (policy) {
                        RefreshPolicy.IfMissing -> {
                            current.value == null
                        }

                        RefreshPolicy.IfStale -> {
                            current.value == null || current.freshness?.kind != FreshnessKind.Fresh
                        }

                        RefreshPolicy.Force -> {
                            true
                        }
                    }
                if (!shouldRefresh) return@withLock

                mutableDiscovery.value = current.copy(refreshing = true, error = null)
                delay(readDelayMs)
                if (policy == RefreshPolicy.Force && current.value != null) {
                    generatedAt += 60.seconds
                }
                mutableDiscovery.value = discoveryResource(generatedAt)
            }
        }

    override suspend fun refreshSubject(
        id: SubjectId,
        policy: RefreshPolicy,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun refreshSection(
        id: SubjectId,
        section: SubjectSection,
        policy: RefreshPolicy,
    ): Result<Unit> = Result.success(Unit)
}

private val FIXTURE_NOW: Instant = Instant.parse("2026-07-19T08:00:00Z")

private data class FixtureSubjectRecord(
    val id: Long,
    val title: String,
    val type: SubjectType,
    val status: AiringStatus,
    val totalEpisodes: Int?,
    val score: Double?,
    val votes: Int,
    val hasPoster: Boolean = true,
    val watchedEpisodes: Int? = null,
)

private val fixtureRecords: List<FixtureSubjectRecord> =
    listOf(
        FixtureSubjectRecord(1001, "星海邮差", SubjectType.Tv, AiringStatus.Airing, 12, 8.6, 1234, watchedEpisodes = 3),
        FixtureSubjectRecord(1002, "雨城备忘录", SubjectType.Tv, AiringStatus.Finished, 12, 9.1, 8021),
        FixtureSubjectRecord(1003, "玻璃庭院", SubjectType.Movie, AiringStatus.Announced, null, null, 0, hasPoster = false),
        FixtureSubjectRecord(1004, "十二点的电车驶过没有月台的海边小镇", SubjectType.Movie, AiringStatus.Announced, 1, 7.4, 86),
        FixtureSubjectRecord(1005, "北岸信号", SubjectType.Web, AiringStatus.Airing, 10, 6.8, 1),
        FixtureSubjectRecord(1006, "纸月亮计划", SubjectType.Tv, AiringStatus.Airing, 24, 8.2, 993, watchedEpisodes = 12),
        FixtureSubjectRecord(1007, "无声航线", SubjectType.Ova, AiringStatus.Finished, 6, 7.9, 412),
        FixtureSubjectRecord(1008, "红茶侦探社", SubjectType.Tv, AiringStatus.Airing, 13, 8.8, 4391),
        FixtureSubjectRecord(1009, "夏末天文台", SubjectType.Tv, AiringStatus.Finished, 12, 9.0, 7012),
        FixtureSubjectRecord(1010, "零号花园", SubjectType.Tv, AiringStatus.Airing, 12, 7.7, 578),
        FixtureSubjectRecord(1011, "风经过旧书店 The Wind／古書店の風", SubjectType.Web, AiringStatus.Airing, 8, 8.1, 251),
        FixtureSubjectRecord(1012, "最后一片云", SubjectType.Tv, AiringStatus.Airing, 48, 8.4, 1650),
    )

private val fixtureSubjects: Map<Long, SubjectSummary> =
    fixtureRecords.associate { record -> record.id to record.toDomain() }

private val fixtureSections: List<DiscoverySection> =
    listOf(
        fixtureSection("airing", "正在热播", 1001, 1006, 1008, 1011, 1012),
        fixtureSection("top-rated", "高分精选", 1002, 1009, 1001, 1008),
        fixtureSection("upcoming", "即将开播", 1003, 1004),
        fixtureSection("continue", "继续观看", 1006, 1001),
    )

private fun FixtureSubjectRecord.toDomain(): SubjectSummary {
    val subjectId = SubjectId(id)
    return SubjectSummary(
        id = subjectId,
        title = title,
        originalTitle = null,
        aliases = emptyList(),
        poster = if (hasPoster) ImageRef.Resource("poster/$id") else null,
        year = if (status == AiringStatus.Finished) 2025 else 2026,
        type = type,
        airingStatus = status,
        rating =
            BangumiRating(
                score = score,
                votes = votes,
                distribution = emptyMap(),
                sourceUpdatedAt = FIXTURE_NOW,
            ),
        collection =
            watchedEpisodes?.let { watched ->
                CollectionSnapshot(
                    subjectId = subjectId,
                    status = CollectionStatus.Watching,
                    watchedEpisodes = watched,
                    note = null,
                    updatedAt = FIXTURE_NOW,
                    sync = SyncState(SyncPhase.Synced),
                )
            },
    )
}

private fun fixtureSection(
    id: String,
    title: String,
    vararg subjectIds: Long,
): DiscoverySection =
    DiscoverySection(
        id = id,
        title = title,
        subjects = subjectIds.map { subjectId -> requireNotNull(fixtureSubjects[subjectId]) },
    )

private fun discoveryResource(now: Instant): ResourceState<DiscoveryFeed> =
    ResourceState(
        value = DiscoveryFeed(fixtureSections, now),
        freshness = Freshness(FreshnessKind.Fresh, now),
        refreshing = false,
        error = null,
    )

private fun subjectResource(subject: SubjectSummary): ResourceState<SubjectDetail> {
    val record = requireNotNull(fixtureRecords.firstOrNull { it.id == subject.id.value })
    return ResourceState(
        value =
            SubjectDetail(
                summary = subject,
                summaryText =
                    fixtureSummaries[subject.id.value]
                        ?: "一部仍在持续完善资料的动画作品。当前页面使用本地 Fixture 验证详情浏览流程。",
                airDate = null,
                endDate = null,
                totalEpisodes = record.totalEpisodes,
                tags =
                    listOf(
                        Tag(subject.type.name.uppercase(), 0),
                        Tag(if (subject.airingStatus == AiringStatus.Airing) "连载中" else "动画", 1),
                        Tag("Bangumi", 2),
                    ),
                backdrop = null,
                sourceUrl = "https://bangumi.tv/subject/${subject.id.value}",
                dataUpdatedAt = FIXTURE_NOW,
            ),
        freshness = fresh(),
        refreshing = false,
        error = null,
    )
}

private val fixtureSummaries: Map<Long, String> =
    mapOf(
        1001L to "星海之间的邮路重新开启，一名见习邮差带着未能寄出的信，踏上跨越群星的旅程。",
        1002L to "雨季笼罩城市，散落在旧街区里的记忆被一页页重新拼起。",
        1003L to "被遗忘的玻璃温室里，四季与时间以不同的速度流动。",
        1006L to "以月光为能源的实验城市即将停摆，一群学生决定完成最后一次发射。",
    )

private fun fresh(): Freshness = Freshness(FreshnessKind.Fresh, FIXTURE_NOW)

private fun <T> emptyResource(): ResourceState<List<T>> =
    ResourceState(
        value = emptyList(),
        freshness = fresh(),
        refreshing = false,
        error = null,
    )

private fun <T> notFound(kind: ResourceKind): ResourceState<T> =
    ResourceState(
        value = null,
        freshness = null,
        refreshing = false,
        error = AppError.NotFound(kind),
    )
