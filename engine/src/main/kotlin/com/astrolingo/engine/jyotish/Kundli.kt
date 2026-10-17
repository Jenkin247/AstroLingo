// The birth chart (kundli): sidereal positions of the nine grahas, the lagna and whole-sign houses.
package com.astrolingo.engine.jyotish

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.equatorialToEcliptic
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.siderealTime
import io.github.cosinekitty.astronomy.sunPosition

/**
 * Where and when someone was born. Holds only what the maths needs: no name, no address.
 *
 * @property utcEpochMillis birth moment as milliseconds since 1970-01-01T00:00Z (already converted from local time).
 * @property latitudeDegrees birthplace latitude, north positive.
 * @property longitudeDegrees birthplace longitude, east positive.
 */
data class BirthMoment(
    val utcEpochMillis: Long,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
)

/**
 * One graha's place in the chart.
 *
 * @property graha the planet.
 * @property siderealLongitude sidereal longitude in degrees, [0, 360).
 * @property rashi the sign it occupies.
 * @property nakshatra its nakshatra and pada.
 * @property house whole-sign house counted from the lagna, 1 to 12.
 */
data class GrahaPosition(
    val graha: Graha,
    val siderealLongitude: Double,
    val rashi: Rashi,
    val nakshatra: NakshatraPosition,
    val house: Int,
) {
    /** Degrees travelled inside the sign, [0, 30). */
    val degreeInRashi: Double get() = siderealLongitude % Rashi.SPAN_DEGREES
}

/**
 * A calculated birth chart.
 *
 * @property ayanamsa the Lahiri ayanamsa used, in degrees.
 * @property lagnaLongitude sidereal longitude of the ascendant in degrees.
 * @property lagna the rising sign, which is house 1.
 * @property grahas all nine grahas in [Graha] order.
 */
data class Kundli(
    val ayanamsa: Double,
    val lagnaLongitude: Double,
    val lagna: Rashi,
    val grahas: List<GrahaPosition>,
) {
    /** Position of one graha. */
    fun of(graha: Graha): GrahaPosition = grahas.first { it.graha == graha }

    /** Chandra rashi: the Moon sign, which the daily card counts from. */
    val moonSign: Rashi get() = of(Graha.CHANDRA).rashi

    /** Janma nakshatra: the Moon's nakshatra at birth. */
    val birthNakshatra: NakshatraPosition get() = of(Graha.CHANDRA).nakshatra
}

/** Calculates kundlis. Pure function of the input; no I/O and no clock. */
object KundliCalculator {
    private const val HALF_CIRCLE_DEGREES = 180.0

    private val bodies = mapOf(
        Graha.MANGAL to Body.Mars,
        Graha.BUDHA to Body.Mercury,
        Graha.GURU to Body.Jupiter,
        Graha.SHUKRA to Body.Venus,
        Graha.SHANI to Body.Saturn,
    )

    /**
     * Calculates the D1 chart for a birth moment: sidereal (Lahiri) longitudes, whole-sign houses, true node.
     *
     * @param birth the birth moment and place.
     */
    fun calculate(birth: BirthMoment): Kundli {
        val time = Time.fromMillisecondsSince1970(birth.utcEpochMillis)
        val centuries = time.tt / 36525.0
        val ayanamsa = Ayanamsa.lahiri(centuries)

        // Apparent geocentric tropical longitudes on the true ecliptic of date.
        val tropical = buildMap {
            put(Graha.SURYA, sunPosition(time).elon)
            put(Graha.CHANDRA, eclipticGeoMoon(time).lon)
            bodies.forEach { (graha, body) ->
                put(graha, equatorialToEcliptic(geoVector(body, time, Aberration.Corrected)).elon)
            }
            val rahu = LunarNode.trueRahu(centuries)
            put(Graha.RAHU, rahu)
            put(Graha.KETU, normalizeDegrees(rahu + HALF_CIRCLE_DEGREES))
        }

        val lagnaTropical = Ascendant.tropical(
            greenwichSiderealHours = siderealTime(time),
            longitudeDegrees = birth.longitudeDegrees,
            latitudeDegrees = birth.latitudeDegrees,
            obliquityDegrees = Ascendant.meanObliquity(centuries),
        )
        val lagnaLongitude = normalizeDegrees(lagnaTropical - ayanamsa)
        val lagna = Rashi.of(lagnaLongitude)

        val grahas = Graha.entries.map { graha ->
            val sidereal = normalizeDegrees(tropical.getValue(graha) - ayanamsa)
            val rashi = Rashi.of(sidereal)
            GrahaPosition(
                graha = graha,
                siderealLongitude = sidereal,
                rashi = rashi,
                nakshatra = Nakshatra.positionOf(sidereal),
                house = wholeSignHouse(rashi, lagna),
            )
        }
        return Kundli(ayanamsa, lagnaLongitude, lagna, grahas)
    }

    /**
     * Whole-sign house: the lagna's sign is house 1, the next sign house 2, and so on.
     * Source: BPHS ch. 7 (bhava from rashi), Astro Guru convention for AstroLingo.
     */
    fun wholeSignHouse(rashi: Rashi, lagna: Rashi): Int = (rashi.ordinal - lagna.ordinal + 12) % 12 + 1
}
