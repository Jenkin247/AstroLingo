// The profile in a SQLCipher database (AES-256) keyed by DatabaseKey (ADR 0002).
// Key/value tables: one for the user's profile, one for saved people (LS-8), rows per person id.
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

    override fun loadPeople(): List<SavedPerson> {
        if (!appContext.getDatabasePath(DB_NAME).exists()) return emptyList()
        val rows = withDatabase { db ->
            db.rawQuery("SELECT id, key, value FROM $PEOPLE", null).use { cursor ->
                val byId = mutableMapOf<String, MutableMap<String, String>>()
                while (cursor.moveToNext()) byId.getOrPut(cursor.getString(0)) { mutableMapOf() }[cursor.getString(1)] = cursor.getString(2)
                byId
            }
        } ?: return emptyList()
        return rows.mapNotNull { (id, values) -> ProfileCodec.decodePerson(id, values) }.sortedBy { it.createdAt }
    }

    override fun savePerson(person: SavedPerson) {
        checkNotNull(
            withDatabase { db ->
                db.beginTransaction()
                try {
                    db.delete(PEOPLE, "id = ?", arrayOf(person.id))
                    ProfileCodec.encodePerson(person).forEach { (k, v) ->
                        db.insert(PEOPLE, null, ContentValues().apply { put("id", person.id); put("key", k); put("value", v) })
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            },
        ) { "person could not be saved" }
    }

    override fun deletePerson(id: String) {
        if (!appContext.getDatabasePath(DB_NAME).exists()) return
        withDatabase { db -> db.delete(PEOPLE, "id = ?", arrayOf(id)) }
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
            it.execSQL("CREATE TABLE IF NOT EXISTS $PEOPLE (id TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (id, key))")
            block(it)
        }
    }

    private companion object {
        const val DB_NAME = "profile.db"
        const val TABLE = "profile"
        const val PEOPLE = "people"
        val loadNativeLibrary: Unit by lazy { System.loadLibrary("sqlcipher") }
    }
}
