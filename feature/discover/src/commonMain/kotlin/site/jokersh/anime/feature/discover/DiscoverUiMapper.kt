package site.jokersh.anime.feature.discover

import site.jokersh.anime.core.designsystem.SubjectCardUi
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

        val heroes =
            feed.sections
                .asSequence()
                .flatMap { it.subjects.asSequence() }
                .distinctBy { it.id }
                .take(5)
                .map(::subjectCard)
                .toList()
                .ifEmpty { return null }

        return DiscoverContentUi(
            heroes = heroes,
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

    internal fun subject(subject: SubjectSummary): SubjectCardUi = subjectCard(subject)

    private fun subjectCard(subject: SubjectSummary): SubjectCardUi {
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
                ?.takeIf { it.status == CollectionStatus.Watching }
                ?.watchedEpisodes,
        )
    }

    private fun sectionDescription(id: String): DiscoverSectionDescription =
        when (id) {
            "continue" -> DiscoverSectionDescription.Continue
            "airing" -> DiscoverSectionDescription.Airing
            "top-rated" -> DiscoverSectionDescription.TopRated
            "upcoming" -> DiscoverSectionDescription.Upcoming
            else -> DiscoverSectionDescription.Curated
        }
}
