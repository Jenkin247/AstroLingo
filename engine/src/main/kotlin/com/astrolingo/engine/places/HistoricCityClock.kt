// Bombay and Calcutta kept their own clocks after IST began. The time zone history does not know this.
package com.astrolingo.engine.places

import java.time.LocalDate
import kotlin.math.abs

/**
 * A city clock that may have been used on old birth records instead of IST.
 * The app does not apply it silently: it asks the user which time the record used, offering this offset.
 *
 * @property utcOffsetSeconds the city clock's offset from UTC.
 * @property usedBefore first date the clock was certainly no longer in civil use.
 */
enum class HistoricCityClock(
    val cityId: Long,
    val utcOffsetSeconds: Int,
    val usedBefore: LocalDate,
) {
    /** Bombay Time, +04:51, kept by the city until 1955. */
    BOMBAY_TIME(cityId = 1275339L, utcOffsetSeconds = 4 * 3600 + 51 * 60, usedBefore = LocalDate.of(1955, 1, 1)),

    /** Calcutta Time, +05:53:20, kept by the city until 1948. */
    CALCUTTA_TIME(cityId = 1275004L, utcOffsetSeconds = 5 * 3600 + 53 * 60 + 20, usedBefore = LocalDate.of(1948, 1, 1)),
    ;

    companion object {
        private const val NEARBY_DEGREES = 0.3

        /**
         * The city clock to ask about for a birth, or null when IST is the only sensible reading.
         *
         * @param birthplace where the person was born; nearby suburbs count as the city.
         * @param date local birth date.
         */
        fun toAskAbout(birthplace: City, date: LocalDate, index: CityIndex): HistoricCityClock? = entries.firstOrNull { clock ->
            val city = index.byId(clock.cityId) ?: return@firstOrNull false
            date.isBefore(clock.usedBefore) &&
                abs(birthplace.latitudeDegrees - city.latitudeDegrees) < NEARBY_DEGREES &&
                abs(birthplace.longitudeDegrees - city.longitudeDegrees) < NEARBY_DEGREES
        }
    }
}
