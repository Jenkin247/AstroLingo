package com.astrolingo.app.learn

import com.astrolingo.engine.jyotish.Dignity
import com.astrolingo.engine.jyotish.Element
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.GrahaPosition
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.jyotish.Nakshatra
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.learn.content.ChartFact
import com.astrolingo.learn.content.CourseLoader
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.LocalizedText
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.content.Visual
import com.astrolingo.learn.content.VisualKind
import com.astrolingo.learn.content.YourChartCard
import com.astrolingo.learn.progress.LearnProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// Charts are made up from signs, not from anyone's birth details.
class ChartLensTest {
    private val course = CourseLoader.bundled()

    /** A chart with every graha 10° into [sunSign], except the Sun's sign which is what the test is about. */
    private fun chart(sunSign: Rashi, lagna: Rashi = Rashi.MESHA): Kundli {
        val grahas = Graha.entries.map { graha ->
            val rashi = if (graha == Graha.SURYA) sunSign else Rashi.KARKA
            val longitude = rashi.ordinal * 30.0 + 10.0
            GrahaPosition(graha, longitude, rashi, Nakshatra.positionOf(longitude), KundliCalculator.wholeSignHouse(rashi, lagna))
        }
        return Kundli(ayanamsa = 24.0, lagnaLongitude = lagna.ordinal * 30.0 + 5.0, lagna = lagna, grahas = grahas)
    }

    private fun person(name: String, sunSign: Rashi, timeKnown: Boolean = true, isYou: Boolean = false) =
        ChartPerson(name, name, chart(sunSign), timeKnown, isYou)

    private fun finished(vararg chapters: String) = LearnProgress(knownChapters = chapters.toSet())

    @Test
    fun `graha cards and your-chart cards have a graha, quizzes do not`() {
        val sun = LearnCard("c", LocalizedText("Surya"), LocalizedText("…"), visual = Visual(VisualKind.GRAHA, "SURYA"))
        val glyph = LearnCard("g", LocalizedText("Jyotish"), LocalizedText("…"), visual = Visual(VisualKind.GLYPH, "✦"))
        val moon = YourChartCard("y", LocalizedText("Your Moon"), ChartFact.MOON_RASHI, LocalizedText("{value}"))
        val lagna = YourChartCard("l", LocalizedText("Your lagna"), ChartFact.LAGNA, LocalizedText("{value}"))
        val quiz = QuizCard("q", LocalizedText("?"), listOf(LocalizedText("a"), LocalizedText("b")), 0, LocalizedText("."))
        assertThat(ChartLens.grahaOf(sun)).isEqualTo(Graha.SURYA)
        assertThat(ChartLens.grahaOf(moon)).isEqualTo(Graha.CHANDRA)
        assertThat(ChartLens.grahaOf(glyph)).isNull()
        assertThat(ChartLens.grahaOf(lagna)).isNull()
        assertThat(ChartLens.grahaOf(quiz)).isNull()
    }

    @Test
    fun `a card's graha tag wins over its picture, so a card about Surya with a zodiac picture marks Surya`() {
        val signs = LearnCard("s", LocalizedText("Surya's day and sign"), LocalizedText("…"), visual = Visual(VisualKind.ZODIAC, "5"), grahas = listOf("SURYA"))
        val pair = LearnCard("p", LocalizedText("Guru and Shani"), LocalizedText("…"), visual = Visual(VisualKind.GRAHA, "SHANI"), grahas = listOf("GURU", "SHANI"))
        val mangal = YourChartCard("m", LocalizedText("Your Mangal"), ChartFact.MANGAL_RASHI, LocalizedText("{value}"))
        assertThat(ChartLens.grahaOf(signs)).isEqualTo(Graha.SURYA)
        assertThat(ChartLens.grahaOf(pair)).isEqualTo(Graha.GURU)
        assertThat(ChartLens.grahaOf(mangal)).isEqualTo(Graha.MANGAL)
    }

    @Test
    fun `every card in a graha's own lesson marks that graha`() {
        course.chapter("grahas")!!.lessons.drop(1).dropLast(1).forEach { lesson ->
            val marked = lesson.cards.filterNot { it is QuizCard }.map { ChartLens.grahaOf(it) }.toSet()
            assertThat(marked).hasSize(1)
            assertThat(marked.single()).isNotNull()
        }
    }

    @Test
    fun `the course has graha cards for the bubble to appear on`() {
        val cards = course.chapters.flatMap { chapter -> chapter.lessons.flatMap { it.cards } }
        assertThat(cards.count { ChartLens.grahaOf(it) != null }).isAtLeast(9)
    }

    @Test
    fun `details unlock chapter by chapter, by finishing or by the knowledge check`() {
        assertThat(ChartLens.unlocked(course, LearnProgress())).isEmpty()
        assertThat(ChartLens.unlocked(course, finished("rashis"))).containsExactly(LensLayer.SIGN)
        val bhavas = course.chapter("bhavas")!!.lessons.map { it.id }.toSet()
        assertThat(ChartLens.unlocked(course, LearnProgress(completedLessons = bhavas))).containsExactly(LensLayer.HOUSE)
        assertThat(ChartLens.chapterFinished(course, LearnProgress(), "grahas")).isFalse()
        assertThat(ChartLens.chapterFinished(course, finished("grahas"), "grahas")).isTrue()
    }

    @Test
    fun `a reading shows only unlocked details, and no house without a birth time`() {
        val all = LensLayer.entries.toSet()
        val ravi = ChartLens.read(person("Ravi", Rashi.MESHA), Graha.SURYA, all)
        assertThat(ravi.rashi).isEqualTo(Rashi.MESHA)
        assertThat(ravi.house).isEqualTo(1)
        assertThat(ravi.dignity).isEqualTo(Dignity.EXALTED)
        val meera = ChartLens.read(person("Meera", Rashi.MAKARA, timeKnown = false), Graha.SURYA, all)
        assertThat(meera.house).isNull()
        val beginner = ChartLens.read(person("Ravi", Rashi.MESHA), Graha.SURYA, emptySet())
        assertThat(listOf(beginner.rashi, beginner.house, beginner.nakshatra, beginner.dignity)).containsExactly(null, null, null, null)
    }

    @Test
    fun `the shared element needs two charts with the same element and unlocked signs`() {
        val layers = setOf(LensLayer.SIGN)
        val readings = listOf(Rashi.TULA, Rashi.SIMHA, Rashi.MESHA, Rashi.MAKARA).mapIndexed { i, sign ->
            ChartLens.read(person("p$i", sign), Graha.SURYA, layers)
        }
        assertThat(ChartLens.sharedElement(readings)).isEqualTo(SharedElement(Element.FIRE, 2, 4))
        assertThat(ChartLens.sharedElement(readings.take(1))).isNull()
        assertThat(ChartLens.sharedElement(listOf(readings[0], readings[1]))).isNull()
        assertThat(ChartLens.sharedElement(readings.map { it.copy(rashi = null) })).isNull()
    }

    @Test
    fun `check yourself asks about a saved person before the learner, once signs are open`() {
        val layers = setOf(LensLayer.SIGN)
        val you = ChartLens.read(person("You", Rashi.TULA, isYou = true), Graha.SURYA, layers)
        val asha = ChartLens.read(person("Asha", Rashi.SIMHA), Graha.SURYA, layers)
        assertThat(ChartLens.checkSubject(listOf(you, asha))?.person?.name).isEqualTo("Asha")
        assertThat(ChartLens.checkSubject(listOf(you))?.person?.name).isEqualTo("You")
        assertThat(ChartLens.checkSubject(listOf(you.copy(rashi = null)))).isNull()
    }
}
