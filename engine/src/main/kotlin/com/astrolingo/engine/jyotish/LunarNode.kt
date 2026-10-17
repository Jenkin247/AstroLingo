// Rahu (the Moon's north node). Ketu is always exactly opposite.
package com.astrolingo.engine.jyotish

import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.geoMoonState
import io.github.cosinekitty.astronomy.rotationEqjEct
import kotlin.math.atan2

/** Lunar node calculations, in tropical degrees of date. */
object LunarNode {
    /**
     * Tropical longitude of the true (osculating) ascending node, Rahu, on the true ecliptic of date.
     *
     * The node is where the plane of the Moon's orbit at this instant crosses the ecliptic: the orbit's
     * normal is the cross product of the Moon's geocentric position and velocity. This is the definition
     * Swiss Ephemeris and Jagannatha Hora use for "true node". The earlier Meeus series (mean node plus five
     * terms) was 2 to 5 arcminutes off; this agrees with an independent JPL DE421 calculation within 0.3′.
     * AstroLingo uses the true node by default (Astro Guru convention).
     *
     * @param time the moment, from Astronomy Engine.
     * @return tropical longitude of Rahu in degrees, normalised to [0, 360).
     */
    fun trueRahu(time: Time): Double {
        val moon = rotationEqjEct(time).rotate(geoMoonState(time))
        // Orbit normal h = r × v; the ascending node points along ẑ × h = (−h_y, h_x, 0).
        val hx = moon.y * moon.vz - moon.z * moon.vy
        val hy = moon.z * moon.vx - moon.x * moon.vz
        return normalizeDegrees(Math.toDegrees(atan2(hx, -hy)))
    }
}
