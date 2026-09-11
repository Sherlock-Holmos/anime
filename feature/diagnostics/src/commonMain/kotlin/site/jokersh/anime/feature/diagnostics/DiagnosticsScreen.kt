package site.jokersh.anime.feature.diagnostics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import site.jokersh.anime.core.designsystem.AnimeBackIcon
import site.jokersh.anime.core.designsystem.AnimeSecondaryButton
import site.jokersh.anime.data.session.ServiceDiagnostic
import site.jokersh.anime.data.session.SessionRepository

@Composable
public fun DiagnosticsScreen(
    repository: SessionRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var diagnostics by remember { mutableStateOf<List<ServiceDiagnostic>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(repository, reload) {
        error = null
        repository.diagnostics()
            .onSuccess { diagnostics = it }
            .onFailure { error = it.message ?: "诊断请求失败" }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 20.dp, 24.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Surface(onClick = onBack, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                    AnimeBackIcon(MaterialTheme.colorScheme.onSurface, Modifier.padding(4.dp))
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("服务诊断", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("检查客户端到 Anime 后端的连通性与服务状态。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (diagnostics == null && error == null) {
            item { Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        error?.let { message ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    AnimeSecondaryButton("重试", onClick = { diagnostics = null; reload++ })
                }
            }
        }
        diagnostics?.let { items ->
            items(items, key = { it.endpoint }) { diagnostic ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .25f)),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(diagnostic.endpoint, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (diagnostic.healthy) "正常 · HTTP ${diagnostic.statusCode}" else "异常 · HTTP ${diagnostic.statusCode ?: "无法连接"}",
                            color = if (diagnostic.healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        if (diagnostic.body.isNotBlank()) {
                            Text(diagnostic.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { AnimeSecondaryButton("重新检查", onClick = { diagnostics = null; reload++ }) }
        }
    }
}
