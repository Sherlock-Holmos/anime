package site.jokersh.anime.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class WindowsProtectedSessionTokenStoreTest {
    @Test
    fun `DPAPI protected session survives store recreation`() {
        val firstStore = WindowsProtectedSessionTokenStore()
        val token = "dpapi-test-token"
        val expiresAt = Instant.parse("2030-01-01T00:00:00Z")

        try {
            firstStore.save(token, expiresAt)

            val restored = WindowsProtectedSessionTokenStore().load()
            assertEquals(token, restored?.token)
            assertEquals(expiresAt, restored?.expiresAt)
        } finally {
            firstStore.clear()
        }

        assertNull(WindowsProtectedSessionTokenStore().load())
    }
}
