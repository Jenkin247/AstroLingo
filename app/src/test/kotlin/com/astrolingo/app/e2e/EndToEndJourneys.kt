package com.astrolingo.app.e2e

import android.Manifest
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.astrolingo.app.learn.BundledCourse
import com.astrolingo.app.learn.InMemoryLearnStore
import com.astrolingo.app.learn.LearnProgressStore
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.onboarding.DailyGoal
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.ProfileRepository
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.today.DayPerson
import com.astrolingo.app.today.LocalShareLauncher
import com.astrolingo.app.today.ShareLauncher
import com.astrolingo.app.today.TodayCard
import com.astrolingo.engine.jyotish.DayStrength
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.progress.LearnProgress
import com.github.takahirom.roborazzi.captureRoboImage
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * End-to-end journeys through the whole app, as a learner would take them (QA, 18 Oct 2026). Each test starts from
 * the app's root composable with in-memory storage, taps through real screens and checks what is shown and saved.
 * Fictional people only.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EndToEndJourneys {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val index = CityIndex.bundled()

    @Before
    fun noAnimations() {
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @get:Rule
    val testName = TestName()

    /** Waits for [condition]; on a timeout saves the screen and names what was missing, so CI says which step failed. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        try {
            compose.waitUntil(WAIT_MILLIS, condition)
        } catch (e: ComposeTimeoutException) {
            val name = "e2e_failure_" + testName.methodName.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)
            // The last root is the top window, such as an open sheet or dialog.
            runCatching { compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png") }
            throw AssertionError("Timed out waiting for: $what", e)
        }
    }

    private fun waitForText(text: String) = waitFor(text) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun waitForDescription(text: String) = waitFor(text) {
        compose.onAllNodesWithContentDescription(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    /**
     * The app as MainActivity shows it, inside the language wrapper. Leaving the wrapper out once hid a crash:
     * "Use my location" could not find the activity through the wrapped context and closed the app.
     */
    private fun showApp(profiles: ProfileRepository, learnStore: LearnProgressStore? = null) {
        compose.setContent {
            AstroLingoTheme {
                InLanguage(AppLanguage.ENGLISH) {
                    // Share the day hands its intent here instead of starting the share sheet's activity.
                    CompositionLocalProvider(LocalShareLauncher provides ShareLauncher { _, chooser -> shared += chooser }) {
                        if (learnStore == null) AstroLingoApp(profiles) else AstroLingoApp(profiles, learnStore = learnStore)
                    }
                }
            }
        }
    }

    /** Share sheets the app asked for, oldest first. */
    private val shared = mutableListOf<Intent>()

    private fun shows(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    /** Taps the first node with exactly [text] (or containing it, with [partial]), scrolling to it when it is in a list. */
    private fun tap(text: String, partial: Boolean = false) {
        waitFor("something to tap: $text") { compose.onAllNodesWithText(text, substring = partial).fetchSemanticsNodes().isNotEmpty() }
        click(compose.onAllNodesWithText(text, substring = partial), text)
    }

    /**
     * Clicks the first of [nodes] that is on screen, scrolling to each in turn when it sits in a list. Pages next to
     * the current card and closed sheets can hold the same text off screen, where a tap can't land.
     */
    private fun click(nodes: SemanticsNodeInteractionCollection, what: String) {
        val count = nodes.fetchSemanticsNodes().size
        for (i in 0 until count) {
            val node = nodes[i]
            // Tabs and dialog buttons have no scrolling parent.
            runCatching { node.performScrollTo() }
            if (runCatching { node.isDisplayed() }.getOrDefault(false)) {
                node.performClick()
                compose.waitForIdle()
                return
            }
        }
        val name = "e2e_failure_" + testName.methodName.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)
        runCatching { compose.onAllNodes(isRoot()).onLast().captureRoboImage("build/outputs/roborazzi/$name.png") }
        throw AssertionError("Nothing on screen to tap: $what ($count found off screen)")
    }

    /** Types a place and picks the first match, as in onboarding and Add a person. */
    private fun pickPlace(name: String) {
        waitFor("the place list to load") { !shows("Loading places") }
        // The place field is the last text field on the screen.
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput(name)
        val result = hasText(name, substring = true) and hasClickAction() and !hasSetTextAction()
        waitFor("a place matching $name") { compose.onAllNodes(result).fetchSemanticsNodes().isNotEmpty() }
        click(compose.onAllNodes(result), "a place matching $name")
    }

    /** A day in the date picker, which opens on January 1990. Each day is named in full, such as "Monday, January 15, 1990". */
    private fun day(number: Int) = hasClickAction() and (
        (hasText(number.toString(), substring = true) and hasText("1990", substring = true)) or
            (hasContentDescription(number.toString(), substring = true) and hasContentDescription("1990", substring = true))
        )

    private fun learnerAnswers(level: Level = Level.NEW) = OnboardingAnswers(
        purpose = Purpose.LEARN,
        goal = DailyGoal.TEN,
        level = level,
        birth = BirthDetails(LocalDate.of(1992, 3, 9), LocalTime.of(5, 50), index.search("Pune").first()),
    )

    @Test
    fun `a new learner onboards, meets their kundli and finishes the first lesson`() {
        val profiles = InMemoryProfileRepository()
        showApp(profiles)

        waitForText("What brings you here?")
        tap("I want to learn Vedic astrology")
        tap("Next")
        waitForText("How long do you want to learn each day?")
        tap("10 minutes a day")
        tap("Next")
        waitForText("How much do you know already?")
        tap("I'm new to astrology")
        tap("Next")
        waitForText("When were you born?")
        // The calendar opens on January 1990.
        click(compose.onAllNodes(day(15)), "15 January 1990")
        tap("Next")
        waitForText("What time were you born?")
        tap("My birth time is", partial = true)
        tap("Next")
        waitForText("Where were you born?")
        pickPlace("Pune")
        tap("Next")
        waitForText("Where do you live now?")
        // Living somewhere else shows "Use my location", which once closed the app on a real phone.
        tap("Somewhere else")
        waitForText("Use my location")
        pickPlace("Mumbai")
        tap("Next")
        waitForText("Show a deity of the day on your card?")
        tap("No, leave deities out")
        tap("Next")
        waitForText("Is this right?")
        tap("Show my kundli")

        // Learners meet their kundli first, and the answers are saved.
        waitForText("Your kundli")
        waitFor("the answers to be saved") { profiles.load() != null }
        val saved = profiles.load()!!
        assertThat(saved.purpose).isEqualTo(Purpose.LEARN)
        assertThat(saved.birth.date).isEqualTo(LocalDate.of(1990, 1, 15))
        assertThat(saved.birth.time).isEqualTo(LocalTime.of(6, 0))
        assertThat(saved.showDeity).isFalse()
        assertThat(saved.birth.city.name).isEqualTo("Pune")
        assertThat(saved.currentPlace?.name).isEqualTo("Mumbai")

        tap("Learn")
        waitForText("0 of 10 min today")
        tap("Start lesson")
        waitForText("Next card")
        check(compose.onAllNodesWithText("Today").fetchSemanticsNodes().isEmpty()) { "the bottom tabs should hide inside a lesson" }
        val lesson = BundledCourse.course.chapters.first().lessons.first()
        lesson.cards.forEach { card ->
            if (card is QuizCard) {
                waitForText(card.question.en)
                click(compose.onAllNodes(hasText(card.options[card.answer].en) and hasClickAction()), "the right answer")
                waitForText("That's right!")
            }
            tap("Next card")
        }
        waitForText("Lesson complete!")

        click(compose.onAllNodesWithContentDescription("Close lesson"), "Close lesson")
        waitForText("Today")
        tap("Today")
        waitForText("Rahu kaal")
        tap("Me")
        waitForText("Saved people")
        waitForText("1 of 4 charts saved")
    }

    @Test
    fun `a saved person shows in the chart bubble on a graha card`() {
        val profiles = InMemoryProfileRepository(learnerAnswers())
        // Grahas, signs and houses studied, so the bubble shows with sign and house.
        val progress = LearnProgress(knownChapters = setOf("welcome", "grahas", "rashis", "bhavas"), checkAnswered = true)
        showApp(profiles, InMemoryLearnStore(progress))

        waitForText("Learn")
        tap("Me")
        waitForText("Add a person")
        tap("Add a person")
        waitForText("Nickname")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Meera")
        tap("Choose the date")
        click(compose.onAllNodes(day(20)), "20 January 1990")
        tap("OK")
        tap("Choose the time")
        tap("OK")
        pickPlace("Nagpur")
        tap("Meera knows I'm saving their birth details", partial = true)
        tap("Save")
        waitForText("2 of 4 charts saved")
        waitFor("Meera to be saved") { profiles.loadPeople().size == 1 }
        assertThat(profiles.loadPeople().single().name).isEqualTo("Meera")

        tap("Learn")
        waitForText("The nine grahas")
        tap("The nine grahas")
        waitForText("Sun and Moon")
        tap("Sun and Moon")
        waitForText("Next card")
        tap("Next card")
        // The second card is about Surya: the bubble shows both charts.
        waitForDescription("Compare with saved charts, 2 charts")
        click(compose.onAllNodesWithContentDescription("Compare with saved charts, 2 charts"), "the chart bubble")
        waitForText("Surya (Sun) in your saved charts")
        waitForText("Meera")
        // Strength is a later chapter, so it says what opens it.
        waitForText("Strength: finish")
        tap("Show the answer")
        tap("You")
        waitForText("Back to all charts")
        waitForText("Surya (Sun) stands for")

        // The card can still be read aloud while the bubble is there.
        check(compose.onAllNodesWithContentDescription("Read this card aloud").fetchSemanticsNodes().isNotEmpty()) { "no speaker" }
    }

    @Test
    fun `a brand-new learner sees the chart bubble on every card but quizzes`() {
        // PO, 19 Oct 2026: the bubble didn't show in the installed app, because it waited for the Grahas chapter.
        val profiles = InMemoryProfileRepository(learnerAnswers())
        showApp(profiles, InMemoryLearnStore(LearnProgress(checkAnswered = true)))
        waitForText("Start lesson")
        tap("Start lesson")
        waitForText("Next card")
        repeat(MAX_CARDS) {
            val bubbles = compose.onAllNodesWithContentDescription("Compare with saved charts", substring = true).fetchSemanticsNodes()
            if (shows("Quick question")) {
                assertThat(bubbles).isEmpty()
            } else {
                assertThat(bubbles).isNotEmpty()
                tap("Next card")
            }
        }
        check(shows("Quick question")) { "the first lesson should reach a quiz" }
    }

    @Test
    fun `the bubble on a general card opens the saved charts`() {
        val profiles = InMemoryProfileRepository(learnerAnswers())
        showApp(profiles, InMemoryLearnStore(LearnProgress(checkAnswered = true)))
        waitForText("Start lesson")
        tap("Start lesson")
        waitForDescription("Compare with saved charts, 1 chart")
        click(compose.onAllNodesWithContentDescription("Compare with saved charts, 1 chart"), "the chart bubble")
        waitForText("Your saved charts")
        waitForText("Moon in")
        waitForText("Compare with up to 3 more people")
        tap("You")
        waitForText("Back to all charts")
        waitForDescription("Your kundli")
    }

    @Test
    fun `changing where I am today moves the day's times there`() {
        val answers = learnerAnswers()
        val profiles = InMemoryProfileRepository(answers.copy(currentPlace = answers.birth.city))
        showApp(profiles)
        tap("Today")
        waitForText("Times for Pune")
        tap("Change")
        waitForText("Where are you today?")
        waitForText("Use my location")
        pickPlace("Nagpur")
        tap("Use this place")
        waitForText("Times for Nagpur")
        waitFor("the new place to be saved") { profiles.load()?.currentPlace?.name == "Nagpur" }
    }

    @Test
    fun `use my location fills the place from a recent reading without waiting`() {
        // The phone already knows it is in Nagpur; the app should take that at once, not wait for a new fix.
        shadowOf(compose.activity.application).grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        val manager = compose.activity.getSystemService(LocationManager::class.java)
        shadowOf(manager).setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
        shadowOf(manager).simulateLocation(
            Location(LocationManager.NETWORK_PROVIDER).apply {
                latitude = 21.1458
                longitude = 79.0882
                accuracy = 2_000f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            },
        )
        val answers = learnerAnswers()
        val profiles = InMemoryProfileRepository(answers.copy(currentPlace = answers.birth.city))
        showApp(profiles)
        tap("Today")
        waitForText("Times for Pune")
        tap("Change")
        waitForText("Where are you today?")
        // The button stays off until the city list has loaded.
        waitFor("Use my location to be ready") {
            compose.onAllNodes(hasText("Use my location") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        tap("Use my location")
        waitForText("Nagpur")
        tap("Use this place")
        waitForText("Times for Nagpur")
    }

    @Test
    fun `I can see a saved friend's day on Today and go back to mine`() {
        val answers = learnerAnswers()
        val asha = SavedPerson("asha", "Asha", BirthDetails(LocalDate.of(1990, 1, 20), LocalTime.of(14, 15), index.search("Nagpur").first()), 1)
        val profiles = InMemoryProfileRepository(answers.copy(currentPlace = answers.birth.city), listOf(asha))
        showApp(profiles)
        tap("Today")
        waitForText("Times for Pune")
        tap("Asha")
        waitForText("Asha's day")
        waitForText("You're seeing Asha's day")
        // The headline comes from Asha's birth Moon, not the user's.
        val today = LocalDate.now(answers.birth.city.zone)
        val ashaCard = TodayCard.build(DayPerson.of(asha)!!.kundli, today, answers.birth.city)
        val headline = compose.activity.resources.getStringArray(focusArray(ashaCard.facts!!.strength))[ashaCard.facts!!.gocharHouse - 1]
        waitForText(headline)

        // A place picked for Asha is for her view only.
        tap("Change")
        waitForText("Where are you today?")
        pickPlace("Nagpur")
        tap("Use this place")
        waitForText("Times for Nagpur")
        assertThat(profiles.load()?.currentPlace?.name).isEqualTo("Pune")

        tap("Back to my day")
        waitForText("Times for Pune")
        assertThat(shows("Asha's day")).isFalse()
    }

    @Test
    fun `with nobody saved, Today offers to add a friend`() {
        showApp(InMemoryProfileRepository(learnerAnswers()))
        tap("Today")
        tap("See a friend's day")
        waitForText("Nickname")
    }

    @Test
    fun `deleting the friend I'm viewing takes Today back to my day`() {
        val asha = SavedPerson("asha", "Asha", BirthDetails(LocalDate.of(1990, 1, 20), null, index.search("Nagpur").first()), 1)
        val profiles = InMemoryProfileRepository(learnerAnswers(), listOf(asha))
        showApp(profiles)
        tap("Today")
        tap("Asha")
        waitForText("Asha's day")
        tap("Me")
        waitForDescription("Delete Asha")
        click(compose.onAllNodesWithContentDescription("Delete Asha"), "Delete Asha")
        tap("Delete")
        waitFor("Asha to be deleted") { profiles.loadPeople().isEmpty() }
        tap("Today")
        waitForText("See a friend's day")
        assertThat(shows("Asha's day")).isFalse()
    }

    @Test
    fun `sharing a friend's day sends their day but none of their birth details`() {
        val answers = learnerAnswers()
        val asha = SavedPerson("asha", "Asha", BirthDetails(LocalDate.of(1990, 1, 20), LocalTime.of(14, 15), index.search("Nagpur").first()), 1)
        val profiles = InMemoryProfileRepository(answers.copy(currentPlace = answers.birth.city), listOf(asha))
        showApp(profiles)
        tap("Today")
        tap("Asha")
        waitForText("Asha's day")
        tap("Share Asha's day")
        val chooser = checkNotNull(shared.singleOrNull()) { "expected one share sheet, got ${shared.size}" }
        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        @Suppress("DEPRECATION")
        val send = checkNotNull(chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
        assertThat(send.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(send.type).isEqualTo("text/plain")
        val text = checkNotNull(send.getStringExtra(Intent.EXTRA_TEXT))
        assertThat(text).startsWith("Asha's day · ")
        assertThat(text).contains("Colour of the day: ")
        assertThat(text).contains("Rahu kaal in Pune")
        assertThat(text).endsWith("Shared from AstroLingo")
        // Asha's birth date, time and birthplace never leave the phone.
        listOf("1990", "14:15", "Nagpur").forEach { assertThat(text).doesNotContain(it) }
    }

    @Test
    fun `deleting my details removes saved people and starts again`() {
        val profiles = InMemoryProfileRepository(learnerAnswers())
        showApp(profiles)
        waitForText("Learn")
        tap("Me")
        waitForText("Delete my birth details")
        tap("Delete my birth details")
        waitForText("Delete your birth details?")
        tap("Delete")
        waitForText("What brings you here?")
        assertThat(profiles.load()).isNull()
        assertThat(profiles.loadPeople()).isEmpty()
    }

    @Test
    fun `I can change my own birth details`() {
        val profiles = InMemoryProfileRepository(learnerAnswers())
        showApp(profiles)
        waitForText("Learn")
        tap("Me")
        waitForDescription("Edit my birth details")
        click(compose.onAllNodesWithContentDescription("Edit my birth details"), "Edit my birth details")
        waitForText("Your kundli, Today and Learn will use the new details")
        tap("I don't know my birth time", partial = true)
        tap("Save")
        waitForText("Saved people")
        waitFor("the new details to be saved") { profiles.load()?.birth?.time == null }
        assertThat(profiles.load()!!.birth.date).isEqualTo(LocalDate.of(1992, 3, 9))
        tap("Kundli")
        waitForText("Your birth time is not known")
    }

    @Test
    fun `the Kundli tab switches to a saved person's chart`() {
        val pune = index.search("Pune").first()
        val asha = SavedPerson("a", "Asha", BirthDetails(LocalDate.of(1990, 1, 1), LocalTime.of(14, 15), pune), 1)
        val profiles = InMemoryProfileRepository(learnerAnswers(), listOf(asha))
        showApp(profiles)
        waitForText("Learn")
        tap("Kundli")
        waitForText("Whose kundli")
        waitForText("Your kundli")
        tap("Asha")
        waitForText("Asha's kundli")
        tap("You")
        waitForText("Your kundli")
    }

    private fun focusArray(strength: DayStrength): Int = when (strength) {
        DayStrength.STRONG -> com.astrolingo.app.R.array.daily_focus_strong
        DayStrength.STEADY -> com.astrolingo.app.R.array.daily_focus_steady
        DayStrength.GENTLE -> com.astrolingo.app.R.array.daily_focus_gentle
    }

    private companion object {
        const val WAIT_MILLIS = 20_000L
        const val MAX_CARDS = 12
    }
}
