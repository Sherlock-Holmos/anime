package site.jokersh.anime.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimeLiquidToggle
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.AnimeCopy
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.animeString
import site.jokersh.anime.core.designsystem.animeColors
import site.jokersh.anime.core.model.AppSettings
import site.jokersh.anime.core.model.GlassPreference
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.LanguagePreference
import site.jokersh.anime.core.model.Provider
import site.jokersh.anime.core.model.ReduceMotionPreference
import site.jokersh.anime.core.model.SessionState
import site.jokersh.anime.core.model.ThemePreference
import site.jokersh.anime.data.session.BangumiSyncConflict
import site.jokersh.anime.data.session.BangumiSyncStatus
import site.jokersh.anime.data.session.SessionRepository
import site.jokersh.anime.data.session.SyncConflictChoice

@Composable
public fun ProfileScreen(
    environmentLabel: String,
    sessionState: SessionState,
    sessionRepository: SessionRepository,
    onAnimeLogin: (String, String) -> Unit,
    onAnimeRegister: (String, String, String) -> Unit,
    onBangumiLogin: () -> Unit,
    onLogout: () -> Unit,
    onBrowseCollection: () -> Unit,
    settings: AppSettings,
    onThemeChange: (ThemePreference) -> Unit,
    onGlassChange: (GlassPreference) -> Unit,
    onLanguageChange: (LanguagePreference) -> Unit,
    onReduceMotionChange: (ReduceMotionPreference) -> Unit,
    onDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentUnderSystemBars: Boolean = false,
) {
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    val theme = settings.theme.key()
    val glass = settings.glass.key()
    val language = settings.language.key()
    val reduceMotion = settings.reduceMotion == ReduceMotionPreference.On

    if (showAccountDialog) {
        AnimeAccountCenter(
            sessionState = sessionState,
            onDismiss = { showAccountDialog = false },
            onLogin = onAnimeLogin,
            onRegister = onAnimeRegister,
            onBangumiLogin = onBangumiLogin,
            onLogout = onLogout,
            onBrowseCollection = onBrowseCollection,
            onExportData = sessionRepository::exportMyData,
            onDeleteAccount = sessionRepository::deleteAccount,
            onUpdateProfile = sessionRepository::updateProfile,
            onChangePassword = sessionRepository::changePassword,
        )
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 620.dp
        val horizontalPadding = if (wide) 32.dp else 16.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = horizontalPadding,
                    top = 20.dp,
                    end = horizontalPadding,
                    bottom = 112.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            item { ProfileHeader(environmentLabel, contentUnderSystemBars) }
            item {
                AccountHero(
                    wide = wide,
                    sessionState = sessionState,
                    onManageAccount = { showAccountDialog = true },
                    onBrowseCollection = onBrowseCollection,
                )
            }
            item { ProfileMetrics(wide, sessionState) }
            if (sessionState is SessionState.Authenticated) {
                item { BangumiSyncPanel(sessionState, sessionRepository) }
            }
            item { PersonalArchive(wide, sessionState) }
            item {
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
                        verticalAlignment = Alignment.Top,
                    ) {
                        AppearancePanel(
                            theme = theme,
                            onThemeChange = { onThemeChange(it.toThemePreference()) },
                            glass = glass,
                            onGlassChange = { onGlassChange(it.toGlassPreference()) },
                            language = language,
                            onLanguageChange = { onLanguageChange(it.toLanguagePreference()) },
                            modifier = Modifier.weight(1.35f),
                        )
                        PreferencePanel(
                            reduceMotion = reduceMotion,
                            onReduceMotionChange = {
                                onReduceMotionChange(
                                    if (it) ReduceMotionPreference.On else ReduceMotionPreference.FollowSystem,
                                )
                            },
                            environmentLabel = environmentLabel,
                            sessionState = sessionState,
                            onDiagnostics = onDiagnostics,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg)) {
                        AppearancePanel(
                            theme = theme,
                            onThemeChange = { onThemeChange(it.toThemePreference()) },
                            glass = glass,
                            onGlassChange = { onGlassChange(it.toGlassPreference()) },
                            language = language,
                            onLanguageChange = { onLanguageChange(it.toLanguagePreference()) },
                        )
                        PreferencePanel(
                            reduceMotion = reduceMotion,
                            onReduceMotionChange = {
                                onReduceMotionChange(
                                    if (it) ReduceMotionPreference.On else ReduceMotionPreference.FollowSystem,
                                )
                            },
                            environmentLabel = environmentLabel,
                            sessionState = sessionState,
                            onDiagnostics = onDiagnostics,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BangumiSyncPanel(
    session: SessionState.Authenticated,
    repository: SessionRepository,
) {
    var status by remember(session.user.summary.id.value) { mutableStateOf<BangumiSyncStatus?>(null) }
    var conflicts by remember(session.user.summary.id.value) { mutableStateOf<List<BangumiSyncConflict>>(emptyList()) }
    var loading by rememberSaveable(session.user.summary.id.value) { mutableStateOf(false) }
    var message by rememberSaveable(session.user.summary.id.value) { mutableStateOf<String?>(null) }
    var messageIsError by rememberSaveable(session.user.summary.id.value) { mutableStateOf(false) }
    var reload by rememberSaveable(session.user.summary.id.value) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val syncStatusError = animeString(AnimeCopy.profileSyncStatusError)
    val syncCompleted = animeString(AnimeCopy.profileSyncCompleted)
    val syncFailed = animeString(AnimeCopy.profileSyncFailed)

    LaunchedEffect(session.user.summary.id, reload) {
        loading = true
        repository
            .syncStatus()
            .onSuccess { loaded ->
                status = loaded
                conflicts =
                    if (loaded.conflictCount > 0) repository.syncConflicts().getOrDefault(emptyList()) else emptyList()
            }.onFailure {
                message = it.message ?: syncStatusError
                messageIsError = true
            }
        loading = false
    }

    SettingsPanel(animeString(AnimeCopy.profileSyncTitle), animeString(AnimeCopy.profileSyncDescription)) {
        val current = status
        StatusLine(
            animeString(AnimeCopy.profileSyncConnection),
            when {
                current == null && loading -> animeString(AnimeCopy.profileSyncReading)
                current?.bangumiLinked == true -> animeString(AnimeCopy.profileSyncConnected)
                else -> animeString(AnimeCopy.profileSyncUnconnected)
            },
        )
        StatusLine(
            animeString(AnimeCopy.profileSyncLastSuccess),
            current?.lastSuccessfulAt?.take(16)?.replace('T', ' ') ?: animeString(AnimeCopy.profileSyncNever),
        )
        StatusLine(
            animeString(AnimeCopy.profileSyncQueue),
            "${current?.pendingCount ?: 0} / ${current?.failedCount ?: 0} / ${current?.conflictCount ?: 0}",
        )
        AnimePrimaryButton(if (loading) animeString(AnimeCopy.profileSyncing) else animeString(AnimeCopy.profileSyncNow), {
            if (!loading) {
                scope.launch {
                    loading = true
                    repository
                        .startSync()
                        .onSuccess {
                            message = syncCompleted
                            messageIsError = false
                            reload++
                        }.onFailure {
                            message = it.message ?: syncFailed
                            messageIsError = true
                            loading = false
                        }
                }
            }
        }, enabled = current?.bangumiLinked == true && !loading)
        message?.let {
            Text(
                it,
                color =
                    if (messageIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }
        conflicts.forEach { conflict ->
            Surface(
                shape = RoundedCornerShape(AnimeRadius.control),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
            ) {
                Column(Modifier.padding(AnimeSpacing.md), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                    Text(
                        animeString(AnimeCopy.profileSyncSubject, conflict.subjectId, conflict.fieldName),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        animeString(AnimeCopy.profileSyncValues, conflict.localValue, conflict.remoteValue),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                        AnimeSecondaryButton(animeString(AnimeCopy.profileKeepLocal), {
                            scope.launch {
                                repository
                                    .resolveSyncConflict(
                                        conflict.id,
                                        conflict.localVersion,
                                        SyncConflictChoice.KeepLocal,
                                    ).onSuccess {
                                        reload++
                                    }.onFailure {
                                        message = it.message
                                        messageIsError = true
                                    }
                            }
                        })
                        AnimeSecondaryButton(animeString(AnimeCopy.profileUseBangumi), {
                            scope.launch {
                                repository
                                    .resolveSyncConflict(
                                        conflict.id,
                                        conflict.localVersion,
                                        SyncConflictChoice.UseRemote,
                                    ).onSuccess {
                                        reload++
                                    }.onFailure {
                                        message = it.message
                                        messageIsError = true
                                    }
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalArchive(
    wide: Boolean,
    sessionState: SessionState,
) {
    val profile = (sessionState as? SessionState.Authenticated)?.user
    val source = if (profile?.connectedProvider == Provider.Bangumi) {
        animeString(AnimeCopy.subjectBangumi)
    } else {
        animeString(AnimeCopy.profileProviderAnime)
    }
    val entries =
        listOf(
            ArchiveEntry(
                animeString(AnimeCopy.profileArchiveRating),
                profile?.ratingCount?.toString() ?: "—",
                animeString(AnimeCopy.profileArchiveRatingSubtitle, source),
            ),
            ArchiveEntry(
                animeString(AnimeCopy.profileArchiveReview),
                profile?.reviewCount?.toString() ?: "—",
                animeString(AnimeCopy.profileArchiveReviewSubtitle, source),
            ),
            ArchiveEntry(
                animeString(AnimeCopy.profileArchiveList),
                profile?.listCount?.toString() ?: "—",
                animeString(AnimeCopy.profileArchiveListSubtitle),
            ),
        )
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
            Text(animeString(AnimeCopy.profileArchiveTitle), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(animeString(AnimeCopy.profileArchiveDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
                entries.forEach { ArchiveCard(it, Modifier.weight(1f)) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) { entries.forEach { ArchiveCard(it) } }
        }
    }
}

private data class ArchiveEntry(
    val title: String,
    val count: String,
    val subtitle: String,
)

@Composable
private fun ArchiveCard(
    entry: ArchiveEntry,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(AnimeSpacing.lg), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    entry.count,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                entry.subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    environmentLabel: String,
    contentUnderSystemBars: Boolean,
) {
    Row(
        modifier =
            Modifier
                .then(if (contentUnderSystemBars) Modifier.statusBarsPadding() else Modifier)
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
            Text(
                text = animeString(AnimeCopy.profilePersonalSpace),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = animeString(AnimeCopy.rootProfile), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                text = animeString(AnimeCopy.profileWatchingDescription),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(animeString(AnimeCopy.profileEnvironmentStatus, environmentLabel, animeString(AnimeCopy.profileLocal)))
    }
}

@Composable
private fun AccountHero(
    wide: Boolean,
    sessionState: SessionState,
    onManageAccount: () -> Unit,
    onBrowseCollection: () -> Unit,
) {
    val authenticated = sessionState as? SessionState.Authenticated
    val restoring = sessionState is SessionState.Restoring
    val loginFailure = (sessionState as? SessionState.Failed)?.message
    val provider = authenticated?.user?.connectedProvider
    val providerName = if (provider == Provider.Bangumi) {
        animeString(AnimeCopy.subjectBangumi)
    } else {
        animeString(AnimeCopy.profileProviderAnime)
    }
    val displayName = authenticated?.user?.summary?.displayName
        ?: if (restoring) animeString(AnimeCopy.profileLoginLoading) else animeString(AnimeCopy.profileLoginTitle)
    val avatarLetter = displayName.firstOrNull()?.uppercase() ?: "A"
    val avatarUrl = (authenticated?.user?.summary?.avatar as? ImageRef.Remote)?.url
    AnimeGlassPanel(
        role = GlassRole.StaticHero,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.panel),
        contentPadding = PaddingValues(0.dp),
    ) {
        val avatar: @Composable () -> Unit = {
            Box(
                modifier =
                    Modifier
                        .size(84.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0A84FF), Color(0xFFBF5AF2))),
                            CircleShape,
                        ).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = animeString(AnimeCopy.profileAvatar, displayName),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        text = avatarLetter,
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        val identity: @Composable (Modifier) -> Unit = { identityModifier ->
            Column(
                modifier = identityModifier,
                verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(displayName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    StatusPill(
                        if (authenticated != null) {
                            providerName
                        } else if (restoring) {
                            animeString(AnimeCopy.profileLoginInProgress)
                        } else {
                            animeString(AnimeCopy.profileNotLoggedIn)
                        },
                    )
                }
                Text(
                    loginFailure
                        ?: if (authenticated !=
                            null
                        ) {
                            animeString(AnimeCopy.profileConnectedDescription)
                        } else if (restoring) {
                            animeString(AnimeCopy.profileVerifying)
                        } else {
                            animeString(AnimeCopy.profileNoProxy)
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (loginFailure ==
                            null
                        ) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
                Text(
                    if (authenticated == null) {
                        animeString(AnimeCopy.profileBangumiOptional)
                    } else {
                        animeString(AnimeCopy.profileProviderSession, providerName)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.animeColors.success,
                )
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                if (authenticated != null) {
                    AnimeSecondaryButton(label = animeString(AnimeCopy.profileBrowseLibrary), onClick = onBrowseCollection)
                    AnimePrimaryButton(label = animeString(AnimeCopy.profileManageAccount), onClick = onManageAccount)
                } else {
                    AnimePrimaryButton(
                        label = animeString(AnimeCopy.profileLoginOrCreate),
                        onClick = onManageAccount,
                        enabled = !restoring,
                        loading = restoring,
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.09f),
                                Color.Transparent,
                            ),
                        ),
                    ),
        ) {
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(AnimeSpacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.xl),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    avatar()
                    identity(Modifier.weight(1f))
                    actions()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(AnimeSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        avatar()
                        identity(Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                    ) {
                        if (authenticated != null) {
                            AnimeSecondaryButton(
                                label = animeString(AnimeCopy.profileBrowseLibrary),
                                onClick = onBrowseCollection,
                                modifier = Modifier.weight(1f),
                            )
                            AnimePrimaryButton(
                                label = animeString(AnimeCopy.profileManageAccount),
                                onClick = onManageAccount,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            AnimePrimaryButton(
                                label = animeString(AnimeCopy.profileLoginOrCreate),
                                onClick = onManageAccount,
                                enabled = !restoring,
                                loading = restoring,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMetrics(
    wide: Boolean,
    sessionState: SessionState,
) {
    val profile = (sessionState as? SessionState.Authenticated)?.user
    val source = if (profile?.connectedProvider == Provider.Bangumi) {
        animeString(AnimeCopy.subjectBangumi)
    } else {
        animeString(AnimeCopy.profileProviderAnime)
    }
    val total = profile?.collectionCounts?.values?.sum()
    val watching = profile?.collectionCounts?.get(site.jokersh.anime.core.model.CollectionStatus.Watching)
    val completed = profile?.collectionCounts?.get(site.jokersh.anime.core.model.CollectionStatus.Completed)
    val metrics =
        listOf(
            Triple(
                total?.toString() ?: "—",
                animeString(AnimeCopy.profileMetricCollection),
                animeString(AnimeCopy.profileMetricAll, source),
            ),
            Triple(
                watching?.toString() ?: "—",
                animeString(AnimeCopy.profileMetricWatching),
                animeString(AnimeCopy.profileMetricWatchingDetail, source),
            ),
            Triple(
                completed?.toString() ?: "—",
                animeString(AnimeCopy.profileMetricCompleted),
                animeString(AnimeCopy.profileMetricCompletedDetail, source),
            ),
        )
    if (wide) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
            metrics.forEach { (value, label, detail) -> MetricCard(value, label, detail, Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
            metrics.forEach { (value, label, detail) -> MetricCard(value, label, detail) }
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(AnimeSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AppearancePanel(
    theme: String,
    onThemeChange: (String) -> Unit,
    glass: String,
    onGlassChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeOptions =
        listOf(
            ChoiceOption(THEME_SYSTEM, AnimeCopy.profileLanguageSystem),
            ChoiceOption(THEME_LIGHT, AnimeCopy.profileThemeLight),
            ChoiceOption(THEME_DARK, AnimeCopy.profileThemeDark),
        )
    val glassOptions =
        listOf(
            ChoiceOption(GLASS_OFF, AnimeCopy.profileGlassOff),
            ChoiceOption(GLASS_AUTO, AnimeCopy.profileGlassAuto),
            ChoiceOption(GLASS_ON, AnimeCopy.profileGlassOn),
        )
    val languageOptions =
        listOf(
            ChoiceOption(LANGUAGE_SYSTEM, AnimeCopy.profileLanguageSystem),
            ChoiceOption(LANGUAGE_SIMPLIFIED, AnimeCopy.profileLanguageSimplified),
            ChoiceOption(LANGUAGE_TRADITIONAL, AnimeCopy.profileLanguageTraditional),
            ChoiceOption(LANGUAGE_ENGLISH, AnimeCopy.profileLanguageEnglish),
            ChoiceOption(LANGUAGE_JAPANESE, AnimeCopy.profileLanguageJapanese),
        )
    SettingsPanel(
        title = animeString(AnimeCopy.profileAppearance),
        subtitle = animeString(AnimeCopy.profileAppearanceDescription),
        modifier = modifier,
    ) {
        ChoiceGroup(animeString(AnimeCopy.profileTheme), themeOptions, theme, onThemeChange)
        ChoiceGroup(animeString(AnimeCopy.profileGlassEffect), glassOptions, glass, onGlassChange)
        ChoiceGroup(animeString(AnimeCopy.profileLanguage), languageOptions, language, onLanguageChange)
        Text(
            animeString(AnimeCopy.profilePreviewPersisted),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreferencePanel(
    reduceMotion: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    environmentLabel: String,
    sessionState: SessionState,
    onDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPanel(
        title = animeString(AnimeCopy.profilePreferences),
        subtitle = animeString(AnimeCopy.profilePreferencesDescription),
        modifier = modifier,
    ) {
        SettingSwitch(
            title = animeString(AnimeCopy.profileReduceMotion),
            description = animeString(AnimeCopy.profileReduceMotionDescription),
            checked = reduceMotion,
            onCheckedChange = onReduceMotionChange,
        )
        ThinDivider()
        val provider = (sessionState as? SessionState.Authenticated)?.user?.connectedProvider
        StatusLine(
            animeString(AnimeCopy.profileDataMode),
            if (provider ==
                Provider.Bangumi
            ) {
                animeString(AnimeCopy.profileBangumiSync)
            } else if (provider == Provider.Anime) {
                animeString(AnimeCopy.profileAnimeCloud)
            } else {
                animeString(AnimeCopy.profileSyncUnconnected)
            },
        )
        StatusLine(animeString(AnimeCopy.profileRuntime), environmentLabel)
        StatusLine(animeString(AnimeCopy.profileAppStatus), animeString(AnimeCopy.profileLocalSyncEnabled))
        AnimeSecondaryButton(animeString(AnimeCopy.profileDiagnostics), onDiagnostics)
    }
}

private const val THEME_SYSTEM = "system"
private const val THEME_LIGHT = "light"
private const val THEME_DARK = "dark"
private const val GLASS_OFF = "off"
private const val GLASS_AUTO = "auto"
private const val GLASS_ON = "on"
private const val LANGUAGE_SYSTEM = "system"
private const val LANGUAGE_SIMPLIFIED = "zh-Hans"
private const val LANGUAGE_TRADITIONAL = "zh-Hant"
private const val LANGUAGE_ENGLISH = "en"
private const val LANGUAGE_JAPANESE = "ja"

private fun ThemePreference.key(): String =
    when (this) {
        ThemePreference.System -> THEME_SYSTEM
        ThemePreference.Light -> THEME_LIGHT
        ThemePreference.Dark -> THEME_DARK
    }

private fun String.toThemePreference(): ThemePreference =
    when (this) {
        THEME_LIGHT -> ThemePreference.Light
        THEME_DARK -> ThemePreference.Dark
        else -> ThemePreference.System
    }

private fun GlassPreference.key(): String =
    when (this) {
        GlassPreference.Off -> GLASS_OFF
        GlassPreference.Auto -> GLASS_AUTO
        GlassPreference.On -> GLASS_ON
    }

private fun String.toGlassPreference(): GlassPreference =
    when (this) {
        GLASS_OFF -> GlassPreference.Off
        GLASS_ON -> GlassPreference.On
        else -> GlassPreference.Auto
    }

private fun LanguagePreference.key(): String =
    when (this) {
        LanguagePreference.System -> LANGUAGE_SYSTEM
        LanguagePreference.SimplifiedChinese -> LANGUAGE_SIMPLIFIED
        LanguagePreference.TraditionalChinese -> LANGUAGE_TRADITIONAL
        LanguagePreference.English -> LANGUAGE_ENGLISH
        LanguagePreference.Japanese -> LANGUAGE_JAPANESE
    }

private fun String.toLanguagePreference(): LanguagePreference =
    when (this) {
        LANGUAGE_SIMPLIFIED -> LanguagePreference.SimplifiedChinese
        LANGUAGE_TRADITIONAL -> LanguagePreference.TraditionalChinese
        LANGUAGE_ENGLISH -> LanguagePreference.English
        LANGUAGE_JAPANESE -> LanguagePreference.Japanese
        else -> LanguagePreference.System
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
public fun AnimeAccountCenter(
    sessionState: SessionState,
    onDismiss: () -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onBangumiLogin: () -> Unit,
    onLogout: () -> Unit,
    onBrowseCollection: () -> Unit,
    onExportData: (suspend () -> Result<String>)? = null,
    onDeleteAccount: (suspend () -> Result<Unit>)? = null,
    onUpdateProfile: (suspend (String) -> Result<Unit>)? = null,
    onChangePassword: (suspend (String, String) -> Result<Unit>)? = null,
) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var authorizationStarted by rememberSaveable { mutableStateOf(false) }
    var accountBusy by remember { mutableStateOf(false) }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    var accountExportLength by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }
    var changePassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loading = sessionState is SessionState.Restoring
    val error = (sessionState as? SessionState.Failed)?.message
    val authenticated = sessionState as? SessionState.Authenticated
    val connectedToBangumi = authenticated?.user?.connectedProvider == Provider.Bangumi
    val valid = username.isNotBlank() && password.isNotBlank() && (!registering || displayName.isNotBlank())
    val exportFailed = animeString(AnimeCopy.profileExportFailed)
    val deleteFailed = animeString(AnimeCopy.profileDeleteFailed)
    val updateFailed = animeString(AnimeCopy.profileUpdateFailed)
    val profileUpdated = animeString(AnimeCopy.profileUpdated)
    val passwordUpdateFailed = animeString(AnimeCopy.profilePasswordUpdateFailed)
    val passwordUpdated = animeString(AnimeCopy.profilePasswordUpdated)

    ModalBottomSheet(
        onDismissRequest = { if (!loading) onDismiss() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = AnimeRadius.panel, topEnd = AnimeRadius.panel),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = AnimeSpacing.xl, vertical = AnimeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                    Text(animeString(AnimeCopy.profileAccount), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (authenticated == null) {
                            animeString(AnimeCopy.profileAccountDescription)
                        } else {
                            animeString(AnimeCopy.profileAccountManageDescription)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimeSecondaryButton(label = animeString(AnimeCopy.actionComplete), onClick = onDismiss, enabled = !loading)
            }

            if (authenticated == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                ) {
                    AccountModeButton(animeString(AnimeCopy.actionLogin), !registering, { registering = false }, Modifier.weight(1f))
                    AccountModeButton(animeString(AnimeCopy.actionRegister), registering, { registering = true }, Modifier.weight(1f))
                }

                Text(
                    if (registering) {
                        animeString(AnimeCopy.profileRegisterDescription)
                    } else {
                        animeString(AnimeCopy.profileLoginDescription)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(animeString(AnimeCopy.profileUsername)) },
                    supportingText = { if (registering) Text(animeString(AnimeCopy.profileUsernameHint)) },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (registering) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(animeString(AnimeCopy.profileDisplayName)) },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(animeString(AnimeCopy.profilePassword)) },
                    supportingText = { if (registering) Text(animeString(AnimeCopy.profilePasswordHint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                AnimePrimaryButton(
                    label = if (registering) {
                        animeString(AnimeCopy.profileCreateAndLogin)
                    } else {
                        animeString(AnimeCopy.profileLoginTitle)
                    },
                    onClick = {
                        if (registering) {
                            onRegister(username.trim(), password, displayName.trim())
                        } else {
                            onLogin(username.trim(), password)
                        }
                    },
                    enabled = valid && !loading,
                    loading = loading,
                )
            } else {
                AccountIdentityCard(authenticated)
            }

            BangumiConnectionCard(
                authenticated = authenticated,
                connected = connectedToBangumi,
                loading = loading,
                authorizationStarted = authorizationStarted,
                onConnect = {
                    authorizationStarted = true
                    onBangumiLogin()
                },
            )

            Text(
                animeString(AnimeCopy.profileBangumiSecurity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (authenticated != null) {
                Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                    ) {
                        AnimeSecondaryButton(
                            label = animeString(AnimeCopy.profileMyCollection),
                            onClick = {
                                onBrowseCollection()
                                onDismiss()
                            },
                        )
                        AnimeSecondaryButton(label = animeString(AnimeCopy.actionLogout), onClick = onLogout)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                        onUpdateProfile?.let { AnimeSecondaryButton(animeString(AnimeCopy.profileEdit), onClick = { editProfile = true }) }
                        onChangePassword?.let { AnimeSecondaryButton(animeString(AnimeCopy.profileChangePassword), onClick = { changePassword = true }) }
                    }
                    onExportData?.let { export ->
                        AnimeSecondaryButton(
                            label = if (accountBusy) {
                                animeString(AnimeCopy.profileExporting)
                            } else {
                                animeString(AnimeCopy.profileExportData)
                            },
                            onClick = {
                                if (!accountBusy) {
                                    scope.launch {
                                        accountBusy = true
                                        export()
                                            .onSuccess {
                                                accountMessage = null
                                                accountExportLength = it.length
                                            }
                                            .onFailure { accountMessage = it.message ?: exportFailed }
                                        accountBusy = false
                                    }
                                }
                            },
                            enabled = !accountBusy,
                        )
                    }
                    onDeleteAccount?.let { delete ->
                        TextButton(onClick = { confirmDelete = true }, enabled = !accountBusy) {
                            Text(animeString(AnimeCopy.profileDeleteAccount), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    accountMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    accountExportLength?.let {
                        Text(
                            animeString(AnimeCopy.profileExportCompleted, it),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!accountBusy) confirmDelete = false },
            title = { Text(animeString(AnimeCopy.profileDeleteConfirmTitle)) },
            text = { Text(animeString(AnimeCopy.profileDeleteConfirmMessage)) },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !accountBusy) { Text(animeString(AnimeCopy.actionCancel)) } },
            confirmButton = {
                TextButton(
                    onClick = {
                        val delete = onDeleteAccount ?: return@TextButton
                        scope.launch {
                            accountBusy = true
                            delete()
                                .onSuccess {
                                    confirmDelete = false
                                    onDismiss()
                                }.onFailure { accountMessage = it.message ?: deleteFailed }
                            accountBusy = false
                        }
                    },
                    enabled = !accountBusy,
                ) { Text(animeString(AnimeCopy.profileConfirmDelete), color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    if (editProfile && authenticated != null) {
        var name by remember(authenticated.user.summary.displayName) { mutableStateOf(authenticated.user.summary.displayName) }
        AlertDialog(
            onDismissRequest = { editProfile = false },
            title = { Text(animeString(AnimeCopy.profileEditTitle)) },
            text = { OutlinedTextField(name, { name = it }, label = { Text(animeString(AnimeCopy.profileDisplayName)) }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val update = onUpdateProfile ?: return@TextButton
                    scope.launch {
                        accountBusy = true
                        update(name.trim())
                            .onSuccess { editProfile = false; accountMessage = profileUpdated }
                            .onFailure { accountMessage = it.message ?: updateFailed }
                        accountBusy = false
                    }
                }, enabled = !accountBusy && name.isNotBlank()) { Text(animeString(AnimeCopy.actionSave)) }
            },
            dismissButton = { TextButton(onClick = { editProfile = false }) { Text(animeString(AnimeCopy.actionCancel)) } },
        )
    }
    if (changePassword) {
        var current by remember { mutableStateOf("") }
        var next by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { changePassword = false },
            title = { Text(animeString(AnimeCopy.profileChangePassword)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(current, { current = it }, label = { Text(animeString(AnimeCopy.profilePasswordCurrent)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(next, { next = it }, label = { Text(animeString(AnimeCopy.profilePasswordNew)) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val update = onChangePassword ?: return@TextButton
                    scope.launch {
                        accountBusy = true
                        update(current, next)
                            .onSuccess { changePassword = false; accountMessage = passwordUpdated }
                            .onFailure { accountMessage = it.message ?: passwordUpdateFailed }
                        accountBusy = false
                    }
                }, enabled = !accountBusy && current.isNotBlank() && next.isNotBlank()) { Text(animeString(AnimeCopy.actionSave)) }
            },
            dismissButton = { TextButton(onClick = { changePassword = false }) { Text(animeString(AnimeCopy.actionCancel)) } },
        )
    }
}

@Composable
private fun AccountModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = AnimeSize.touch),
        shape = RoundedCornerShape(AnimeRadius.control),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(Modifier.fillMaxWidth().padding(AnimeSpacing.md), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AccountIdentityCard(session: SessionState.Authenticated) {
    val profile = session.user
    val provider = if (profile.connectedProvider == Provider.Bangumi) {
        animeString(AnimeCopy.profileProviderAnimeBangumi)
    } else {
        animeString(AnimeCopy.profileProviderAnime)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(AnimeSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxs)) {
                Text(
                    profile.summary.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(animeString(AnimeCopy.profileCurrentAccount), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusPill(provider)
        }
    }
}

@Composable
private fun BangumiConnectionCard(
    authenticated: SessionState.Authenticated?,
    connected: Boolean,
    loading: Boolean,
    authorizationStarted: Boolean,
    onConnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.card),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AnimeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                Text(
                    if (connected) {
                        animeString(AnimeCopy.profileBangumiConnected)
                    } else {
                        animeString(AnimeCopy.profileConnectBangumi)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        connected -> animeString(AnimeCopy.profileBangumiSyncDescription)
                        authorizationStarted -> animeString(AnimeCopy.profileAuthorizationOpened)
                        authenticated != null -> animeString(AnimeCopy.profileBindImport)
                        else -> animeString(AnimeCopy.profileCreateImport)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (connected) {
                StatusPill(animeString(AnimeCopy.profileSyncConnected))
            } else {
                AnimePrimaryButton(
                    label =
                        when {
                            authorizationStarted -> animeString(AnimeCopy.profileReopenAuthorization)
                            authenticated == null -> animeString(AnimeCopy.profileUseBangumiContinue)
                            else -> animeString(AnimeCopy.profileBindAccount)
                        },
                    onClick = onConnect,
                    enabled = !loading,
                    loading = loading,
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AnimeRadius.panel),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(AnimeSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AnimeSpacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun ChoiceGroup(
    label: String,
    values: List<ChoiceOption>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
            values.forEach { option ->
                val active = option.key == selected
                Surface(
                    onClick = { onSelected(option.key) },
                    modifier = Modifier.weight(1f).heightIn(min = AnimeSize.touch),
                    shape = RoundedCornerShape(AnimeRadius.control),
                    color =
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                                .copy(
                                    alpha = 0.7f,
                                )
                        },
                    contentColor =
                        if (active) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    border =
                        BorderStroke(
                            AnimeSize.border,
                            MaterialTheme.colorScheme.outline.copy(
                                alpha = if (active) 0f else 0.2f,
                            ),
                        ),
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = AnimeSpacing.sm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            animeString(option.label),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private data class ChoiceOption(
    val key: String,
    val label: StringResource,
)

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xxs)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(AnimeSpacing.lg))
        AnimeLiquidToggle(selected = checked, onSelectedChange = onCheckedChange)
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(AnimeSize.border, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = AnimeSpacing.md, vertical = AnimeSpacing.xs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ThinDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(
                AnimeSize.border,
            ).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    )
}
