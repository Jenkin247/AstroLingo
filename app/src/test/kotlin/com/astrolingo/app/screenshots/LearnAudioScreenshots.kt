package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.DailyGoal
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.places.CityIndex
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Read-aloud in the lesson player (LS-7). Fictional learner born in Pune. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LearnAudioScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val index = CityIndex.bundled()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun waitForText(text: String) = compose.waitUntil(WAIT_MILLIS) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun speakerAndListening() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val answers = OnboardingAnswers(
            purpose = Purpose.LEARN,
            goal = DailyGoal.TEN,
            level = Level.NEW,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
        )
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers)) } }
        waitForText("Start lesson")
        compose.onNodeWithText("Start lesson").performClick()
        waitForText("Next card")
        // Every card has a speaker beside its bookmark, and quiz cards have one too.
        check(compose.onAllNodesWithContentDescription("Read this card aloud").fetchSemanticsNodes().isNotEmpty()) { "no speaker on the card" }
        compose.onNodeWithContentDescription("Listening settings").performClick()
        waitForText("Listen mode")
        shot("30_learn_listening")
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
