// The 27 nakshatras (lunar mansions) and how to find the nakshatra and pada of a longitude.
package com.astrolingo.engine.jyotish

import kotlin.math.floor

/**
 * Nakshatra (lunar mansion), in order from Ashwini at 0° sidereal.
 *
 * @property lord the Vimshottari dasha lord of the nakshatra. Source: BPHS ch. 46.
 */
enum class Nakshatra(val lord: Graha) {
    ASHWINI(Graha.KETU),
    BHARANI(Graha.SHUKRA),
    KRITTIKA(Graha.SURYA),
    ROHINI(Graha.CHANDRA),
    MRIGASHIRA(Graha.MANGAL),
    ARDRA(Graha.RAHU),
    PUNARVASU(Graha.GURU),
    PUSHYA(Graha.SHANI),
    ASHLESHA(Graha.BUDHA),
    MAGHA(Graha.KETU),
    PURVA_PHALGUNI(Graha.SHUKRA),
    UTTARA_PHALGUNI(Graha.SURYA),
    HASTA(Graha.CHANDRA),
    CHITRA(Graha.MANGAL),
    SWATI(Graha.RAHU),
    VISHAKHA(Graha.GURU),
    ANURADHA(Graha.SHANI),
    JYESHTHA(Graha.BUDHA),
    MULA(Graha.KETU),
    PURVA_ASHADHA(Graha.SHUKRA),
    UTTARA_ASHADHA(Graha.SURYA),
    SHRAVANA(Graha.CHANDRA),
    DHANISHTA(Graha.MANGAL),
    SHATABHISHA(Graha.RAHU),
    PURVA_BHADRAPADA(Graha.GURU),
    UTTARA_BHADRAPADA(Graha.SHANI),
    REVATI(Graha.BUDHA);

    /** 1-based nakshatra number, Ashwini = 1, as used in tara bala counting. */
    val number: Int get() = ordinal + 1

    companion object {
        /** Each nakshatra spans 13°20′ = 360° / 27. Source: BPHS ch. 3. */
        const val SPAN_DEGREES = FULL_CIRCLE_DEGREES / 27.0

        /** Each pada (quarter) spans 3°20′. Source: BPHS ch. 3. */
        const val PADA_SPAN_DEGREES = SPAN_DEGREES / 4.0

        /**
         * Returns the nakshatra and pada containing a sidereal longitude.
         *
         * @param siderealLongitude longitude in degrees on the sidereal zodiac; any value is normalised.
         */
        fun positionOf(siderealLongitude: Double): NakshatraPosition {
            val longitude = normalizeDegrees(siderealLongitude)
            val index = floor(longitude / SPAN_DEGREES).toInt().coerceAtMost(entries.size - 1)
            val withinNakshatra = longitude - index * SPAN_DEGREES
            val pada = floor(withinNakshatra / PADA_SPAN_DEGREES).toInt().coerceIn(0, 3) + 1
            return NakshatraPosition(entries[index], pada)
        }
    }
}

/**
 * A nakshatra together with its pada.
 *
 * @property nakshatra the lunar mansion.
 * @property pada the quarter within it, 1 to 4.
 */
data class NakshatraPosition(val nakshatra: Nakshatra, val pada: Int)
