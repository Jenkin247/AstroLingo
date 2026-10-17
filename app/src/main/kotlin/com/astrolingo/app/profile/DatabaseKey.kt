// The database passphrase: random, wrapped by an Android Keystore key, never stored in the clear (ADR 0002).
package com.astrolingo.app.profile

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.ProviderException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Creates, unwraps and destroys the SQLCipher passphrase. */
class DatabaseKey(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * The passphrase, created on first use. Returns null if a wrapped passphrase exists but can't be
     * unwrapped (Keystore key lost); the caller then starts fresh and the user re-enters their details.
     */
    fun passphrase(): ByteArray? {
        val stored = prefs.getString(WRAPPED, null) ?: return create()
        return runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val key = keystore().getKey(ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, bytes, 0, GCM_IV_BYTES))
            cipher.doFinal(bytes, GCM_IV_BYTES, bytes.size - GCM_IV_BYTES)
        }.getOrNull()
    }

    /** Deletes the wrapped passphrase and the Keystore key. The old database can never be opened again. */
    fun destroy() {
        prefs.edit().remove(WRAPPED).commit()
        runCatching { keystore().deleteEntry(ALIAS) }
    }

    private fun create(): ByteArray {
        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM).apply { init(Cipher.ENCRYPT_MODE, wrappingKey()) }
        val wrapped = cipher.iv + cipher.doFinal(fresh)
        prefs.edit().putString(WRAPPED, Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit()
        return fresh
    }

    private fun wrappingKey(): SecretKey {
        (keystore().getKey(ALIAS, null) as? SecretKey)?.let { return it }
        fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_BITS)
            .apply { if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setIsStrongBoxBacked(true) }
            .build()
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        return try {
            generator.init(spec(strongBox = true))
            generator.generateKey()
        } catch (e: ProviderException) {
            // No StrongBox on this phone: the trusted environment (TEE) is used instead.
            generator.init(spec(strongBox = false))
            generator.generateKey()
        }
    }

    private fun keystore() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val PREFS = "database_key"
        const val WRAPPED = "wrapped_passphrase"
        const val ALIAS = "astrolingo_database_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val AES_GCM = "AES/GCM/NoPadding"
        const val AES_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_IV_BYTES = 12
        const val PASSPHRASE_BYTES = 32
    }
}
