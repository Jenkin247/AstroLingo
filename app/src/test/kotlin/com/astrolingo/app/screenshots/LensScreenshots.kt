package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.astrolingo.app.learn.ChartPerson
import com.astrolingo.app.learn.InMemoryLearnStore
import com.astrolingo.app.learn.LearnRequest
import com.astrolingo.app.learn.LearnTab
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.learn.progress.LearnProgress
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The chart bubble on Learn cards and its compare sheet (LS-9). Fictional people only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LensScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val index = CityIndex.bundled()

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    /** Waits for [condition]; on a timeout saves the screen and names what was missing, so CI says which step failed. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(WAIT_MILLIS, condition)
        } catch (e: ComposeTimeoutException) {
            runCatching { shot("lens_failure") }
            throw AssertionError("Timed out waiting for: $what", e)
        }
    }

    private fun waitForText(text: String) = waitFor(text) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun person(id: String, name: String, birth: BirthDetails, isYou: Boolean = false): ChartPerson {
        val resolved = BirthDetailsResolver.resolve(birth, index)
        return ChartPerson(id, name, KundliCalculator.calculate(resolved.moment), resolved.timeKnown, isYou)
    }

    @Test
    fun bubbleAndCompareSheet() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val you = person("you", "You", BirthDetails(LocalDate.of(1994, 10, 12), LocalTime.of(6, 40), index.search("Jaipur").first()), isYou = true)
        val people = listOf(
            you,
            person("a", "Asha", BirthDetails(LocalDate.of(1990, 1, 1), LocalTime.of(14, 15), index.search("Pune").first())),
            person("r", "Ravi", BirthDetails(LocalDate.of(1988, 5, 2), null, index.search("Kolkata").first())),
        )
        // Signs, houses and nakshatras studied; strength (chapter 8) not yet.
        val progress = LearnProgress(knownChapters = setOf("welcome", "grahas", "rashis", "bhavas", "nakshatras"), checkAnswered = true)
        compose.setContent {
            AstroLingoTheme {
                LearnTab(
                    kundli = you.kundli,
                    timeKnown = true,
                    store = InMemoryLearnStore(progress),
                    request = LearnRequest.Lesson("grahas.mars-mercury-jupiter"),
                    people = people,
                )
            }
        }
        waitFor("the chart bubble") {
            compose.onAllNodesWithContentDescription("Compare with saved charts", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        shot("33_learn_chart_bubble")

        compose.onNodeWithContentDescription("Compare with saved charts", substring = true).performClick()
        waitForText("in your saved charts")
        // Asha is the Check yourself question, so her row waits for the answer.
        waitForText("Hidden until you answer")
        shot("34_learn_compare_sheet")

        compose.onNodeWithText("Show the answer").performScrollTo().performClick()
        compose.onNodeWithText("Ravi").performScrollTo().performClick()
        waitForText("Back to all charts")
        waitForText("Without a birth time the house isn't known.")
        shot("35_learn_person_chart")
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
