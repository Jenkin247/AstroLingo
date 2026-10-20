// The daily job: decides whether a note is due, asks the app for its words and posts it. All on the phone.
package com.astrolingo.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.ZonedDateTime

class NoteWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        NoteRun.run(applicationContext, ZonedDateTime.now())
        return Result.success()
    }
}

/** One run of the daily job, separate from the worker so tests can call it with a chosen clock. */
internal object NoteRun {
    /** Returns the kind of note posted, or null when none went out. */
    fun run(context: Context, now: ZonedDateTime): NoteKind? {
        val notes = DailyNotes(context)
        if (!notes.isOn) return null
        val time = notes.time
        val kind = post(context, notes, now)
        // Summer time or a long sleep moves the 24-hour job; set it again so the next note keeps to the clock. Last,
        // because setting it again stops this run.
        if (NotePlan.drifted(now.toLocalTime(), time)) NoteScheduler.schedule(context, time, now)
        return kind
    }

    private fun post(context: Context, notes: DailyNotes, now: ZonedDateTime): NoteKind? {
        if (!NotePlan.onTime(now.toLocalTime(), notes.time)) return null
        val today = now.toLocalDate()
        val kind = notes.due(today) ?: return null
        val content = context.applicationContext as? NoteContent ?: return null
        // A note that can't be worked out (no profile yet, or it can't be read) is simply left out.
        val text = runCatching { content.note(context, kind) }.getOrNull() ?: return null
        if (!NotePoster.post(context, text)) return null
        notes.sent(kind, today)
        return kind
    }
}

/** Sets the schedule again when the user changes the phone's time or time zone, so 7:30 stays 7:30 where they are. */
class ClockReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED && intent.action != Intent.ACTION_TIME_CHANGED) return
        val notes = DailyNotes(context)
        if (notes.isOn) NoteScheduler.schedule(context, notes.time)
    }
}
