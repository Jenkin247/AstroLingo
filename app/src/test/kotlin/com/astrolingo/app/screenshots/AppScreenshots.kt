package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.astrolingo.app.learn.BundledCourse
import com.astrolingo.app.learn.GoalCard
import com.astrolingo.app.learn.InMemoryLearnStore
import com.astrolingo.app.learn.LessonDonePage
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.DailyGoal
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
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.progress.GoalProgress
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.review.ReviewItem
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
        // Someone who knows some astrology gets the knowledge check first, full screen without tabs (LS-3).
        compose.onNodeWithText("Start from the beginning").performScrollTo().performClick()
        waitForText("Start lesson")
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
        shot("19_language_picker")
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
            shot("20_today_hindi")
            compose.onNodeWithText("कुंडली").performClick()
            compose.waitForIdle()
            shot("21_kundli_hindi")
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun learn() {
        // Animations off, as with the phone's "Remove animations" setting: pictures are still and motion snaps.
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val answers = OnboardingAnswers(
            purpose = Purpose.LEARN,
            goal = DailyGoal.TEN,
            level = Level.NEW,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
        )
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers)) } }
        // A learner with a saved profile opens straight on Learn (LS-1), with no tab tapped, and sees the goal (LS-4).
        waitForText("Start lesson")
        waitForText("0 of 10 min today")
        shot("10_learn_home")
        compose.onNodeWithText("Start lesson").performClick()
        waitForText("Next card")
        // Inside a lesson the bottom tabs are hidden (LS-3).
        check(compose.onAllNodesWithText("Today").fetchSemanticsNodes().isEmpty()) { "tabs shown inside a lesson" }
        shot("11_learn_card")
        // On to the first quiz of the lesson.
        repeat(MAX_CARDS) {
            if (compose.onAllNodesWithText("Quick question").fetchSemanticsNodes().isEmpty()) {
                compose.onAllNodesWithText("Next card")[0].performClick()
                compose.waitForIdle()
            }
        }
        shot("12_learn_quiz")
        compose.onNodeWithContentDescription("Close lesson").performClick()
        waitForText("The nine grahas")
        compose.onNodeWithText("The nine grahas").performClick()
        waitForText("Sun and Moon")
        shot("13_learn_chapter")
        compose.onNodeWithText("Sun and Moon").performClick()
        waitForText("Next card")
        compose.onAllNodesWithText("Next card")[0].performClick()
        waitForText("Tap to turn over")
        shot("14_learn_graha_front")
        compose.onNodeWithText("Tap to turn over").performClick()
        compose.waitForIdle()
        shot("15_learn_graha_back")
        compose.onAllNodesWithContentDescription("Save this card")[0].performClick()
        compose.onNodeWithContentDescription("Close lesson").performClick()
        waitForText("Saved")
        compose.onNodeWithText("Saved").performClick()
        waitForText("Saved cards")
        shot("16_learn_saved")
        compose.onNodeWithContentDescription("Back").performClick()
        waitForText("Take the check")
        compose.onNodeWithText("Take the check").performClick()
        waitForText("What do you know already?")
        compose.onNodeWithText("The nine grahas", substring = true).performClick()
        shot("17_learn_check_pick")
        compose.onNodeWithText("Check me", substring = true).performScrollTo().performClick()
        waitForText("Question 1 of 2")
        shot("18_learn_check_question")
    }

    @Test
    fun chapterDone() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val chapter = BundledCourse.course.chapter("reading")!!
        compose.setContent {
            AstroLingoTheme {
                LessonDonePage(
                    chapter = chapter,
                    lesson = chapter.lessons.last(),
                    language = Language.ENGLISH,
                    right = 3,
                    total = 3,
                    active = true,
                    next = BundledCourse.course.chapter("drishti")!!.lessons.first(),
                    onNext = {},
                    onClose = {},
                    chapterDone = true,
                )
            }
        }
        waitForText("Chapter complete!")
        shot("22_learn_chapter_done")
    }

    @Test
    fun review() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val today = LocalDate.now().toEpochDay()
        // A missed quiz and a saved card, both due today.
        val store = InMemoryLearnStore(
            LearnProgress(
                checkAnswered = true,
                completedLessons = setOf("grahas.sun-moon"),
                review = listOf(
                    ReviewItem("grahas.sun-moon.7", 1, today),
                    ReviewItem("grahas.sun-moon.2", 1, today),
                ).associateBy { it.cardId },
            ),
        )
        val answers = OnboardingAnswers(
            purpose = Purpose.GUIDANCE,
            goal = null,
            level = Level.NEW,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
            currentPlace = index.search("London").first(),
        )
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers), learnStore = store) } }
        waitForText("Cards to review: 2")
        compose.onNodeWithText("Cards to review: 2").performScrollTo()
        shot("24_today_review")
        compose.onNodeWithText("Start review").performScrollTo().performClick()
        waitForText("Review 1 of 2")
        shot("25_learn_review_quiz")
        compose.onNodeWithText("Surya").performClick()
        compose.onNodeWithText("Next card").performScrollTo().performClick()
        waitForText("Do you remember?")
        shot("26_learn_review_recall")
        compose.onNodeWithText("Show me").performClick()
        compose.onNodeWithText("I remembered").performScrollTo().performClick()
        waitForText("Review done")
        shot("27_learn_review_done")
    }

    @Test
    fun goalMet() {
        compose.setContent { AstroLingoTheme { GoalCard(GoalProgress(goalMinutes = 10, seconds = 660)) } }
        waitForText("Goal met today")
        shot("23_learn_goal_met")
    }

    private companion object {
        const val MAX_CARDS = 8
        const val WAIT_MILLIS = 15_000L
    }
}
