// When a note goes out, and which one. Pure rules with no Android imports, so they are unit tested on the JVM.
package com.astrolingo.notify

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** The two kinds of note. */
enum class NoteKind {
    /** The morning note about today: the day in one line, its colour and its deity. */
    DAILY,

    /** Sent on Mondays instead, once the user has stopped opening the app: what the week ahead holds. */
    WEEKLY,
}

/**
 * The PO's rules (20 Oct 2026): at most one note a day, in the morning. Once a daily note goes unanswered (the app is
 * not opened before the next one is due), the daily notes stop and one note about the week ahead goes out each
 * Monday. Opening the app brings the daily notes back.
 */
object NotePlan {
    /** The week starts on Monday, the day the weekly note goes out. */
    val WEEK_START: DayOfWeek = DayOfWeek.MONDAY

    /**
     * Which note to post on [today], or null for none.
     *
     * @param lastOpened the last day the app was opened, or null if never recorded.
     * @param lastDaily the day the last daily note went out, or null if none yet.
     * @param lastWeekly the day the last weekly note went out, or null if none yet.
     */
    fun decide(today: LocalDate, lastOpened: LocalDate?, lastDaily: LocalDate?, lastWeekly: LocalDate?): NoteKind? {
        // Already seen today's card in the app, or already told today: nothing more today.
        if (lastOpened == today || lastDaily == today || lastWeekly == today) return null
        if (!isAway(lastOpened, lastDaily)) return NoteKind.DAILY
        return if (today.dayOfWeek == WEEK_START) NoteKind.WEEKLY else null
    }

    /** True once the last daily note went out and the app has not been opened since that day. */
    fun isAway(lastOpened: LocalDate?, lastDaily: LocalDate?): Boolean =
        lastDaily != null && (lastOpened == null || lastOpened < lastDaily)

    /**
     * The next time the note is due after [now]: today at [time] if that is still ahead, otherwise tomorrow.
     * Works in the phone's zone, so a note set for 7:30 stays at 7:30 across a clock change.
     */
    fun nextRun(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }

    /** How long to wait from [now] until [nextRun]. */
    fun delayUntilNext(now: ZonedDateTime, time: LocalTime): Duration = Duration.between(now, nextRun(now, time))

    /**
     * Minutes from [time] to [now], from -720 to 719: negative when early, positive when late. Wraps round midnight,
     * so 00:10 is 20 minutes after 23:50.
     */
    fun minutesLate(now: LocalTime, time: LocalTime): Int {
        val diff = (now.toSecondOfDay() - time.toSecondOfDay()) / 60
        return Math.floorMod(diff + HALF_DAY_MINUTES, 2 * HALF_DAY_MINUTES) - HALF_DAY_MINUTES
    }

    /**
     * True when a note may still go out at [now]: up to three hours late, as Android can hold background work back
     * while the phone sleeps. A morning note that would arrive in the evening is left out.
     */
    fun onTime(now: LocalTime, time: LocalTime): Boolean = minutesLate(now, time) in -EARLY_MINUTES..LATE_MINUTES

    /** True when the schedule has drifted from [time] (a summer-time change, or a long delay) and should be set again. */
    fun drifted(now: LocalTime, time: LocalTime): Boolean = kotlin.math.abs(minutesLate(now, time)) > DRIFT_MINUTES

    private const val HALF_DAY_MINUTES = 720
    private const val EARLY_MINUTES = 60
    private const val LATE_MINUTES = 180
    private const val DRIFT_MINUTES = 20

    /** The morning time a note goes out unless the user picks another. */
    val DEFAULT_TIME: LocalTime = LocalTime.of(7, 30)
}
