package site.jokersh.anime.core.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** A lightweight, serializable description of every application destination. */
@Serializable
public sealed interface AppRoute : NavKey {
    @Serializable
    public data object Discover : AppRoute

    @Serializable
    public data class Search(
        val query: String? = null,
    ) : AppRoute

    @Serializable
    public data class Collection(
        val filter: CollectionRouteFilter? = null,
    ) : AppRoute

    @Serializable
    public data object Profile : AppRoute

    @Serializable
    public data class SearchResults(
        val request: SearchRouteRequest,
    ) : AppRoute

    @Serializable
    public data class Subject(
        val subjectId: Long,
        val origin: RouteOrigin = RouteOrigin.Unknown,
    ) : AppRoute {
        init {
            require(subjectId > 0) { "subjectId must be positive" }
        }
    }

    @Serializable
    public data class Episodes(
        val subjectId: Long,
    ) : AppRoute

    @Serializable
    public data class Characters(
        val subjectId: Long,
    ) : AppRoute

    @Serializable
    public data class Relations(
        val subjectId: Long,
    ) : AppRoute

    @Serializable
    public data class Comments(
        val subjectId: Long,
        val sort: CommentRouteSort = CommentRouteSort.Newest,
    ) : AppRoute

    @Serializable
    public data class Login(
        val requestId: String,
    ) : AppRoute

    @Serializable
    public data class OAuthResult(
        val ticket: String,
    ) : AppRoute

    @Serializable
    public data object Settings : AppRoute

    @Serializable
    public data object Diagnostics : AppRoute
}

@Serializable
public enum class RouteOrigin {
    Discover,
    Search,
    Collection,
    Related,
    DeepLink,
    Unknown,
}

@Serializable
public data class SearchRouteRequest(
    val query: String,
    val types: Set<SearchRouteSubjectType> = emptySet(),
    val yearStart: Int? = null,
    val yearEnd: Int? = null,
    val airing: Set<SearchRouteAiringStatus> = emptySet(),
) {
    init {
        require(query.isNotBlank()) { "query must not be blank" }
        require(yearStart == null || yearStart >= 1900) { "yearStart must be 1900 or later" }
        require(yearEnd == null || yearEnd >= 1900) { "yearEnd must be 1900 or later" }
        require(yearStart == null || yearEnd == null || yearStart <= yearEnd) {
            "yearStart must not be later than yearEnd"
        }
    }
}

@Serializable
public enum class SearchRouteSubjectType { Tv, Web, Ova, Movie, Other }

@Serializable
public enum class SearchRouteAiringStatus { Announced, Airing, Finished, Unknown }

@Serializable
public enum class CommentRouteSort { Newest, Oldest }

@Serializable
public enum class CollectionRouteFilter { Wish, Watching, Completed, OnHold, Dropped }

public enum class AppRoot {
    Discover,
    Search,
    Collection,
    Profile,
}

public fun AppRoot.initialRoute(): AppRoute =
    when (this) {
        AppRoot.Discover -> AppRoute.Discover
        AppRoot.Search -> AppRoute.Search()
        AppRoot.Collection -> AppRoute.Collection()
        AppRoot.Profile -> AppRoute.Profile
    }

public val AppRoute.root: AppRoot?
    get() =
        when (this) {
            AppRoute.Discover -> AppRoot.Discover
            is AppRoute.Search -> AppRoot.Search
            is AppRoute.Collection -> AppRoot.Collection
            AppRoute.Profile -> AppRoot.Profile
            else -> null
        }

/** Explicit registration keeps route restoration working in common Compose code, not only Android. */
public val AppNavigationSavedStateConfiguration: SavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule =
            SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(AppRoute.Discover.serializer())
                    subclass(AppRoute.Search.serializer())
                    subclass(AppRoute.Collection.serializer())
                    subclass(AppRoute.Profile.serializer())
                    subclass(AppRoute.SearchResults.serializer())
                    subclass(AppRoute.Subject.serializer())
                    subclass(AppRoute.Episodes.serializer())
                    subclass(AppRoute.Characters.serializer())
                    subclass(AppRoute.Relations.serializer())
                    subclass(AppRoute.Comments.serializer())
                    subclass(AppRoute.Login.serializer())
                    subclass(AppRoute.OAuthResult.serializer())
                    subclass(AppRoute.Settings.serializer())
                    subclass(AppRoute.Diagnostics.serializer())
                }
            }
    }
