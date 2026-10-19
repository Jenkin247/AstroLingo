package com.astrolingo.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.ui.AstroLingoApp
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

/** Today for a saved person (LS-10), in light, dark and Hindi. Fictional people only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class TodayPeopleScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val index = CityIndex.bundled()

    private val answers = OnboardingAnswers(
        purpose = Purpose.GUIDANCE,
        goal = null,
        level = Level.SOME,
        birth = BirthDetails(LocalDate.of(1994, 10, 12), LocalTime.of(6, 40), index.search("Jaipur").first()),
        currentPlace = index.search("Pune").first(),
    )

    private val people = listOf(
        SavedPerson("a", "Asha", BirthDetails(LocalDate.of(1990, 1, 1), LocalTime.of(14, 15), index.search("Pune").first()), 1),
        // No birth time: the banner may add the Moon warning on days their Moon is uncertain.
        SavedPerson("r", "Ravi", BirthDetails(LocalDate.of(1988, 5, 2), null, index.search("Kolkata").first()), 2),
    )

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun waitForText(text: String) = compose.waitUntil(WAIT_MILLIS) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun showAsha(name: String, dayTitle: String, content: @Composable () -> Unit) {
        compose.setContent(content)
        waitForText("Asha")
        compose.onNodeWithText("Asha").performClick()
        waitForText(dayTitle)
        shot(name)
    }

    @Test
    fun light() = showAsha("40_today_friend", "Asha's day") {
        AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers, people)) }
    }

    @Test
    fun dark() = showAsha("41_today_friend_dark", "Asha's day") {
        AstroLingoTheme(darkTheme = true) { AstroLingoApp(InMemoryProfileRepository(answers, people)) }
    }

    @Test
    fun hindi() {
        val before = Locale.getDefault()
        try {
            showAsha("42_today_friend_hindi", "Asha का दिन") {
                AstroLingoTheme { InLanguage(AppLanguage.HINDI) { AstroLingoApp(InMemoryProfileRepository(answers, people)) } }
            }
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun nobodySaved() {
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers)) } }
        waitForText("See a friend's day")
        shot("43_today_see_friend")
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
