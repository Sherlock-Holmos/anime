package site.jokersh.anime.data.session

import kotlinx.coroutines.flow.Flow
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.ExternalAuthRequest
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.UserCollectionSummary

public interface SessionRepository {
    public fun observeSession(): Flow<SessionState>

    /** Returns the currently authenticated user id, when the session has been restored. */
    public fun currentUserId(): String? = null

    public suspend fun beginLogin(request: LoginRequest): Result<ExternalAuthRequest>

    public suspend fun loginWithAnime(credentials: AnimeLoginCredentials): Result<SessionState.Authenticated>

    public suspend fun registerAnime(registration: AnimeRegistration): Result<SessionState.Authenticated>

    public suspend fun completeLogin(callback: AuthCallback): Result<SessionState.Authenticated>

    public suspend fun refresh(): Result<SessionState.Authenticated>

    public suspend fun logout(): Result<Unit>

    public suspend fun updateProfile(displayName: String): Result<Unit> =
        Result.failure(IllegalStateException("Profile service is unavailable"))

    public suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
        Result.failure(IllegalStateException("Password service is unavailable"))

    public suspend fun diagnostics(): Result<List<ServiceDiagnostic>> =
        Result.failure(IllegalStateException("Diagnostics service is unavailable"))

    public suspend fun exportMyData(): Result<String> =
        Result.failure(IllegalStateException("Export service is unavailable"))

    public suspend fun deleteAccount(): Result<Unit> =
        Result.failure(IllegalStateException("Account service is unavailable"))

    public suspend fun syncStatus(): Result<BangumiSyncStatus> =
        Result.failure(IllegalStateException("Sync service is unavailable"))

    public suspend fun startSync(): Result<Unit> = Result.failure(IllegalStateException("Sync service is unavailable"))

    public suspend fun syncConflicts(): Result<List<BangumiSyncConflict>> = Result.success(emptyList())

    public suspend fun resolveSyncConflict(
        id: String,
        expectedVersion: Long,
        choice: SyncConflictChoice,
    ): Result<Unit> = Result.failure(IllegalStateException("Sync service is unavailable"))

    public suspend fun collectionPage(
        status: CollectionStatus?,
        cursor: String? = null,
        limit: Int = 30,
    ): Result<UserCollectionPage> = Result.failure(IllegalStateException("Collection service is unavailable"))
}

public data class UserCollectionPage(
    val items: List<UserCollectionSummary>,
    val nextCursor: String?,
)

public data class ServiceDiagnostic(
    val endpoint: String,
    val statusCode: Int?,
    val healthy: Boolean,
    val body: String,
)

public data class BangumiSyncStatus(
    val pendingCount: Long,
    val failedCount: Long,
    val conflictCount: Long,
    val lastSuccessfulAt: String?,
    val bangumiLinked: Boolean,
)

public data class BangumiSyncConflict(
    val id: String,
    val subjectId: Long,
    val localVersion: Long,
    val fieldName: String,
    val localValue: String,
    val remoteValue: String,
    val detectedAt: String,
)

public enum class SyncConflictChoice(
    public val wireValue: String,
) {
    KeepLocal("keep_local"),
    UseRemote("use_remote"),
    Later("later"),
}
