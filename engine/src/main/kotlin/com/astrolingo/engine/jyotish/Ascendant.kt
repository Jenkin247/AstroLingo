// Lagna (ascendant): the point of the ecliptic rising on the eastern horizon.
package com.astrolingo.engine.jyotish

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Ascendant calculation. */
object Ascendant {
    private const val DEG = Math.PI / 180.0
    private const val DEGREES_PER_SIDEREAL_HOUR = 15.0

    /**
     * Tropical longitude of the ascendant.
     *
     * Formula: λ = atan2(cos RAMC, −(sin RAMC · cos ε + tan φ · sin ε)), where RAMC is the
     * local sidereal time in degrees, ε the obliquity of the ecliptic and φ the latitude.
     * Source: Meeus, Astronomical Algorithms, 2nd ed., ch. 14.
     *
     * @param greenwichSiderealHours apparent Greenwich sidereal time in hours.
     * @param longitudeDegrees observer longitude, east positive.
     * @param latitudeDegrees observer latitude, north positive.
     * @param obliquityDegrees obliquity of the ecliptic in degrees.
     * @return tropical longitude of the ascendant in degrees, [0, 360).
     */
    fun tropical(
        greenwichSiderealHours: Double,
        longitudeDegrees: Double,
        latitudeDegrees: Double,
        obliquityDegrees: Double,
    ): Double {
        val ramc = normalizeDegrees(greenwichSiderealHours * DEGREES_PER_SIDEREAL_HOUR + longitudeDegrees) * DEG
        val eps = obliquityDegrees * DEG
        val phi = latitudeDegrees * DEG
        val y = cos(ramc)
        val x = -(sin(ramc) * cos(eps) + tan(phi) * sin(eps))
        return normalizeDegrees(atan2(y, x) / DEG)
    }

    /**
     * Mean obliquity of the ecliptic. Source: IAU 2006 (Capitaine et al. 2003).
     * Nutation in obliquity (under 10″) is ignored; its effect on the lagna is far below 1′.
     *
     * @param julianCenturiesTt Julian centuries of Terrestrial Time since J2000.0.
     */
    fun meanObliquity(julianCenturiesTt: Double): Double {
        val t = julianCenturiesTt
        val arcseconds = 84381.406 - 46.836769 * t - 0.0001831 * t * t + 0.00200340 * t * t * t
        return arcseconds / 3600.0
    }
}
