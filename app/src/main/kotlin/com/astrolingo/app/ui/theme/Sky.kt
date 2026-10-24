// The living sky at the top of Today: which part of the day it is, its colours, and how full the Moon is.
package com.astrolingo.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos

/** The part of the day the sky shows. */
enum class SkyPhase {
    NIGHT,
    DAWN,
    DAY,
    DUSK,
    ;

    /** Top to bottom colours of the sky. White text stays readable on every one of them (WCAG AA for large text). */
    val colors: List<Color>
        get() = when (this) {
            NIGHT -> listOf(Color(0xFF070D1E), Color(0xFF101C3A), Color(0xFF1B2A52))
            DAWN -> listOf(Color(0xFF2E3F7E), Color(0xFF6E5590), Color(0xFFA35F6E))
            DAY -> listOf(Color(0xFF4F92D2), Color(0xFF3576B8), Color(0xFF265E9C))
            DUSK -> listOf(Color(0xFF2B2558), Color(0xFF6A3A6E), Color(0xFF9C4F5E))
        }

    /** The Sun's core and glow: pale gold by day, warmer near the horizon. */
    val sunCore: Color get() = if (this == DAY) Color(0xFFFFF6CF) else Color(0xFFFFE3B0)
    val sunGlow: Color get() = if (this == DAY) Color(0xFFFFD36E) else Color(0xFFFF9F5A)

    /** Whether the Sun is up, so the sky draws the Sun rather than the Moon. */
    val sunUp: Boolean get() = this != NIGHT

    /** How bright the stars are: clear at night, faint by day. */
    val starStrength: Float
        get() = when (this) {
            NIGHT -> 0.9f
            DAWN, DUSK -> 0.5f
            DAY -> 0.2f
        }

    companion object {
        /**
         * The phase at [nowMillis], given the day's sunrise and sunset. Dawn runs from a little before sunrise to a
         * little after, dusk likewise around sunset. Where the Sun doesn't rise or set, [hour] (0 to 23, local) decides.
         */
        fun at(nowMillis: Long, sunriseMillis: Long?, sunsetMillis: Long?, hour: Int): SkyPhase {
            if (sunriseMillis == null || sunsetMillis == null) {
                return when (hour) {
                    in DAWN_HOURS -> DAWN
                    in DAY_HOURS -> DAY
                    in DUSK_HOURS -> DUSK
                    else -> NIGHT
                }
            }
            return when {
                nowMillis < sunriseMillis - BEFORE_SUNRISE_MILLIS -> NIGHT
                nowMillis < sunriseMillis + AFTER_SUNRISE_MILLIS -> DAWN
                nowMillis < sunsetMillis - BEFORE_SUNSET_MILLIS -> DAY
                nowMillis < sunsetMillis + AFTER_SUNSET_MILLIS -> DUSK
                else -> NIGHT
            }
        }

        private const val MINUTE_MILLIS = 60_000L
        private const val BEFORE_SUNRISE_MILLIS = 50 * MINUTE_MILLIS
        private const val AFTER_SUNRISE_MILLIS = 70 * MINUTE_MILLIS
        private const val BEFORE_SUNSET_MILLIS = 70 * MINUTE_MILLIS
        private const val AFTER_SUNSET_MILLIS = 60 * MINUTE_MILLIS
        private val DAWN_HOURS = 5..6
        private val DAY_HOURS = 7..16
        private val DUSK_HOURS = 17..18
    }
}

/** The lit part of the Moon. */
val MoonLight = Color(0xFFFFF3D6)

/** Text and stars on the sky, which is dark enough in every phase for white. */
val SkyInk = Color.White

/**
 * How much of the Moon is lit on tithi [number] (1 to 30), from 0 (new) to 1 (full). Each tithi is 12 degrees of the
 * Moon pulling away from the Sun, so the middle of the tithi is used.
 */
fun moonLit(number: Int): Float {
    val elongation = (number - HALF) * TITHI_DEGREES * PI / HALF_TURN_DEGREES
    return ((1 - cos(elongation)) / 2).toFloat()
}

private const val HALF = 0.5
private const val TITHI_DEGREES = 12.0
private const val HALF_TURN_DEGREES = 180.0
