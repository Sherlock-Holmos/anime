package site.jokersh.anime.feature.discover

import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.FreshnessKind
import site.jokersh.anime.core.model.ResourceState

internal object DiscoverReducer {
    fun repositoryChanged(
        previous: DiscoverUiState,
        resource: ResourceState<DiscoveryFeed>,
    ): DiscoverUiState {
        val error = resource.error
        val mappedContent = resource.value?.let(DiscoverUiMapper::map)
        val content =
            when {
                mappedContent != null -> AsyncContent.Content(mappedContent)
                resource.value != null -> AsyncContent.Empty
                error != null -> AsyncContent.Failure(DiscoverUiMapper.error(error))
                resource.refreshing -> AsyncContent.Loading
                else -> previous.content
            }
        return DiscoverUiState(
            content = content,
            isRefreshing = mappedContent != null && resource.refreshing,
            isOffline =
                error == AppError.Offline ||
                    resource.freshness?.kind == FreshnessKind.OfflineCache,
            lastUpdatedLabel =
                resource.freshness
                    ?.updatedAt
                    ?.toString()
                    ?.takeIf { it.length >= 16 }
                    ?.substring(11, 16),
        )
    }
}
