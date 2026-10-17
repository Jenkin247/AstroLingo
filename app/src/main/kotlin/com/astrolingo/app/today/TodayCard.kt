// The Today card's content, chosen by the engine's fixed rules (docs/spec/daily-card-rules.md). No Android imports.
package com.astrolingo.app.today

import com.astrolingo.engine.jyotish.DailyCardFacts
import com.astrolingo.engine.jyotish.DailyCardRules
import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.Deity
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.PanchangCalculator
import com.astrolingo.engine.jyotish.Vara
import com.astrolingo.engine.places.City
import java.time.LocalDate

/**
 * One day's card.
 *
 * @property date the local civil date at [place].
 * @property place where sunrise, sunset and Rahu kaal are worked out.
 * @property vara the weekday, which sets the colour and deity even when there is no sunrise.
 * @property facts the full card, or null on a polar day or night with no sunrise (colour and deity only).
 */
data class TodayCard(
    val date: LocalDate,
    val place: City,
    val vara: Vara,
    val facts: DailyCardFacts?,
) {
    val colour: DayColour get() = DayColour.of(vara.lord)
    val deity: Deity get() = Deity.of(vara.lord)

    /** Index into the message arrays for this day, so wordings rotate and a week never repeats a line. */
    val rotation: Int get() = date.dayOfYear

    override fun toString(): String = "TodayCard($date)" // no place: it can be the user's home

    companion object {
        /** Works out today's card from the user's birth Moon. Needs only the Moon, so it works without a birth time. */
        fun build(kundli: Kundli, date: LocalDate, place: City): TodayCard {
            val panchang = PanchangCalculator.forDay(date, place.zone, place.latitudeDegrees, place.longitudeDegrees)
            val facts = panchang?.let { DailyCardRules.facts(kundli.moonSign, kundli.birthNakshatra.nakshatra, it) }
            return TodayCard(date, place, Vara.of(date.dayOfWeek), facts)
        }
    }
}

/** Picks one of several wordings for a slot, rotating by day. */
fun <T> List<T>.forDay(rotation: Int): T = this[Math.floorMod(rotation, size)]
