package site.jokersh.anime.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnimeDeepLinkTest {
    @Test
    fun parsesVerifiedSubjectLink() {
        assertEquals(
            AppRoute.Subject(5949, RouteOrigin.DeepLink),
            AnimeDeepLink.parse("https://anime.jokersh.site/subjects/5949?from=share"),
        )
    }

    @Test
    fun rejectsUntrustedOrInvalidLinks() {
        assertNull(AnimeDeepLink.parse("http://anime.jokersh.site/subjects/5949"))
        assertNull(AnimeDeepLink.parse("https://evil.example/subjects/5949"))
        assertNull(AnimeDeepLink.parse("https://anime.jokersh.site/subjects/0"))
        assertNull(AnimeDeepLink.parse("https://anime.jokersh.site/users/5949"))
    }
}
