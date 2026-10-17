// Rahu (the Moon's north node). Ketu is always exactly opposite.
package com.astrolingo.engine.jyotish

import kotlin.math.sin

/** Lunar node calculations, in tropical degrees of date. */
object LunarNode {
    private const val DEG = Math.PI / 180.0

    /**
     * Tropical longitude of the true ascending node (Rahu), using the mean node plus the five
     * largest periodic terms. Source: Meeus, Astronomical Algorithms, 2nd ed., ch. 47.
     * AstroLingo uses the true node by default (Astro Guru convention).
     *
     * @param julianCenturiesTt Julian centuries of Terrestrial Time since J2000.0.
     * @return tropical longitude of Rahu in degrees, normalised to [0, 360).
     */
    fun trueRahu(julianCenturiesTt: Double): Double {
        val t = julianCenturiesTt
        // Mean longitude of the ascending node (Ω).
        val meanNode = 125.0445479 - 1934.1362891 * t + 0.0020754 * t * t + t * t * t / 467441.0
        // Fundamental arguments: Moon's mean elongation (D), Sun's mean anomaly (M),
        // Moon's mean anomaly (M′) and Moon's argument of latitude (F).
        val d = 297.8501921 + 445267.1114034 * t
        val m = 357.5291092 + 35999.0502909 * t
        val mPrime = 134.9633964 + 477198.8675055 * t
        val f = 93.2720950 + 483202.0175233 * t
        val correction =
            -1.4979 * sin(2 * (d - f) * DEG) -
                0.1500 * sin(m * DEG) -
                0.1226 * sin(2 * d * DEG) +
                0.1176 * sin(2 * f * DEG) -
                0.0801 * sin(2 * (mPrime - f) * DEG)
        return normalizeDegrees(meanNode + correction)
    }
}
