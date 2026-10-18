package com.astrolingo.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.LanguagePicker
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.places.CityIndex
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the main screens to PNG for the PO and test users; CI uploads them as the "screenshots" artifact.
 * Fictional user only: Asha Testwal, born in Pune, living in London.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AppScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val index = CityIndex.bundled()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun waitForText(text: String) = compose.waitUntil(WAIT_MILLIS) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun onboarding() {
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository()) } }
        waitForText("What brings you here?")
        shot("01_onboarding_purpose")
        compose.onNodeWithText("I want daily guidance").performClick()
        compose.onNodeWithText("Next").performClick()
        waitForText("How much do you know already?")
        shot("02_onboarding_level")
    }

    @Test
    fun tabs() {
        val answers = OnboardingAnswers(
            purpose = Purpose.GUIDANCE,
            goal = null,
            level = Level.SOME,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
            currentPlace = index.search("London").first(),
        )
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers)) } }
        waitForText("Rahu kaal")
        shot("03_today")
        compose.onNodeWithText("Change").performClick()
        waitForText("Use my location")
        shot("04_today_change_place")
        compose.onNodeWithText("Keep").performClick()
        compose.onNodeWithText("Kundli").performClick()
        compose.waitForIdle()
        shot("05_kundli")
        compose.onNodeWithText("Learn").performClick()
        compose.waitForIdle()
        shot("06_learn")
        compose.onNodeWithText("Me").performClick()
        compose.waitForIdle()
        shot("07_me")
    }

    @Test
    fun darkTheme() {
        val answers = OnboardingAnswers(
            purpose = Purpose.GUIDANCE,
            goal = null,
            level = Level.SOME,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
            currentPlace = index.search("London").first(),
        )
        compose.setContent { AstroLingoTheme(darkTheme = true) { AstroLingoApp(InMemoryProfileRepository(answers)) } }
        waitForText("Rahu kaal")
        shot("08_today_dark")
        compose.onNodeWithText("Kundli").performClick()
        compose.waitForIdle()
        shot("09_kundli_dark")
    }

    @Test
    fun languagePicker() {
        compose.setContent { AstroLingoTheme { LanguagePicker(onPick = {}) } }
        waitForText("Choose your language")
        shot("10_language_picker")
    }

    @Test
    fun hindi() {
        val answers = OnboardingAnswers(
            purpose = Purpose.GUIDANCE,
            goal = null,
            level = Level.SOME,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
            currentPlace = index.search("London").first(),
        )
        val before = Locale.getDefault()
        try {
            compose.setContent {
                AstroLingoTheme { InLanguage(AppLanguage.HINDI) { AstroLingoApp(InMemoryProfileRepository(answers)) } }
            }
            waitForText("कुंडली")
            shot("11_today_hindi")
            compose.onNodeWithText("कुंडली").performClick()
            compose.waitForIdle()
            shot("12_kundli_hindi")
        } finally {
            Locale.setDefault(before)
        }
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
