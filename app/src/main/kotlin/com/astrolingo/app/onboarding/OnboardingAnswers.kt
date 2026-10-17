// What onboarding collects, and how birth details become the engine's BirthMoment. No Android imports: unit tested on the JVM.
package com.astrolingo.app.onboarding

import com.astrolingo.engine.jyotish.BirthMoment
import com.astrolingo.engine.jyotish.BirthTimeNote
import com.astrolingo.engine.jyotish.BirthTimeResolver
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.jyotish.Nakshatra
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.engine.places.City
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.engine.places.HistoricCityClock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Why the user is here; decides whether they land on Learn or Today. Changeable later in Me. */
enum class Purpose { LEARN, GUIDANCE }

/** Daily learning goal in minutes (BA: 5 to 30; longer goals break streaks). */
enum class DailyGoal(val minutes: Int) { FIVE(5), TEN(10), TWENTY(20), THIRTY(30) }

/** Self-reported experience; picks the starting unit. */
enum class Level { NEW, SOME, EXPERIENCED }

/**
 * Rough part of the day, asked when the birth time is unknown and the Moon changed sign or nakshatra that day.
 *
 * @property start first minute of the part, local time.
 * @property end last minute of the part, local time.
 * @property middle the time used for the chart.
 */
enum class DayPart(val start: LocalTime, val end: LocalTime, val middle: LocalTime) {
    NIGHT(LocalTime.MIDNIGHT, LocalTime.of(5, 59), LocalTime.of(3, 0)),
    MORNING(LocalTime.of(6, 0), LocalTime.of(11, 59), LocalTime.of(9, 0)),
    AFTERNOON(LocalTime.NOON, LocalTime.of(17, 59), LocalTime.of(15, 0)),
    EVENING(LocalTime.of(18, 0), LocalTime.of(23, 59), LocalTime.of(21, 0)),
}

/** The Moon's sign and nakshatra, which is what an unknown-time chart leans on. */
data class MoonPlace(val rashi: Rashi, val nakshatra: Nakshatra)

/**
 * Birth details as the user entered them.
 *
 * @property date local birth date.
 * @property time local clock time, or null when the user doesn't know it.
 * @property city birthplace from the offline city list.
 * @property useCityClock true when the user said an old Bombay or Calcutta record used the city's own clock.
 * @property dayPart with no [time], the part of the day the user picked, if they were asked and knew.
 */
data class BirthDetails(
    val date: LocalDate,
    val time: LocalTime?,
    val city: City,
    val useCityClock: Boolean = false,
    val dayPart: DayPart? = null,
) {
    /** Never print birth details, so they can't reach logs or crash reports (ADR 0002). */
    override fun toString(): String = "BirthDetails(redacted)"
}

/**
 * Birth details turned into a moment the engine can use.
 *
 * @property moment UTC moment and birthplace coordinates.
 * @property timeKnown false when the user didn't know their birth time; the app then leans on the Moon, not the lagna.
 * @property notes clock-change notes to show or confirm.
 * @property laterMoment for a time that happened twice, the second occurrence, so the app can offer it.
 * @property moonUncertain true when the time is unknown and the Moon changed sign or nakshatra within the
 *   window the user gave (the whole day, or their part of it), so even the Moon chart may be off.
 */
data class ResolvedBirth(
    val moment: BirthMoment,
    val timeKnown: Boolean,
    val notes: Set<BirthTimeNote>,
    val laterMoment: BirthMoment?,
    val moonUncertain: Boolean = false,
) {
    override fun toString(): String = "ResolvedBirth(redacted)"
}

/**
 * Everything onboarding collects, plus settings changed later in Me. Kept on the phone only (ADR 0002).
 *
 * @property showDeity false when the user turned the deity of the day off; the app then never shows one.
 * @property currentPlace where the user lives now, for sunrise, Rahu kaal and the greeting. Never used for the chart,
 *   which always comes from [birth]. Null means they haven't said: times then follow the birthplace and the
 *   greeting stays generic.
 */
data class OnboardingAnswers(
    val purpose: Purpose,
    val goal: DailyGoal?,
    val level: Level,
    val birth: BirthDetails,
    val showDeity: Boolean = true,
    val currentPlace: City? = null,
) {
    /** Where Today's times are worked out. */
    val todayPlace: City get() = currentPlace ?: birth.city
}

object BirthDetailsResolver {
    /** Unknown birth time: the Astro Guru's rule is to use local noon (docs/spec/daily-card-rules.md). */
    val UNKNOWN_TIME: LocalTime = LocalTime.NOON

    /**
     * For an unknown birth time, where the Moon was in each part of the day, or null when it stayed in
     * one sign and nakshatra all day (then no question is needed). Test users: on about half of all days
     * the Moon changes sign or nakshatra, and noon alone can give the wrong Moon sign.
     *
     * @return for each part of the day, the distinct Moon places from its start to its end (two when it changed inside it).
     */
    fun moonByDayPart(details: BirthDetails, index: CityIndex): Map<DayPart, List<MoonPlace>>? {
        if (details.time != null) return null
        val byPart = DayPart.entries.associateWith { part ->
            listOf(part.start, part.end).map { moonAt(details, it, index) }.distinct()
        }
        return if (byPart.values.flatten().distinct().size > 1) byPart else null
    }

    /** The city clock to ask about for these details, or null. */
    fun cityClockToAsk(details: BirthDetails, index: CityIndex): HistoricCityClock? =
        if (details.time == null) null else HistoricCityClock.toAskAbout(details.city, details.date, index)

    /**
     * Resolves birth details to UTC.
     *
     * @param index the city list, needed to recognise Bombay and Calcutta for their old clocks.
     */
    fun resolve(details: BirthDetails, index: CityIndex): ResolvedBirth {
        val local = LocalDateTime.of(details.date, details.time ?: details.dayPart?.middle ?: UNKNOWN_TIME)
        val clock = if (details.useCityClock) cityClockToAsk(details, index) else null
        val resolved = if (clock != null) {
            BirthTimeResolver.resolve(local, clock.utcOffsetSeconds)
        } else {
            BirthTimeResolver.resolve(local, details.city.zone)
        }
        fun moment(millis: Long) = BirthMoment(millis, details.city.latitudeDegrees, details.city.longitudeDegrees)
        return ResolvedBirth(
            moment = moment(resolved.utcEpochMillis),
            timeKnown = details.time != null,
            notes = resolved.notes,
            laterMoment = resolved.laterUtcEpochMillis?.let(::moment),
            moonUncertain = details.time == null && moonChangesWithin(details, index),
        )
    }

    private fun moonChangesWithin(details: BirthDetails, index: CityIndex): Boolean {
        val part = details.dayPart
        val window = if (part != null) listOf(part.start, part.end) else listOf(LocalTime.MIDNIGHT, LocalTime.of(23, 59))
        return window.map { moonAt(details, it, index) }.distinct().size > 1
    }

    private fun moonAt(details: BirthDetails, time: LocalTime, index: CityIndex): MoonPlace {
        val moment = resolve(details.copy(time = time, dayPart = null), index).moment
        val moon = KundliCalculator.calculate(moment).of(Graha.CHANDRA)
        return MoonPlace(moon.rashi, moon.nakshatra.nakshatra)
    }
}
