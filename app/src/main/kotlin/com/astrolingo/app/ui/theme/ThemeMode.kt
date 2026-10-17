// The user's light or dark choice. Not personal data, so it lives in plain preferences, not the encrypted profile.
package com.astrolingo.app.ui.theme

import android.content.Context

/** Light, dark, or whatever the phone uses. The phone's setting is the default. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Whether to draw dark, given the phone's own setting. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }
}

/** Reads and saves the [ThemeMode] choice. */
class ThemePreference(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): ThemeMode = prefs.getString(KEY, null)?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.SYSTEM

    fun save(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
    }

    private companion object {
        const val PREFS = "astrolingo_settings"
        const val KEY = "theme_mode"
    }
}
