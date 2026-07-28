package site.jokersh.anime.feature.search

import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.SearchHistoryItem
import site.jokersh.anime.core.model.SearchSuggestion
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.core.model.SubjectType

internal object SearchUiMapper {
    fun history(item: SearchHistoryItem): SearchHistoryUi = SearchHistoryUi(id = item.id, query = item.query)

    fun suggestion(item: SearchSuggestion): SearchSuggestionUi = SearchSuggestionUi(item.value)

    fun subject(subject: SubjectSummary): SubjectCardUi {
        val metadata =
            listOfNotNull(
                subject.year?.toString(),
                subject.type.label,
                subject.airingStatus.label,
            ).joinToString(" · ")
        val bangumiRating = subject.rating
        val rating = bangumiRating?.score?.let { score -> "$score  Bangumi" }
        val collectionLabel =
            subject.collection
                ?.takeIf { collection -> collection.status == CollectionStatus.Watching }
                ?.let { collection -> "已看 ${collection.watchedEpisodes} 集" }
        val accessibility =
            buildList {
                add(subject.title)
                add(metadata)
                collectionLabel?.let(::add)
                bangumiRating?.score?.let { score ->
                    add("Bangumi 评分 $score 分，${bangumiRating.votes} 人评分")
                }
            }.joinToString("，")

        return SubjectCardUi(
            id = subject.id,
            title = subject.title,
            originalTitle = subject.originalTitle,
            poster = subject.poster,
            metadata = metadata,
            rating = rating,
            collectionLabel = collectionLabel,
            accessibilityLabel = accessibility,
        )
    }
}

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
