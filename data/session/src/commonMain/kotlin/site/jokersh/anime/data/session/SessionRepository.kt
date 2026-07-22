package site.jokersh.anime.data.session

import kotlinx.coroutines.flow.Flow
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.ExternalAuthRequest
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.SessionState

public interface SessionRepository {
    public fun observeSession(): Flow<SessionState>

    public suspend fun beginLogin(request: LoginRequest): Result<ExternalAuthRequest>

    public suspend fun completeLogin(callback: AuthCallback): Result<SessionState.Authenticated>

    public suspend fun refresh(): Result<SessionState.Authenticated>

    public suspend fun logout(): Result<Unit>
}
