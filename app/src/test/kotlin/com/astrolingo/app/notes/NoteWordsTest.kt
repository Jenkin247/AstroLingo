package com.astrolingo.app.notes

import android.content.Context
import com.astrolingo.app.R
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.today.TodayCard
import com.astrolingo.engine.jyotish.BirthMoment
import com.astrolingo.engine.jyotish.DayStrength
import com.astrolingo.engine.jyotish.Deity
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.City
import com.astrolingo.notify.NoteKind
import com.astrolingo.notify.Planet
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Fictional people only.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteWordsTest {
    private val context = RuntimeEnvironment.getApplication() as Context
    private val res = context.resources
    private val pune = City(1259229, "Pune", "Maharashtra", "IN", 18.51957, 73.85535, ZoneId.of("Asia/Kolkata"), 0, emptyList())
    private val longyearbyen = City(2729907, "Longyearbyen", "Svalbard", "SJ", 78.2232, 15.6267, ZoneId.of("Arctic/Longyearbyen"), 0, emptyList())
    private val asha = KundliCalculator.calculate(BirthMoment(650682000000, 18.5196, 73.8554))
    private val thursday = LocalDate.of(2026, 10, 22)

    @Before
    fun english() {
        // Other tests switch the default to Hindi; a profile's note follows the default when no language is saved.
        Locale.setDefault(Locale.ENGLISH)
    }

    @Test fun `daily note has the day in a line, then colour and deity`() {
        val note = NoteWords.daily(res, TodayCard.build(asha, thursday, pune), showDeity = true)
        assertThat(note.title).containsMatch("^A (strong|steady|gentle) day: .+")
        assertThat(note.text).isEqualTo("Colour: Yellow · Deity: Vishnu")
        assertThat(note.planet).isEqualTo(Planet.JUPITER)
    }

    @Test fun `no deity when the user turned deities off`() {
        val note = NoteWords.daily(res, TodayCard.build(asha, thursday, pune), showDeity = false)
        assertThat(note.text).isEqualTo("Colour: Yellow")
    }

    @Test fun `a day with no sunrise still names the weekday and colour`() {
        val note = NoteWords.daily(res, TodayCard.build(asha, LocalDate.of(2026, 6, 21), longyearbyen), showDeity = true)
        assertThat(note.title).isEqualTo("Sunday · Surya's day")
        assertThat(note.text).isEqualTo("Colour: Orange · Deity: Surya")
    }

    @Test fun `weekly note names the week's best day`() {
        val monday = LocalDate.of(2026, 10, 19)
        val week = (0L..6L).map { TodayCard.build(asha, monday.plusDays(it), pune) }
        val note = NoteWords.weekly(res, week, Locale.ENGLISH)
        assertThat(note.title).isEqualTo("A new week begins")
        val best = week.minBy { it.facts!!.strength.ordinal }
        assertThat(note.planet).isEqualTo(best.vara.planet)
        // A strong or steady best day is named; a gentle week isn't.
        if (best.facts!!.strength != DayStrength.GENTLE) assertThat(note.text).startsWith(best.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
    }

    @Test fun `every deity has a name in both languages`() {
        assertThat(res.getStringArray(R.array.notes_deity_names)).hasLength(Deity.entries.size)
    }

    @Test @Config(qualifiers = "hi") fun `hindi note`() {
        val note = NoteWords.daily(context.resources, TodayCard.build(asha, thursday, pune), showDeity = true)
        assertThat(note.text).contains("विष्णु")
        assertThat(context.resources.getStringArray(R.array.notes_deity_names)).hasLength(Deity.entries.size)
    }

    @Test fun `no note before onboarding`() {
        assertThat(AppNotes(InMemoryProfileRepository()).note(context, NoteKind.DAILY)).isNull()
    }

    @Test fun `a note for a saved profile, and the text never shows birth details`() {
        val answers = OnboardingAnswers(Purpose.GUIDANCE, null, Level.NEW, BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), pune), currentPlace = pune)
        val note = AppNotes(InMemoryProfileRepository(answers)).note(context, NoteKind.DAILY)
        assertThat(note).isNotNull()
        assertThat(note!!.title + note.text).doesNotContain("1990")
        assertThat(note.toString()).doesNotContain(note.title)
        assertThat(AppNotes(InMemoryProfileRepository(answers)).note(context, NoteKind.WEEKLY)?.title).isEqualTo("A new week begins")
    }
}
