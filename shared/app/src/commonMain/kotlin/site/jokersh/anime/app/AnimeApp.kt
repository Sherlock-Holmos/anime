package site.jokersh.anime.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import site.jokersh.anime.app.generated.resources.Res
import site.jokersh.anime.app.generated.resources.app_name
import site.jokersh.anime.app.generated.resources.data_mode_fixture
import site.jokersh.anime.app.generated.resources.data_mode_remote
import site.jokersh.anime.app.generated.resources.data_mode_remote_fixture
import site.jokersh.anime.app.generated.resources.f0_description
import site.jokersh.anime.app.generated.resources.f0_footer
import site.jokersh.anime.app.generated.resources.f0_status_title
import site.jokersh.anime.app.generated.resources.ic_activity
import site.jokersh.anime.app.generated.resources.ic_explore
import site.jokersh.anime.app.generated.resources.ic_person
import site.jokersh.anime.app.generated.resources.ic_search
import site.jokersh.anime.app.generated.resources.navigation_not_implemented
import site.jokersh.anime.app.generated.resources.profile_data_mode
import site.jokersh.anime.app.generated.resources.profile_diagnostics
import site.jokersh.anime.app.generated.resources.profile_diagnostics_disabled
import site.jokersh.anime.app.generated.resources.profile_diagnostics_enabled
import site.jokersh.anime.app.generated.resources.profile_page_size
import site.jokersh.anime.app.generated.resources.profile_page_size_value
import site.jokersh.anime.app.generated.resources.root_activity
import site.jokersh.anime.app.generated.resources.root_discover
import site.jokersh.anime.app.generated.resources.root_library
import site.jokersh.anime.app.generated.resources.root_profile
import site.jokersh.anime.app.generated.resources.shell_environment
import site.jokersh.anime.core.designsystem.AnimeBackdropHost
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimeLiquidTabBar
import site.jokersh.anime.core.designsystem.AnimeMotion
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.AnimeTheme
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AnimeLoginCredentials
import site.jokersh.anime.core.model.AnimeRegistration
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.GlassPreference
import site.jokersh.anime.core.model.LoginRequest
import site.jokersh.anime.core.model.ReduceMotionPreference
import site.jokersh.anime.core.model.SearchSort
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.core.model.ThemePreference
import site.jokersh.anime.core.navigation.AppNavigationSavedStateConfiguration
import site.jokersh.anime.core.navigation.AppNavigator
import site.jokersh.anime.core.navigation.AppRoot
import site.jokersh.anime.core.navigation.AppRoute
import site.jokersh.anime.core.navigation.AuthGateDecision
import site.jokersh.anime.core.navigation.CommentRouteSort
import site.jokersh.anime.core.navigation.PendingAuthAction
import site.jokersh.anime.core.navigation.RootSelectionResult
import site.jokersh.anime.core.navigation.RouteOrigin
import site.jokersh.anime.core.navigation.SearchRouteAiringStatus
import site.jokersh.anime.core.navigation.SearchRouteSort
import site.jokersh.anime.core.navigation.SearchRouteSubjectType
import site.jokersh.anime.core.navigation.root
import site.jokersh.anime.feature.activity.ActivityScreen
import site.jokersh.anime.feature.collection.CollectionScreen
import site.jokersh.anime.feature.comment.CommentsScreen
import site.jokersh.anime.feature.community.CuratedListScreen
import site.jokersh.anime.feature.community.RatingEditorScreen
import site.jokersh.anime.feature.community.ReviewDetailScreen
import site.jokersh.anime.feature.community.UserProfileScreen
import site.jokersh.anime.feature.diagnostics.DiagnosticsScreen
import site.jokersh.anime.feature.discover.CalendarRoute
import site.jokersh.anime.feature.discover.DiscoverRoute
import site.jokersh.anime.feature.discover.DiscoverSectionRoute
import site.jokersh.anime.feature.profile.AnimeAccountCenter
import site.jokersh.anime.feature.profile.ProfileScreen
import site.jokersh.anime.feature.search.SearchEffect
import site.jokersh.anime.feature.search.SearchRoute
import site.jokersh.anime.feature.subject.CharactersRoute
import site.jokersh.anime.feature.subject.EpisodesRoute
import site.jokersh.anime.feature.subject.RelationsRoute
import site.jokersh.anime.feature.subject.SubjectRoute
import kotlin.time.Clock

private enum class RootDestination(
    val root: AppRoot,
    val label: StringResource,
    val icon: DrawableResource,
) {
    Discover(AppRoot.Discover, Res.string.root_discover, Res.drawable.ic_explore),
    Library(AppRoot.Library, Res.string.root_library, Res.drawable.ic_search),
    Activity(AppRoot.Activity, Res.string.root_activity, Res.drawable.ic_activity),
    Profile(AppRoot.Profile, Res.string.root_profile, Res.drawable.ic_person),
}

public class AnimeAppShortcutDispatcher {
    private var handler: (KeyEvent) -> Boolean = { false }

    public fun dispatch(event: KeyEvent): Boolean = handler(event)

    internal fun connect(handler: (KeyEvent) -> Boolean) {
        this.handler = handler
    }

    internal fun disconnect() {
        handler = { false }
    }
}

@Composable
fun AnimeApp(
    appContainer: AppContainer,
    shortcutDispatcher: AnimeAppShortcutDispatcher? = null,
    initialRoot: AppRoot = AppRoot.Discover,
    initialSearchQuery: String? = null,
    deepLinkRoute: AppRoute? = null,
    openExternalUrl: (String) -> Unit = {},
    nativeRootNavigation: Boolean = false,
    bindRootSelectionHandler: (((Int) -> Unit) -> Unit)? = null,
    onRootSelectionChanged: (Int) -> Unit = {},
    onNativeGlassStateChanged: (Boolean) -> Unit = {},
    onNativeRootNavigationVisibilityChanged: (Boolean) -> Unit = {},
    lifecycleOwner: Boolean = true,
) {
    val sessionState by appContainer.sessionRepository.observeSession().collectAsState(SessionState.Guest)
    val settings by appContainer.settingsRepository.observeSettings().collectAsState(
        site.jokersh.anime.core.model.AppSettings(
            theme = ThemePreference.System,
            dynamicColor = true,
            glass = site.jokersh.anime.core.model.GlassPreference.Auto,
            reduceMotion = site.jokersh.anime.core.model.ReduceMotionPreference.FollowSystem,
            diagnosticsConsent = false,
        ),
    )
    val sessionScope = rememberCoroutineScope()
    if (lifecycleOwner) {
        LaunchedEffect(appContainer.sessionRepository) {
            appContainer.sessionRepository.refresh()
            appContainer.collectionRepository.requestSync()
            appContainer.communityRepository.retryPendingRatings()
        }
    }
    var selectedIndex by
        rememberSaveable {
            mutableStateOf(
                RootDestination.entries
                    .indexOfFirst { it.root == initialRoot }
                    .coerceAtLeast(0),
            )
        }
    val selectedDestination = RootDestination.entries[selectedIndex]
    val discoverBackStack =
        rememberNavBackStack(AppNavigationSavedStateConfiguration, AppRoute.Discover)
    val libraryBackStack =
        rememberNavBackStack(
            AppNavigationSavedStateConfiguration,
            AppRoute.Library(query = initialSearchQuery),
        )
    val activityBackStack =
        rememberNavBackStack(AppNavigationSavedStateConfiguration, AppRoute.Activity())
    val profileBackStack =
        rememberNavBackStack(AppNavigationSavedStateConfiguration, AppRoute.Profile)
    val backStacks =
        remember(discoverBackStack, libraryBackStack, activityBackStack, profileBackStack) {
            mapOf(
                AppRoot.Discover to discoverBackStack,
                AppRoot.Library to libraryBackStack,
                AppRoot.Activity to activityBackStack,
                AppRoot.Profile to profileBackStack,
            )
        }
    // Keep the navigator identity stable while session/settings flows emit their initial
    // snapshots. Recreating it on every emission also recreates the shortcut effect and
    // changes callback identities passed into the liquid tab bar during the warm-up window.
    val navigator =
        remember(backStacks) {
            AppNavigator(
                currentRoot = { RootDestination.entries[selectedIndex].root },
                updateRoot = { root -> selectedIndex = RootDestination.entries.indexOfFirst { it.root == root } },
                stackFor = backStacks::getValue,
            )
        }
    var suppressNextNavigationTransition by remember { mutableStateOf(false) }

    fun selectRoot(root: AppRoot) {
        when (navigator.selectRoot(root)) {
            RootSelectionResult.Switched,
            RootSelectionResult.ReturnedToRoot,
            -> {
                // Root-tab changes and returning to the active tab's root must not be
                // interpreted as child-page navigation by NavDisplay.
                suppressNextNavigationTransition = true
            }

            RootSelectionResult.Unchanged -> {
                // Keep the current child-page transition state for a true no-op.
            }
        }
    }

    DisposableEffect(navigator) {
        bindRootSelectionHandler?.invoke { index ->
            RootDestination.entries.getOrNull(index)?.let { destination ->
                selectRoot(destination.root)
            }
        }
        onDispose {
            bindRootSelectionHandler?.invoke {}
        }
    }
    LaunchedEffect(suppressNextNavigationTransition) {
        if (suppressNextNavigationTransition) {
            // Keep the flag through the recomposition that applies the new back stack,
            // then restore child-page animations on the next frame.
            withFrameNanos { }
            suppressNextNavigationTransition = false
        }
    }
    LaunchedEffect(selectedIndex) {
        onRootSelectionChanged(selectedIndex)
    }
    val activeBackStack = backStacks.getValue(selectedDestination.root)
    val showRootNavigation = (activeBackStack.lastOrNull() as? AppRoute)?.root != null
    LaunchedEffect(showRootNavigation) {
        onNativeRootNavigationVisibilityChanged(showRootNavigation)
    }
    var pendingAuthAction by remember { mutableStateOf<PendingAuthAction?>(null) }
    var showAccountCenter by rememberSaveable { mutableStateOf(false) }
    var transientMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val onRootSelected =
        { index: Int -> selectRoot(RootDestination.entries[index].root) }

    val executeProtectedAction: (PendingAuthAction) -> Unit = { action ->
        when (action) {
            is PendingAuthAction.SetCollection -> {
                sessionScope.launch {
                    appContainer.collectionRepository.setStatus(
                        site.jokersh.anime.core.model
                            .SubjectId(action.subjectId),
                        action.status.toCollectionStatus(),
                    )
                    appContainer.sessionRepository.refresh()
                }
            }

            is PendingAuthAction.OpenRating -> {
                navigator.push(AppRoute.RatingEditor(action.subjectId))
            }

            is PendingAuthAction.OpenComments -> {
                navigator.push(AppRoute.Comments(action.subjectId))
            }
        }
    }
    val requestProtectedAction: (PendingAuthAction) -> Unit = { action ->
        if (sessionState is SessionState.Authenticated) {
            pendingAuthAction = null
            executeProtectedAction(action)
        } else {
            pendingAuthAction = action
            showAccountCenter = true
        }
    }

    LaunchedEffect(deepLinkRoute) {
        val route = deepLinkRoute ?: return@LaunchedEffect
        if (route is AppRoute.Subject) {
            selectRoot(AppRoot.Discover)
            if (discoverBackStack.lastOrNull() != route) navigator.push(route)
        }
    }
    LaunchedEffect(sessionState, pendingAuthAction) {
        if (sessionState is SessionState.Authenticated) {
            if (lifecycleOwner) {
                appContainer.collectionRepository.requestSync()
                appContainer.communityRepository.retryPendingRatings()
            }
            val action = pendingAuthAction ?: return@LaunchedEffect
            pendingAuthAction = null
            showAccountCenter = false
            executeProtectedAction(action)
        }
    }
    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(3_500)
            transientMessage = null
        }
    }

    DisposableEffect(shortcutDispatcher, navigator) {
        shortcutDispatcher?.connect { event -> handleAppShortcut(event, navigator, ::selectRoot) }
        onDispose {
            shortcutDispatcher?.disconnect()
        }
    }

    val systemDark = isSystemInDarkTheme()
    val reduceMotionEnabled = settings.reduceMotion == ReduceMotionPreference.On
    LaunchedEffect(settings.glass, reduceMotionEnabled) {
        onNativeGlassStateChanged(settings.glass != GlassPreference.Off && !reduceMotionEnabled)
    }
    AnimeTheme(
        darkTheme =
            when (settings.theme) {
                ThemePreference.System -> systemDark
                ThemePreference.Light -> false
                ThemePreference.Dark -> true
            },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val discoverLabel = stringResource(Res.string.root_discover)
            val libraryLabel = stringResource(Res.string.root_library)
            val activityLabel = stringResource(Res.string.root_activity)
            val profileLabel = stringResource(Res.string.root_profile)
            val labels =
                remember(discoverLabel, libraryLabel, activityLabel, profileLabel) {
                    listOf(discoverLabel, libraryLabel, activityLabel, profileLabel)
                }
            val useDesktopSidebar = maxWidth >= 700.dp

            AnimeBackdropHost(
                modifier = Modifier.fillMaxSize(),
                glassEnabled = settings.glass != GlassPreference.Off,
                reduceMotion = reduceMotionEnabled,
                background = {
                    Row(Modifier.fillMaxSize()) {
                        if (useDesktopSidebar) {
                            DesktopSidebar(
                                labels = labels,
                                selectedIndex = selectedIndex,
                                onSelected = onRootSelected,
                            )
                        }
                        AppNavigationLayer(
                            backStack = activeBackStack,
                            suppressTransition = suppressNextNavigationTransition,
                            navigator = navigator,
                            onAppRootSelected = { root -> selectRoot(root) },
                            appContainer = appContainer,
                            sessionState = sessionState,
                            onAnimeLogin = { username, password ->
                                sessionScope.launch {
                                    appContainer.sessionRepository.loginWithAnime(
                                        AnimeLoginCredentials(username, password),
                                    )
                                }
                            },
                            onAnimeRegister = { username, password, displayName ->
                                sessionScope.launch {
                                    appContainer.sessionRepository.registerAnime(
                                        AnimeRegistration(username, password, displayName),
                                    )
                                }
                            },
                            onBangumiLogin = {
                                sessionScope.launch {
                                    appContainer.sessionRepository
                                        .beginLogin(
                                            LoginRequest(
                                                requestId = "desktop-${Clock.System.now().toEpochMilliseconds()}",
                                                pendingActionId = pendingAuthAction?.actionId,
                                            ),
                                        ).onSuccess { openExternalUrl(it.authorizeUrl) }
                                }
                            },
                            onLogout = {
                                sessionScope.launch { appContainer.sessionRepository.logout() }
                            },
                            onProtectedAction = requestProtectedAction,
                            onMessage = { transientMessage = it },
                            reduceMotion = reduceMotionEnabled,
                            settings = settings,
                            onThemeChange = { sessionScope.launch { appContainer.settingsRepository.setTheme(it) } },
                            onGlassChange = { sessionScope.launch { appContainer.settingsRepository.setGlass(it) } },
                            onReduceMotionChange = {
                                sessionScope.launch {
                                    appContainer.settingsRepository
                                        .setReduceMotion(
                                            it,
                                        )
                                }
                            },
                            nativeRootNavigation = nativeRootNavigation,
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
                content = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (showRootNavigation && !useDesktopSidebar && !nativeRootNavigation) {
                            AnimeLiquidTabBar(
                                labels = labels,
                                selectedIndex = selectedIndex,
                                onSelected = onRootSelected,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        .fillMaxWidth(),
                            ) { index, _, tint ->
                                Icon(
                                    painter = painterResource(RootDestination.entries[index].icon),
                                    contentDescription = labels[index],
                                    modifier = Modifier.size(22.dp),
                                    tint = tint,
                                )
                            }
                        }
                        if (showAccountCenter) {
                            AnimeAccountCenter(
                                sessionState = sessionState,
                                onDismiss = {
                                    showAccountCenter = false
                                    pendingAuthAction = null
                                },
                                onLogin = { username, password ->
                                    sessionScope.launch {
                                        appContainer.sessionRepository.loginWithAnime(
                                            AnimeLoginCredentials(username, password),
                                        )
                                    }
                                },
                                onRegister = { username, password, displayName ->
                                    sessionScope.launch {
                                        appContainer.sessionRepository.registerAnime(
                                            AnimeRegistration(username, password, displayName),
                                        )
                                    }
                                },
                                onBangumiLogin = {
                                    sessionScope.launch {
                                        appContainer.sessionRepository
                                            .beginLogin(
                                                LoginRequest(
                                                    requestId = "auth-gate-${Clock.System.now().toEpochMilliseconds()}",
                                                    pendingActionId = pendingAuthAction?.actionId,
                                                ),
                                            ).onSuccess { openExternalUrl(it.authorizeUrl) }
                                    }
                                },
                                onLogout = { sessionScope.launch { appContainer.sessionRepository.logout() } },
                                onBrowseCollection = {
                                    showAccountCenter = false
                                    navigator.push(AppRoute.Collection())
                                },
                            )
                        }
                        transientMessage?.let { message ->
                            AnimeGlassPanel(
                                role = site.jokersh.anime.core.designsystem.GlassRole.FloatingPanel,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(bottom = if (useDesktopSidebar) 24.dp else 88.dp)
                                        .clickable { transientMessage = null },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(message, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
            )
        }
    }
}

private fun String.toCollectionStatus(): CollectionStatus =
    when (this) {
        "watching" -> CollectionStatus.Watching
        "completed" -> CollectionStatus.Completed
        "on_hold" -> CollectionStatus.OnHold
        "dropped" -> CollectionStatus.Dropped
        else -> CollectionStatus.Wish
    }

@Composable
private fun DesktopSidebar(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Surface(
        modifier = Modifier.width(220.dp).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(AnimeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AnimeSpacing.sm, vertical = AnimeSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "媒体资料库",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "资料库",
                modifier = Modifier.padding(start = AnimeSpacing.md, top = AnimeSpacing.sm, bottom = AnimeSpacing.xs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            RootDestination.entries.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                val shape = RoundedCornerShape(AnimeRadius.control)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            ).clickable { onSelected(index) }
                            .semantics { this.selected = selected }
                            .padding(horizontal = AnimeSpacing.md, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                ) {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = null,
                        modifier = Modifier.size(AnimeSize.iconSm),
                        tint =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Text(
                        text = labels[index],
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    Text(
                        text = "Ctrl ${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Text(
                text = "Bangumi · 实时资料",
                modifier = Modifier.padding(AnimeSpacing.md),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Box(
        modifier =
            Modifier
                .width(0.5.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
    )
}

@Composable
private fun AppNavigationLayer(
    backStack: NavBackStack<NavKey>,
    suppressTransition: Boolean,
    navigator: AppNavigator,
    onAppRootSelected: (AppRoot) -> Unit,
    appContainer: AppContainer,
    sessionState: SessionState,
    onAnimeLogin: (String, String) -> Unit,
    onAnimeRegister: (String, String, String) -> Unit,
    onBangumiLogin: () -> Unit,
    onLogout: () -> Unit,
    onProtectedAction: (PendingAuthAction) -> Unit,
    onMessage: (String) -> Unit,
    reduceMotion: Boolean,
    settings: site.jokersh.anime.core.model.AppSettings,
    onThemeChange: (site.jokersh.anime.core.model.ThemePreference) -> Unit,
    onGlassChange: (site.jokersh.anime.core.model.GlassPreference) -> Unit,
    onReduceMotionChange: (site.jokersh.anime.core.model.ReduceMotionPreference) -> Unit,
    nativeRootNavigation: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigationScope = rememberCoroutineScope()
    // Keep page motion directional and bounded. Fade transitions are intentionally avoided
    // because a Compose/iOS frame can expose the opaque shell between two scene snapshots.
    val navigationOffsetPx = with(LocalDensity.current) { AnimeMotion.pageOffset.roundToPx() }
    val pageTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
        remember(reduceMotion, navigationOffsetPx, suppressTransition) {
            if (reduceMotion || suppressTransition) {
                { EnterTransition.None togetherWith ExitTransition.None }
            } else {
                {
                    (
                        slideInHorizontally(
                            initialOffsetX = { navigationOffsetPx },
                            animationSpec = tween(durationMillis = AnimeMotion.standard),
                        )
                    ) togetherWith
                        (
                            slideOutHorizontally(
                                targetOffsetX = { -navigationOffsetPx / 2 },
                                animationSpec = tween(durationMillis = AnimeMotion.standard),
                            )
                        )
                }
            }
        }
    val pagePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
        remember(reduceMotion, navigationOffsetPx, suppressTransition) {
            if (reduceMotion || suppressTransition) {
                { EnterTransition.None togetherWith ExitTransition.None }
            } else {
                {
                    (
                        slideInHorizontally(
                            initialOffsetX = { -navigationOffsetPx },
                            animationSpec = tween(durationMillis = AnimeMotion.standard),
                        )
                    ) togetherWith
                        (
                            slideOutHorizontally(
                                targetOffsetX = { navigationOffsetPx / 2 },
                                animationSpec = tween(durationMillis = AnimeMotion.standard),
                            )
                        )
                }
            }
        }
    val predictivePopTransitionSpec: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform =
        remember(reduceMotion, navigationOffsetPx, suppressTransition) {
            if (reduceMotion || suppressTransition) {
                { EnterTransition.None togetherWith ExitTransition.None }
            } else {
                {
                    (
                        slideInHorizontally(
                            initialOffsetX = { -navigationOffsetPx },
                            animationSpec = tween(durationMillis = AnimeMotion.standard),
                        )
                    ) togetherWith
                        (
                            slideOutHorizontally(
                                targetOffsetX = { navigationOffsetPx },
                                animationSpec = tween(durationMillis = AnimeMotion.standard),
                            )
                        )
                }
            }
        }
    // Root tabs stay mounted in independent back stacks. A full-screen fade while the Backdrop
    // graph is also updating exposes a blank frame on iOS, so root selection is instantaneous.
    val rootTabMetadata = remember { rootTabTransitionMetadata() }
    // Native iOS chrome is full-bleed. Root screens move only their top controls below the
    // status bar so the scrollable page can continue behind the native material layer.
    val rootScreenModifier = if (nativeRootNavigation) Modifier else Modifier.statusBarsPadding()
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                transitionSpec = pageTransitionSpec,
                popTransitionSpec = pagePopTransitionSpec,
                predictivePopTransitionSpec = predictivePopTransitionSpec,
                onBack = { navigator.pop() },
                entryProvider =
                    entryProvider(
                        fallback = { key ->
                            NavEntry(key) {
                                Text(
                                    text = stringResource(Res.string.navigation_not_implemented),
                                    modifier = Modifier.statusBarsPadding().padding(AnimeSpacing.xl),
                                )
                            }
                        },
                    ) {
                        entry<AppRoute.Discover>(metadata = rootTabMetadata) {
                            DiscoverRoute(
                                repository = appContainer.catalogRepository,
                                onSubjectClick = { subjectId ->
                                    navigator.push(
                                        AppRoute.Subject(
                                            subjectId = subjectId.value,
                                            origin = RouteOrigin.Discover,
                                        ),
                                    )
                                },
                                onSeeAll = { sectionId -> navigator.push(AppRoute.DiscoverSection(sectionId)) },
                                onCalendarClick = { navigator.push(AppRoute.Calendar) },
                                onMessage = { message ->
                                    onMessage(
                                        when (message) {
                                            site.jokersh.anime.feature.discover.DiscoverMessageUi.RefreshFailed -> {
                                                "刷新失败，已保留上次内容"
                                            }
                                        },
                                    )
                                },
                                contentUnderSystemBars = nativeRootNavigation,
                                modifier = rootScreenModifier,
                            )
                        }
                        entry<AppRoute.DiscoverSection> { route ->
                            DiscoverSectionRoute(
                                sectionId = route.sectionId,
                                repository = appContainer.catalogRepository,
                                onBack = { navigator.pop() },
                                onSubjectClick = { navigator.push(AppRoute.Subject(it.value, RouteOrigin.Discover)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Calendar> {
                            CalendarRoute(
                                repository = appContainer.catalogRepository,
                                onBack = { navigator.pop() },
                                onSubjectClick = { navigator.push(AppRoute.Subject(it, RouteOrigin.Discover)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Library>(metadata = rootTabMetadata) {
                            SearchRoute(
                                repository = appContainer.searchRepository,
                                initialQuery = it.query,
                                pageSize = appContainer.profile.searchPageSize,
                                onResultsRequested = { effect ->
                                    navigator.push(AppRoute.SearchResults(effect.toSearchRouteRequest()))
                                },
                                onSubjectClick = { subjectId ->
                                    navigator.push(
                                        AppRoute.Subject(
                                            subjectId = subjectId.value,
                                            origin = RouteOrigin.Library,
                                        ),
                                    )
                                },
                                contentUnderSystemBars = nativeRootNavigation,
                                modifier = rootScreenModifier,
                            )
                        }
                        entry<AppRoute.SearchResults> { route ->
                            SearchRoute(
                                repository = appContainer.searchRepository,
                                initialQuery = route.request.query,
                                initialTypes = route.request.types.mapTo(mutableSetOf()) { it.toSubjectType() },
                                initialYears =
                                    route.request.yearStart?.let { start ->
                                        start..(route.request.yearEnd ?: start)
                                    },
                                initialAiring = route.request.airing.mapTo(mutableSetOf()) { it.toAiringStatus() },
                                initialSort = route.request.sort.toSearchSort(),
                                pageSize = appContainer.profile.searchPageSize,
                                onResultsRequested = { effect ->
                                    navigator.replaceTop(AppRoute.SearchResults(effect.toSearchRouteRequest()))
                                },
                                onSubjectClick = { subjectId ->
                                    navigator.push(
                                        AppRoute.Subject(
                                            subjectId = subjectId.value,
                                            origin = RouteOrigin.Library,
                                        ),
                                    )
                                },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Activity>(metadata = rootTabMetadata) {
                            ActivityScreen(
                                repository = appContainer.communityRepository,
                                initialFeed =
                                    when (it.feed) {
                                        site.jokersh.anime.core.navigation.ActivityFeedRoute.Popular -> "popular"
                                        site.jokersh.anime.core.navigation.ActivityFeedRoute.Following -> "following"
                                    },
                                onSubjectClick = { subjectId ->
                                    navigator.push(AppRoute.Subject(subjectId, RouteOrigin.Activity))
                                },
                                onReviewClick = { navigator.push(AppRoute.Review(it)) },
                                onListClick = { navigator.push(AppRoute.CuratedList(it)) },
                                onCommentsClick = { subjectId -> navigator.push(AppRoute.Comments(subjectId)) },
                                onUserClick = { navigator.push(AppRoute.User(it)) },
                                onCreateList = { navigator.push(AppRoute.CuratedList("new")) },
                                contentUnderSystemBars = nativeRootNavigation,
                                modifier = rootScreenModifier,
                            )
                        }
                        entry<AppRoute.Collection> {
                            CollectionScreen(
                                sessionState = sessionState,
                                collectionRepository = appContainer.collectionRepository,
                                onSubjectClick = { subjectId ->
                                    navigator.push(
                                        AppRoute.Subject(
                                            subjectId = subjectId.value,
                                            origin = RouteOrigin.Profile,
                                        ),
                                    )
                                },
                                onDiscoverClick = { onAppRootSelected(AppRoot.Discover) },
                                onBack = { navigator.pop() },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Profile>(metadata = rootTabMetadata) {
                            ProfileScreen(
                                environmentLabel = appContainer.profile.environment.name,
                                sessionState = sessionState,
                                sessionRepository = appContainer.sessionRepository,
                                onAnimeLogin = onAnimeLogin,
                                onAnimeRegister = onAnimeRegister,
                                onBangumiLogin = onBangumiLogin,
                                onLogout = onLogout,
                                onBrowseCollection = { navigator.push(AppRoute.Collection()) },
                                settings = settings,
                                onThemeChange = onThemeChange,
                                onGlassChange = onGlassChange,
                                onReduceMotionChange = onReduceMotionChange,
                                onDiagnostics = { navigator.push(AppRoute.Diagnostics) },
                                contentUnderSystemBars = nativeRootNavigation,
                                modifier = rootScreenModifier,
                            )
                        }
                        entry<AppRoute.Subject> { route ->
                            SubjectRoute(
                                subjectId = route.subjectId,
                                repository = appContainer.catalogRepository,
                                communityRepository = appContainer.communityRepository,
                                onBack = { navigator.pop() },
                                onCollect = { onProtectedAction(PendingAuthAction.SetCollection(it)) },
                                onEpisodesClick = { navigator.push(AppRoute.Episodes(it)) },
                                onCharactersClick = { navigator.push(AppRoute.Characters(it)) },
                                onRelationsClick = { navigator.push(AppRoute.Relations(it)) },
                                onCommentsClick = { onProtectedAction(PendingAuthAction.OpenComments(it)) },
                                onReviewsClick = { navigator.push(AppRoute.SubjectReviews(it)) },
                                onReviewClick = { navigator.push(AppRoute.Review(it)) },
                                onListClick = { navigator.push(AppRoute.CuratedList(it)) },
                            )
                        }
                        entry<AppRoute.Episodes> { route ->
                            EpisodesRoute(
                                subjectId = route.subjectId,
                                repository = appContainer.catalogRepository,
                                onBack = { navigator.pop() },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Characters> { route ->
                            CharactersRoute(
                                subjectId = route.subjectId,
                                repository = appContainer.catalogRepository,
                                onBack = { navigator.pop() },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Relations> { route ->
                            RelationsRoute(
                                subjectId = route.subjectId,
                                repository = appContainer.catalogRepository,
                                onBack = { navigator.pop() },
                                onSubjectClick = { navigator.push(AppRoute.Subject(it, RouteOrigin.Related)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Comments> { route ->
                            CommentsScreen(
                                subjectId = route.subjectId,
                                initialSort =
                                    if (route.sort ==
                                        CommentRouteSort.Oldest
                                    ) {
                                        site.jokersh.anime.core.model.CommentSort.Oldest
                                    } else {
                                        site.jokersh.anime.core.model.CommentSort.Newest
                                    },
                                repository = appContainer.commentRepository,
                                onBack = { navigator.pop() },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.SubjectReviews> { route ->
                            site.jokersh.anime.feature.community.SubjectReviewsScreen(
                                repository = appContainer.communityRepository,
                                subjectId = route.subjectId,
                                currentUserId =
                                    (sessionState as? SessionState.Authenticated)
                                        ?.user
                                        ?.summary
                                        ?.id
                                        ?.value,
                                onBack = { navigator.pop() },
                                onReviewClick = { navigator.push(AppRoute.Review(it)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.RatingEditor> { route ->
                            RatingEditorScreen(
                                repository = appContainer.communityRepository,
                                subjectId = route.subjectId,
                                onBack = { navigator.pop() },
                                onSaved = {
                                    navigationScope.launch { appContainer.sessionRepository.refresh() }
                                    navigator.pop()
                                },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Review> { route ->
                            ReviewDetailScreen(
                                repository = appContainer.communityRepository,
                                reviewId = route.reviewId,
                                onBack = { navigator.pop() },
                                onSubjectClick = { navigator.push(AppRoute.Subject(it, RouteOrigin.Activity)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.CuratedList> { route ->
                            CuratedListScreen(
                                repository = appContainer.communityRepository,
                                listId = route.listId,
                                onBack = { navigator.pop() },
                                onSubjectClick = { navigator.push(AppRoute.Subject(it, RouteOrigin.Activity)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.User> { route ->
                            UserProfileScreen(
                                repository = appContainer.communityRepository,
                                userId = route.userId,
                                onBack = { navigator.pop() },
                                onReviewClick = { navigator.push(AppRoute.Review(it)) },
                                onListClick = { navigator.push(AppRoute.CuratedList(it)) },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                        entry<AppRoute.Diagnostics> {
                            DiagnosticsScreen(
                                repository = appContainer.sessionRepository,
                                onBack = { navigator.pop() },
                                modifier = Modifier.statusBarsPadding(),
                            )
                        }
                    },
            )
        }
    }
}

private fun rootTabTransitionMetadata(): Map<String, Any> =
    NavDisplay.transitionSpec {
        EnterTransition.None togetherWith ExitTransition.None
    }

private fun handleAppShortcut(
    event: KeyEvent,
    navigator: AppNavigator,
    onRootSelected: (AppRoot) -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false

    val targetRoot = shortcutRoot(event.key, event.isCtrlPressed)
    if (targetRoot != null) {
        onRootSelected(targetRoot)
        return true
    }

    if (event.key == Key.Escape || (event.isAltPressed && event.key == Key.DirectionLeft)) {
        return navigator.pop()
    }
    return false
}

internal fun shortcutRoot(
    key: Key,
    ctrlPressed: Boolean,
): AppRoot? {
    if (!ctrlPressed) return null
    return when (key) {
        Key.One -> AppRoot.Discover
        Key.Two, Key.K -> AppRoot.Library
        Key.Three -> AppRoot.Activity
        Key.Four -> AppRoot.Profile
        else -> null
    }
}

private fun SearchEffect.NavigateToResults.toSearchRouteRequest():
    site.jokersh.anime.core.navigation.SearchRouteRequest =
    site.jokersh.anime.core.navigation.SearchRouteRequest(
        query = query.trim(),
        types = types.mapTo(mutableSetOf()) { it.toSearchRouteType() },
        yearStart = years?.first,
        yearEnd = years?.last,
        airing = airing.mapTo(mutableSetOf()) { it.toSearchRouteAiring() },
        sort = sort.toSearchRouteSort(),
    )

private fun SubjectType.toSearchRouteType(): SearchRouteSubjectType =
    when (this) {
        SubjectType.Tv -> SearchRouteSubjectType.Tv
        SubjectType.Web -> SearchRouteSubjectType.Web
        SubjectType.Ova -> SearchRouteSubjectType.Ova
        SubjectType.Movie -> SearchRouteSubjectType.Movie
        SubjectType.Other -> SearchRouteSubjectType.Other
    }

private fun AiringStatus.toSearchRouteAiring(): SearchRouteAiringStatus =
    when (this) {
        AiringStatus.Announced -> SearchRouteAiringStatus.Announced
        AiringStatus.Airing -> SearchRouteAiringStatus.Airing
        AiringStatus.Finished -> SearchRouteAiringStatus.Finished
        AiringStatus.Unknown -> SearchRouteAiringStatus.Unknown
    }

private fun SearchSort.toSearchRouteSort(): SearchRouteSort =
    when (this) {
        SearchSort.Relevance -> SearchRouteSort.Relevance
        SearchSort.Rating -> SearchRouteSort.Rating
        SearchSort.Updated -> SearchRouteSort.Updated
    }

private fun SearchRouteSort.toSearchSort(): SearchSort =
    when (this) {
        SearchRouteSort.Relevance -> SearchSort.Relevance
        SearchRouteSort.Rating -> SearchSort.Rating
        SearchRouteSort.Updated -> SearchSort.Updated
    }

private fun SearchRouteSubjectType.toSubjectType(): SubjectType =
    when (this) {
        SearchRouteSubjectType.Tv -> SubjectType.Tv
        SearchRouteSubjectType.Web -> SubjectType.Web
        SearchRouteSubjectType.Ova -> SubjectType.Ova
        SearchRouteSubjectType.Movie -> SubjectType.Movie
        SearchRouteSubjectType.Other -> SubjectType.Other
    }

private fun SearchRouteAiringStatus.toAiringStatus(): AiringStatus =
    when (this) {
        SearchRouteAiringStatus.Announced -> AiringStatus.Announced
        SearchRouteAiringStatus.Airing -> AiringStatus.Airing
        SearchRouteAiringStatus.Finished -> AiringStatus.Finished
        SearchRouteAiringStatus.Unknown -> AiringStatus.Unknown
    }
