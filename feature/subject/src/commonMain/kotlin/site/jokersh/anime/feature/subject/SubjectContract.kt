package site.jokersh.anime.feature.subject

import androidx.compose.runtime.Immutable
import site.jokersh.anime.core.model.AppError

@Immutable
public data class SubjectUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val content: SubjectContentUi? = null,
    val error: AppError? = null,
)

@Immutable
public data class SubjectContentUi(
    val id: Long,
    val title: String,
    val originalTitle: String?,
    val metadata: String,
    val status: String,
    val score: String?,
    val votes: Int,
    val episodeCount: Int?,
    val summary: String,
    val tags: List<String>,
)
