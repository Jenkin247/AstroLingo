// Turns the birth time a person types (local clock time at the birthplace) into the UTC moment the engine needs.
package com.astrolingo.engine.jyotish

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Something about the conversion the app should tell the user, or ask them to confirm. */
enum class BirthTimeNote {
    /** Clocks ran ahead of standard time that day (summer time, or India's 1941 to 1945 war time). Already applied. */
    CLOCKS_AHEAD,

    /** The typed time never existed on the clock (clocks jumped forward). We moved it forward by the jump. */
    SKIPPED_BY_CLOCK_CHANGE,

    /** The typed time happened twice (clocks went back). We used the first one; [ResolvedBirthTime.laterUtcEpochMillis] holds the other. */
    REPEATED_BY_CLOCK_CHANGE,

    /** The zone used local mean time then, not a standard time zone, so old records may differ by minutes. */
    LOCAL_MEAN_TIME_ERA,
}

/**
 * Result of converting a local birth time.
 *
 * @property utcEpochMillis the moment to feed into [BirthMoment].
 * @property utcOffsetSeconds the offset from UTC that was applied.
 * @property notes anything the user should see or confirm; empty in the ordinary case.
 * @property laterUtcEpochMillis for [BirthTimeNote.REPEATED_BY_CLOCK_CHANGE], the second occurrence; otherwise null.
 */
data class ResolvedBirthTime(
    val utcEpochMillis: Long,
    val utcOffsetSeconds: Int,
    val notes: Set<BirthTimeNote>,
    val laterUtcEpochMillis: Long? = null,
)

/**
 * Converts local birth times to UTC using the IANA time zone history bundled with the platform.
 *
 * For India, `Asia/Kolkata` covers IST (+05:30) from 1906, war time at +06:30 (1 October 1941 to 15 May 1942
 * and 1 September 1942 to 15 October 1945), and Madras time before 1906.
 * It does not cover the separate Bombay (until 1955) and Calcutta (until 1948) city clocks;
 * the city database story handles those by city. Pure function: no I/O and no clock.
 */
object BirthTimeResolver {
    private const val FIRST_STANDARD_TIME_YEAR_INDIA = 1906
    private val india = ZoneId.of("Asia/Kolkata")

    /**
     * Resolves a local birth time at a place in a time zone.
     *
     * @param local the date and clock time as the person would have read it at the birthplace.
     * @param zoneId the IANA zone of the birthplace, for example `Asia/Kolkata`.
     */
    fun resolve(local: LocalDateTime, zoneId: ZoneId): ResolvedBirthTime {
        val rules = zoneId.rules
        val transition = rules.getTransition(local)
        val notes = mutableSetOf<BirthTimeNote>()
        var later: Long? = null

        val offset: ZoneOffset
        val instantLocal: LocalDateTime
        when {
            transition == null -> {
                offset = rules.getValidOffsets(local).single()
                instantLocal = local
            }
            transition.isGap -> {
                // Same choice as java.time: shift forward by the gap and use the offset after it.
                notes += BirthTimeNote.SKIPPED_BY_CLOCK_CHANGE
                offset = transition.offsetAfter
                instantLocal = local.plusSeconds(transition.duration.seconds)
            }
            else -> {
                notes += BirthTimeNote.REPEATED_BY_CLOCK_CHANGE
                offset = transition.offsetBefore
                instantLocal = local
                later = local.toInstant(transition.offsetAfter).toEpochMilli()
            }
        }

        val instant = instantLocal.toInstant(offset)
        if (rules.isDaylightSavings(instant) || offset.totalSeconds > rules.getStandardOffset(instant).totalSeconds) {
            notes += BirthTimeNote.CLOCKS_AHEAD
        }
        if (zoneId.normalized() == india && local.year < FIRST_STANDARD_TIME_YEAR_INDIA) {
            notes += BirthTimeNote.LOCAL_MEAN_TIME_ERA
        }
        return ResolvedBirthTime(instant.toEpochMilli(), offset.totalSeconds, notes, later)
    }

    /**
     * Resolves a local birth time when the user knows the exact offset (for example from a birth certificate).
     *
     * @param local the date and clock time as written.
     * @param utcOffsetSeconds the offset in seconds, east positive; +05:30 is 19800.
     */
    fun resolve(local: LocalDateTime, utcOffsetSeconds: Int): ResolvedBirthTime {
        val offset = ZoneOffset.ofTotalSeconds(utcOffsetSeconds)
        return ResolvedBirthTime(local.toInstant(offset).toEpochMilli(), offset.totalSeconds, emptySet())
    }

}
