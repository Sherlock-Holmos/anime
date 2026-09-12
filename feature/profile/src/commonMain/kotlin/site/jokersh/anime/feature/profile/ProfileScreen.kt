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
import site.jokersh.anime.core.designsystem.AnimeGlassPanel
import site.jokersh.anime.core.designsystem.AnimeLiquidToggle
import site.jokersh.anime.core.designsystem.AnimePrimaryButton
import site.jokersh.anime.core.designsystem.AnimeRadius
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSize
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.core.designsystem.GlassRole
import site.jokersh.anime.core.designsystem.animeColors
import site.jokersh.anime.core.model.AppSettings
import site.jokersh.anime.core.model.GlassPreference
import site.jokersh.anime.core.model.ImageRef
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
    onReduceMotionChange: (ReduceMotionPreference) -> Unit,
    onDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    val theme = settings.theme.label()
    val glass = settings.glass.label()
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
            item { ProfileHeader(environmentLabel) }
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
    var reload by rememberSaveable(session.user.summary.id.value) { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session.user.summary.id, reload) {
        loading = true
        repository
            .syncStatus()
            .onSuccess { loaded ->
                status = loaded
                conflicts =
                    if (loaded.conflictCount > 0) repository.syncConflicts().getOrDefault(emptyList()) else emptyList()
            }.onFailure { message = it.message ?: "无法读取同步状态" }
        loading = false
    }

    SettingsPanel("Bangumi 同步", "Anime 服务器负责访问 Bangumi，客户端无需代理。") {
        val current = status
        StatusLine(
            "连接状态",
            when {
                current == null && loading -> "正在读取"
                current?.bangumiLinked == true -> "已绑定"
                else -> "未绑定"
            },
        )
        StatusLine("上次成功", current?.lastSuccessfulAt?.take(16)?.replace('T', ' ') ?: "尚未同步")
        StatusLine(
            "等待 / 失败 / 冲突",
            "${current?.pendingCount ?: 0} / ${current?.failedCount ?: 0} / ${current?.conflictCount ?: 0}",
        )
        AnimePrimaryButton(if (loading) "同步中" else "立即同步", {
            if (!loading) {
                scope.launch {
                    loading = true
                    repository
                        .startSync()
                        .onSuccess {
                            message = "同步已完成"
                            reload++
                        }.onFailure {
                            message = it.message ?: "同步失败"
                            loading = false
                        }
                }
            }
        }, enabled = current?.bangumiLinked == true && !loading)
        message?.let {
            Text(
                it,
                color =
                    if (it.contains("失败") ||
                        it.contains("无法")
                    ) {
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
                    Text("作品 ${conflict.subjectId} · ${conflict.fieldName}", fontWeight = FontWeight.SemiBold)
                    Text(
                        "本地 ${conflict.localValue}  /  Bangumi ${conflict.remoteValue}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                        AnimeSecondaryButton("保留本地", {
                            scope.launch {
                                repository
                                    .resolveSyncConflict(
                                        conflict.id,
                                        conflict.localVersion,
                                        SyncConflictChoice.KeepLocal,
                                    ).onSuccess {
                                        reload++
                                    }.onFailure {
                                        message =
                                            it.message
                                    }
                            }
                        })
                        AnimeSecondaryButton("采用 Bangumi", {
                            scope.launch {
                                repository
                                    .resolveSyncConflict(
                                        conflict.id,
                                        conflict.localVersion,
                                        SyncConflictChoice.UseRemote,
                                    ).onSuccess {
                                        reload++
                                    }.onFailure {
                                        message =
                                            it.message
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
    val source = if (profile?.connectedProvider == Provider.Bangumi) "Bangumi" else "Anime"
    val entries =
        listOf(
            ArchiveEntry("我的评分", profile?.ratingCount?.toString() ?: "—", "$source 动画评分"),
            ArchiveEntry("我的评价", profile?.reviewCount?.toString() ?: "—", "$source 作品评价"),
            ArchiveEntry("我的片单", profile?.listCount?.toString() ?: "—", "Anime 主题片单"),
        )
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
            Text("我的档案", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("评分、表达和策展共同组成你的兴趣画像。", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ProfileHeader(environmentLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.xs)) {
            Text(
                text = "个人空间",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "我的", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                text = "管理你的观看足迹与桌面体验。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill("$environmentLabel · 本地")
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
    val providerName = if (provider == Provider.Bangumi) "Bangumi" else "Anime"
    val displayName = authenticated?.user?.summary?.displayName ?: if (restoring) "正在登录" else "登录 Anime"
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
                        contentDescription = "$displayName 的头像",
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
                            "登录中"
                        } else {
                            "未登录"
                        },
                    )
                }
                Text(
                    loginFailure
                        ?: if (authenticated !=
                            null
                        ) {
                            "收藏、评分和社区档案已连接。"
                        } else if (restoring) {
                            "正在校验登录信息…"
                        } else {
                            "无需代理，使用 Anime 账号保存你的收藏与评分。"
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
                    if (authenticated == null) "Bangumi 仅作为可选的数据导入来源。" else "$providerName 身份已连接 · Session 有效",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.animeColors.success,
                )
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                if (authenticated != null) {
                    AnimeSecondaryButton(label = "查看片库", onClick = onBrowseCollection)
                    AnimePrimaryButton(label = "管理账户", onClick = onManageAccount)
                } else {
                    AnimePrimaryButton(
                        label = "登录或创建账户",
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
                                label = "查看片库",
                                onClick = onBrowseCollection,
                                modifier = Modifier.weight(1f),
                            )
                            AnimePrimaryButton(
                                label = "管理账户",
                                onClick = onManageAccount,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            AnimePrimaryButton(
                                label = "登录或创建账户",
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
    val source = if (profile?.connectedProvider == Provider.Bangumi) "Bangumi" else "Anime"
    val total = profile?.collectionCounts?.values?.sum()
    val watching = profile?.collectionCounts?.get(site.jokersh.anime.core.model.CollectionStatus.Watching)
    val completed = profile?.collectionCounts?.get(site.jokersh.anime.core.model.CollectionStatus.Completed)
    val metrics =
        listOf(
            Triple(total?.toString() ?: "—", "动画收藏", "$source 全部收藏"),
            Triple(watching?.toString() ?: "—", "正在追", "$source 在看"),
            Triple(completed?.toString() ?: "—", "已经看过", "$source 看过"),
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
    modifier: Modifier = Modifier,
) {
    SettingsPanel(title = "外观", subtitle = "选择更适合当前桌面的视觉氛围。", modifier = modifier) {
        ChoiceGroup("主题", listOf("跟随系统", "浅色", "深色"), theme, onThemeChange)
        ChoiceGroup("玻璃效果", listOf("节能", "平衡", "通透"), glass, onGlassChange)
        Text(
            "设置在当前预览中即时生效，正式数据接入后会持久化到本机。",
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
    SettingsPanel(title = "偏好与状态", subtitle = "让动画与显示效果适应当前设备。", modifier = modifier) {
        SettingSwitch(
            title = "减少动态效果",
            description = "降低切换动画和背景位移。",
            checked = reduceMotion,
            onCheckedChange = onReduceMotionChange,
        )
        ThinDivider()
        val provider = (sessionState as? SessionState.Authenticated)?.user?.connectedProvider
        StatusLine(
            "数据模式",
            if (provider ==
                Provider.Bangumi
            ) {
                "Bangumi 同步"
            } else if (provider == Provider.Anime) {
                "Anime 云端"
            } else {
                "未连接"
            },
        )
        StatusLine("运行环境", environmentLabel)
        StatusLine("应用状态", "已启用本地偏好同步")
        AnimeSecondaryButton("服务诊断", onDiagnostics)
    }
}

private fun ThemePreference.label(): String =
    when (this) {
        ThemePreference.System -> "跟随系统"
        ThemePreference.Light -> "浅色"
        ThemePreference.Dark -> "深色"
    }

private fun String.toThemePreference(): ThemePreference =
    when (this) {
        "浅色" -> ThemePreference.Light
        "深色" -> ThemePreference.Dark
        else -> ThemePreference.System
    }

private fun GlassPreference.label(): String =
    when (this) {
        GlassPreference.Off -> "节能"
        GlassPreference.Auto -> "平衡"
        GlassPreference.On -> "通透"
    }

private fun String.toGlassPreference(): GlassPreference =
    when (this) {
        "节能" -> GlassPreference.Off
        "通透" -> GlassPreference.On
        else -> GlassPreference.Auto
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
    var confirmDelete by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }
    var changePassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loading = sessionState is SessionState.Restoring
    val error = (sessionState as? SessionState.Failed)?.message
    val authenticated = sessionState as? SessionState.Authenticated
    val connectedToBangumi = authenticated?.user?.connectedProvider == Provider.Bangumi
    val valid = username.isNotBlank() && password.isNotBlank() && (!registering || displayName.isNotBlank())

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
                    Text("Anime 账户", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (authenticated == null) "一个账户，连接收藏、评分与社区档案。" else "管理登录身份与外部数据连接。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AnimeSecondaryButton(label = "完成", onClick = onDismiss, enabled = !loading)
            }

            if (authenticated == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm),
                ) {
                    AccountModeButton("登录", !registering, { registering = false }, Modifier.weight(1f))
                    AccountModeButton("创建账户", registering, { registering = true }, Modifier.weight(1f))
                }

                Text(
                    if (registering) "创建后即可跨设备保存收藏、评分与社区内容。" else "使用 Anime 账号登录，无需代理。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("用户名") },
                    supportingText = { if (registering) Text("3–32 位字母、数字或下划线") },
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (registering) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("昵称") },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("密码") },
                    supportingText = { if (registering) Text("至少 10 个字符") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                AnimePrimaryButton(
                    label = if (registering) "创建并登录" else "登录 Anime",
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
                "Bangumi 密码不会交给 Anime。授权令牌仅加密保存在服务器，客户端始终使用 Anime Session。",
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
                            label = "查看我的收藏",
                            onClick = {
                                onBrowseCollection()
                                onDismiss()
                            },
                        )
                        AnimeSecondaryButton(label = "退出登录", onClick = onLogout)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
                        onUpdateProfile?.let { AnimeSecondaryButton("编辑资料", onClick = { editProfile = true }) }
                        onChangePassword?.let { AnimeSecondaryButton("修改密码", onClick = { changePassword = true }) }
                    }
                    onExportData?.let { export ->
                        AnimeSecondaryButton(
                            label = if (accountBusy) "导出中…" else "导出我的数据",
                            onClick = {
                                if (!accountBusy) {
                                    scope.launch {
                                        accountBusy = true
                                        export()
                                            .onSuccess { accountMessage = "数据导出完成（${it.length} 个字符）" }
                                            .onFailure { accountMessage = it.message ?: "导出失败" }
                                        accountBusy = false
                                    }
                                }
                            },
                            enabled = !accountBusy,
                        )
                    }
                    onDeleteAccount?.let { delete ->
                        TextButton(onClick = { confirmDelete = true }, enabled = !accountBusy) {
                            Text("注销账户", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    accountMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!accountBusy) confirmDelete = false },
            title = { Text("注销 Anime 账户？") },
            text = { Text("这会删除账户数据并清理当前设备会话，操作不可撤销。建议先导出数据。") },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !accountBusy) { Text("取消") } },
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
                                }.onFailure { accountMessage = it.message ?: "注销失败" }
                            accountBusy = false
                        }
                    },
                    enabled = !accountBusy,
                ) { Text("确认注销", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
    if (editProfile && authenticated != null) {
        var name by remember(authenticated.user.summary.displayName) { mutableStateOf(authenticated.user.summary.displayName) }
        AlertDialog(
            onDismissRequest = { editProfile = false },
            title = { Text("编辑个人资料") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("昵称") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val update = onUpdateProfile ?: return@TextButton
                    scope.launch {
                        accountBusy = true
                        update(name.trim())
                            .onSuccess { editProfile = false; accountMessage = "资料已更新" }
                            .onFailure { accountMessage = it.message ?: "资料更新失败" }
                        accountBusy = false
                    }
                }, enabled = !accountBusy && name.isNotBlank()) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editProfile = false }) { Text("取消") } },
        )
    }
    if (changePassword) {
        var current by remember { mutableStateOf("") }
        var next by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { changePassword = false },
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(current, { current = it }, label = { Text("当前密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(next, { next = it }, label = { Text("新密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val update = onChangePassword ?: return@TextButton
                    scope.launch {
                        accountBusy = true
                        update(current, next)
                            .onSuccess { changePassword = false; accountMessage = "密码已更新" }
                            .onFailure { accountMessage = it.message ?: "密码更新失败" }
                        accountBusy = false
                    }
                }, enabled = !accountBusy && current.isNotBlank() && next.isNotBlank()) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { changePassword = false }) { Text("取消") } },
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
    val provider = if (profile.connectedProvider == Provider.Bangumi) "Anime + Bangumi" else "Anime"
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
                Text("当前登录账户", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    if (connected) "Bangumi 已连接" else "连接 Bangumi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        connected -> "收藏与头像可由 Anime 服务器同步，无需客户端代理。"
                        authorizationStarted -> "授权页已打开；完成后这里会自动更新连接状态。"
                        authenticated != null -> "绑定到当前 Anime 账户，并导入你的收藏与观看状态。"
                        else -> "也可以通过 Bangumi 授权创建 Anime 身份并导入已有收藏。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (connected) {
                StatusPill("已绑定")
            } else {
                AnimePrimaryButton(
                    label =
                        when {
                            authorizationStarted -> "重新打开授权"
                            authenticated == null -> "使用 Bangumi 继续"
                            else -> "绑定账户"
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
    values: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AnimeSpacing.sm)) {
            values.forEach { value ->
                val active = value == selected
                Surface(
                    onClick = { onSelected(value) },
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
                        Text(value, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

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
