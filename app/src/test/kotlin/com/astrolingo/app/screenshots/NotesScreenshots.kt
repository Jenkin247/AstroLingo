package com.astrolingo.app.screenshots

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.astrolingo.app.notes.NoteWords
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.settings.InLanguage
import com.astrolingo.app.today.TodayCard
import com.astrolingo.app.ui.AstroLingoApp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.notify.InMemoryNoteSettings
import com.astrolingo.notify.NoteText
import com.astrolingo.notify.Planet
import com.astrolingo.notify.PlanetArt
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
 * The daily note: the offer on Today, its settings in Me, and how the note itself looks (drawn here as a stand-in, as
 * Robolectric can't show the phone's notification shade). Fictional user only: Asha Testwal, born in Pune.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class NotesScreenshots {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun noAnimations() {
        // The star field twinkles forever, which a test clock never waits out.
        Settings.Global.putFloat(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Locale.setDefault(Locale.ENGLISH)
    }

    private val index = CityIndex.bundled()
    private val pune = index.search("Pune").first()

    private val answers = OnboardingAnswers(
        purpose = Purpose.GUIDANCE,
        goal = null,
        level = Level.NEW,
        birth = BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), pune),
        currentPlace = pune,
    )

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")

    private fun waitForText(text: String) = compose.waitUntil(WAIT_MILLIS) {
        compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun offerOnToday() {
        compose.setContent {
            AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers), notes = InMemoryNoteSettings(offer = true)) }
        }
        waitForText("A calm note each morning?")
        compose.onNodeWithText("A calm note each morning?").performScrollTo()
        compose.waitForIdle()
        shot("50_today_note_offer")
        compose.onNodeWithText("Yes, please").performClick()
        compose.waitForIdle()
        check(compose.onAllNodesWithText("A calm note each morning?").fetchSemanticsNodes().isEmpty()) { "the offer stays after Yes" }
    }

    @Test
    fun settingsInMe() {
        compose.setContent {
            AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository(answers), notes = InMemoryNoteSettings(on = true, blocked = true)) }
        }
        waitForText("Rahu kaal")
        compose.onNodeWithText("Me").performClick()
        waitForText("Daily note")
        compose.onNodeWithText("Open phone settings").performScrollTo()
        compose.waitForIdle()
        shot("51_me_daily_note")
        compose.onNodeWithText("Change").performClick()
        waitForText("When should the note come?")
        shot("52_me_note_time")
    }

    @Test
    fun noteInEnglish() {
        compose.setContent { AstroLingoTheme(darkTheme = true) { NotePreviews() } }
        waitForText("A new week begins")
        shot("53_note_preview")
    }

    @Test
    fun noteInHindi() {
        compose.setContent { AstroLingoTheme(darkTheme = true) { InLanguage(AppLanguage.HINDI) { NotePreviews(Locale.forLanguageTag("hi")) } } }
        waitForText("नया हफ़्ता")
        shot("54_note_preview_hindi")
    }

    @Test
    fun planets() {
        compose.setContent {
            AstroLingoTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Planet.entries.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { planet -> Scene(planet, Modifier.weight(1f)) }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        shot("55_note_planets")
    }

    /** Asha's note for Thursday 22 October 2026 and her week, as the phone would word them. */
    @Composable
    private fun NotePreviews(locale: Locale = Locale.ENGLISH) {
        val res = LocalContext.current.resources
        val kundli = KundliCalculator.calculate(BirthDetailsResolver.resolve(answers.birth, index).moment)
        val day = LocalDate.of(2026, 10, 22)
        val monday = LocalDate.of(2026, 10, 19)
        val daily = NoteWords.daily(res, TodayCard.build(kundli, day, pune), showDeity = true)
        val weekly = NoteWords.weekly(res, (0L..6L).map { TodayCard.build(kundli, monday.plusDays(it), pune) }, locale)
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FakeNote(daily)
                FakeNote(weekly)
            }
        }
    }

    /** Roughly how Android draws an opened-out note with a big picture. */
    @Composable
    private fun FakeNote(note: NoteText) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(PlanetArt.icon(note.planet).asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp))
                    Column {
                        Text("AstroLingo · now", style = MaterialTheme.typography.labelSmall)
                        Text(note.title, style = MaterialTheme.typography.titleSmall)
                        Text(note.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Scene(note.planet, Modifier.fillMaxWidth())
            }
        }
    }

    @Composable
    private fun Scene(planet: Planet, modifier: Modifier) {
        Image(
            PlanetArt.scene(planet).asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
        )
    }

    private companion object {
        const val WAIT_MILLIS = 15_000L
    }
}
