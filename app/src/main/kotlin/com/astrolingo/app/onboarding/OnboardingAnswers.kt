// What onboarding collects, and how birth details become the engine's BirthMoment. No Android imports: unit tested on the JVM.
package com.astrolingo.app.onboarding

import com.astrolingo.engine.jyotish.BirthMoment
import com.astrolingo.engine.jyotish.BirthTimeNote
import com.astrolingo.engine.jyotish.BirthTimeResolver
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
 * Birth details as the user entered them.
 *
 * @property date local birth date.
 * @property time local clock time, or null when the user doesn't know it.
 * @property city birthplace from the offline city list.
 * @property useCityClock true when the user said an old Bombay or Calcutta record used the city's own clock.
 */
data class BirthDetails(
    val date: LocalDate,
    val time: LocalTime?,
    val city: City,
    val useCityClock: Boolean = false,
)

/**
 * Birth details turned into a moment the engine can use.
 *
 * @property moment UTC moment and birthplace coordinates.
 * @property timeKnown false when the user didn't know their birth time; the app then leans on the Moon, not the lagna.
 * @property notes clock-change notes to show or confirm.
 * @property laterMoment for a time that happened twice, the second occurrence, so the app can offer it.
 */
data class ResolvedBirth(
    val moment: BirthMoment,
    val timeKnown: Boolean,
    val notes: Set<BirthTimeNote>,
    val laterMoment: BirthMoment?,
)

/** Everything onboarding collects. Kept on the phone only (ADR 0002). */
data class OnboardingAnswers(
    val purpose: Purpose,
    val goal: DailyGoal?,
    val level: Level,
    val birth: BirthDetails,
)

object BirthDetailsResolver {
    /** Unknown birth time: the Astro Guru's rule is to use local noon (docs/spec/daily-card-rules.md). */
    val UNKNOWN_TIME: LocalTime = LocalTime.NOON

    /** The city clock to ask about for these details, or null. */
    fun cityClockToAsk(details: BirthDetails, index: CityIndex): HistoricCityClock? =
        if (details.time == null) null else HistoricCityClock.toAskAbout(details.city, details.date, index)

    /**
     * Resolves birth details to UTC.
     *
     * @param index the city list, needed to recognise Bombay and Calcutta for their old clocks.
     */
    fun resolve(details: BirthDetails, index: CityIndex): ResolvedBirth {
        val local = LocalDateTime.of(details.date, details.time ?: UNKNOWN_TIME)
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
        )
    }
}
