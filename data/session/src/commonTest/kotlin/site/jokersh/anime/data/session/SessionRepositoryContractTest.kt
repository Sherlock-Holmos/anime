package site.jokersh.anime.data.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.ExternalAuthRequest
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.Provider
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.UserId
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.core.model.UserSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

private val fixtureUser =
    UserProfile(
        summary = UserSummary(UserId("fixture-user"), "演示用户", null),
        collectionCounts = CollectionStatus.entries.associateWith { 0 },
        connectedProvider = Provider.Anime,
    )

abstract class SessionRepositoryContract {
    protected abstract fun createRepository(): SessionRepository

    @Test
    fun `CT-AUTH-001 login completes once and becomes observable`() =
        runTest {
            val repository = createRepository()
            val request = repository.beginLogin(LoginRequest("request-1", null)).getOrThrow()

            val authenticated = repository.completeLogin(AuthCallback(request.requestId, "ticket")).getOrThrow()

            assertEquals(fixtureUser, authenticated.user)
            assertIs<SessionState.Authenticated>(repository.observeSession().first())
        }

    @Test
    fun `CT-AUTH-003 refresh fails for a guest without changing session`() =
        runTest {
            val repository = createRepository()

            assertTrue(repository.refresh().isFailure)
            assertEquals(SessionState.Guest, repository.observeSession().first())
        }

    @Test
    fun `CT-AUTH-004 logout clears authenticated state`() =
        runTest {
            val repository = createRepository()
            val request = repository.beginLogin(LoginRequest("request-2", null)).getOrThrow()
            repository.completeLogin(AuthCallback(request.requestId, "ticket")).getOrThrow()

            assertTrue(repository.logout().isSuccess)
            assertEquals(SessionState.Guest, repository.observeSession().first())
        }
}

class FixtureSessionRepositoryContractTest : SessionRepositoryContract() {
    override fun createRepository(): SessionRepository = InMemorySessionRepository()
}

private class InMemorySessionRepository : SessionRepository {
    private val state = MutableStateFlow<SessionState>(SessionState.Guest)
    private var pendingRequestId: String? = null

    override fun observeSession(): Flow<SessionState> = state

    override suspend fun beginLogin(request: LoginRequest): Result<ExternalAuthRequest> {
        pendingRequestId = request.requestId
        return Result.success(
            ExternalAuthRequest(
                requestId = request.requestId,
                authorizeUrl = "https://fixture.invalid/authorize",
                expiresAt = Instant.parse("2026-07-19T08:05:00Z"),
            ),
        )
    }

    override suspend fun completeLogin(callback: AuthCallback): Result<SessionState.Authenticated> {
        if (callback.requestId != pendingRequestId || callback.ticket.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid auth callback"))
        }
        val authenticated =
            SessionState.Authenticated(
                user = fixtureUser,
                expiresAt = Instant.parse("2026-07-19T09:00:00Z"),
            )
        pendingRequestId = null
        state.value = authenticated
        return Result.success(authenticated)
    }

    override suspend fun refresh(): Result<SessionState.Authenticated> {
        val current = state.value as? SessionState.Authenticated
        return current?.let(Result.Companion::success)
            ?: Result.failure(IllegalStateException("Guest sessions cannot refresh"))
    }

    override suspend fun logout(): Result<Unit> {
        pendingRequestId = null
        state.value = SessionState.Guest
        return Result.success(Unit)
    }
}
