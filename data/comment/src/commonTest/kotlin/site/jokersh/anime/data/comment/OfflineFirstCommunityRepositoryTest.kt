package site.jokersh.anime.data.comment

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OfflineFirstCommunityRepositoryTest {
    @Test
    fun `FR-042 offline rating survives recreation and retries`() =
        runTest {
            val store = InMemoryRatingOutboxStore()
            val offline = RatingRemote(fail = true)
            val first = OfflineFirstCommunityRepository(offline, store)

            assertTrue(first.saveRating(1001, 9, setOf("作画"), "public").isSuccess)
            assertTrue(store.read().orEmpty().contains("1001"))

            val online = RatingRemote(fail = false)
            val recreated = OfflineFirstCommunityRepository(online, store)
            assertTrue(recreated.retryPendingRatings().isSuccess)
            assertEquals(listOf(1001L to 9), online.saved)
            assertEquals("[]", store.read())
        }

    @Test
    fun `FR-042 repeated offline rating keeps only latest intent`() =
        runTest {
            val store = InMemoryRatingOutboxStore()
            val repository = OfflineFirstCommunityRepository(RatingRemote(fail = true), store)
            repository.saveRating(1001, 6, emptySet(), "public")
            repository.saveRating(1001, 8, emptySet(), "public")

            val online = RatingRemote(fail = false)
            OfflineFirstCommunityRepository(online, store).retryPendingRatings()
            assertEquals(listOf(1001L to 8), online.saved)
        }
}

private class RatingRemote(
    private val fail: Boolean,
) : CommunityRepository by EmptyCommunityRepository() {
    val saved = mutableListOf<Pair<Long, Int>>()

    override suspend fun saveRating(
        subjectId: Long,
        score: Int,
        tags: Set<String>,
        visibility: String,
    ): Result<Unit> {
        if (fail) return Result.failure(IllegalStateException("offline"))
        saved += subjectId to score
        return Result.success(Unit)
    }
}
