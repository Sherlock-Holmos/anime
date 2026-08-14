package site.jokersh.anime.core.model

import kotlin.jvm.JvmInline

@JvmInline
public value class SubjectId(
    public val value: Long,
) {
    init {
        require(value > 0) { "SubjectId must be positive" }
    }
}

@JvmInline
public value class EpisodeId(
    public val value: Long,
) {
    init {
        require(value > 0) { "EpisodeId must be positive" }
    }
}

@JvmInline
public value class CharacterId(
    public val value: Long,
) {
    init {
        require(value > 0) { "CharacterId must be positive" }
    }
}

@JvmInline
public value class PersonId(
    public val value: Long,
) {
    init {
        require(value > 0) { "PersonId must be positive" }
    }
}

@JvmInline
public value class CommentId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "CommentId must not be blank" }
    }
}

@JvmInline
public value class UserId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank" }
    }
}

@JvmInline
public value class RatingId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "RatingId must not be blank" }
    }
}

@JvmInline
public value class ReviewId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "ReviewId must not be blank" }
    }
}

@JvmInline
public value class CuratedListId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "CuratedListId must not be blank" }
    }
}

@JvmInline
public value class ActivityId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "ActivityId must not be blank" }
    }
}

@JvmInline
public value class MutationId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "MutationId must not be blank" }
    }
}

@JvmInline
public value class Cursor(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "Cursor must not be blank" }
    }
}

public sealed interface ImageRef {
    public data class Remote(
        public val url: String,
        public val cacheKey: String,
    ) : ImageRef {
        init {
            require(
                url.startsWith("https://") ||
                    url.startsWith("http://127.0.0.1") ||
                    url.startsWith("http://localhost"),
            ) { "Remote images must use HTTPS except for loopback development servers" }
            require(cacheKey.isNotBlank()) { "Image cacheKey must not be blank" }
        }
    }

    public data class Resource(
        public val path: String,
    ) : ImageRef {
        init {
            require(path.isNotBlank() && ".." !in path) { "Resource image path is invalid" }
        }
    }
}

internal fun String.unicodeCodePointCount(): Int {
    var count = 0
    var index = 0
    while (index < length) {
        val current = this[index].code
        val isHighSurrogate = current in 0xD800..0xDBFF
        val hasLowSurrogate =
            index + 1 < length && this[index + 1].code in 0xDC00..0xDFFF
        index += if (isHighSurrogate && hasLowSurrogate) 2 else 1
        count += 1
    }
    return count
}
