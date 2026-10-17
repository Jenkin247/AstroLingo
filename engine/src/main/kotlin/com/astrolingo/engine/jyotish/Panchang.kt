// The day's panchang at a place: sunrise and sunset, vara, tithi, the Moon's nakshatra and Rahu kaal.
package com.astrolingo.engine.jyotish

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.searchRiseSet
import io.github.cosinekitty.astronomy.sunPosition
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * The seven weekdays and their lords.
 *
 * @property lord the graha that rules the day.
 * @property rahuKaalPart which eighth of the daytime is Rahu kaal, 1 to 8 (traditional table).
 */
enum class Vara(val lord: Graha, val rahuKaalPart: Int) {
    RAVIVARA(Graha.SURYA, 8),
    SOMAVARA(Graha.CHANDRA, 2),
    MANGALAVARA(Graha.MANGAL, 7),
    BUDHAVARA(Graha.BUDHA, 5),
    GURUVARA(Graha.GURU, 6),
    SHUKRAVARA(Graha.SHUKRA, 4),
    SHANIVARA(Graha.SHANI, 3),
    ;

    companion object {
        /** The vara for a civil weekday. */
        fun of(day: DayOfWeek): Vara = when (day) {
            DayOfWeek.SUNDAY -> RAVIVARA
            DayOfWeek.MONDAY -> SOMAVARA
            DayOfWeek.TUESDAY -> MANGALAVARA
            DayOfWeek.WEDNESDAY -> BUDHAVARA
            DayOfWeek.THURSDAY -> GURUVARA
            DayOfWeek.FRIDAY -> SHUKRAVARA
            DayOfWeek.SATURDAY -> SHANIVARA
        }
    }
}

/** A span of time, as milliseconds since 1970-01-01T00:00Z, start inclusive and end exclusive. */
data class TimeWindow(val startUtcMillis: Long, val endUtcMillis: Long)

/**
 * The lunar day: each tithi is 12 degrees of the Moon pulling ahead of the Sun.
 *
 * @property number 1 to 30; 1 to 15 are Shukla paksha (waxing), 16 to 30 Krishna paksha (waning).
 */
data class Tithi(val number: Int) {
    init {
        require(number in 1..TITHIS_PER_MONTH) { "tithi number must be 1..30, was $number" }
    }

    /** True in the waxing half of the month. */
    val isShukla: Boolean get() = number <= TITHIS_PER_PAKSHA

    /** Day within the paksha, 1 to 15. */
    val numberInPaksha: Int get() = (number - 1) % TITHIS_PER_PAKSHA + 1

    /** Full Moon day. */
    val isPurnima: Boolean get() = number == TITHIS_PER_PAKSHA

    /** New Moon day. */
    val isAmavasya: Boolean get() = number == TITHIS_PER_MONTH

    companion object {
        const val TITHIS_PER_PAKSHA = 15
        const val TITHIS_PER_MONTH = 30
        const val SPAN_DEGREES = 12.0

        /** The tithi for a Moon-minus-Sun elongation in degrees. */
        fun of(elongationDegrees: Double): Tithi =
            Tithi((normalizeDegrees(elongationDegrees) / SPAN_DEGREES).toInt() + 1)
    }
}

/**
 * The Moon moving into a new nakshatra or sign between sunrise and sunset.
 *
 * @property atUtcMillis when it moves, to the second.
 * @property next where it moves to.
 */
data class MoonChange<T>(val atUtcMillis: Long, val next: T)

/**
 * One day's panchang at one place. The Moon's values are taken at sunrise, as a panchang does, and a change
 * during the daytime is listed with its time, as a printed panchang lists a nakshatra's end time.
 *
 * @property date the civil date it describes.
 * @property sunriseUtcMillis visible sunrise (upper limb, standard refraction).
 * @property sunsetUtcMillis visible sunset.
 * @property vara the weekday.
 * @property tithi the tithi running at sunrise.
 * @property moonRashi the Moon's sidereal sign at sunrise.
 * @property nakshatra the Moon's nakshatra at sunrise.
 * @property rahuKaal the eighth of the daytime ruled by Rahu.
 * @property nakshatraChange when the Moon leaves [nakshatra] before sunset, or null if it stays all day.
 * @property moonRashiChange when the Moon leaves [moonRashi] before sunset, or null if it stays all day.
 */
data class DayPanchang(
    val date: LocalDate,
    val sunriseUtcMillis: Long,
    val sunsetUtcMillis: Long,
    val vara: Vara,
    val tithi: Tithi,
    val moonRashi: Rashi,
    val nakshatra: NakshatraPosition,
    val rahuKaal: TimeWindow,
    val nakshatraChange: MoonChange<Nakshatra>? = null,
    val moonRashiChange: MoonChange<Rashi>? = null,
) {
    private val middayUtcMillis: Long get() = sunriseUtcMillis + (sunsetUtcMillis - sunriseUtcMillis) / 2

    /** The nakshatra that holds for most of the daytime. The Moon changes at most once between sunrise and sunset. */
    val daytimeNakshatra: Nakshatra
        get() = nakshatraChange?.takeIf { it.atUtcMillis <= middayUtcMillis }?.next ?: nakshatra.nakshatra

    /** The Moon sign that holds for most of the daytime. */
    val daytimeMoonRashi: Rashi
        get() = moonRashiChange?.takeIf { it.atUtcMillis <= middayUtcMillis }?.next ?: moonRashi
}

/** Calculates the daily panchang. Pure function of the input; no I/O and no clock. */
object PanchangCalculator {
    private const val DAYTIME_PARTS = 8
    private const val SEARCH_DAYS = 1.0
    private const val RASHI_SPAN_DEGREES = 30.0
    private const val MILLIS_PER_SECOND = 1000L

    /**
     * Panchang for a civil date at a place.
     *
     * @param date the local date.
     * @param zone the place's time zone, used to find the local day.
     * @param latitudeDegrees north positive.
     * @param longitudeDegrees east positive.
     * @return null where the Sun does not rise or set that day (polar regions).
     */
    fun forDay(date: LocalDate, zone: ZoneId, latitudeDegrees: Double, longitudeDegrees: Double): DayPanchang? {
        val observer = Observer(latitudeDegrees, longitudeDegrees, 0.0)
        val midnight = Time.fromMillisecondsSince1970(date.atStartOfDay(zone).toInstant().toEpochMilli())
        val sunrise = searchRiseSet(Body.Sun, observer, Direction.Rise, midnight, SEARCH_DAYS) ?: return null
        val sunset = searchRiseSet(Body.Sun, observer, Direction.Set, sunrise, SEARCH_DAYS) ?: return null

        val moonTropical = eclipticGeoMoon(sunrise).lon
        val moonSidereal = moonSidereal(sunrise)
        val moonAtSunset = moonSidereal(sunset)
        val nakshatra = Nakshatra.positionOf(moonSidereal)
        val rashi = Rashi.of(moonSidereal)
        val sunTropical = sunPosition(sunrise).elon

        val vara = Vara.of(date.dayOfWeek)
        val riseMillis = sunrise.toMillisecondsSince1970()
        val setMillis = sunset.toMillisecondsSince1970()
        return DayPanchang(
            date = date,
            sunriseUtcMillis = riseMillis,
            sunsetUtcMillis = setMillis,
            vara = vara,
            tithi = Tithi.of(moonTropical - sunTropical),
            moonRashi = rashi,
            nakshatra = nakshatra,
            rahuKaal = rahuKaal(riseMillis, setMillis, vara),
            nakshatraChange = Nakshatra.positionOf(moonAtSunset).nakshatra.takeIf { it != nakshatra.nakshatra }?.let {
                MoonChange(crossing(sunrise, sunset, (nakshatra.nakshatra.ordinal + 1) * Nakshatra.SPAN_DEGREES), it)
            },
            moonRashiChange = Rashi.of(moonAtSunset).takeIf { it != rashi }?.let {
                MoonChange(crossing(sunrise, sunset, (rashi.ordinal + 1) * RASHI_SPAN_DEGREES), it)
            },
        )
    }

    /** The Moon's sidereal longitude (Lahiri) at a moment. */
    private fun moonSidereal(time: Time): Double =
        normalizeDegrees(eclipticGeoMoon(time).lon - Ayanamsa.lahiri(time.tt / 36525.0))

    /** When the Moon crosses [boundaryDegrees] between [from] and [to], found by halving to under a second. */
    private fun crossing(from: Time, to: Time, boundaryDegrees: Double): Long {
        var before = from.toMillisecondsSince1970()
        var after = to.toMillisecondsSince1970()
        while (after - before > MILLIS_PER_SECOND) {
            val middle = before + (after - before) / 2
            val past = normalizeDegrees(moonSidereal(Time.fromMillisecondsSince1970(middle)) - boundaryDegrees + 180.0) >= 180.0
            if (past) after = middle else before = middle
        }
        return after
    }

    /** Rahu kaal: the daytime split into eight equal parts, and the vara's part from the traditional table. */
    fun rahuKaal(sunriseUtcMillis: Long, sunsetUtcMillis: Long, vara: Vara): TimeWindow {
        val part = (sunsetUtcMillis - sunriseUtcMillis) / DAYTIME_PARTS.toDouble()
        val start = sunriseUtcMillis + part * (vara.rahuKaalPart - 1)
        return TimeWindow(Math.round(start), Math.round(start + part))
    }
}
