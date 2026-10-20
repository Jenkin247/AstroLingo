// Hora, the planetary hour: the Vedic day from sunrise to the next sunrise split into 24 parts, each ruled by a graha.
package com.astrolingo.engine.jyotish

import java.time.Instant
import java.time.ZoneId

/**
 * One planetary hour.
 *
 * @property lord the graha that rules it.
 * @property window when it starts and ends. Daytime horas are a twelfth of sunrise to sunset, night horas a twelfth
 *   of sunset to the next sunrise, so they are rarely exactly an hour long.
 */
data class Hora(val lord: Graha, val window: TimeWindow)

/** Works out the hora at a moment. Pure function of the input; no clock. */
object HoraCalculator {
    /** The seven grahas in the Chaldean order, slowest first. Each hora's lord is the next one in this order. */
    private val ORDER = listOf(Graha.SHANI, Graha.GURU, Graha.MANGAL, Graha.SURYA, Graha.SHUKRA, Graha.BUDHA, Graha.CHANDRA)
    private const val PARTS = 12

    /**
     * The hora at [utcMillis] at a place.
     *
     * Before sunrise the moment still belongs to the previous Vedic day and its night horas.
     *
     * @return null where the Sun does not rise or set (polar regions), since horas start from sunrise.
     */
    fun horaAt(utcMillis: Long, zone: ZoneId, latitudeDegrees: Double, longitudeDegrees: Double): Hora? {
        val date = Instant.ofEpochMilli(utcMillis).atZone(zone).toLocalDate()
        val today = PanchangCalculator.forDay(date, zone, latitudeDegrees, longitudeDegrees) ?: return null
        if (utcMillis < today.sunriseUtcMillis) {
            val yesterday = PanchangCalculator.forDay(date.minusDays(1), zone, latitudeDegrees, longitudeDegrees) ?: return null
            return horaAt(utcMillis, yesterday.vara, yesterday.sunriseUtcMillis, yesterday.sunsetUtcMillis, today.sunriseUtcMillis)
        }
        if (utcMillis < today.sunsetUtcMillis) {
            // The next sunrise is not needed for a daytime hora.
            return horaAt(utcMillis, today.vara, today.sunriseUtcMillis, today.sunsetUtcMillis, today.sunsetUtcMillis)
        }
        val tomorrow = PanchangCalculator.forDay(date.plusDays(1), zone, latitudeDegrees, longitudeDegrees) ?: return null
        return horaAt(utcMillis, today.vara, today.sunriseUtcMillis, today.sunsetUtcMillis, tomorrow.sunriseUtcMillis)
    }

    /**
     * The hora at [utcMillis] within one Vedic day.
     *
     * @param vara the weekday whose sunrise starts the day; its lord rules the first hora.
     * @param nextSunriseUtcMillis the following sunrise; only read for a moment after sunset.
     */
    fun horaAt(utcMillis: Long, vara: Vara, sunriseUtcMillis: Long, sunsetUtcMillis: Long, nextSunriseUtcMillis: Long): Hora {
        require(utcMillis >= sunriseUtcMillis) { "The moment is before this day's sunrise" }
        val night = utcMillis >= sunsetUtcMillis
        val start = if (night) sunsetUtcMillis else sunriseUtcMillis
        val end = if (night) nextSunriseUtcMillis else sunsetUtcMillis
        val length = (end - start) / PARTS.toDouble()
        val part = ((utcMillis - start) / length).toInt().coerceAtMost(PARTS - 1)
        val number = part + if (night) PARTS else 0
        val lord = ORDER[(ORDER.indexOf(vara.lord) + number) % ORDER.size]
        return Hora(lord, TimeWindow(Math.round(start + length * part), Math.round(start + length * (part + 1))))
    }
}
