// What the widget shows at a moment: the planet of the day, its colour and the mood of the planetary hour.
// No Android imports: unit tested on the JVM.
package com.astrolingo.widget

import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.HoraCalculator
import com.astrolingo.engine.jyotish.Vara
import com.astrolingo.engine.places.City
import java.time.Instant
import java.time.ZoneId

/** One word for how an hour feels, from the graha that rules the planetary hour (hora). */
enum class HourMood(val lord: Graha) {
    BRIGHT(Graha.SURYA),
    CALM(Graha.CHANDRA),
    ENERGISING(Graha.MANGAL),
    SHARP(Graha.BUDHA),
    WISE(Graha.GURU),
    JOYFUL(Graha.SHUKRA),
    STEADY(Graha.SHANI),
    ;

    companion object {
        fun of(lord: Graha): HourMood? = entries.firstOrNull { it.lord == lord }
    }
}

/**
 * The widget's content.
 *
 * @property planet the lord of today's weekday, as on the Today card.
 * @property colour the colour of the day; the same for everyone.
 * @property mood the mood of the current planetary hour, or null when the main user's place is not known yet or the
 *   Sun does not rise there today.
 * @property refreshAtUtcMillis when the content next changes: the end of this hour or midnight, whichever is first.
 */
data class WidgetDay(
    val planet: Graha,
    val colour: DayColour,
    val mood: HourMood?,
    val refreshAtUtcMillis: Long,
) {
    companion object {
        /** Never wake up more than once a minute, even if an hour boundary rounds to just before now. */
        private const val MIN_WAIT_MILLIS = 60_000L

        /**
         * The widget at [nowUtcMillis].
         *
         * @param place the main user's place for Today, or null before onboarding; the day and hours follow its clock.
         * @param phoneZone used for the day when there is no place.
         */
        fun at(nowUtcMillis: Long, place: City?, phoneZone: ZoneId): WidgetDay {
            val zone = place?.zone ?: phoneZone
            val date = Instant.ofEpochMilli(nowUtcMillis).atZone(zone).toLocalDate()
            val lord = Vara.of(date.dayOfWeek).lord
            val hora = place?.let {
                runCatching { HoraCalculator.horaAt(nowUtcMillis, it.zone, it.latitudeDegrees, it.longitudeDegrees) }.getOrNull()
            }
            val midnight = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val next = minOf(midnight, hora?.window?.endUtcMillis ?: midnight).coerceAtLeast(nowUtcMillis + MIN_WAIT_MILLIS)
            return WidgetDay(lord, DayColour.of(lord), hora?.lord?.let(HourMood::of), next)
        }
    }
}
