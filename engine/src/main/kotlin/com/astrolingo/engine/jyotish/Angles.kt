// Small angle helpers shared by the jyotish engine. All angles are in degrees.
package com.astrolingo.engine.jyotish

/** Degrees in a full circle of the zodiac. */
const val FULL_CIRCLE_DEGREES = 360.0

/**
 * Normalises an angle to the range [0, 360).
 *
 * @param degrees any angle in degrees, positive or negative.
 * @return the same direction expressed in [0, 360) degrees.
 */
fun normalizeDegrees(degrees: Double): Double {
    val wrapped = degrees % FULL_CIRCLE_DEGREES
    return if (wrapped < 0.0) wrapped + FULL_CIRCLE_DEGREES else wrapped
}
