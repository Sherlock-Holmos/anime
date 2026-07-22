package site.jokersh.anime.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.RefreshPolicy
import site.jokersh.anime.core.model.ResourceState
import site.jokersh.anime.data.catalog.CatalogRepository

public class DiscoverViewModel(
    private val repository: CatalogRepository,
) : ViewModel() {
    private val mutableState: MutableStateFlow<DiscoverUiState> = MutableStateFlow(DiscoverUiState())
    private val mutableEffects: MutableSharedFlow<DiscoverEffect> = MutableSharedFlow(extraBufferCapacity = 1)
    private var observationJob: Job? = null
    private var refreshJob: Job? = null

    public val state: StateFlow<DiscoverUiState> = mutableState.asStateFlow()
    public val effects: SharedFlow<DiscoverEffect> = mutableEffects.asSharedFlow()

    public fun accept(intent: DiscoverIntent) {
        when (intent) {
            DiscoverIntent.Entered -> {
                enter()
            }

            DiscoverIntent.Refresh -> {
                refresh(RefreshPolicy.Force)
            }

            DiscoverIntent.Retry -> {
                refresh(RefreshPolicy.IfMissing)
            }

            is DiscoverIntent.SubjectClicked -> {
                mutableEffects.tryEmit(DiscoverEffect.NavigateToSubject(intent.subjectId))
            }

            is DiscoverIntent.SectionMoreClicked -> {
                mutableEffects.tryEmit(DiscoverEffect.NavigateToSection(intent.sectionId))
            }
        }
    }

    private fun enter() {
        if (observationJob != null) return
        observationJob =
            viewModelScope.launch {
                repository
                    .observeDiscovery()
                    .catch { error ->
                        if (error is CancellationException) throw error
                        emit(
                            ResourceState<DiscoveryFeed>(
                                value = null,
                                freshness = null,
                                refreshing = false,
                                error = AppError.Unknown("discover-observe"),
                            ),
                        )
                    }.collect { resource ->
                        val hadContent = mutableState.value.content is AsyncContent.Content
                        mutableState.update { state -> DiscoverReducer.repositoryChanged(state, resource) }
                        if (hadContent && resource.error != null) {
                            mutableEffects.emit(DiscoverEffect.ShowMessage(DiscoverMessageUi.RefreshFailed))
                        }
                    }
            }
        refresh(RefreshPolicy.IfMissing)
    }

    private fun refresh(policy: RefreshPolicy) {
        if (refreshJob?.isActive == true) return
        refreshJob =
            viewModelScope.launch {
                val result = repository.refreshDiscovery(policy)
                if (result.isFailure && mutableState.value.content is AsyncContent.Content) {
                    mutableEffects.emit(DiscoverEffect.ShowMessage(DiscoverMessageUi.RefreshFailed))
                } else if (result.isFailure) {
                    mutableState.update { state ->
                        state.copy(content = AsyncContent.Failure(DiscoverErrorUi.Unknown), isRefreshing = false)
                    }
                }
            }
    }
}
