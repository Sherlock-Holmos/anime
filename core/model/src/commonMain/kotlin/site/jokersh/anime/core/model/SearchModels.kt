package site.jokersh.anime.core.model

import kotlin.time.Instant

public data class SearchRequest(
    public val query: String,
    public val types: Set<SubjectType> = emptySet(),
    public val years: IntRange? = null,
    public val airing: Set<AiringStatus> = emptySet(),
    public val sort: SearchSort = SearchSort.Relevance,
    public val cursor: Cursor? = null,
    public val pageSize: Int,
) {
    init {
        require(query == query.trim() && query.unicodeCodePointCount() in 1..100) {
            "query must be trimmed and contain 1..100 Unicode code points"
        }
        require(pageSize in 1..50) { "pageSize must be in 1..50" }
        require(years == null || years.first >= 1900) { "year range must start at 1900 or later" }
    }
}

public enum class SearchSort { Relevance, Rating, Updated }

public data class Page<T>(
    public val items: List<T>,
    public val nextCursor: Cursor?,
    public val hasMore: Boolean,
) {
    init {
        require(hasMore == (nextCursor != null)) { "hasMore and nextCursor must agree" }
    }
}

public data class SearchHistoryItem(
    public val id: String,
    public val query: String,
    public val usedAt: Instant,
)

public data class SearchSuggestion(
    public val value: String,
)
