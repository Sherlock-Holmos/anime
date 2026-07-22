package site.jokersh.anime.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import site.jokersh.anime.app.generated.resources.ic_bookmarks
import site.jokersh.anime.app.generated.resources.ic_explore
import site.jokersh.anime.app.generated.resources.ic_person
import site.jokersh.anime.app.generated.resources.ic_search
import site.jokersh.anime.app.generated.resources.profile_data_mode
import site.jokersh.anime.app.generated.resources.profile_diagnostics
import site.jokersh.anime.app.generated.resources.profile_diagnostics_disabled
import site.jokersh.anime.app.generated.resources.profile_diagnostics_enabled
import site.jokersh.anime.app.generated.resources.profile_page_size
import site.jokersh.anime.app.generated.resources.profile_page_size_value
import site.jokersh.anime.app.generated.resources.root_collection
import site.jokersh.anime.app.generated.resources.root_discover
import site.jokersh.anime.app.generated.resources.root_profile
import site.jokersh.anime.app.generated.resources.root_search
import site.jokersh.anime.app.generated.resources.shell_environment
import site.jokersh.anime.core.designsystem.AnimeBackdropHost
import site.jokersh.anime.core.designsystem.AnimeLiquidTabBar
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.AnimeTheme
import site.jokersh.anime.feature.discover.DiscoverRoute

private enum class RootDestination(
    val label: StringResource,
    val icon: DrawableResource,
) {
    Discover(Res.string.root_discover, Res.drawable.ic_explore),
    Search(Res.string.root_search, Res.drawable.ic_search),
    Collection(Res.string.root_collection, Res.drawable.ic_bookmarks),
    Profile(Res.string.root_profile, Res.drawable.ic_person),
}

@Composable
fun AnimeApp(appContainer: AppContainer) {
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val selectedRoot = RootDestination.entries[selectedIndex]

    AnimeTheme {
        AnimeBackdropHost(
            modifier = Modifier.fillMaxSize(),
            background = {
                AppContentLayer(
                    destination = selectedRoot,
                    appContainer = appContainer,
                )
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val labels = RootDestination.entries.map { stringResource(it.label) }
                AnimeLiquidTabBar(
                    labels = labels,
                    selectedIndex = selectedIndex,
                    onSelected = { selectedIndex = it },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = AnimeSpacing.md)
                            .padding(bottom = AnimeSpacing.sm)
                            .fillMaxWidth(),
                    icon = { index, _, tint ->
                        Icon(
                            painter = painterResource(RootDestination.entries[index].icon),
                            contentDescription = null,
                            tint = tint,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AppContentLayer(
    destination: RootDestination,
    appContainer: AppContainer,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f),
                        ),
                    ),
                ),
    ) {
        if (destination == RootDestination.Discover) {
            DiscoverRoute(
                repository = appContainer.catalogRepository,
                onSubjectClick = {},
                onSeeAll = {},
                onMessage = {},
                modifier = Modifier.statusBarsPadding(),
            )
        } else {
            F0Content(
                destination = destination,
                profile = appContainer.profile,
            )
        }
    }
}

@Composable
private fun F0Content(
    destination: RootDestination,
    profile: BuildProfile,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = AnimeSpacing.lg)
                .padding(top = AnimeSpacing.lg, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(destination.label),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Surface(
                shape = RoundedCornerShape(AnimeRadius.round),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                border =
                    BorderStroke(
                        AnimeSize.border,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                    ),
            ) {
                Text(
                    text =
                        stringResource(
                            Res.string.shell_environment,
                            profile.environment.name,
                        ),
                    modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.sm),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = stringResource(Res.string.f0_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(AnimeRadius.card),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            border =
                BorderStroke(
                    width = AnimeSize.border,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                ),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(AnimeSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
            ) {
                Text(
                    text = stringResource(Res.string.f0_status_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                ProfileRow(
                    label = stringResource(Res.string.profile_data_mode),
                    value = stringResource(profile.dataModePolicy.resource),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                ProfileRow(
                    label = stringResource(Res.string.profile_page_size),
                    value =
                        stringResource(
                            Res.string.profile_page_size_value,
                            profile.searchPageSize,
                        ),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                ProfileRow(
                    label = stringResource(Res.string.profile_diagnostics),
                    value =
                        stringResource(
                            if (profile.diagnosticsEnabled) {
                                Res.string.profile_diagnostics_enabled
                            } else {
                                Res.string.profile_diagnostics_disabled
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(AnimeSpacing.sm))
        Text(
            text = stringResource(Res.string.f0_footer),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        DesignSystemCatalog()
    }
}

private val DataModePolicy.resource: StringResource
    get() =
        when (this) {
            DataModePolicy.FixtureOnly -> Res.string.data_mode_fixture
            DataModePolicy.RemoteWithFixtureSwitch -> Res.string.data_mode_remote_fixture
            DataModePolicy.RemoteOnly -> Res.string.data_mode_remote
        }

@Composable
private fun ProfileRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
