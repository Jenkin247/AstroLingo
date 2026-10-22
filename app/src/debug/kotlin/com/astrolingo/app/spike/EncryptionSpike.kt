// Sprint 1 spike: proves the encryption design (ADR 0002 and 0003) on a real phone before sign-in is built.
// Debug builds only. Uses a fictional profile; no real birth data.
package com.astrolingo.app.spike

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.DeleteBytesRequest
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData
import com.google.android.gms.tasks.Task
import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.TinkProtoKeysetFormat
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import java.io.File
import java.security.KeyStore
import java.security.ProviderException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import net.zetetic.database.sqlcipher.SQLiteDatabase

/** One line of the spike report. */
data class SpikeCheck(val name: String, val passed: Boolean, val detail: String)

/** Runs the spike checks. Each check catches its own failure so the report always completes. */
class EncryptionSpike(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val blockstore = Blockstore.getClient(context)

    /** Fresh run on this install: Keystore, SQLCipher, Tink and storing the key in Block Store. */
    suspend fun runAll(): List<SpikeCheck> = buildList {
        add(deviceInfo())
        val wrapKey = attempt("Keystore key created") { keystoreKey() }
        add(wrapKey.first)
        wrapKey.second?.let { key ->
            add(attempt("Key is in secure hardware") { secureHardwareLevel(key) }.first)
            add(attempt("Database opens with SQLCipher and wrapped passphrase") { sqlCipherRoundTrip(key) }.first)
        }
        add(attempt("Database file is unreadable without the key") { databaseHeaderIsEncrypted() }.first)
        add(attempt("Block Store end-to-end encryption available") { blockstoreE2e() }.first)
        add(attempt("Tink encrypts profile and keyset is saved to Block Store") { storeInBlockstore() }.first)
    }

    /** After reinstalling, or on a new phone after restore: can we read the profile back? */
    suspend fun restoreCheck(): List<SpikeCheck> = buildList {
        add(deviceInfo())
        add(
            SpikeCheck(
                "Data was created on this install",
                passed = true,
                detail = if (prefs.getBoolean(CREATED_HERE, false)) "yes" else "no, so a pass below means restore worked",
            ),
        )
        add(attempt("Profile decrypts with the key from Block Store") { restoreFromBlockstore() }.first)
    }

    /** Removes everything the spike stored, as Delete everything will. */
    suspend fun clearAll(): List<SpikeCheck> = listOf(
        attempt("Block Store entries deleted") {
            val request = DeleteBytesRequest.Builder().setKeys(listOf(KEYSET_LABEL, CIPHERTEXT_LABEL)).build()
            "deleted: ${blockstore.deleteBytes(request).await()}"
        }.first,
        attempt("Local data deleted") {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
            context.deleteDatabase(DB_NAME)
            prefs.edit().clear().commit()
            "Keystore key, database and preferences removed"
        }.first,
    )

    private fun deviceInfo() = SpikeCheck(
        "Device",
        passed = true,
        detail = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    )

    private fun keystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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
            // StrongBox missing on this phone: fall back to the TEE.
            generator.init(spec(strongBox = false))
            generator.generateKey()
        }
    }

    private fun secureHardwareLevel(key: SecretKey): String {
        val info = SecretKeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE).getKeySpec(key, KeyInfo::class.java) as KeyInfo
        val level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when (info.securityLevel) {
                KeyProperties.SECURITY_LEVEL_STRONGBOX -> "StrongBox"
                KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "trusted environment (TEE)"
                else -> error("software only")
            }
        } else {
            @Suppress("DEPRECATION")
            if (info.isInsideSecureHardware) "secure hardware" else error("software only")
        }
        return level
    }

    private fun passphrase(key: SecretKey): ByteArray {
        prefs.getString(WRAPPED_PASSPHRASE, null)?.let { stored ->
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, bytes, 0, GCM_IV_BYTES))
            return cipher.doFinal(bytes, GCM_IV_BYTES, bytes.size - GCM_IV_BYTES)
        }
        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM).apply { init(Cipher.ENCRYPT_MODE, key) }
        val wrapped = cipher.iv + cipher.doFinal(fresh)
        prefs.edit().putString(WRAPPED_PASSPHRASE, Base64.encodeToString(wrapped, Base64.NO_WRAP)).commit()
        return fresh
    }

    private fun sqlCipherRoundTrip(key: SecretKey): String {
        System.loadLibrary("sqlcipher")
        val file = context.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openDatabase(
            file.path, passphrase(key), null, SQLiteDatabase.CREATE_IF_NECESSARY, null, null,
        )
        db.use {
            it.execSQL("CREATE TABLE IF NOT EXISTS profile (id INTEGER PRIMARY KEY, body TEXT NOT NULL)")
            it.execSQL("INSERT OR REPLACE INTO profile (id, body) VALUES (1, ?)", arrayOf(FICTIONAL_PROFILE))
            it.rawQuery("SELECT body FROM profile WHERE id = 1", null).use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == FICTIONAL_PROFILE) { "row did not read back" }
            }
        }
        return "wrote and read a fictional profile"
    }

    private fun databaseHeaderIsEncrypted(): String {
        val file: File = context.getDatabasePath(DB_NAME)
        check(file.exists()) { "no database file" }
        val header = file.inputStream().use { input -> ByteArray(SQLITE_HEADER.length).also { input.read(it) } }
        check(String(header, Charsets.US_ASCII) != SQLITE_HEADER) { "plain SQLite header found" }
        val text = file.readBytes().toString(Charsets.ISO_8859_1)
        check(!text.contains("Testwal")) { "profile text visible in file" }
        return "no SQLite header and no readable profile text"
    }

    private suspend fun blockstoreE2e(): String {
        check(blockstore.isEndToEndEncryptionAvailable().await()) {
            "not available: turn on Google backup and a screen lock, then run again"
        }
        return "yes, keys will be backed up end to end"
    }

    private suspend fun storeInBlockstore(): String {
        AeadConfig.register()
        val handle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
        val aead = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val ciphertext = aead.encrypt(FICTIONAL_PROFILE.toByteArray(), ASSOCIATED_DATA)
        val keyset = TinkProtoKeysetFormat.serializeKeyset(handle, InsecureSecretKeyAccess.get())
        // In the app, the ciphertext goes to Firestore; the spike keeps it in Block Store to test without a backend.
        store(KEYSET_LABEL, keyset)
        store(CIPHERTEXT_LABEL, ciphertext)
        prefs.edit().putBoolean(CREATED_HERE, true).commit()
        return "keyset ${keyset.size} bytes, ciphertext ${ciphertext.size} bytes, backup to cloud requested"
    }

    private suspend fun store(label: String, bytes: ByteArray) {
        val data = StoreBytesData.Builder().setKey(label).setBytes(bytes).setShouldBackupToCloud(true).build()
        blockstore.storeBytes(data).await()
    }

    private suspend fun restoreFromBlockstore(): String {
        AeadConfig.register()
        val request = RetrieveBytesRequest.Builder().setKeys(listOf(KEYSET_LABEL, CIPHERTEXT_LABEL)).build()
        val found = blockstore.retrieveBytes(request).await().blockstoreDataMap
        val keyset = checkNotNull(found[KEYSET_LABEL]) { "no key in Block Store: user would re-enter birth details" }.bytes
        val ciphertext = checkNotNull(found[CIPHERTEXT_LABEL]) { "no ciphertext in Block Store" }.bytes
        val handle = TinkProtoKeysetFormat.parseKeyset(keyset, InsecureSecretKeyAccess.get())
        val aead = handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        val plain = String(aead.decrypt(ciphertext, ASSOCIATED_DATA))
        check(plain == FICTIONAL_PROFILE) { "decrypted text differs" }
        return "decrypted the fictional profile"
    }

    private inline fun <T> attempt(name: String, block: () -> T): Pair<SpikeCheck, T?> = try {
        val value = block()
        SpikeCheck(name, passed = true, detail = value.toString()) to value
    } catch (e: Exception) {
        SpikeCheck(name, passed = false, detail = "${e.javaClass.simpleName}: ${e.message}") to null
    }

    private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }

    private companion object {
        const val PREFS = "encryption_spike"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "astrolingo_spike_db_wrap"
        const val AES_GCM = "AES/GCM/NoPadding"
        const val AES_BITS = 256
        const val GCM_TAG_BITS = 128
        const val GCM_IV_BYTES = 12
        const val PASSPHRASE_BYTES = 32
        const val WRAPPED_PASSPHRASE = "wrapped_passphrase"
        const val CREATED_HERE = "created_here"
        const val DB_NAME = "spike.db"
        const val SQLITE_HEADER = "SQLite format 3"
        const val KEYSET_LABEL = "astrolingo_spike_keyset"
        const val CIPHERTEXT_LABEL = "astrolingo_spike_ciphertext"
        const val FICTIONAL_PROFILE = "Asha Testwal, 1990-08-15 06:30, Pune (fictional)"
        val ASSOCIATED_DATA = "spike-user".toByteArray()
    }
}
