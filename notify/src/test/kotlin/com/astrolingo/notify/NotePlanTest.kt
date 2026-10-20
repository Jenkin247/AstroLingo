package com.astrolingo.notify

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class NotePlanTest {
    private val monday = LocalDate.of(2026, 10, 19)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)

    @Test fun `first note is the daily one`() {
        assertThat(NotePlan.decide(monday, lastOpened = monday.minusDays(1), lastDaily = null, lastWeekly = null)).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `no note on a day the app was already opened`() {
        assertThat(NotePlan.decide(tuesday, lastOpened = tuesday, lastDaily = monday, lastWeekly = null)).isNull()
    }

    @Test fun `never two notes in a day`() {
        assertThat(NotePlan.decide(tuesday, lastOpened = monday, lastDaily = tuesday, lastWeekly = null)).isNull()
        assertThat(NotePlan.decide(monday, lastOpened = null, lastDaily = monday.minusDays(3), lastWeekly = monday)).isNull()
    }

    @Test fun `daily notes go on while the user keeps coming back`() {
        // Note on Monday morning, app opened Monday afternoon: Tuesday's note goes out.
        assertThat(NotePlan.decide(tuesday, lastOpened = monday, lastDaily = monday, lastWeekly = null)).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `a daily note left unanswered stops the daily notes`() {
        // Note on Monday, app not opened since Sunday: nothing on Tuesday or Wednesday.
        val sunday = monday.minusDays(1)
        assertThat(NotePlan.decide(tuesday, lastOpened = sunday, lastDaily = monday, lastWeekly = null)).isNull()
        assertThat(NotePlan.decide(wednesday, lastOpened = sunday, lastDaily = monday, lastWeekly = null)).isNull()
    }

    @Test fun `while away, one note each Monday about the week`() {
        val nextMonday = monday.plusWeeks(1)
        assertThat(NotePlan.decide(nextMonday, lastOpened = monday.minusDays(1), lastDaily = monday, lastWeekly = null)).isEqualTo(NoteKind.WEEKLY)
        // And the Monday after, if they still haven't come back.
        assertThat(NotePlan.decide(nextMonday.plusWeeks(1), lastOpened = monday.minusDays(1), lastDaily = monday, lastWeekly = nextMonday))
            .isEqualTo(NoteKind.WEEKLY)
    }

    @Test fun `coming back brings the daily notes back`() {
        val nextMonday = monday.plusWeeks(1)
        assertThat(NotePlan.decide(nextMonday.plusDays(1), lastOpened = nextMonday, lastDaily = monday, lastWeekly = nextMonday)).isEqualTo(NoteKind.DAILY)
    }

    @Test fun `never opened and never told counts as present`() {
        assertThat(NotePlan.isAway(lastOpened = null, lastDaily = null)).isFalse()
        assertThat(NotePlan.isAway(lastOpened = null, lastDaily = monday)).isTrue()
    }

    @Test fun `next run is later today or tomorrow`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val early = ZonedDateTime.of(monday, LocalTime.of(6, 0), zone)
        assertThat(NotePlan.nextRun(early, LocalTime.of(7, 30))).isEqualTo(ZonedDateTime.of(monday, LocalTime.of(7, 30), zone))
        val late = ZonedDateTime.of(monday, LocalTime.of(7, 30), zone)
        assertThat(NotePlan.nextRun(late, LocalTime.of(7, 30))).isEqualTo(ZonedDateTime.of(tuesday, LocalTime.of(7, 30), zone))
    }

    @Test fun `next run keeps the clock time across a summer time change`() {
        val london = ZoneId.of("Europe/London")
        // Clocks go back on Sunday 25 October 2026; the note stays at 7:30 on the wall clock.
        val saturdayEvening = ZonedDateTime.of(LocalDate.of(2026, 10, 24), LocalTime.of(20, 0), london)
        val next = NotePlan.nextRun(saturdayEvening, LocalTime.of(7, 30))
        assertThat(next.toLocalTime()).isEqualTo(LocalTime.of(7, 30))
        assertThat(NotePlan.delayUntilNext(saturdayEvening, LocalTime.of(7, 30)).toMinutes()).isEqualTo(12 * 60 + 30)
    }

    @Test fun `on time up to three hours late, and an hour early`() {
        val time = LocalTime.of(7, 30)
        assertThat(NotePlan.onTime(LocalTime.of(7, 30), time)).isTrue()
        assertThat(NotePlan.onTime(LocalTime.of(10, 30), time)).isTrue()
        assertThat(NotePlan.onTime(LocalTime.of(10, 31), time)).isFalse()
        assertThat(NotePlan.onTime(LocalTime.of(6, 30), time)).isTrue()
        assertThat(NotePlan.onTime(LocalTime.of(6, 29), time)).isFalse()
    }

    @Test fun `lateness wraps round midnight`() {
        assertThat(NotePlan.minutesLate(LocalTime.of(0, 10), LocalTime.of(23, 50))).isEqualTo(20)
        assertThat(NotePlan.minutesLate(LocalTime.of(23, 50), LocalTime.of(0, 10))).isEqualTo(-20)
    }

    @Test fun `drift is anything over twenty minutes`() {
        assertThat(NotePlan.drifted(LocalTime.of(7, 45), LocalTime.of(7, 30))).isFalse()
        assertThat(NotePlan.drifted(LocalTime.of(8, 30), LocalTime.of(7, 30))).isTrue()
        assertThat(NotePlan.drifted(LocalTime.of(6, 30), LocalTime.of(7, 30))).isTrue()
    }
}
