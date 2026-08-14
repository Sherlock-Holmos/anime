package site.jokersh.anime

import android.content.Context
import androidx.core.content.edit
import site.jokersh.anime.data.catalog.CatalogCacheStore
import site.jokersh.anime.data.collection.CollectionStore
import site.jokersh.anime.data.comment.CommentDraftStore
import site.jokersh.anime.data.comment.RatingOutboxStore
import site.jokersh.anime.data.settings.SettingsStore

internal class AndroidSettingsStore(
    context: Context,
) : SettingsStore {
    private val preferences = context.getSharedPreferences("anime_settings", Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(
        key: String,
        value: String,
    ) {
        preferences.edit { putString(key, value) }
    }

    override fun remove(key: String) {
        preferences.edit { remove(key) }
    }
}

internal class AndroidCommentDraftStore(
    context: Context,
) : CommentDraftStore {
    private val preferences = context.getSharedPreferences("anime_comment_drafts", Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString("drafts", null)

    override fun write(value: String) {
        preferences.edit { putString("drafts", value) }
    }
}

internal class AndroidCollectionStore(
    context: Context,
) : CollectionStore {
    private val preferences = context.getSharedPreferences("anime_collection", Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString("snapshots", null)

    override fun write(value: String) {
        preferences.edit { putString("snapshots", value) }
    }
}

internal class AndroidCatalogCacheStore(
    context: Context,
) : CatalogCacheStore {
    private val preferences = context.getSharedPreferences("anime_catalog_cache", Context.MODE_PRIVATE)

    override fun read(key: String): String? = preferences.getString(key, null)

    override fun write(
        key: String,
        value: String,
    ) {
        preferences.edit { putString(key, value) }
    }
}

internal class AndroidRatingOutboxStore(
    context: Context,
) : RatingOutboxStore {
    private val preferences = context.getSharedPreferences("anime_rating_outbox", Context.MODE_PRIVATE)

    override fun read(): String? = preferences.getString("pending", null)

    override fun write(value: String) {
        preferences.edit { putString("pending", value) }
    }
}
