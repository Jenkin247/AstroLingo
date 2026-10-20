package com.astrolingo.notify

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Stands in for the app: a fixed note of each kind. */
class TestApp : Application(), NoteContent {
    override fun note(context: Context, kind: NoteKind): NoteText? = when (kind) {
        NoteKind.DAILY -> NoteText("A strong day: Begin what matters", "Colour: Yellow · Deity: Vishnu", Planet.JUPITER)
        NoteKind.WEEKLY -> NoteText("A new week begins", "Thursday looks like your strongest day.", Planet.MOON)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApp::class)
class NoteRunTest {
    private val app = RuntimeEnvironment.getApplication()
    private val zone = ZoneId.of("Asia/Kolkata")
    private val monday = LocalDate.of(2026, 10, 19)
    private val notes = DailyNotes(app)
    private val manager = app.getSystemService(NotificationManager::class.java)

    private fun at(day: LocalDate, time: LocalTime = LocalTime.of(7, 31)) = ZonedDateTime.of(day, time, zone)

    private fun posted() = shadowOf(manager).allNotifications

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(app, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test fun `turning on schedules one daily job, turning off cancels it`() {
        notes.turnOn()
        val work = WorkManager.getInstance(app).getWorkInfosForUniqueWork(NoteScheduler.WORK_NAME).get()
        assertThat(work.map { it.state }).containsExactly(WorkInfo.State.ENQUEUED)
        notes.turnOff()
        val after = WorkManager.getInstance(app).getWorkInfosForUniqueWork(NoteScheduler.WORK_NAME).get()
        assertThat(after.map { it.state }).containsExactly(WorkInfo.State.CANCELLED)
    }

    @Test fun `nothing goes out while notes are off`() {
        assertThat(NoteRun.run(app, at(monday))).isNull()
        assertThat(posted()).isEmpty()
    }

    @Test fun `the morning note is quiet and shows the day`() {
        notes.turnOn()
        assertThat(NoteRun.run(app, at(monday))).isEqualTo(NoteKind.DAILY)
        val note = posted().single()
        assertThat(note.extras.getCharSequence("android.title").toString()).isEqualTo("A strong day: Begin what matters")
        assertThat(note.extras.getCharSequence("android.text").toString()).isEqualTo("Colour: Yellow · Deity: Vishnu")
        // The lock screen sees only the public version, with nothing from the chart.
        assertThat(note.publicVersion.extras.getCharSequence("android.title").toString()).isEqualTo("AstroLingo")
        val channel = manager.getNotificationChannel(NotePoster.CHANNEL_ID)
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
    }

    @Test fun `once a day at most`() {
        notes.turnOn()
        assertThat(NoteRun.run(app, at(monday))).isEqualTo(NoteKind.DAILY)
        assertThat(NoteRun.run(app, at(monday, LocalTime.of(8, 0)))).isNull()
    }

    @Test fun `away after an unanswered note, weekly on Monday, daily again once back`() {
        notes.turnOn()
        assertThat(NoteRun.run(app, at(monday))).isEqualTo(NoteKind.DAILY)
        // Not opened: the rest of the week stays quiet.
        (1L..6L).forEach { assertThat(NoteRun.run(app, at(monday.plusDays(it)))).isNull() }
        val nextMonday = monday.plusWeeks(1)
        assertThat(NoteRun.run(app, at(nextMonday))).isEqualTo(NoteKind.WEEKLY)
        assertThat(posted().single().extras.getCharSequence("android.title").toString()).isEqualTo("A new week begins")
        // They come back: the note is cleared and the daily notes return.
        notes.opened(nextMonday)
        assertThat(posted()).isEmpty()
        assertThat(NoteRun.run(app, at(nextMonday.plusDays(1)))).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `no note on a day the app was already opened`() {
        notes.turnOn()
        notes.opened(monday)
        assertThat(NoteRun.run(app, at(monday))).isNull()
    }

    @Test fun `a note hours late is left out`() {
        notes.turnOn()
        assertThat(NoteRun.run(app, at(monday, LocalTime.of(14, 0)))).isNull()
        // Not counted as sent, so tomorrow's note still goes out.
        assertThat(NoteRun.run(app, at(monday.plusDays(1)))).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `without permission nothing is posted or counted`() {
        notes.turnOn()
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(notes.needsPermission).isTrue()
        assertThat(NoteRun.run(app, at(monday))).isNull()
        assertThat(posted()).isEmpty()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(NoteRun.run(app, at(monday.plusDays(1)))).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `the offer waits for the second day and goes once answered`() {
        assertThat(notes.state(monday).ask).isFalse()
        notes.opened(monday)
        assertThat(notes.state(monday).ask).isFalse()
        assertThat(notes.state(monday.plusDays(1)).ask).isTrue()
        notes.declineOffer()
        assertThat(notes.state(monday.plusDays(1)).ask).isFalse()
        assertThat(notes.state(monday.plusDays(1)).on).isFalse()
    }

    @Test fun `the chosen time is kept`() {
        notes.setTime(LocalTime.of(6, 15))
        assertThat(notes.state().time).isEqualTo(LocalTime.of(6, 15))
        notes.turnOn()
        assertThat(NoteRun.run(app, at(monday, LocalTime.of(6, 16)))).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `the note's words stay out of logs`() {
        assertThat(NoteText("A strong day", "Colour: Yellow", Planet.SUN).toString()).doesNotContain("strong")
    }
}
