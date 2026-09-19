package site.jokersh.anime.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import site.jokersh.anime.core.model.ExternalAuthRequest
import site.jokersh.anime.data.session.InMemorySessionTokenStore

/**
 * Stable, platform-neutral contracts for the iOS host's KMP-facing boundaries.
 *
 * These tests intentionally stop at the shared abstractions. The real Keychain
 * implementation is private to iosMain and requires an Apple runtime, while
 * the SwiftUI host has no XCTest target yet.
 */
class IosHostQualityContractTest {
    @Test
    fun oauthAuthorizeUrlMustRemainHttps() {
        assertFailsWith<IllegalArgumentException> {
            ExternalAuthRequest(
                requestId = "request-1",
                authorizeUrl = "http://example.invalid/authorize",
                expiresAt = Instant.parse("2026-09-19T00:00:00Z"),
            )
        }
    }

    @Test
    fun tokenStoreRoundTripPreservesAccessRefreshAndExpiry() {
        val store = InMemorySessionTokenStore()
        val expiresAt = Instant.parse("2026-09-19T01:00:00Z")

        store.save("access-token", expiresAt, "refresh-token")

        val restored = store.load()
        assertEquals("access-token", restored?.token)
        assertEquals("refresh-token", restored?.refreshToken)
        assertEquals(expiresAt, restored?.expiresAt)
    }

    @Test
    fun tokenStoreClearRemovesAllSessionMaterial() {
        val store = InMemorySessionTokenStore()
        store.save("access-token", Instant.parse("2026-09-19T01:00:00Z"), "refresh-token")

        store.clear()

        assertNull(store.load())
    }

}
