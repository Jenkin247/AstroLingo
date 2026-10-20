// Shows the note. Quiet by design: a low-importance channel (no sound, no pop-up), one note at a time, and the
// lock screen shows only "Your day is ready" until the phone is unlocked.
package com.astrolingo.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotePoster {
    internal const val CHANNEL_ID = "daily_note"
    internal const val NOTE_ID = 1

    /** Posts [note], replacing any earlier one. Returns false when the phone doesn't allow it. */
    fun post(context: Context, note: NoteText): Boolean {
        if (!allowed(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.createNotificationChannel(channel(context))
        manager.notify(NOTE_ID, build(context, note))
        return true
    }

    /** Removes the note, once the user has opened the app or turned notes off. */
    fun clear(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTE_ID)
    }

    private fun allowed(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun channel(context: Context) = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.note_channel_name),
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = context.getString(R.string.note_channel_description)
        setShowBadge(false)
        enableVibration(false)
    }

    internal fun build(context: Context, note: NoteText): Notification {
        val picture = PlanetArt.scene(note.planet)
        val icon = PlanetArt.icon(note.planet)
        // Taps open the app at its start screen; the note itself carries no data.
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
            PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        // What shows on a locked phone: no words from the user's chart.
        val public = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle(context.getString(R.string.note_public_title))
            .setContentText(context.getString(R.string.note_public_text))
            .build()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note)
            .setColor(ContextCompat.getColor(context, R.color.note_accent))
            .setLargeIcon(icon)
            .setContentTitle(note.title)
            .setContentText(note.text)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(picture)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(note.text),
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .apply { if (open != null) setContentIntent(open) }
            .build()
    }
}
