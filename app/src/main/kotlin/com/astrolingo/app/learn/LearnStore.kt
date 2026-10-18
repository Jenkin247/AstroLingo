// Where Learn progress and bookmarks are kept: app-private storage, excluded from backup and device transfer
// (res/xml/*_rules.xml) and wiped by "Delete my details". Not birth data (ADR 0011).
package com.astrolingo.app.learn

import android.content.Context
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.progress.LearnProgressCodec

/** Loads and saves the learner's progress. [load] may block: call it off the main thread. */
interface LearnProgressStore {
    fun load(): LearnProgress

    /** Saves without blocking the caller, in call order. */
    fun save(progress: LearnProgress)

    fun deleteAll()
}

/** Progress in private SharedPreferences. [save] uses apply(), which writes in order in the background. */
class SharedPreferencesLearnStore(context: Context) : LearnProgressStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun load(): LearnProgress =
        LearnProgressCodec.decode(prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap())

    override fun save(progress: LearnProgress) {
        val editor = prefs.edit().clear()
        LearnProgressCodec.encode(progress).forEach { (key, value) -> editor.putString(key, value) }
        editor.apply()
    }

    override fun deleteAll() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val FILE = "learn_progress"
    }
}

/** Keeps progress in memory only. For previews and tests. */
class InMemoryLearnStore(private var progress: LearnProgress = LearnProgress()) : LearnProgressStore {
    override fun load(): LearnProgress = progress

    override fun save(progress: LearnProgress) {
        this.progress = progress
    }

    override fun deleteAll() {
        progress = LearnProgress()
    }
}
