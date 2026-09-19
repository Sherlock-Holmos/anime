package site.jokersh.anime.app

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SubjectType

/** Tests the URL-only OAuth boundary without creating the network facade. */
class IosBridgeAuthCallbackTest {
    @BeforeTest
    fun resetPendingCallback() {
        IosBridge.clearPendingCallback()
    }

    @AfterTest
    fun clearPendingCallback() {
        IosBridge.clearPendingCallback()
    }

    @Test
    fun acceptsBangumiCallbackAndStoresCodeAndState() {
        assertTrue(IosBridge.handleOpenUrl("anime://bangumi-auth?code=ticket-1&state=state-1"))

        val callback = IosBridge.pendingCallback()
        assertEquals("ticket-1", callback?.authorizationCode)
        assertEquals("state-1", callback?.state)
    }

    @Test
    fun callbackSchemeAndHostAreCaseInsensitive() {
        assertTrue(IosBridge.handleOpenUrl("ANIME://BANGUMI-AUTH?code=ticket-2&state=state-2"))

        assertEquals("ticket-2", IosBridge.pendingCallback()?.authorizationCode)
    }

    @Test
    fun rejectsWrongRouteOrIncompleteCallback() {
        assertFalse(IosBridge.handleOpenUrl("https://bangumi-auth?code=ticket&state=state"))
        assertFalse(IosBridge.handleOpenUrl("anime://other?code=ticket&state=state"))
        assertFalse(IosBridge.handleOpenUrl("anime://bangumi-auth?code=ticket"))
        assertFalse(IosBridge.handleOpenUrl("anime://bangumi-auth?state=state"))
        assertNull(IosBridge.pendingCallback())
    }

    @Test
    fun latestCallbackReplacesPendingCallbackAndClearIsTerminal() {
        IosBridge.handleOpenUrl("anime://bangumi-auth?code=old&state=old-state")
        IosBridge.handleOpenUrl("anime://bangumi-auth?code=new&state=new-state")

        assertEquals("new", IosBridge.pendingCallback()?.authorizationCode)
        IosBridge.clearPendingCallback()
        assertNull(IosBridge.pendingCallback())
    }

    @Test
    fun bridgeFilterHelpersNormalizeCsvAndKnownValues() {
        assertEquals(listOf("tv", "movie"), parseCsv(" tv, ,movie "))
        assertEquals(emptyList(), parseCsv(null))
        assertEquals(SubjectType.Tv, "TV".toNativeSubjectType())
        assertEquals(AiringStatus.Airing, "airing".toNativeAiringStatus())
        assertEquals(SearchSort.Rating, "rating".toNativeSearchSort())
        assertEquals(SearchSort.Relevance, "unsupported".toNativeSearchSort())
    }
}
