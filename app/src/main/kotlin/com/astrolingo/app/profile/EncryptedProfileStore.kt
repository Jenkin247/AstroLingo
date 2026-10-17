// The profile in a SQLCipher database (AES-256) keyed by DatabaseKey (ADR 0002).
// One key/value table for now; Room comes in when lesson progress needs real tables.
package com.astrolingo.app.profile

import android.content.ContentValues
import android.content.Context
import com.astrolingo.app.onboarding.OnboardingAnswers
import net.zetetic.database.sqlcipher.SQLiteDatabase

/** [ProfileRepository] backed by an encrypted database in app-private storage, excluded from backup. */
class EncryptedProfileStore(context: Context) : ProfileRepository {
    private val appContext = context.applicationContext
    private val key = DatabaseKey(appContext)

    override fun load(): OnboardingAnswers? {
        if (!appContext.getDatabasePath(DB_NAME).exists()) return null
        val rows = withDatabase { db ->
            db.rawQuery("SELECT key, value FROM $TABLE", null).use { cursor ->
                buildMap { while (cursor.moveToNext()) put(cursor.getString(0), cursor.getString(1)) }
            }
        }
        return rows?.let(ProfileCodec::decode)
    }

    override fun save(answers: OnboardingAnswers) {
        // A second try covers the case where the first found an unreadable database and started fresh.
        if (write(answers) == null) checkNotNull(write(answers)) { "profile could not be saved" }
    }

    private fun write(answers: OnboardingAnswers): Unit? =
        withDatabase { db ->
            db.beginTransaction()
            try {
                db.delete(TABLE, null, null)
                ProfileCodec.encode(answers).forEach { (k, v) ->
                    db.insert(TABLE, null, ContentValues().apply { put("key", k); put("value", v) })
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    override fun deleteAll() {
        appContext.deleteDatabase(DB_NAME)
        key.destroy()
    }

    /**
     * Opens the database, runs [block] and closes it. If the key can't be unwrapped or the file
     * can't be opened with it, the old file is deleted and a new key made: the user re-enters details.
     */
    private fun <T> withDatabase(block: (SQLiteDatabase) -> T): T? {
        loadNativeLibrary
        val passphrase = key.passphrase() ?: run {
            deleteAll()
            key.passphrase()
        } ?: return null
        val file = appContext.getDatabasePath(DB_NAME).apply { parentFile?.mkdirs() }
        val db = try {
            SQLiteDatabase.openDatabase(file.path, passphrase, null, SQLiteDatabase.CREATE_IF_NECESSARY, null, null)
        } catch (e: Exception) {
            deleteAll()
            return null
        } finally {
            passphrase.fill(0)
        }
        return db.use {
            it.execSQL("CREATE TABLE IF NOT EXISTS $TABLE (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
            block(it)
        }
    }

    private companion object {
        const val DB_NAME = "profile.db"
        const val TABLE = "profile"
        val loadNativeLibrary: Unit by lazy { System.loadLibrary("sqlcipher") }
    }
}
