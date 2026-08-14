package site.jokersh.anime.data.session

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.ExternalAuthRequest
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.Provider
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.UserCollectionSummary
import site.jokersh.anime.core.model.UserId
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.core.model.UserSummary
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

public interface SessionTokenStore {
    public fun load(): StoredSessionToken?

    public fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String? = null,
    )

    public fun clear()
}

public data class StoredSessionToken(
    public val token: String,
    public val expiresAt: Instant,
    public val refreshToken: String? = null,
)

public class RemoteSessionRepository(
    private val client: HttpClient,
    apiBaseUrl: String,
    private val tokenStore: SessionTokenStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SessionRepository {
    private val baseUrl: String = apiBaseUrl.trimEnd('/')
    private val state: MutableStateFlow<SessionState> =
        MutableStateFlow(if (tokenStore.load() == null) SessionState.Guest else SessionState.Restoring)

    init {
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            "apiBaseUrl must use HTTP or HTTPS"
        }
    }

    override fun observeSession(): Flow<SessionState> = state

    override suspend fun beginLogin(request: LoginRequest): Result<ExternalAuthRequest> =
        runCatching {
            val response =
                client.post("$baseUrl/api/v1/auth/bangumi/start") {
                    tokenStore.load()?.let { header(HttpHeaders.Authorization, "Bearer ${it.token}") }
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(StartLoginRequest(request.pendingActionId?.let { "/pending/$it" })))
                }
            check(response.status.value in 200..299) { "OAuth start failed with ${response.status.value}" }
            val body = json.decodeFromString<StartLoginResponse>(response.bodyAsText())
            ExternalAuthRequest(
                requestId = request.requestId,
                authorizeUrl = body.authorizationUrl,
                expiresAt = Clock.System.now() + 5.minutes,
            )
        }

    override suspend fun loginWithAnime(credentials: AnimeLoginCredentials): Result<SessionState.Authenticated> =
        nativeAuthenticate(
            path = "/api/v1/auth/login",
            request = NativeAuthRequest(credentials.username, credentials.password),
        )

    override suspend fun registerAnime(registration: AnimeRegistration): Result<SessionState.Authenticated> =
        nativeAuthenticate(
            path = "/api/v1/auth/register",
            request =
                NativeAuthRequest(
                    username = registration.username,
                    password = registration.password,
                    displayName = registration.displayName,
                ),
        )

    override suspend fun completeLogin(callback: AuthCallback): Result<SessionState.Authenticated> =
        runCatching {
            require(callback.authorizationCode.isNotBlank() && callback.state.isNotBlank()) {
                "OAuth callback is incomplete"
            }
            val response =
                client.post("$baseUrl/api/v1/auth/bangumi/complete") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        json.encodeToString(
                            CompleteLoginRequest(
                                code = callback.authorizationCode,
                                state = callback.state,
                            ),
                        ),
                    )
                }
            val responseText = response.bodyAsText()
            if (response.status.value !in 200..299) {
                val message =
                    runCatching { json.decodeFromString<ErrorResponse>(responseText).error.message }
                        .getOrDefault("OAuth completion failed with ${response.status.value}")
                error(message)
            }
            val body = json.decodeFromString<CompleteLoginResponse>(responseText)
            val expiresAt = Instant.fromEpochSeconds(body.sessionExpiresAt)
            tokenStore.save(body.sessionToken, expiresAt, body.refreshToken)
            val authenticated = fetchCurrentUser(body.sessionToken, expiresAt)
            state.value = authenticated
            authenticated
        }.onFailure { failure ->
            state.value = SessionState.Failed(loginFailureMessage(failure))
        }

    override suspend fun refresh(): Result<SessionState.Authenticated> {
        val stored = tokenStore.load()
        if (stored == null) {
            state.value = SessionState.Guest
            return Result.failure(IllegalStateException("No saved session"))
        }
        if (stored.expiresAt <= Clock.System.now()) {
            tokenStore.clear()
            state.value = SessionState.Expired(null)
            return Result.failure(IllegalStateException("Session expired"))
        }
        state.value = SessionState.Restoring
        return runCatching {
            val active =
                if (stored.expiresAt <= Clock.System.now() + 2.minutes &&
                    stored.refreshToken != null
                ) {
                    rotateSession(stored.refreshToken)
                } else {
                    stored
                }
            fetchCurrentUser(active.token, active.expiresAt)
        }.onSuccess { state.value = it }
            .onFailure { failure ->
                if (failure is UnauthorizedSessionException) {
                    tokenStore.clear()
                    state.value = SessionState.Expired(null)
                } else {
                    state.value = SessionState.Failed(loginFailureMessage(failure))
                }
            }
    }

    private suspend fun rotateSession(refreshToken: String): StoredSessionToken {
        val response =
            client.post("$baseUrl/api/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(RefreshSessionRequest(refreshToken)))
            }
        val responseText = response.bodyAsText()
        if (response.status.value == 401) throw UnauthorizedSessionException()
        if (response.status.value !in 200..299) {
            val message =
                runCatching { json.decodeFromString<ErrorResponse>(responseText).error.message }
                    .getOrDefault("Session refresh failed with ${response.status.value}")
            error(message)
        }
        val body = json.decodeFromString<RefreshSessionResponse>(responseText)
        val refreshed =
            StoredSessionToken(body.sessionToken, Instant.fromEpochSeconds(body.sessionExpiresAt), body.refreshToken)
        tokenStore.save(refreshed.token, refreshed.expiresAt, refreshed.refreshToken)
        return refreshed
    }

    override suspend fun logout(): Result<Unit> =
        runCatching {
            tokenStore.load()?.let { stored ->
                client.post("$baseUrl/api/v1/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer ${stored.token}")
                }
            }
            tokenStore.clear()
            state.value = SessionState.Guest
        }

    override suspend fun syncStatus(): Result<BangumiSyncStatus> =
        authenticatedGet("/api/v1/me/sync/status") { body: SyncStatusDto ->
            BangumiSyncStatus(
                body.pendingCount,
                body.failedCount,
                body.conflictCount,
                body.lastSuccessfulAt,
                body.bangumiLinked,
            )
        }

    override suspend fun startSync(): Result<Unit> = authenticatedPost<Unit>("/api/v1/me/sync", null)

    override suspend fun syncConflicts(): Result<List<BangumiSyncConflict>> =
        authenticatedGet("/api/v1/me/sync/conflicts") { items: List<SyncConflictDto> ->
            items.map { it.toModel() }
        }

    override suspend fun resolveSyncConflict(
        id: String,
        expectedVersion: Long,
        choice: SyncConflictChoice,
    ): Result<Unit> =
        authenticatedPost(
            "/api/v1/me/sync/conflicts/$id/resolve",
            ResolveSyncConflictRequest(choice.wireValue, expectedVersion),
        )

    override suspend fun collectionPage(
        status: CollectionStatus?,
        cursor: String?,
        limit: Int,
    ): Result<UserCollectionPage> {
        val statusQuery = status?.let { "&status=${it.wireValue}" }.orEmpty()
        val cursorQuery = cursor?.let { "&cursor=${it.encodeURLParameter()}" }.orEmpty()
        return authenticatedGet(
            "/api/v1/me/collections?limit=$limit$statusQuery$cursorQuery",
        ) { page: CollectionPageDto ->
            UserCollectionPage(page.items.map { it.toModel(baseUrl) }, page.nextCursor)
        }
    }

    private suspend inline fun <reified T, R> authenticatedGet(
        path: String,
        transform: (T) -> R,
    ): Result<R> =
        runCatching {
            val stored = tokenStore.load() ?: error("请先登录 Anime")
            val response = client.get("$baseUrl$path") { header(HttpHeaders.Authorization, "Bearer ${stored.token}") }
            val text = response.bodyAsText()
            check(response.status.value in 200..299) { text.ifBlank { "请求失败：${response.status.value}" } }
            transform(json.decodeFromString<T>(text))
        }

    private suspend inline fun <reified T> authenticatedPost(
        path: String,
        body: T?,
    ): Result<Unit> =
        runCatching {
            val stored = tokenStore.load() ?: error("请先登录 Anime")
            val response =
                client.post("$baseUrl$path") {
                    header(HttpHeaders.Authorization, "Bearer ${stored.token}")
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(json.encodeToString(body))
                    }
                }
            val text = response.bodyAsText()
            check(response.status.value in 200..299) { text.ifBlank { "请求失败：${response.status.value}" } }
        }

    private suspend fun nativeAuthenticate(
        path: String,
        request: NativeAuthRequest,
    ): Result<SessionState.Authenticated> {
        state.value = SessionState.Restoring
        return runCatching {
            val response =
                client.post("$baseUrl$path") {
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(request))
                }
            val responseText = response.bodyAsText()
            if (response.status.value !in 200..299) {
                val message =
                    runCatching { json.decodeFromString<ErrorResponse>(responseText).error.message }
                        .getOrDefault("Anime 登录失败，请稍后重试。")
                error(message)
            }
            val body = json.decodeFromString<CompleteLoginResponse>(responseText)
            val expiresAt = Instant.fromEpochSeconds(body.sessionExpiresAt)
            tokenStore.save(body.sessionToken, expiresAt, body.refreshToken)
            fetchCurrentUser(body.sessionToken, expiresAt).also { state.value = it }
        }.onFailure { failure ->
            state.value = SessionState.Failed(loginFailureMessage(failure))
        }
    }

    private suspend fun fetchCurrentUser(
        token: String,
        expiresAt: Instant,
    ): SessionState.Authenticated {
        val response =
            client.get("$baseUrl/api/v1/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        val responseText = response.bodyAsText()
        if (response.status.value == 401) throw UnauthorizedSessionException()
        if (response.status.value !in 200..299) {
            val message =
                runCatching { json.decodeFromString<ErrorResponse>(responseText).error.message }
                    .getOrDefault("Profile refresh failed with ${response.status.value}")
            error(message)
        }
        val body = json.decodeFromString<MeResponse>(responseText)
        return SessionState.Authenticated(
            user =
                UserProfile(
                    summary =
                        UserSummary(
                            id = UserId(body.user.id),
                            displayName = body.user.displayName,
                            avatar =
                                body.user.avatarUrl
                                    .takeIf(String::isNotBlank)
                                    ?.let {
                                        ImageRef.Remote(
                                            resolveUrl(baseUrl, it),
                                            "bangumi-avatar-${body.user.id}",
                                        )
                                    },
                        ),
                    collectionCounts =
                        body.collectionCounts
                            .mapNotNull { (key, value) ->
                                collectionStatus(key)?.let { it to value }
                            }.toMap(),
                    connectedProvider = if (body.connectedProvider == "bangumi") Provider.Bangumi else Provider.Anime,
                    ratingCount = body.ratingCount,
                    reviewCount = body.reviewCount,
                    listCount = body.listCount,
                    collections = body.collections.map { it.toModel(baseUrl) },
                    syncedAt = Instant.fromEpochSeconds(body.syncedAt),
                ),
            expiresAt = expiresAt,
        )
    }
}

private class UnauthorizedSessionException : IllegalStateException("Session is invalid or expired")

private fun collectionStatus(value: String): CollectionStatus? =
    when (value) {
        "wish" -> CollectionStatus.Wish
        "watching" -> CollectionStatus.Watching
        "completed" -> CollectionStatus.Completed
        "on_hold" -> CollectionStatus.OnHold
        "dropped" -> CollectionStatus.Dropped
        else -> null
    }

private val CollectionStatus.wireValue: String
    get() =
        when (this) {
            CollectionStatus.Wish -> "wish"
            CollectionStatus.Watching -> "watching"
            CollectionStatus.Completed -> "completed"
            CollectionStatus.OnHold -> "on_hold"
            CollectionStatus.Dropped -> "dropped"
        }

private fun loginFailureMessage(failure: Throwable): String {
    val detail = failure.message.orEmpty()
    val normalized = detail.lowercase()
    return when {
        normalized.contains("connect") && normalized.contains("127.0.0.1:8080") -> {
            "本地登录服务未启动，请启动 anime-backend 后重试。"
        }

        normalized.contains("timeout") -> {
            "登录请求超时：本地服务未能及时连接 Bangumi，请检查代理或网络后重试。"
        }

        detail.isNotBlank() -> {
            detail
        }

        else -> {
            "Bangumi 登录失败，请稍后重试。"
        }
    }
}

@Serializable
private data class StartLoginRequest(
    @SerialName("redirect_to") val redirectTo: String?,
)

@Serializable
private data class StartLoginResponse(
    @SerialName("authorization_url") val authorizationUrl: String,
)

@Serializable
private data class CompleteLoginRequest(
    val code: String,
    val state: String,
)

@Serializable
private data class CompleteLoginResponse(
    val user: LoginUser,
    @SerialName("session_token") val sessionToken: String,
    @SerialName("session_expires_at") val sessionExpiresAt: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
private data class RefreshSessionResponse(
    @SerialName("session_token") val sessionToken: String,
    @SerialName("session_expires_at") val sessionExpiresAt: Long,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class RefreshSessionRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
private data class LoginUser(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String,
)

@Serializable
private data class MeResponse(
    val user: LoginUser,
    @SerialName("connected_provider") val connectedProvider: String = "bangumi",
    @SerialName("collection_counts") val collectionCounts: Map<String, Int>,
    @SerialName("rating_count") val ratingCount: Int,
    @SerialName("review_count") val reviewCount: Int,
    @SerialName("list_count") val listCount: Int,
    val collections: List<RemoteCollectionSummary>,
    @SerialName("synced_at") val syncedAt: Long,
)

@Serializable
private data class NativeAuthRequest(
    val username: String,
    val password: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
private data class RemoteCollectionSummary(
    @SerialName("subject_id") val subjectId: Long,
    val title: String,
    @SerialName("original_title") val originalTitle: String,
    @SerialName("poster_url") val posterUrl: String?,
    @SerialName("air_date") val airDate: String?,
    val score: Double,
    val status: String,
    @SerialName("user_rating") val userRating: Int,
    val comment: String,
    @SerialName("episode_progress") val episodeProgress: Int,
    @SerialName("total_episodes") val totalEpisodes: Int,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toModel(baseUrl: String): UserCollectionSummary =
        UserCollectionSummary(
            subjectId =
                site.jokersh.anime.core.model
                    .SubjectId(subjectId),
            title = title,
            originalTitle = originalTitle,
            posterUrl = posterUrl?.let { resolveUrl(baseUrl, it) },
            airDate = airDate,
            score = score,
            status = collectionStatus(status) ?: CollectionStatus.Wish,
            userRating = userRating,
            comment = comment,
            episodeProgress = episodeProgress,
            totalEpisodes = totalEpisodes,
            updatedAt = updatedAt,
        )
}

private fun resolveUrl(
    baseUrl: String,
    value: String,
): String =
    if (value.startsWith("http://") || value.startsWith("https://")) value else "$baseUrl/${value.trimStart('/')}"

@Serializable
private data class ErrorResponse(
    val error: ErrorDetail,
)

@Serializable
private data class ErrorDetail(
    val message: String,
)

@Serializable
private data class SyncStatusDto(
    @SerialName("pending_count") val pendingCount: Long,
    @SerialName("failed_count") val failedCount: Long,
    @SerialName("conflict_count") val conflictCount: Long,
    @SerialName("last_successful_at") val lastSuccessfulAt: String? = null,
    @SerialName("bangumi_linked") val bangumiLinked: Boolean,
)

@Serializable
private data class SyncConflictDto(
    val id: String,
    @SerialName("subject_id") val subjectId: Long,
    @SerialName("local_version") val localVersion: Long,
    @SerialName("field_name") val fieldName: String,
    @SerialName("local_value") val localValue: kotlinx.serialization.json.JsonElement,
    @SerialName("remote_value") val remoteValue: kotlinx.serialization.json.JsonElement,
    @SerialName("detected_at") val detectedAt: String,
) {
    fun toModel(): BangumiSyncConflict =
        BangumiSyncConflict(
            id,
            subjectId,
            localVersion,
            fieldName,
            localValue.toString(),
            remoteValue.toString(),
            detectedAt,
        )
}

@Serializable
private data class ResolveSyncConflictRequest(
    val choice: String,
    @SerialName("expected_version") val expectedVersion: Long,
)

@Serializable
private data class CollectionPageDto(
    val items: List<RemoteCollectionSummary>,
    @SerialName("next_cursor") val nextCursor: String? = null,
)
