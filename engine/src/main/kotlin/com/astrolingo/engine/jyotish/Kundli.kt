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

    /** The Sun's sign in Western (tropical) astrology, used to explain why the Vedic one differs. */
    val westernSunSign: Rashi get() = Rashi.of(normalizeDegrees(of(Graha.SURYA).siderealLongitude + ayanamsa))

    /**
     * The neighbouring sign the lagna would be in if the birth time were a few minutes off, or null when
     * the lagna sits well inside its sign. The lagna moves about a degree every four minutes.
     *
     * @param marginDegrees how close to a sign edge counts as near (test users' feedback: about 2°).
     */
    fun lagnaNeighbourIfNearEdge(marginDegrees: Double = 2.0): Rashi? = neighbourIfNearEdge(lagnaLongitude, marginDegrees)

    /**
     * For a Western Sun within [marginDegrees] of a sign change, the sign next door: newspaper horoscopes
     * draw the line on fixed dates, so a "cusp" birth may be told the neighbouring sign.
     */
    fun westernSunCuspNeighbour(marginDegrees: Double = 1.0): Rashi? =
        neighbourIfNearEdge(normalizeDegrees(of(Graha.SURYA).siderealLongitude + ayanamsa), marginDegrees)

    /**
     * Chandra lagna chart: the same grahas with houses counted from the Moon's sign.
     * Used when the birth time is unknown, because the Moon's sign rarely changes within a day
     * while the lagna changes every two hours or so. Source: Astro Guru convention, BPHS ch. 7.
     */
    fun fromMoon(): Kundli {
        val moon = of(Graha.CHANDRA)
        return copy(
            lagnaLongitude = moon.siderealLongitude,
            lagna = moon.rashi,
            grahas = grahas.map { it.copy(house = KundliCalculator.wholeSignHouse(it.rashi, moon.rashi)) },
        )
    }
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
            val rahu = LunarNode.trueRahu(time)
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

/** The sign next to [longitude]'s sign when it is within [marginDegrees] of the shared edge, else null. */
private fun neighbourIfNearEdge(longitude: Double, marginDegrees: Double): Rashi? {
    val sign = Rashi.of(longitude)
    val inSign = longitude % Rashi.SPAN_DEGREES
    val count = Rashi.entries.size
    return when {
        inSign < marginDegrees -> Rashi.entries[(sign.ordinal + count - 1) % count]
        inSign > Rashi.SPAN_DEGREES - marginDegrees -> Rashi.entries[(sign.ordinal + 1) % count]
        else -> null
    }
}
