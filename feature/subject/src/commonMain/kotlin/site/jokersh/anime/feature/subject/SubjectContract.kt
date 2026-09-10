package site.jokersh.anime.feature.subject

import androidx.compose.runtime.Immutable
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.data.comment.CommunityComment
import site.jokersh.anime.data.comment.CommunityListSummary
import site.jokersh.anime.data.comment.CommunityReview

@Immutable
public data class SubjectUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val content: SubjectContentUi? = null,
    val error: AppError? = null,
    val communityLoading: Boolean = true,
    val reviews: List<CommunityReview> = emptyList(),
    val comments: List<CommunityComment> = emptyList(),
    val lists: List<CommunityListSummary> = emptyList(),
    val communityError: String? = null,
    val collectionStatus: String? = null,
    val actionMessage: String? = null,
)

@Immutable
public data class SubjectContentUi(
    val id: Long,
    val title: String,
    val originalTitle: String?,
    val poster: ImageRef?,
    val metadata: String,
    val status: String,
    val score: String?,
    val votes: Int,
    val episodeCount: Int?,
    val summary: String,
    val tags: List<String>,
    val ratingDistribution: Map<Int, Int>,
    val dataStatusLabel: String,
)
