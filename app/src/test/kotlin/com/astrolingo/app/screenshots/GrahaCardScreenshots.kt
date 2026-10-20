package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The richer graha cards and the Navagraha Sangraha page (PO, 20 Oct 2026). Fictional learner only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class GrahaCardScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val index = CityIndex.bundled()

    // The full page is a dialog, which is a second root: capture the last one.
    private fun shot(name: String) = compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun waitForText(text: String) {
        try {
            compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            runCatching { shot("graha_failure") }
            throw AssertionError("Timed out waiting for: $text", e)
        }
    }

    private fun next() {
        compose.onAllNodesWithText("Next card")[0].performClick()
        compose.waitForIdle()
    }

    @Before
    fun animationsOff() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun open(lessonId: String) {
        val resolved = BirthDetailsResolver.resolve(BirthDetails(LocalDate.of(1994, 10, 12), LocalTime.of(6, 40), index.search("Jaipur").first()), index)
        compose.setContent {
            AstroLingoTheme {
                LearnTab(
                    kundli = KundliCalculator.calculate(resolved.moment),
                    timeKnown = true,
                    store = InMemoryLearnStore(LearnProgress(checkAnswered = true)),
                    request = LearnRequest.Lesson(lessonId),
                )
            }
        }
    }

    @Test
    fun suryaCards() {
        open("grahas.surya")
        waitForText("Tap to turn over")
        compose.onNodeWithText("Tap to turn over").performClick()
        compose.waitForIdle()
        next()
        waitForText("Mild malefic")
        shot("40_graha_nature")
        next()
        waitForText("What Surya rules")
        shot("41_graha_rules")
        next()
        waitForText("Surya at a glance")
        shot("42_graha_glance")
        next()
        waitForText("Your Surya is in")
        shot("43_graha_yours")
    }

    @Test
    fun rahuNature() {
        open("grahas.rahu")
        waitForText("Tap to turn over")
        compose.onNodeWithText("Tap to turn over").performClick()
        compose.waitForIdle()
        next()
        waitForText("Shadow graha")
        shot("44_graha_rahu_nature")
    }

    @Test
    fun navagrahaSangraha() {
        open("grahas.sangraha")
        waitForText("Think of: soul")
        shot("45_sangraha_card")
        compose.onNodeWithText("Open the full page").performScrollTo().performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithContentDescription("Close the full page").fetchSemanticsNodes().isNotEmpty() }
        shot("46_sangraha_full_page")
        compose.onAllNodesWithContentDescription("Close the full page")[0].performClick()
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithContentDescription("Close the full page").fetchSemanticsNodes().isEmpty() }
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
