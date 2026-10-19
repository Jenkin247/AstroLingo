package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.places.CityIndex
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Saved people in Me (LS-8). Fictional people only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class PeopleScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun noAnimations() {
        // The star field twinkles forever, which a test clock never waits out; still pictures, as on the phone's
        // "Remove animations" setting.
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private val index = CityIndex.bundled()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun waitForText(text: String) = compose.waitUntil(WAIT_MILLIS) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun savedPeople() {
        val answers = OnboardingAnswers(
            purpose = Purpose.GUIDANCE,
            goal = null,
            level = Level.SOME,
            birth = BirthDetails(LocalDate.of(1994, 10, 12), LocalTime.of(6, 40), index.search("Jaipur").first()),
        )
        val people = listOf(
            SavedPerson("a", "Asha", BirthDetails(LocalDate.of(1990, 1, 1), LocalTime.of(14, 15), index.search("Pune").first()), 1),
            SavedPerson("r", "Ravi", BirthDetails(LocalDate.of(1988, 5, 2), LocalTime.of(7, 5), index.search("Kolkata").first()), 2),
        )
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers, people)) } }
        waitForText("Rahu kaal")
        compose.onNodeWithText("Me").performClick()
        waitForText("3 of 4 charts saved")
        shot("31_me_saved_people")
        compose.onNodeWithText("Add a person").performClick()
        waitForText("Nickname")
        shot("32_me_add_person")
    }

    @Test
    fun kundliPickerAndEditMe() {
        val answers = OnboardingAnswers(
            purpose = Purpose.GUIDANCE,
            goal = null,
            level = Level.SOME,
            birth = BirthDetails(LocalDate.of(1994, 10, 12), LocalTime.of(6, 40), index.search("Jaipur").first()),
        )
        val people = listOf(
            SavedPerson("a", "Asha", BirthDetails(LocalDate.of(1990, 1, 1), LocalTime.of(14, 15), index.search("Pune").first()), 1),
        )
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers, people)) } }
        waitForText("Rahu kaal")
        compose.onNodeWithText("Kundli").performClick()
        waitForText("Whose kundli")
        compose.onNodeWithText("Asha").performClick()
        waitForText("Asha's kundli")
        shot("36_kundli_picker")
        compose.onNodeWithText("Me").performClick()
        waitForText("Saved people")
        compose.onNodeWithContentDescription("Edit my birth details").performClick()
        waitForText("Your kundli, Today and Learn")
        shot("37_me_edit_my_details")
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
