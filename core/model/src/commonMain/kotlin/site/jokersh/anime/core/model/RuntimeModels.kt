package site.jokersh.anime.core.model

import kotlin.time.Instant

public enum class ResourceKind { Discovery, Subject, Episode, Character, Person, Comment, User, Collection }

public enum class FieldId { Query, Year, PageSize, Comment, Progress, Status, Url, Identifier }

public enum class ValidationReason { Blank, OutOfRange, InvalidFormat, Unsupported, Conflict }

public sealed interface AppError {
    public data object Offline : AppError

    public data object Timeout : AppError

    public data class Unauthorized(
        public val recoverable: Boolean,
    ) : AppError

    public data class NotFound(
        public val resource: ResourceKind,
    ) : AppError

    public data class RateLimited(
        public val retryAt: Instant?,
    ) : AppError

    public data class Validation(
        public val field: FieldId,
        public val reason: ValidationReason,
    ) : AppError

    public data class Upstream(
        public val provider: Provider,
        public val traceId: String?,
    ) : AppError

    public data class Server(
        public val traceId: String?,
    ) : AppError

    public data class Data(
        public val diagnosticId: String,
    ) : AppError

    public data class Unknown(
        public val diagnosticId: String,
    ) : AppError
}

public enum class RefreshPolicy { IfMissing, IfStale, Force }

public enum class FreshnessKind { Fresh, Stale, OfflineCache }

public data class Freshness(
    public val kind: FreshnessKind,
    public val updatedAt: Instant,
)

public data class ResourceState<T>(
    public val value: T?,
    public val freshness: Freshness?,
    public val refreshing: Boolean,
    public val error: AppError?,
)

public enum class ThemePreference { System, Light, Dark }

public enum class GlassPreference { Auto, On, Off }

public enum class ReduceMotionPreference { FollowSystem, On, Off }

public data class AppSettings(
    public val theme: ThemePreference,
    public val dynamicColor: Boolean,
    public val glass: GlassPreference,
    public val reduceMotion: ReduceMotionPreference,
    public val diagnosticsConsent: Boolean,
)
