package site.jokersh.anime.core.navigation

import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.Provider
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.UserId
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.core.model.UserSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class AuthGateTest {
    @Test
    fun guestActionIsReplayedExactlyOnceAfterAuthentication() {
        val gate = AuthGate()
        val action = PendingAuthAction.OpenRating(42)

        assertIs<AuthGateDecision.Authenticate>(gate.request(action, SessionState.Guest))
        assertNull(gate.replayAfterAuthentication(SessionState.Guest))
        assertEquals(action, gate.replayAfterAuthentication(authenticatedSession()))
        assertNull(gate.replayAfterAuthentication(authenticatedSession()))
    }

    @Test
    fun cancellationDiscardsPendingAction() {
        val gate = AuthGate()
        gate.request(PendingAuthAction.SetCollection(9), SessionState.Guest)

        gate.cancel()

        assertNull(gate.replayAfterAuthentication(authenticatedSession()))
    }

    private fun authenticatedSession(): SessionState.Authenticated =
        SessionState.Authenticated(
            user =
                UserProfile(
                    summary = UserSummary(id = UserId("u1"), displayName = "User", avatar = null),
                    collectionCounts = CollectionStatus.entries.associateWith { 0 },
                    connectedProvider = Provider.Anime,
                ),
            expiresAt = Instant.parse("2099-01-01T00:00:00Z"),
        )
}
