package site.jokersh.anime.app

import site.jokersh.anime.core.model.AiringStatus
import site.jokersh.anime.core.model.CollectionItem
import site.jokersh.anime.core.model.CollectionSnapshot
import site.jokersh.anime.core.model.ImageRef
import site.jokersh.anime.core.model.SubjectSummary
import site.jokersh.anime.data.session.SessionRepository
import kotlin.time.Clock
import kotlin.time.Instant

/** Loads every collection page so the offline-first collection store has a complete server snapshot. */
public suspend fun SessionRepository.loadAllCollectionItems(): Result<List<CollectionItem>> {
    val items = mutableListOf<CollectionItem>()
    val seenCursors = mutableSetOf<String>()
    var cursor: String? = null
    do {
        val page = collectionPage(status = null, cursor = cursor, limit = 50).getOrElse { return Result.failure(it) }
        items += page.items.map { it.toCollectionItem() }
        cursor = page.nextCursor?.takeIf(seenCursors::add)
    } while (cursor != null)
    return Result.success(items)
}

private fun site.jokersh.anime.core.model.UserCollectionSummary.toCollectionItem(): CollectionItem {
    val subjectId = subjectId
    val updatedAt = runCatching { Instant.parse(updatedAt) }.getOrDefault(Clock.System.now())
    val snapshot =
        CollectionSnapshot(
            subjectId = subjectId,
            status = status,
            watchedEpisodes = episodeProgress,
            note = comment.takeIf(String::isNotBlank),
            updatedAt = updatedAt,
            sync = site.jokersh.anime.core.model.SyncState(site.jokersh.anime.core.model.SyncPhase.Synced),
        )
    val subject =
        SubjectSummary(
            id = subjectId,
            title = title.trim().ifBlank { "未命名作品" },
            originalTitle = originalTitle.trim().takeIf(String::isNotBlank),
            aliases = emptyList(),
            poster = posterUrl?.takeIf(String::isNotBlank)?.let { ImageRef.Remote(it, "collection-poster-${subjectId.value}") },
            year = airDate?.take(4)?.toIntOrNull()?.takeIf { it >= 1900 },
            type = site.jokersh.anime.core.model.SubjectType.Other,
            airingStatus = AiringStatus.Unknown,
            rating = null,
            collection = snapshot,
        )
    return CollectionItem(subject, snapshot)
}
