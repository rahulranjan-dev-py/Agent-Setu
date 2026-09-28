package app.agentsetu.data.db

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the SQLCipher passphrase. A random passphrase is generated once, encrypted with an
 * AES-256-GCM key that never leaves the Android Keystore, and only the encrypted form is stored.
 * App data is excluded from Android backup, so the pair can never be restored onto another phone;
 * moving data between phones goes through the user's own password-protected backup file.
 */
/** The stored passphrase can no longer be unwrapped on this phone; the data cannot be read. */
class DatabaseKeyUnavailableException(cause: Throwable?) :
    IllegalStateException("The database key cannot be recovered on this phone", cause)

@Singleton
class DatabaseKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * False when a wrapped passphrase exists but can no longer be opened (Keystore key lost after a
     * system repair, or the prefs file damaged). The database is then unreadable for good; the app
     * shows a screen offering to erase and restore from a backup instead of crashing at start-up.
     */
    fun isUsable(): Boolean = try {
        passphrase()
        true
    } catch (e: DatabaseKeyUnavailableException) {
        false
    }

    fun passphrase(): ByteArray {
        val stored = prefs.getString(KEY_WRAPPED, null)
        if (stored != null) {
            // Never generate a new Keystore key here: it could not open the stored passphrase anyway,
            // and it would hide the real problem behind a decryption error.
            val key = existingKey() ?: throw DatabaseKeyUnavailableException(null)
            return try {
                unwrap(stored, key)
            } catch (e: Exception) {
                throw DatabaseKeyUnavailableException(e)
            }
        }

        val raw = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        // Hex text, so the passphrase contains no zero bytes for the native layer to trip on.
        val passphrase = raw.joinToString("") { "%02x".format(it) }.toByteArray(Charsets.US_ASCII)
        check(prefs.edit().putString(KEY_WRAPPED, wrap(passphrase)).commit()) {
            "Could not store database key"
        }
        return passphrase
    }

    private fun wrap(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey())
        val encrypted = cipher.doFinal(plain)
        return encode(cipher.iv) + SEPARATOR + encode(encrypted)
    }

    private fun unwrap(stored: String, key: SecretKey): ByteArray {
        val parts = stored.split(SEPARATOR)
        require(parts.size == 2) { "Malformed wrapped key" }
        val (iv, encrypted) = parts.map { Base64.decode(it, Base64.NO_WRAP) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(encrypted)
    }

    private fun existingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun keystoreKey(): SecretKey {
        existingKey()?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "agentsetu_db_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PASSPHRASE_BYTES = 32
        const val PREFS_NAME = "db_key"
        const val KEY_WRAPPED = "wrapped_passphrase"
        const val SEPARATOR = ":"
    }
}
