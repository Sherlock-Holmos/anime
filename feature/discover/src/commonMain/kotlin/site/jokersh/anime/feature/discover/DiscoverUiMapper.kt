package site.jokersh.anime.feature.discover

import site.jokersh.anime.core.designsystem.SubjectCardUi
import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.AppError
import site.jokersh.anime.core.model.CollectionStatus
import site.jokersh.anime.core.model.DiscoveryFeed
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.core.model.SubjectType

internal object DiscoverUiMapper {
    fun map(feed: DiscoveryFeed): DiscoverContentUi? {
        val visibleSections =
            feed.sections
                .mapNotNull { section ->
                    val subjects =
                        if (section.id == "continue") {
                            section.subjects.filter { it.collection?.status == CollectionStatus.Watching }
                        } else {
                            section.subjects
                        }
                    if (subjects.isEmpty()) {
                        null
                    } else {
                        DiscoverSectionUi(
                            id = section.id,
                            title = section.title,
                            description = sectionDescription(section.id),
                            subjects = subjects.map(::subjectCard),
                        )
                    }
                }.sortedBy { section -> if (section.id == "continue") 0 else 1 }

        val hero =
            feed.sections
                .firstOrNull { it.id == "airing" }
                ?.subjects
                ?.firstOrNull()
                ?.let(::subjectCard)
                ?: visibleSections.firstOrNull()?.subjects?.firstOrNull()
                ?: return null

        return DiscoverContentUi(
            hero = hero,
            sections = visibleSections,
        )
    }

    fun error(error: AppError): DiscoverErrorUi =
        when (error) {
            AppError.Offline -> DiscoverErrorUi.Offline

            is AppError.RateLimited -> DiscoverErrorUi.RateLimited

            is AppError.Server,
            is AppError.Upstream,
            AppError.Timeout,
            -> DiscoverErrorUi.ServiceUnavailable

            is AppError.Data -> DiscoverErrorUi.InvalidData

            else -> DiscoverErrorUi.Unknown
        }

    private fun subjectCard(subject: SubjectSummary): SubjectCardUi {
        val metadata =
            listOfNotNull(
                subject.year?.toString(),
                subject.type.label,
                subject.airingStatus.label,
            ).joinToString(" · ")
        val subjectRating = subject.rating
        val rating = subjectRating?.score?.let { score -> "$score  Bangumi" }
        val collectionLabel =
            subject.collection
                ?.takeIf { it.status == CollectionStatus.Watching }
                ?.let { "已看 ${it.watchedEpisodes} 集" }
        val accessibility =
            buildList {
                add(subject.title)
                add(metadata)
                collectionLabel?.let(::add)
                subjectRating?.score?.let { score ->
                    add("Bangumi 评分 $score 分，${subjectRating.votes} 人评分")
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

    private fun sectionDescription(id: String): String =
        when (id) {
            "continue" -> "从上次停下的地方继续"
            "airing" -> "这一刻，大家正在追"
            "top-rated" -> "来自 Bangumi 的高口碑作品"
            "upcoming" -> "值得提前留意的新作"
            else -> "为你整理的作品"
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
