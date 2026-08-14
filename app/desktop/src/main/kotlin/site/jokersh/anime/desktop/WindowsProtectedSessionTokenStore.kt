@file:Suppress("ktlint:standard:function-naming") // JNA function names must match the Win32 ABI.

package site.jokersh.anime.desktop

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import site.jokersh.anime.data.session.SessionTokenStore
import site.jokersh.anime.data.session.StoredSessionToken
import java.util.Base64
import java.util.prefs.Preferences
import kotlin.time.Instant

internal class WindowsProtectedSessionTokenStore : SessionTokenStore {
    private val preferences = Preferences.userRoot().node("site/jokersh/anime/desktop/session")

    override fun load(): StoredSessionToken? =
        runCatching {
            val encoded = preferences.get(TOKEN_KEY, null) ?: return null
            val expiresAt = preferences.getLong(EXPIRES_AT_KEY, Long.MIN_VALUE)
            if (expiresAt == Long.MIN_VALUE) return null
            val token = unprotect(Base64.getDecoder().decode(encoded)).decodeToString()
            val refreshToken =
                preferences.get(REFRESH_TOKEN_KEY, null)?.let {
                    unprotect(Base64.getDecoder().decode(it)).decodeToString()
                }
            StoredSessionToken(token, Instant.fromEpochSeconds(expiresAt), refreshToken)
        }.getOrElse {
            clear()
            null
        }

    override fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String?,
    ) {
        val protected = protect(token.encodeToByteArray())
        preferences.put(TOKEN_KEY, Base64.getEncoder().encodeToString(protected))
        preferences.putLong(EXPIRES_AT_KEY, expiresAt.epochSeconds)
        if (refreshToken == null) {
            preferences.remove(REFRESH_TOKEN_KEY)
        } else {
            preferences.put(
                REFRESH_TOKEN_KEY,
                Base64.getEncoder().encodeToString(protect(refreshToken.encodeToByteArray())),
            )
        }
        preferences.flush()
    }

    override fun clear() {
        preferences.remove(TOKEN_KEY)
        preferences.remove(EXPIRES_AT_KEY)
        preferences.remove(REFRESH_TOKEN_KEY)
        preferences.flush()
    }

    private fun protect(value: ByteArray): ByteArray = crypt(value, decrypt = false)

    private fun unprotect(value: ByteArray): ByteArray = crypt(value, decrypt = true)

    private fun crypt(
        value: ByteArray,
        decrypt: Boolean,
    ): ByteArray {
        val inputMemory = Memory(value.size.toLong()).apply { write(0, value, 0, value.size) }
        val input = DataBlob(value.size, inputMemory)
        val output = DataBlob()
        val succeeded =
            if (decrypt) {
                Crypt32.INSTANCE.CryptUnprotectData(input, null, null, null, null, UI_FORBIDDEN, output)
            } else {
                Crypt32.INSTANCE.CryptProtectData(input, null, null, null, null, UI_FORBIDDEN, output)
            }
        check(succeeded) { "Windows DPAPI failed with error ${Native.getLastError()}" }
        return try {
            checkNotNull(output.pbData).getByteArray(0, output.cbData)
        } finally {
            output.pbData?.let { Kernel32.INSTANCE.LocalFree(it) }
        }
    }

    @Structure.FieldOrder("cbData", "pbData")
    internal class DataBlob(
        @JvmField public var cbData: Int = 0,
        @JvmField public var pbData: Pointer? = null,
    ) : Structure()

    private interface Crypt32 : Library {
        fun CryptProtectData(
            input: DataBlob,
            description: Pointer?,
            entropy: DataBlob?,
            reserved: Pointer?,
            prompt: Pointer?,
            flags: Int,
            output: DataBlob,
        ): Boolean

        fun CryptUnprotectData(
            input: DataBlob,
            description: Pointer?,
            entropy: DataBlob?,
            reserved: Pointer?,
            prompt: Pointer?,
            flags: Int,
            output: DataBlob,
        ): Boolean

        companion object {
            val INSTANCE: Crypt32 = Native.load("Crypt32", Crypt32::class.java)
        }
    }

    private interface Kernel32 : Library {
        fun LocalFree(memory: Pointer): Pointer?

        companion object {
            val INSTANCE: Kernel32 = Native.load("Kernel32", Kernel32::class.java)
        }
    }

    private companion object {
        const val UI_FORBIDDEN = 0x1
        const val TOKEN_KEY = "protected_token"
        const val EXPIRES_AT_KEY = "expires_at_epoch_seconds"
        const val REFRESH_TOKEN_KEY = "protected_refresh_token"
    }
}
