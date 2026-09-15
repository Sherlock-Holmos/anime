package site.jokersh.anime.feature.search

import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.SearchHistoryItem
import site.jokersh.anime.core.model.SearchSuggestion
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.core.model.SubjectType

internal object SearchUiMapper {
    fun history(item: SearchHistoryItem): SearchHistoryUi = SearchHistoryUi(id = item.id, query = item.query)

    fun suggestion(item: SearchSuggestion): SearchSuggestionUi = SearchSuggestionUi(item.value)

    fun subject(subject: SubjectSummary): SubjectCardUi {
        return SubjectCardUi(
            id = subject.id,
            title = subject.title,
            originalTitle = subject.originalTitle,
            poster = subject.poster,
            metadata = "",
            rating = null,
            collectionLabel = null,
            accessibilityLabel = subject.title,
            year = subject.year,
            subjectType = subject.type,
            airingStatus = subject.airingStatus,
            ratingScore = subject.rating?.score,
            ratingVotes = subject.rating?.votes,
            watchedEpisodes = subject.collection
                ?.takeIf { collection -> collection.status == CollectionStatus.Watching }
                ?.watchedEpisodes,
        )
    }
}
