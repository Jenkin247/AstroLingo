// The app's own language, chosen on first launch and in Me. English and Hindi for MVP1; more come later.
package com.astrolingo.app.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** A language the app is translated into. [tag] is the BCP 47 tag of its resource folder (values-hi). */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    HINDI("hi"),
}

/** Reads and saves the language choice. Null until the user has picked one, so the first launch asks. */
class LanguagePreference(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): AppLanguage? = prefs.getString(KEY, null)?.let { saved -> AppLanguage.entries.firstOrNull { it.tag == saved } }

    fun save(language: AppLanguage) {
        prefs.edit().putString(KEY, language.tag).apply()
    }

    private companion object {
        const val PREFS = "astrolingo_settings"
        const val KEY = "language"
    }
}

/**
 * Shows [content] in [language], whatever the phone's own language is. Works on every Android version the app
 * supports, without AppCompat: string resources follow the overridden configuration.
 */
@Composable
fun InLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val phoneConfig = LocalConfiguration.current
    // Dates and times are formatted with java.time, which reads the default locale, so that follows the choice too.
    val locale = remember(language) { Locale.forLanguageTag(language.tag).also(Locale::setDefault) }
    val config = remember(phoneConfig, locale) { Configuration(phoneConfig).apply { setLocale(locale) } }
    val context = remember(base, config) { base.createConfigurationContext(config) }
    CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config, content = content)
}
