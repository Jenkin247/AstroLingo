// The twelve signs of the sidereal zodiac and how to find the sign of a longitude.
package com.astrolingo.engine.jyotish

import kotlin.math.floor

/**
 * Rashi (sign) of the sidereal zodiac, in order from Mesha (Aries).
 *
 * @property englishName the Western name, used as a gloss in lessons.
 * @property lord the graha that rules the sign.
 */
enum class Rashi(val englishName: String, val lord: Graha) {
    MESHA("Aries", Graha.MANGAL),
    VRISHABHA("Taurus", Graha.SHUKRA),
    MITHUNA("Gemini", Graha.BUDHA),
    KARKA("Cancer", Graha.CHANDRA),
    SIMHA("Leo", Graha.SURYA),
    KANYA("Virgo", Graha.BUDHA),
    TULA("Libra", Graha.SHUKRA),
    VRISHCHIKA("Scorpio", Graha.MANGAL),
    DHANU("Sagittarius", Graha.GURU),
    MAKARA("Capricorn", Graha.SHANI),
    KUMBHA("Aquarius", Graha.SHANI),
    MEENA("Pisces", Graha.GURU);

    /** 1-based sign number, Mesha = 1, as used when counting houses. */
    val number: Int get() = ordinal + 1

    companion object {
        /** Each rashi spans 30 degrees. Source: BPHS ch. 4 (rashi swaroopa). */
        const val SPAN_DEGREES = 30.0

        /**
         * Returns the rashi containing a sidereal longitude.
         *
         * @param siderealLongitude longitude in degrees on the sidereal zodiac; any value is normalised.
         */
        fun of(siderealLongitude: Double): Rashi {
            val index = floor(normalizeDegrees(siderealLongitude) / SPAN_DEGREES).toInt()
            return entries[index]
        }
    }
}
