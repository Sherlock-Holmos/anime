package site.jokersh.anime.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

public enum class SubjectType { Tv, Web, Ova, Movie, Other }

public enum class AiringStatus { Announced, Airing, Finished, Unknown }

public data class BangumiRating(
    public val score: Double?,
    public val votes: Int,
    public val distribution: Map<Int, Int>,
    public val sourceUpdatedAt: Instant?,
) {
    init {
        require(votes >= 0) { "votes must be non-negative" }
        require(score == null || score in 0.0..10.0) { "score must be null or in 0..10" }
        require(votes != 0 || score == null) { "zero-vote ratings cannot have a score" }
        require(distribution.keys.all { it in 1..10 }) { "rating buckets must be in 1..10" }
        require(distribution.values.all { it >= 0 }) { "rating bucket counts must be non-negative" }
    }
}

public data class SubjectSummary(
    public val id: SubjectId,
    public val title: String,
    public val originalTitle: String?,
    public val aliases: List<String>,
    public val poster: ImageRef?,
    public val year: Int?,
    public val type: SubjectType,
    public val airingStatus: AiringStatus,
    public val rating: BangumiRating?,
    public val collection: CollectionSnapshot?,
) {
    init {
        require(
            title == title.trim() && title.length in 1..200,
        ) { "title must be trimmed and contain 1..200 characters" }
        require(
            aliases.size <= 20 && aliases.distinct() == aliases,
        ) { "aliases must be unique and contain at most 20 values" }
        require(year == null || year >= 1900) { "year must be 1900 or later" }
    }
}

public data class SubjectDetail(
    public val summary: SubjectSummary,
    public val summaryText: String?,
    public val airDate: LocalDate?,
    public val endDate: LocalDate?,
    public val totalEpisodes: Int?,
    public val tags: List<Tag>,
    public val backdrop: ImageRef?,
    public val sourceUrl: String,
    public val dataUpdatedAt: Instant,
) {
    init {
        require(summaryText == null || summaryText.length <= 10_000) { "summaryText exceeds 10,000 characters" }
        require(totalEpisodes == null || totalEpisodes >= 0) { "totalEpisodes must be null or non-negative" }
        require(sourceUrl.startsWith("https://")) { "sourceUrl must use HTTPS" }
    }
}

public data class Tag(
    public val name: String,
    public val order: Int,
) {
    init {
        require(name.isNotBlank()) { "tag name must not be blank" }
    }
}

public enum class EpisodeType { Main, Special, Opening, Ending, Other }

public enum class EpisodeAirStatus { Unreleased, Aired, Delayed, Unknown }

public data class Episode(
    public val id: EpisodeId,
    public val subjectId: SubjectId,
    public val number: Double?,
    public val sort: Int,
    public val title: String?,
    public val originalTitle: String?,
    public val type: EpisodeType,
    public val airDate: LocalDate?,
    public val airStatus: EpisodeAirStatus,
)

public data class CharacterCredit(
    public val characterId: CharacterId,
    public val name: String,
    public val image: ImageRef?,
    public val relation: String,
    public val actors: List<PersonCredit>,
)

public data class PersonCredit(
    public val personId: PersonId,
    public val name: String,
    public val image: ImageRef?,
    public val role: String?,
)

public enum class RelationKind { Prequel, Sequel, SameWorld, Alternative, Character, Summary, Other }

public data class SubjectRelation(
    public val subject: SubjectSummary,
    public val kind: RelationKind,
    public val label: String,
)

public data class DiscoveryFeed(
    public val sections: List<DiscoverySection>,
    public val generatedAt: Instant,
)

public data class DiscoverySection(
    public val id: String,
    public val title: String,
    public val subjects: List<SubjectSummary>,
)

public data class SubjectCredits(
    public val characters: List<CharacterCredit>,
    public val persons: List<PersonCredit>,
)

public enum class SubjectSection { Episodes, Credits, Relations }
