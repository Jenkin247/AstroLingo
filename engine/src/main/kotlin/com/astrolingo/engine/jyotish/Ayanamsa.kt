// Lahiri (Chitrapaksha) ayanamsa: the offset between the tropical and sidereal zodiacs.
package com.astrolingo.engine.jyotish

/** Ayanamsa calculations. All results are in degrees. */
object Ayanamsa {
    /** Julian date (TT) of the Lahiri reference epoch, 21 March 1956 0h. Source: Indian Calendar Reform Committee (1955). */
    private const val LAHIRI_EPOCH_JD = 2435553.5

    /**
     * Lahiri ayanamsa at the reference epoch, in degrees (23°14′43.9″).
     * This is the IAU-1976-adjusted value also used by Swiss Ephemeris for SE_SIDM_LAHIRI.
     */
    private const val LAHIRI_AT_EPOCH_DEGREES = 23.245524743

    /** Julian date of J2000.0, the zero point of Julian centuries. */
    private const val J2000_JD = 2451545.0

    private const val DAYS_PER_JULIAN_CENTURY = 36525.0
    private const val ARCSECONDS_PER_DEGREE = 3600.0

    /**
     * Lahiri ayanamsa for a moment in time.
     *
     * Method: the epoch value plus the general precession in longitude accumulated since the
     * epoch, using the IAU 2006 series p = 5028.796195″·T + 1.1054348″·T² (T in Julian centuries
     * from J2000). This is the mean ayanamsa; nutation is already inside the true-of-date
     * tropical longitudes it is subtracted from.
     *
     * @param julianCenturiesTt time as Julian centuries of Terrestrial Time since J2000.0.
     * @return the ayanamsa in degrees, about 23.86° in the year 2000.
     */
    fun lahiri(julianCenturiesTt: Double): Double {
        val epochCenturies = (LAHIRI_EPOCH_JD - J2000_JD) / DAYS_PER_JULIAN_CENTURY
        val precessionSinceEpoch =
            generalPrecessionArcseconds(julianCenturiesTt) - generalPrecessionArcseconds(epochCenturies)
        return LAHIRI_AT_EPOCH_DEGREES + precessionSinceEpoch / ARCSECONDS_PER_DEGREE
    }

    /** General precession in longitude since J2000, in arcseconds. Source: IAU 2006 (Capitaine et al. 2003). */
    private fun generalPrecessionArcseconds(t: Double): Double = 5028.796195 * t + 1.1054348 * t * t
}
