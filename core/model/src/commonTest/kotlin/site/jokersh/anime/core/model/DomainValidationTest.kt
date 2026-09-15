package site.jokersh.anime.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

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

    @Test
    fun animeRatingUsesIndependentOneToTenScale() {
        assertFailsWith<IllegalArgumentException> {
            UserRating(
                id = RatingId("rating-1"),
                subjectId = SubjectId(1001),
                userId = UserId("user-1"),
                score = 11,
                tags = emptySet(),
                visibility = Visibility.Public,
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
            )
        }
    }

    @Test
    fun usersCannotFollowThemselves() {
        assertFailsWith<IllegalArgumentException> {
            FollowRelation(
                followerId = UserId("user-1"),
                followedId = UserId("user-1"),
                createdAt = Instant.fromEpochMilliseconds(0),
            )
        }
    }

    @Test
    fun languagePreferencesUseStableLocaleTags() {
        assertEquals(null, LanguagePreference.System.localeTag)
        assertEquals("zh-Hans", LanguagePreference.SimplifiedChinese.localeTag)
        assertEquals("zh-TW", LanguagePreference.TraditionalChinese.localeTag)
        assertEquals("en", LanguagePreference.English.localeTag)
        assertEquals("ja", LanguagePreference.Japanese.localeTag)
    }
}
