package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import org.junit.Before
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

    @Before
    fun noAnimations() {
        // Animations off, as with the phone's "Remove animations" setting: pictures are still and motion snaps. The
        // star field behind Today and Learn twinkles forever otherwise, which a test clock never waits out.
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

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
        // "What is Rahu kaal?" opens that lesson, and closing it comes back to Today (LS-6).
        compose.onNodeWithText("What is Rahu kaal?").performScrollTo().performClick()
        waitForText("Next card")
        shot("28_today_learn_why")
        compose.onNodeWithContentDescription("Close lesson").performClick()
        waitForText("What is Rahu kaal?")
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
        val answers = OnboardingAnswers(
            purpose = Purpose.LEARN,
            goal = DailyGoal.TEN,
            level = Level.NEW,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
        )
        val store = InMemoryLearnStore()
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers), learnStore = store) } }
        // A learner with a saved profile opens straight on Learn (LS-1), with no tab tapped, and sees the goal (LS-4).
        waitForText("Start lesson")
        waitForText("0 of 10 min today")
        shot("10_learn_home")
        compose.onNodeWithText("Start lesson").performClick()
        waitForText("Next card")
        // Inside a lesson the bottom tabs are hidden (LS-3).
        check(compose.onAllNodesWithText("Today").fetchSemanticsNodes().isEmpty()) { "tabs shown inside a lesson" }
        shot("11_learn_card")
        compose.onAllNodesWithContentDescription("Save this card")[0].performClick()
        // On to the first quiz of the lesson.
        repeat(MAX_CARDS) {
            if (compose.onAllNodesWithText("Quick question").fetchSemanticsNodes().isEmpty()) {
                compose.onAllNodesWithText("Next card")[0].performClick()
                compose.waitForIdle()
            }
        }
        shot("12_learn_quiz")
        compose.onNodeWithContentDescription("Close lesson").performClick()
        // Lessons open in order: a new learner sees the later chapters locked.
        waitForText("Opens after chapter 1")
        shot("60_learn_locked")
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
    fun learnGraha() {
        val answers = OnboardingAnswers(
            purpose = Purpose.LEARN,
            goal = DailyGoal.TEN,
            level = Level.NEW,
            birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), index.search("Pune").first()),
        )
        // Further along: Welcome passed and the first Grahas lesson done, so Surya is up next.
        val welcome = BundledCourse.course.chapter("welcome")!!.lessons.map { it.id }
        val store = InMemoryLearnStore(LearnProgress(checkAnswered = true, completedLessons = (welcome + "grahas.meet").toSet()))
        compose.setContent { AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers), learnStore = store) } }
        waitForText("Surya, the Sun")
        compose.onNodeWithText("Start lesson").performClick()
        // Each graha has its own lesson, which opens on its flip card (PO, 20 Oct 2026).
        waitForText("Tap to turn over")
        shot("14_learn_graha_front")
        compose.onNodeWithText("Tap to turn over").performClick()
        compose.waitForIdle()
        shot("15_learn_graha_back")
        compose.onNodeWithContentDescription("Close lesson").performClick()
        // The lesson's cards also show the chapter's name, so wait for the lesson to be gone before looking for it.
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithContentDescription("Close lesson").fetchSemanticsNodes().isEmpty() }
        waitForText("Up next")
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText("The nine grahas"))
        compose.onNodeWithText("The nine grahas").performClick()
        // The sheet shows the lessons after Surya locked until the one before is passed.
        waitForText("Opens after “Surya, the Sun”")
        shot("13_learn_chapter")
    }

    @Test
    fun chapterDone() {
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
    fun quizRetry() {
        // Fewer than 2 of 3 right: the lesson isn't passed, so the card offers the quiz again instead of the next lesson.
        val chapter = BundledCourse.course.chapter("welcome")!!
        compose.setContent {
            AstroLingoTheme {
                LessonDonePage(
                    chapter = chapter,
                    lesson = chapter.lessons.first(),
                    language = Language.ENGLISH,
                    right = 1,
                    total = 3,
                    active = true,
                    next = chapter.lessons[1],
                    onNext = {},
                    onClose = {},
                    passed = false,
                )
            }
        }
        waitForText("Try the quiz again")
        check(compose.onAllNodesWithText("Next:", substring = true).fetchSemanticsNodes().isEmpty()) { "no next lesson before a pass" }
        shot("61_learn_quiz_retry")
    }

    @Test
    fun review() {
        val today = LocalDate.now().toEpochDay()
        // A missed quiz and a saved card, both due today.
        val store = InMemoryLearnStore(
            LearnProgress(
                checkAnswered = true,
                completedLessons = setOf("grahas.surya"),
                review = listOf(
                    ReviewItem("grahas.surya.6", 1, today),
                    ReviewItem("grahas.surya.1", 1, today),
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
        // Both are in box 1 and due today, so they come in card order: the saved card, then the quiz.
        waitForText("Do you remember?")
        shot("25_learn_review_recall")
        compose.onNodeWithText("Show me").performClick()
        compose.onNodeWithText("I remembered").performScrollTo().performClick()
        waitForText("Which rashi does Surya own?")
        compose.onNodeWithText("Simha").performClick()
        shot("26_learn_review_quiz")
        compose.onNodeWithText("Next card").performClick()
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
