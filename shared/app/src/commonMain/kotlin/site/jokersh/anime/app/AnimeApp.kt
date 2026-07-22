package site.jokersh.anime.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.AnimeTheme

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
    AnimeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppShell(appContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(appContainer: AppContainer) {
    var selectedRoot by rememberSaveable { mutableStateOf(RootDestination.Discover) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.app_name),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text =
                                stringResource(
                                    Res.string.shell_environment,
                                    appContainer.profile.environment.name,
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    ),
            )
        },
        bottomBar = {
            NavigationBar {
                RootDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedRoot == destination,
                        onClick = { selectedRoot = destination },
                        icon = {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { contentPadding ->
        F0Content(
            destination = selectedRoot,
            profile = appContainer.profile,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun F0Content(
    destination: RootDestination,
    profile: BuildProfile,
    contentPadding: PaddingValues,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = AnimeSpacing.lg, vertical = AnimeSpacing.xxl),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            Text(
                text = stringResource(destination.label),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.f0_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                shape = RoundedCornerShape(AnimeRadius.card),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                border =
                    BorderStroke(
                        width = AnimeSize.border,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileRow(
                        label = stringResource(Res.string.profile_page_size),
                        value =
                            stringResource(
                                Res.string.profile_page_size_value,
                                profile.searchPageSize,
                            ),
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
        }
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
