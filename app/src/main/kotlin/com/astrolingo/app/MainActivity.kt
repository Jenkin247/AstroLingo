// Single activity hosting the whole Compose UI.
package com.astrolingo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.astrolingo.account.AccountGate
import com.astrolingo.account.AccountPreferences
import com.astrolingo.account.GoogleAccountRepository
import com.astrolingo.app.learn.SharedPreferencesLearnStore
import com.astrolingo.app.profile.EncryptedProfileStore
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.settings.LanguagePreference
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.LanguagePicker
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.ThemePreference
import com.astrolingo.notify.DailyNotes

/** Entry point of the app. Everything visible is drawn by [AstroLingoApp]. */
class MainActivity : ComponentActivity() {
    private val notes by lazy { DailyNotes(this) }

    // Goes up on each return to the app, so screens read the note settings again.
    private var resumed by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        // Opening the app is what keeps the daily notes coming (NotePlan) and clears today's note.
        notes.opened()
        resumed++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profiles = EncryptedProfileStore(this)
        val learnStore = SharedPreferencesLearnStore(this)
        val themePreference = ThemePreference(this)
        val languagePreference = LanguagePreference(this)
        val account = AccountGate(GoogleAccountRepository(this), AccountPreferences(this))
        setContent {
            var themeMode by remember { mutableStateOf(themePreference.load()) }
            var language by remember { mutableStateOf(languagePreference.load()) }
            val dark = themeMode.isDark(isSystemInDarkTheme())
            // Status and navigation bar icons follow the app's choice, not only the phone's, so they stay readable.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            AstroLingoTheme(darkTheme = dark) {
                val chosen = language
                val pickLanguage = { picked: AppLanguage ->
                    language = picked
                    languagePreference.save(picked)
                }
                if (chosen == null) {
                    LanguagePicker(onPick = pickLanguage)
                } else {
                    InLanguage(chosen) {
                        AstroLingoApp(
                            profiles = profiles,
                            learnStore = learnStore,
                            themeMode = themeMode,
                            onThemeMode = { mode ->
                                themeMode = mode
                                themePreference.save(mode)
                            },
                            language = chosen,
                            onLanguage = pickLanguage,
                            notes = notes,
                            resumed = resumed,
                            account = account,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        // The scrims Android itself uses behind three-button navigation (androidx.activity defaults).
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
