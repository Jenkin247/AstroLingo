// The real note settings: kept in the app's private preferences, with the schedule kept in step.
package com.astrolingo.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalTime

/**
 * Note settings and history on this phone. Nothing here is personal: dates the app was opened and notes went out,
 * and the chosen time. Birth details stay in the encrypted profile.
 */
class DailyNotes(context: Context) : NoteSettings {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isOn: Boolean get() = prefs.getBoolean(K_ON, false)

    val time: LocalTime get() = LocalTime.ofSecondOfDay(prefs.getInt(K_MINUTE, NotePlan.DEFAULT_TIME.toSecondOfDay() / 60) * 60L)

    override fun state(today: LocalDate): NoteState {
        val first = date(K_FIRST_OPENED)
        // Offered from the second day the app is used, once the user has seen what a day's card is like.
        val ask = !isOn && !prefs.getBoolean(K_ANSWERED, false) && first != null && first < today
        return NoteState(on = isOn, time = time, blocked = !phoneAllows(), ask = ask)
    }

    override val needsPermission: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** False when notifications for the app are off in the phone's settings (or not allowed yet on Android 13+). */
    fun phoneAllows(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun turnOn() {
        prefs.edit {
            putBoolean(K_ON, true)
            putBoolean(K_ANSWERED, true)
        }
        NoteScheduler.schedule(context, time)
    }

    override fun turnOff() {
        prefs.edit { putBoolean(K_ON, false) }
        NoteScheduler.cancel(context)
        NotePoster.clear(context)
    }

    override fun setTime(time: LocalTime) {
        prefs.edit { putInt(K_MINUTE, time.toSecondOfDay() / 60) }
        if (isOn) NoteScheduler.schedule(context, time)
    }

    override fun declineOffer() {
        prefs.edit { putBoolean(K_ANSWERED, true) }
    }

    override fun opened(today: LocalDate) {
        prefs.edit {
            if (date(K_FIRST_OPENED) == null) putLong(K_FIRST_OPENED, today.toEpochDay())
            putLong(K_LAST_OPENED, today.toEpochDay())
        }
        // The card is on screen now, so the note about it has done its job.
        NotePoster.clear(context)
    }

    /** Which note to post on [today], if any, by [NotePlan]'s rules. */
    internal fun due(today: LocalDate): NoteKind? =
        if (!isOn) null else NotePlan.decide(today, date(K_LAST_OPENED), date(K_LAST_DAILY), date(K_LAST_WEEKLY))

    /** Remembers that a [kind] note went out on [today]. */
    internal fun sent(kind: NoteKind, today: LocalDate) {
        prefs.edit { putLong(if (kind == NoteKind.DAILY) K_LAST_DAILY else K_LAST_WEEKLY, today.toEpochDay()) }
    }

    private fun date(key: String): LocalDate? = if (prefs.contains(key)) LocalDate.ofEpochDay(prefs.getLong(key, 0)) else null

    private companion object {
        const val PREFS = "astrolingo_notes"
        const val K_ON = "on"
        const val K_MINUTE = "minute_of_day"
        const val K_ANSWERED = "offer_answered"
        const val K_FIRST_OPENED = "first_opened"
        const val K_LAST_OPENED = "last_opened"
        const val K_LAST_DAILY = "last_daily"
        const val K_LAST_WEEKLY = "last_weekly"
    }
}
