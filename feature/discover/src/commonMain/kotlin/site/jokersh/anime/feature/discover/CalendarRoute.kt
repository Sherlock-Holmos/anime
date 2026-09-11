package site.jokersh.anime.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimePosterArtwork
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.core.designsystem.AnimeSpacing
import site.jokersh.anime.data.catalog.CalendarPage
import site.jokersh.anime.data.catalog.CatalogRepository
import kotlin.time.Clock

@Composable
public fun CalendarRoute(
    repository: CatalogRepository,
    onBack: () -> Unit,
    onSubjectClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today =
        remember {
            Clock.System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
        }
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    val selectedDate = remember(selectedDateText) { LocalDate.parse(selectedDateText) }
    var page by remember(selectedDate) { mutableStateOf<CalendarPage?>(null) }
    var loading by remember(selectedDate) { mutableStateOf(true) }
    var error by remember(selectedDate) { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, selectedDate) {
        loading = true
        error = null
        repository
            .calendar(selectedDate)
            .onSuccess { page = it }
            .onFailure { error = it.message ?: "播出日历暂时不可用" }
        loading = false
    }

    val dates = (-2..2).map { today.plus(DatePeriod(days = it)) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 20.dp, 16.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimeBackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(20.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("发现", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Text("播出日历", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("按日期查看当天更新的作品。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dates.forEach { date ->
                    val selected = date == selectedDate
                    Surface(
                        onClick = { selectedDateText = date.toString() },
                        modifier = Modifier.weight(1f).height(68.dp),
                        shape = RoundedCornerShape(18.dp),
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        contentColor =
                            if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        border =
                            if (selected) {
                                null
                            } else {
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f),
                                )
                            },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                if (date ==
                                    today
                                ) {
                                    "今天"
                                } else {
                                    shortWeekday(date)
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text("${date.month}/${date.day}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        when {
            loading -> {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            }

            error != null -> {
                item { Text(error.orEmpty(), color = MaterialTheme.colorScheme.error) }
            }

            page?.items.isNullOrEmpty() -> {
                item { Text("这一天还没有已同步的播出作品。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }

            else -> {
                items(page?.items.orEmpty(), key = { it.id.value }) { subject ->
                    Surface(
                        onClick = { onSubjectClick(subject.id.value) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f)),
                    ) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            AnimePosterArtwork(subject.poster, subject.title, subject.id, Modifier.size(72.dp, 104.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(
                                    subject.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    listOfNotNull(subject.year?.toString(), subject.type.name).joinToString(" · "),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                subject.rating?.score?.let {
                                    Text(
                                        "Bangumi $it",
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                AnimeSecondaryButton("查看作品", { onSubjectClick(subject.id.value) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shortWeekday(date: LocalDate): String =
    when (date.dayOfWeek.name) {
        "MONDAY" -> "周一"
        "TUESDAY" -> "周二"
        "WEDNESDAY" -> "周三"
        "THURSDAY" -> "周四"
        "FRIDAY" -> "周五"
        "SATURDAY" -> "周六"
        else -> "周日"
    }
