package site.jokersh.anime.app

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDate
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.Foundation.NSUserDefaults
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AuthCallback
import site.jokersh.anime.core.model.CharacterCredit
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.Cursor
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.Episode
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.SearchRequest
import site.jokersh.anime.core.model.SearchDiscovery
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.SubjectCredits
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.core.model.SubjectRelation
import site.jokersh.anime.core.model.SubjectSection
import site.jokersh.anime.core.model.UserCollectionSummary
import site.jokersh.anime.core.model.UserProfile
import site.jokersh.anime.data.catalog.CalendarPage
import site.jokersh.anime.data.catalog.CatalogCacheStore
import site.jokersh.anime.data.catalog.RemoteCatalogRepository
import site.jokersh.anime.data.catalog.RemoteSearchRepository
import site.jokersh.anime.data.collection.CollectionStore
import site.jokersh.anime.data.collection.OfflineFirstCollectionRepository
import site.jokersh.anime.data.comment.CommentDraftStore
import site.jokersh.anime.data.comment.CommunityActivity
import site.jokersh.anime.data.comment.CommunityComment
import site.jokersh.anime.data.comment.CommunityListDetail
import site.jokersh.anime.data.comment.CommunityListItem
import site.jokersh.anime.data.comment.CommunityListSummary
import site.jokersh.anime.data.comment.CommunityNotification
import site.jokersh.anime.data.comment.CommunityReaction
import site.jokersh.anime.data.comment.CommunityReview
import site.jokersh.anime.data.comment.CommunityReviewPage
import site.jokersh.anime.data.comment.CommunityUserProfile
import site.jokersh.anime.data.comment.OfflineFirstCommunityRepository
import site.jokersh.anime.data.comment.RatingOutboxStore
import site.jokersh.anime.data.comment.RemoteCommentRepository
import site.jokersh.anime.data.comment.RemoteCommunityRepository
import site.jokersh.anime.data.session.RemoteSessionRepository
import site.jokersh.anime.data.session.BangumiSyncConflict
import site.jokersh.anime.data.session.BangumiSyncStatus
import site.jokersh.anime.data.session.AdminOverview
import site.jokersh.anime.data.session.AdminComment
import site.jokersh.anime.data.session.AdminReport
import site.jokersh.anime.data.session.ServiceDiagnostic
import site.jokersh.anime.data.session.ServiceDiagnosticEndpoint
import site.jokersh.anime.data.session.SessionTokenStore
import site.jokersh.anime.data.session.StoredSessionToken
import site.jokersh.anime.data.session.SyncConflictChoice
import site.jokersh.anime.data.session.UserRatingPage
import site.jokersh.anime.data.session.UserRatingSummary
import site.jokersh.anime.data.settings.PersistentSettingsRepository
import site.jokersh.anime.data.settings.SettingsStore
import kotlin.time.Instant

internal class IosKeychainSessionTokenStore(
    private val readSecret: (String) -> String?,
    private val writeSecret: (String, String) -> Unit,
    private val removeSecret: (String) -> Unit,
    private val clearLocalUserData: () -> Unit,
) : SessionTokenStore {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): StoredSessionToken? {
        readSecret(SESSION_RECORD_ACCOUNT)?.let { value ->
            runCatching { json.decodeFromString<IosStoredSessionRecord>(value) }
                .getOrNull()
                ?.takeIf { it.accessToken.isNotBlank() }
                ?.let {
                    return StoredSessionToken(
                        token = it.accessToken,
                        expiresAt = Instant.fromEpochSeconds(it.expiresAtEpochSeconds),
                        refreshToken = it.refreshToken?.takeIf(String::isNotBlank),
                    )
                }
            // A damaged bundle should not prevent a legacy item from being recovered.
            removeSecret(SESSION_RECORD_ACCOUNT)
        }

        val token = readSecret(ACCESS_TOKEN_ACCOUNT)?.takeIf(String::isNotBlank) ?: return null
        // Older builds stored this as a string, while a partially completed save can
        // leave only the Keychain values behind. Treat a missing/invalid timestamp as
        // already expired so a saved refresh token gets a chance to restore the session.
        val expiresAt =
            defaults.stringForKey(EXPIRY_KEY)?.toLongOrNull()
                ?: defaults.objectForKey(EXPIRY_KEY)?.toString()?.toLongOrNull()
                ?: 0L
        val legacy = StoredSessionToken(
            token = token,
            expiresAt = Instant.fromEpochSeconds(expiresAt),
            refreshToken = readSecret(REFRESH_TOKEN_ACCOUNT)?.takeIf(String::isNotBlank),
        )
        // Migrate the old split representation after it has been read successfully.
        // The legacy values remain as a fallback for one upgrade cycle and are removed
        // together with the new record by clear().
        save(legacy.token, legacy.expiresAt, legacy.refreshToken)
        return legacy
    }

    override fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String?,
    ) {
        // Keep all session fields in one Keychain item. This prevents a process
        // interruption between three independent writes from creating a token with
        // a mismatched expiry or refresh token.
        writeSecret(
            SESSION_RECORD_ACCOUNT,
            json.encodeToString(
                IosStoredSessionRecord(
                    accessToken = token,
                    refreshToken = refreshToken,
                    expiresAtEpochSeconds = expiresAt.epochSeconds,
                ),
            ),
        )
    }

    override fun clear() {
        removeSecret(SESSION_RECORD_ACCOUNT)
        removeSecret(ACCESS_TOKEN_ACCOUNT)
        removeSecret(REFRESH_TOKEN_ACCOUNT)
        defaults.removeObjectForKey(EXPIRY_KEY)
        // These stores contain user-owned offline work. They must not survive a
        // logout/expired session and become visible or replayed under another account.
        clearLocalUserData()
    }
}

@Serializable
private data class IosStoredSessionRecord(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochSeconds: Long,
)

internal class IosLocalUserDataStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    fun clear() {
        defaults.removeObjectForKey(COLLECTION_SNAPSHOT_KEY)
        defaults.removeObjectForKey(COMMENT_DRAFTS_KEY)
        defaults.removeObjectForKey(RATING_OUTBOX_KEY)
    }

    private companion object {
        const val COLLECTION_SNAPSHOT_KEY = "anime.collection.snapshots"
        const val COMMENT_DRAFTS_KEY = "anime.comment.drafts"
        const val RATING_OUTBOX_KEY = "anime.rating.outbox"
    }
}

internal abstract class IosStringStore(
    private val key: String,
) {
    private val defaults = NSUserDefaults.standardUserDefaults

    protected fun readValue(): String? = defaults.stringForKey(key)

    protected fun writeValue(value: String) = defaults.setObject(value, forKey = key)
}

internal class IosSettingsStore : SettingsStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey("anime.settings.$key")

    override fun write(
        key: String,
        value: String,
    ) = defaults.setObject(value, forKey = "anime.settings.$key")

    override fun remove(key: String) = defaults.removeObjectForKey("anime.settings.$key")
}

internal class IosCollectionStore :
    IosStringStore("anime.collection.snapshots"),
    CollectionStore {
    override fun read(): String? = readValue()

    override fun write(value: String) = writeValue(value)
}

internal class IosCommentDraftStore :
    IosStringStore("anime.comment.drafts"),
    CommentDraftStore {
    override fun read(): String? = readValue()

    override fun write(value: String) = writeValue(value)
}

internal class IosCatalogCacheStore : CatalogCacheStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(key: String): String? = defaults.stringForKey("anime.catalog.$key")

    override fun write(
        key: String,
        value: String,
    ) = defaults.setObject(value, forKey = "anime.catalog.$key")
}

internal class IosRatingOutboxStore :
    IosStringStore("anime.rating.outbox"),
    RatingOutboxStore {
    override fun read(): String? = readValue()

    override fun write(value: String) = writeValue(value)
}
