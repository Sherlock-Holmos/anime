package site.jokersh.anime.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainValidationTest {
    @Test
    fun identifiersRejectInvalidValues() {
        assertFailsWith<IllegalArgumentException> { SubjectId(0) }
        assertFailsWith<IllegalArgumentException> { CommentId(" ") }
        assertFailsWith<IllegalArgumentException> { ImageRef.Remote("http://example.com/a.jpg", "a") }
    }

    @Test
    fun zeroVoteRatingCannotExposeScore() {
        assertFailsWith<IllegalArgumentException> {
            BangumiRating(score = 0.0, votes = 0, distribution = emptyMap(), sourceUpdatedAt = null)
        }
    }

    @Test
    fun searchCountsUnicodeCodePoints() {
        val request = SearchRequest(query = "星海🚀", pageSize = 5)
        assertEquals("星海🚀", request.query)
    }

    @Test
    fun pageCursorAndHasMoreMustAgree() {
        assertFailsWith<IllegalArgumentException> {
            Page(items = emptyList<String>(), nextCursor = null, hasMore = true)
        }
    }
}
