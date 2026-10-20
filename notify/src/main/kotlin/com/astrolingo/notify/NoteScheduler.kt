// The schedule (ADR 0010): one periodic WorkManager job a day at the chosen time. No exact alarms, so no special
// permission; WorkManager keeps the job across reboots and app updates.
package com.astrolingo.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NoteScheduler {
    internal const val WORK_NAME = "daily_note"

    /** Runs the note every 24 hours, first at the next [time]. Replaces any schedule already set. */
    fun schedule(context: Context, time: LocalTime, now: ZonedDateTime = ZonedDateTime.now()) {
        val delay = NotePlan.delayUntilNext(now, time)
        val request = PeriodicWorkRequestBuilder<NoteWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .build()
        // A fresh job, not an update: WorkManager uses the first delay only for a job's first run, so an update would
        // keep the old time of day.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
