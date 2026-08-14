package site.jokersh.anime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import site.jokersh.anime.data.session.SessionTokenStore
import site.jokersh.anime.data.session.StoredSessionToken
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.time.Instant

internal class AndroidEncryptedSessionTokenStore(
    context: Context,
) : SessionTokenStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun load(): StoredSessionToken? =
        runCatching {
            val encrypted = preferences.getString(KEY_TOKEN, null) ?: return null
            val separator = encrypted.indexOf(':')
            require(separator > 0)
            val initializationVector = Base64.decode(encrypted.substring(0, separator), Base64.NO_WRAP)
            val ciphertext = Base64.decode(encrypted.substring(separator + 1), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, initializationVector))
            val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)?.let(::decrypt)
            StoredSessionToken(
                token = cipher.doFinal(ciphertext).decodeToString(),
                expiresAt = Instant.fromEpochSeconds(preferences.getLong(KEY_EXPIRES_AT, 0L)),
                refreshToken = refreshToken,
            )
        }.getOrElse {
            clear()
            null
        }

    override fun save(
        token: String,
        expiresAt: Instant,
        refreshToken: String?,
    ) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted =
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
                Base64.encodeToString(cipher.doFinal(token.encodeToByteArray()), Base64.NO_WRAP)
        preferences.edit {
            putString(KEY_TOKEN, encrypted)
            putLong(KEY_EXPIRES_AT, expiresAt.epochSeconds)
            if (refreshToken == null) {
                remove(KEY_REFRESH_TOKEN)
            } else {
                putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
            }
        }
    }

    override fun clear() {
        preferences.edit { clear() }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(value.encodeToByteArray()), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val separator = value.indexOf(':')
        require(separator > 0)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(128, Base64.decode(value.substring(0, separator), Base64.NO_WRAP)),
        )
        return cipher.doFinal(Base64.decode(value.substring(separator + 1), Base64.NO_WRAP)).decodeToString()
    }

    private companion object {
        const val PREFERENCES_NAME = "anime_session"
        const val KEY_TOKEN = "token"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ALIAS = "anime_session_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
