package site.jokersh.anime.data.comment

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

public interface RatingOutboxStore {
    public fun read(): String?

    public fun write(value: String)
}

public class InMemoryRatingOutboxStore : RatingOutboxStore {
    private var value: String? = null

    override fun read(): String? = value

    override fun write(value: String) {
        this.value = value
    }
}

public class OfflineFirstCommunityRepository(
    private val remote: CommunityRepository,
    private val outboxStore: RatingOutboxStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CommunityRepository by remote {
    private var pending = load()

    override suspend fun saveRating(
        subjectId: Long,
        score: Int,
        tags: Set<String>,
        visibility: String,
    ): Result<Unit> {
        if (score !in 1..10) return Result.failure(IllegalArgumentException("评分必须在 1–10 分之间"))
        val mutation = PendingRating(subjectId, score, tags.sorted(), visibility)
        pending = pending.filterNot { it.subjectId == subjectId } + mutation
        persist()
        val result = remote.saveRating(subjectId, score, tags, visibility)
        if (result.isSuccess) {
            pending = pending.filterNot { it.subjectId == subjectId }
            persist()
        }
        // 本地事务已经接受写入；网络失败由持久 Outbox 稍后重放。
        return Result.success(Unit)
    }

    override suspend fun retryPendingRatings(): Result<Unit> =
        runCatching {
            val snapshot = pending
            snapshot.forEach { item ->
                if (remote.saveRating(item.subjectId, item.score, item.tags.toSet(), item.visibility).isSuccess) {
                    pending = pending.filterNot { it.subjectId == item.subjectId }
                    persist()
                }
            }
        }

    private fun load(): List<PendingRating> =
        outboxStore.read()?.let { runCatching { json.decodeFromString<List<PendingRating>>(it) }.getOrNull() }.orEmpty()

    private fun persist() = outboxStore.write(json.encodeToString(pending))
}

@Serializable
private data class PendingRating(
    val subjectId: Long,
    val score: Int,
    val tags: List<String>,
    val visibility: String,
)
