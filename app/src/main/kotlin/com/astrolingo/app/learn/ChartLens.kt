// The rules behind the chart bubble on Learn cards (LS-9): which cards show it, what each saved chart says about the
// card's graha, and which details the learner has unlocked by finishing chapters. Rule-based, never AI.
// No Android imports: unit tested on the JVM.
package com.astrolingo.app.learn

import com.astrolingo.engine.jyotish.Dignity
import com.astrolingo.engine.jyotish.Element
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.NakshatraPosition
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.engine.jyotish.element
import com.astrolingo.learn.content.Card
import com.astrolingo.learn.content.ChartFact
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.VisualKind
import com.astrolingo.learn.content.YourChartCard
import com.astrolingo.learn.progress.LearnProgress

/**
 * A detail of a graha the compare sheet can show, and the chapter that unlocks it (e-learning: the sheet only shows
 * what the learner has studied).
 */
enum class LensLayer(val chapterId: String) {
    SIGN("rashis"),
    HOUSE("bhavas"),
    NAKSHATRA("nakshatras"),
    STRENGTH("dignity"),
}

/**
 * A chart on the compare sheet: the learner's own or a saved person's.
 *
 * @property timeKnown false when the birth time is unknown: houses are then not shown.
 */
data class ChartPerson(val id: String, val name: String, val kundli: Kundli, val timeKnown: Boolean, val isYou: Boolean) {
    override fun toString(): String = "ChartPerson(redacted)"
}

/**
 * Where one graha sits in one chart, with only the unlocked details filled in.
 *
 * @property house null when houses are locked or the birth time is unknown.
 */
data class GrahaReading(
    val person: ChartPerson,
    val graha: Graha,
    val rashi: Rashi?,
    val house: Int?,
    val nakshatra: NakshatraPosition?,
    val dignity: Dignity?,
)

/** "2 of 4 have the Sun in a fire sign": the most shared element, when at least two charts share it. */
data class SharedElement(val element: Element, val count: Int, val total: Int)

object ChartLens {
    /** The chapter that opens the bubble at all: before it, a chart means nothing yet (PO default, 18 Oct 2026). */
    const val GRAHAS_CHAPTER = "grahas"

    /** Finished means every lesson done, or skipped as known by the knowledge check. */
    fun chapterFinished(course: Course, progress: LearnProgress, chapterId: String): Boolean {
        if (chapterId in progress.knownChapters) return true
        val chapter = course.chapter(chapterId) ?: return false
        return chapter.lessons.all { it.id in progress.completedLessons }
    }

    fun unlocked(course: Course, progress: LearnProgress): Set<LensLayer> =
        LensLayer.entries.filter { chapterFinished(course, progress, it.chapterId) }.toSet()

    /**
     * The graha a card is about, or null when the bubble has nothing to compare (quizzes and general cards).
     * Learn cards name it through their graha picture; "your chart" cards through their fact.
     */
    fun grahaOf(card: Card): Graha? = when (card) {
        is LearnCard -> card.visual?.takeIf { it.kind == VisualKind.GRAHA }?.let { visual -> Graha.entries.firstOrNull { it.name == visual.value } }
        is YourChartCard -> when (card.fact) {
            ChartFact.SUN_RASHI, ChartFact.SUN_HOUSE -> Graha.SURYA
            ChartFact.MOON_RASHI, ChartFact.MOON_HOUSE, ChartFact.MOON_NAKSHATRA -> Graha.CHANDRA
            ChartFact.GURU_HOUSE -> Graha.GURU
            ChartFact.SHANI_HOUSE -> Graha.SHANI
            ChartFact.MOON_NAKSHATRA_LORD, ChartFact.LAGNA, ChartFact.LAGNA_LORD -> null
        }
        else -> null
    }

    fun read(person: ChartPerson, graha: Graha, layers: Set<LensLayer>): GrahaReading {
        val position = person.kundli.of(graha)
        return GrahaReading(
            person = person,
            graha = graha,
            rashi = position.rashi.takeIf { LensLayer.SIGN in layers },
            house = position.house.takeIf { LensLayer.HOUSE in layers && person.timeKnown },
            nakshatra = position.nakshatra.takeIf { LensLayer.NAKSHATRA in layers },
            dignity = Dignity.of(graha, position.rashi).takeIf { LensLayer.STRENGTH in layers },
        )
    }

    /** The element most charts share for this graha, or null with fewer than two charts, signs locked, or no pair. */
    fun sharedElement(readings: List<GrahaReading>): SharedElement? {
        val elements = readings.mapNotNull { it.rashi?.element }
        if (readings.size < 2 || elements.size < readings.size) return null
        val (element, count) = elements.groupingBy { it }.eachCount().maxByOrNull { it.value } ?: return null
        return if (count >= 2) SharedElement(element, count, readings.size) else null
    }

    /**
     * Who the "Check yourself" question asks about: the first saved person, or the learner when no one else is saved.
     * Null when signs are still locked, since there is nothing yet to ask.
     */
    fun checkSubject(readings: List<GrahaReading>): GrahaReading? =
        (readings.firstOrNull { !it.person.isYou } ?: readings.firstOrNull())?.takeIf { it.rashi != null }
}
