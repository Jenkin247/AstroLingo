// The user's note settings, as the app's screens see them. DailyNotes keeps them on the phone; the in-memory one is
// for previews and tests.
package com.astrolingo.notify

import java.time.LocalDate
import java.time.LocalTime

/**
 * What the Me and Today screens show.
 *
 * @property on the user wants the daily note.
 * @property time when it goes out, in the phone's own time.
 * @property blocked notifications for the app are off in the phone's settings, so nothing can show.
 * @property ask Today should offer the note: the user hasn't said yes or no yet, and this isn't their first day.
 */
data class NoteState(val on: Boolean, val time: LocalTime, val blocked: Boolean, val ask: Boolean)

interface NoteSettings {
    fun state(today: LocalDate = LocalDate.now()): NoteState

    /** True on Android 13 and up until the user lets the app post notifications. Ask before [turnOn]. */
    val needsPermission: Boolean

    /** Starts the daily note at the saved time. Also counts as the user's answer to the offer. */
    fun turnOn()

    fun turnOff()

    fun setTime(time: LocalTime)

    /** "Not now" on Today's offer: it is not shown again. Me still has the switch. */
    fun declineOffer()

    /** The app was opened on [today]: today's note is no longer needed, and daily notes come back after a quiet spell. */
    fun opened(today: LocalDate = LocalDate.now())
}

/** Keeps the settings in memory only. */
class InMemoryNoteSettings(
    private var on: Boolean = false,
    private var time: LocalTime = NotePlan.DEFAULT_TIME,
    private val blocked: Boolean = false,
    private var offer: Boolean = false,
    override val needsPermission: Boolean = false,
) : NoteSettings {
    override fun state(today: LocalDate) = NoteState(on, time, blocked, ask = offer && !on)

    override fun turnOn() {
        on = true
        offer = false
    }

    override fun turnOff() {
        on = false
    }

    override fun setTime(time: LocalTime) {
        this.time = time
    }

    override fun declineOffer() {
        offer = false
    }

    override fun opened(today: LocalDate) = Unit
}
