package site.jokersh.anime.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.core.model.SubjectDetail
import site.jokersh.anime.core.model.SubjectId
import site.jokersh.anime.core.model.SubjectType
import site.jokersh.anime.data.catalog.CatalogRepository
import site.jokersh.anime.data.comment.CommunityRepository
import site.jokersh.anime.data.session.SessionRepository

public class SubjectViewModel(
    private val subjectId: SubjectId,
    private val repository: CatalogRepository,
    private val communityRepository: CommunityRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SubjectUiState())
    private var refreshJob: Job? = null

    public val state: StateFlow<SubjectUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository
                .observeSubject(subjectId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(ResourceState(null, null, false, AppError.Unknown("subject-observe")))
                }.collect { resource ->
                    mutableState.update { state ->
                        state.copy(
                            loading = resource.value == null && resource.error == null,
                            refreshing = resource.refreshing,
                            content = resource.value?.toUi(resource.freshness?.kind) ?: state.content,
                            error = resource.error,
                            collectionStatus =
                                resource.value
                                    ?.summary
                                    ?.collection
                                    ?.status
                                    ?.name
                                    ?.lowercase()
                                    ?: state.collectionStatus,
                        )
                    }
                }
        }
        refresh(RefreshPolicy.IfMissing)
        refreshCommunity()
    }

    public fun retry() {
        refresh(RefreshPolicy.Force)
        refreshCommunity()
    }

    public fun collect() {
        viewModelScope.launch {
            mutableState.update { it.copy(actionMessage = "正在同步收藏…") }
            communityRepository
                .setCollection(subjectId.value, "wish")
                .onSuccess {
                    sessionRepository.refresh()
                    mutableState.update { it.copy(collectionStatus = "wish", actionMessage = "已加入想看") }
                }.onFailure { error -> mutableState.update { it.copy(actionMessage = error.message ?: "收藏失败") } }
        }
    }

    private fun refreshCommunity() {
        viewModelScope.launch {
            mutableState.update { it.copy(communityLoading = true, communityError = null) }
            val reviews =
                communityRepository.reviews(subjectId.value).getOrElse { error ->
                    mutableState.update { it.copy(communityLoading = false, communityError = error.message) }
                    emptyList()
                }
            val comments = communityRepository.comments(subjectId.value).getOrDefault(emptyList())
            val lists = communityRepository.lists(5).getOrDefault(emptyList())
            mutableState.update {
                it.copy(
                    communityLoading = false,
                    reviews = reviews,
                    comments = comments,
                    lists = lists,
                )
            }
        }
    }

    private fun refresh(policy: RefreshPolicy) {
        if (refreshJob?.isActive == true) return
        refreshJob =
            viewModelScope.launch {
                val result = repository.refreshSubject(subjectId, policy)
                if (result.isFailure && mutableState.value.content == null) {
                    mutableState.update {
                        it.copy(loading = false, error = AppError.Unknown("subject-refresh"))
                    }
                }
            }
    }
}

private fun SubjectDetail.toUi(freshnessKind: FreshnessKind?): SubjectContentUi =
    SubjectContentUi(
        id = summary.id.value,
        title = summary.title,
        originalTitle = summary.originalTitle,
        poster = summary.poster,
        metadata =
            listOfNotNull(
                summary.year?.toString(),
                summary.type.label,
                summary.airingStatus.label,
            ).joinToString(" · "),
        status = summary.airingStatus.label,
        score = summary.rating?.score?.toString(),
        votes = summary.rating?.votes ?: 0,
        episodeCount = totalEpisodes,
        summary = summaryText.orEmpty(),
        tags = tags.sortedBy { it.order }.map { it.name },
        ratingDistribution = summary.rating?.distribution.orEmpty(),
        dataStatusLabel =
            when (freshnessKind) {
                FreshnessKind.OfflineCache -> "离线缓存 · 更新于 ${dataUpdatedAt.toString().take(16).replace('T', ' ')}"
                FreshnessKind.Stale -> "数据可能已过期 · 更新于 ${dataUpdatedAt.toString().take(16).replace('T', ' ')}"
                else -> "Bangumi 最近同步 · ${dataUpdatedAt.toString().take(16).replace('T', ' ')}"
            },
    )

private val SubjectType.label: String
    get() =
        when (this) {
            SubjectType.Tv -> "TV"
            SubjectType.Web -> "Web"
            SubjectType.Ova -> "OVA"
            SubjectType.Movie -> "剧场版"
            SubjectType.Other -> "其他"
        }

private val AiringStatus.label: String
    get() =
        when (this) {
            AiringStatus.Announced -> "未开播"
            AiringStatus.Airing -> "连载中"
            AiringStatus.Finished -> "已完结"
            AiringStatus.Unknown -> "状态未知"
        }
