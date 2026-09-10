package site.jokersh.anime.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNavigatorTest {
    @Test
    fun eachRootRetainsItsOwnBackStack() {
        var selectedRoot = AppRoot.Discover
        val stacks = AppRoot.entries.associateWith { mutableListOf<NavKey>(it.initialRoute()) }
        val navigator = AppNavigator({ selectedRoot }, { selectedRoot = it }, stacks::getValue)

        navigator.push(AppRoute.Subject(1001, RouteOrigin.Discover))
        navigator.selectRoot(AppRoot.Library)
        navigator.push(AppRoute.SearchResults(SearchRouteRequest("星海")))

        assertEquals(
            AppRoute.SearchResults(SearchRouteRequest("星海")),
            stacks.getValue(AppRoot.Library).last(),
        )
        navigator.selectRoot(AppRoot.Discover)
        assertEquals(AppRoute.Subject(1001, RouteOrigin.Discover), stacks.getValue(AppRoot.Discover).last())
    }

    @Test
    fun popNeverRemovesRoot() {
        var selectedRoot = AppRoot.Discover
        val stack = mutableListOf<NavKey>(AppRoute.Discover)
        val navigator = AppNavigator({ selectedRoot }, { selectedRoot = it }, { stack })

        assertFalse(navigator.pop())
        navigator.push(AppRoute.Subject(1001))
        assertTrue(navigator.pop())
        assertEquals(listOf<NavKey>(AppRoute.Discover), stack)
    }

    @Test
    fun popToRootKeepsInitialDestination() {
        var selectedRoot = AppRoot.Discover
        val stack =
            mutableListOf<NavKey>(
                AppRoute.Discover,
                AppRoute.Subject(1001),
                AppRoute.Comments(1001),
            )
        val navigator = AppNavigator({ selectedRoot }, { selectedRoot = it }, { stack })

        navigator.popToRoot()

        assertEquals(listOf<NavKey>(AppRoute.Discover), stack)
    }

    @Test
    fun selectingActiveRootReturnsToItsRoot() {
        var selectedRoot = AppRoot.Discover
        val stack = mutableListOf<NavKey>(AppRoute.Discover, AppRoute.Subject(1001))
        val navigator = AppNavigator({ selectedRoot }, { selectedRoot = it }, { stack })

        navigator.selectRoot(AppRoot.Discover)

        assertEquals(listOf<NavKey>(AppRoute.Discover), stack)
        assertEquals(AppRoot.Discover, selectedRoot)
    }

    @Test
    fun routesRoundTripThroughTheSavedStateSerializerModule() {
        val json =
            Json {
                serializersModule = AppNavigationSavedStateConfiguration.serializersModule
            }
        val route: NavKey = AppRoute.Subject(1001, RouteOrigin.Discover)

        val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)
        val restored = json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded)

        assertEquals(route, restored)
    }

    @Test
    fun socialRoutesRoundTripThroughSavedState() {
        val json = Json { serializersModule = AppNavigationSavedStateConfiguration.serializersModule }
        val routes: List<NavKey> =
            listOf(
                AppRoute.Activity(ActivityFeedRoute.Popular),
                AppRoute.RatingEditor(1001),
                AppRoute.Review("review-1"),
                AppRoute.CuratedList("list-1"),
                AppRoute.User("user-1"),
            )

        routes.forEach { route ->
            val encoded = json.encodeToString(PolymorphicSerializer(NavKey::class), route)
            assertEquals(route, json.decodeFromString(PolymorphicSerializer(NavKey::class), encoded))
        }
    }
}
