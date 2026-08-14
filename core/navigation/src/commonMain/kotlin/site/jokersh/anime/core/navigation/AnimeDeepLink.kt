package site.jokersh.anime.core.navigation

/** Production deep links are deliberately limited to verified HTTPS subject routes. */
public object AnimeDeepLink {
    public const val PRODUCTION_HOST: String = "anime.jokersh.site"

    public fun parse(
        rawUrl: String?,
        trustedHosts: Set<String> = setOf(PRODUCTION_HOST),
    ): AppRoute? {
        if (rawUrl.isNullOrBlank()) return null
        val match = HTTPS_URL.matchEntire(rawUrl.trim()) ?: return null
        val host = match.groupValues[1].substringBefore(':').lowercase()
        if (host !in trustedHosts.map { it.lowercase() }) return null
        val subjectId = match.groupValues[2].toLongOrNull()?.takeIf { it > 0 } ?: return null
        return AppRoute.Subject(subjectId = subjectId, origin = RouteOrigin.DeepLink)
    }

    private val HTTPS_URL =
        Regex(
            pattern = "^https://([^/?#]+)/subjects/([0-9]+)(?:[/?#].*)?$",
            option = RegexOption.IGNORE_CASE,
        )
}
