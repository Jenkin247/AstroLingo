package com.astrolingo.widget

import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.HoraCalculator
import com.astrolingo.engine.places.City
import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class WidgetDayTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private val pune = City(1259229, "Pune", "Maharashtra", "IN", 18.51957, 73.85535, india, 0, emptyList())
    private val longyearbyen = City(2729907, "Longyearbyen", "Svalbard", "SJ", 78.2232, 15.6267, ZoneId.of("Arctic/Longyearbyen"), 0, emptyList())

    private fun millis(text: String, zone: ZoneId = india) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test fun `a Saturday is Shani's day in dark blue`() {
        val day = WidgetDay.at(millis("2026-10-24T10:00"), pune, india)
        assertThat(day.planet).isEqualTo(Graha.SHANI)
        assertThat(day.colour).isEqualTo(DayColour.DARK_BLUE)
    }

    @Test fun `the mood follows the lord of the planetary hour`() {
        val now = millis("2026-10-24T10:00")
        val hora = checkNotNull(HoraCalculator.horaAt(now, india, pune.latitudeDegrees, pune.longitudeDegrees))
        val day = WidgetDay.at(now, pune, india)
        assertThat(day.mood).isEqualTo(HourMood.of(hora.lord))
        assertThat(day.refreshAtUtcMillis).isEqualTo(hora.window.endUtcMillis)
    }

    @Test fun `the day changes at midnight on the place's clock`() {
        val day = WidgetDay.at(millis("2026-10-24T23:59"), pune, india)
        assertThat(day.refreshAtUtcMillis).isAtMost(millis("2026-10-25T00:00"))
        assertThat(WidgetDay.at(millis("2026-10-25T00:01"), pune, india).planet).isEqualTo(Graha.SURYA)
    }

    @Test fun `before onboarding there is a planet and colour but no mood, and it refreshes at midnight`() {
        val day = WidgetDay.at(millis("2026-10-22T09:00"), null, india)
        assertThat(day.planet).isEqualTo(Graha.GURU)
        assertThat(day.colour).isEqualTo(DayColour.YELLOW)
        assertThat(day.mood).isNull()
        assertThat(day.refreshAtUtcMillis).isEqualTo(millis("2026-10-23T00:00"))
    }

    @Test fun `with no sunrise there is no mood`() {
        val zone = longyearbyen.zone
        val day = WidgetDay.at(millis("2026-06-21T12:00", zone), longyearbyen, india)
        assertThat(day.planet).isEqualTo(Graha.SURYA)
        assertThat(day.mood).isNull()
    }

    @Test fun `every weekday lord has a mood word`() {
        listOf(Graha.SURYA, Graha.CHANDRA, Graha.MANGAL, Graha.BUDHA, Graha.GURU, Graha.SHUKRA, Graha.SHANI).forEach {
            assertThat(HourMood.of(it)).isNotNull()
        }
    }
}
