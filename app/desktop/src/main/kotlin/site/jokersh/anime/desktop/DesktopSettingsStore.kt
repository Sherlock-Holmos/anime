package site.jokersh.anime.desktop

import site.jokersh.anime.data.catalog.CatalogCacheStore
import site.jokersh.anime.data.collection.CollectionStore
import site.jokersh.anime.data.comment.CommentDraftStore
import site.jokersh.anime.data.comment.RatingOutboxStore
import site.jokersh.anime.data.settings.SettingsStore
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.prefs.Preferences

internal class DesktopSettingsStore : SettingsStore {
    private val preferences = Preferences.userRoot().node("site/jokersh/anime/desktop/settings")

    override fun read(key: String): String? = preferences.get(key, null)

    override fun write(
        key: String,
        value: String,
    ) = preferences.put(key, value)

    override fun remove(key: String) = preferences.remove(key)
}

internal class DesktopCommentDraftStore : CommentDraftStore {
    private val preferences = Preferences.userRoot().node("site/jokersh/anime/desktop/comments")

    override fun read(): String? = preferences.get("drafts", null)

    override fun write(value: String) = preferences.put("drafts", value)
}

internal class DesktopCollectionStore : CollectionStore {
    private val preferences = Preferences.userRoot().node("site/jokersh/anime/desktop/collection")

    override fun read(): String? = preferences.get("snapshots", null)

    override fun write(value: String) = preferences.put("snapshots", value)
}

internal class DesktopCatalogCacheStore(
    private val directory: Path = Path.of(System.getProperty("user.home"), ".anime", "catalog-cache"),
) : CatalogCacheStore {
    override fun read(key: String): String? = runCatching { Files.readString(pathFor(key)) }.getOrNull()

    override fun write(
        key: String,
        value: String,
    ) {
        runCatching {
            Files.createDirectories(directory)
            val target = pathFor(key)
            val temporary = directory.resolve("${target.fileName}.tmp")
            Files.writeString(temporary, value)
            runCatching {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun pathFor(key: String): Path = directory.resolve("${key.replace(Regex("[^A-Za-z0-9._-]"), "_")}.json")
}

internal class DesktopRatingOutboxStore : RatingOutboxStore {
    private val preferences = Preferences.userRoot().node("site/jokersh/anime/desktop/ratings")

    override fun read(): String? = preferences.get("outbox", null)

    override fun write(value: String) = preferences.put("outbox", value)
}
