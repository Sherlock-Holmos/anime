package site.jokersh.anime.data.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.ExternalAuthRequest
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.Provider
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.UserId
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.core.model.UserSummary
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

public class FixtureSessionRepository : SessionRepository {
    private val state: MutableStateFlow<SessionState> = MutableStateFlow(SessionState.Guest)

    override fun observeSession(): Flow<SessionState> = state

    override suspend fun beginLogin(request: LoginRequest): Result<ExternalAuthRequest> =
        Result.success(
            ExternalAuthRequest(
                requestId = request.requestId,
                authorizeUrl = "https://fixture.invalid/authorize",
                expiresAt = Clock.System.now() + 5.minutes,
            ),
        )

    override suspend fun loginWithAnime(credentials: AnimeLoginCredentials): Result<SessionState.Authenticated> =
        completeFixtureLogin(credentials.username)

    override suspend fun registerAnime(registration: AnimeRegistration): Result<SessionState.Authenticated> =
        completeFixtureLogin(registration.displayName)

    override suspend fun completeLogin(callback: AuthCallback): Result<SessionState.Authenticated> =
        runCatching {
            require(callback.authorizationCode.isNotBlank() && callback.state.isNotBlank())
            completeFixtureLogin("演示用户").getOrThrow()
        }

    override suspend fun refresh(): Result<SessionState.Authenticated> =
        (state.value as? SessionState.Authenticated)?.let(Result.Companion::success)
            ?: Result.failure(IllegalStateException("Guest sessions cannot refresh"))

    override suspend fun logout(): Result<Unit> =
        runCatching {
            state.value = SessionState.Guest
        }

    private fun completeFixtureLogin(displayName: String): Result<SessionState.Authenticated> =
        runCatching {
            val authenticated =
                SessionState.Authenticated(
                    user =
                        UserProfile(
                            summary = UserSummary(UserId("fixture-user"), displayName.ifBlank { "演示用户" }, null),
                            collectionCounts = CollectionStatus.entries.associateWith { 0 },
                            connectedProvider = Provider.Anime,
                        ),
                    expiresAt = Clock.System.now() + 1.hours,
                )
            state.value = authenticated
            authenticated
        }
}

public class InMemorySessionTokenStore : SessionTokenStore {
    private var stored: StoredSessionToken? = null

    override fun load(): StoredSessionToken? = stored

    override fun save(
        token: String,
        expiresAt: kotlin.time.Instant,
        refreshToken: String?,
    ) {
        stored = StoredSessionToken(token, expiresAt, refreshToken)
    }

    override fun clear() {
        stored = null
    }
}
