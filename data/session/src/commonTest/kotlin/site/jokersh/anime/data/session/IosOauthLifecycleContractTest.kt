package site.jokersh.anime.data.session

import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.LoginRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Shared session contract used by the iOS OAuth and Keychain lifecycle. */
class IosOauthLifecycleContractTest {
    @Test
    fun fixtureOauthLifecycleRejectsIncompleteCallbackAndCanLogout() = runTest {
        val repository = FixtureSessionRepository()

        assertTrue(repository.completeLogin(AuthCallback("", "state")).isFailure)
        assertTrue(repository.completeLogin(AuthCallback("code", "")).isFailure)

        repository.beginLogin(LoginRequest("request-1", null))
        val authenticated = repository.completeLogin(AuthCallback("code", "state"))
        assertTrue(authenticated.isSuccess)
        assertTrue(repository.logout().isSuccess)
        assertEquals(null, repository.currentUserId())
    }
}
