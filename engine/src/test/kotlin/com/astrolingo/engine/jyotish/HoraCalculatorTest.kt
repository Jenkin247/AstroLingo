package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class HoraCalculatorTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private val puneLat = 18.5196
    private val puneLon = 73.8554
    private val hour = 3_600_000L

    // A made-up day: sunrise 06:00, sunset 18:00, next sunrise 06:00 (UTC millis from 0 for easy reading).
    private val sunrise = 6 * hour
    private val sunset = 18 * hour
    private val nextSunrise = 30 * hour

    @Test fun `the first hora after sunrise belongs to the lord of the day`() {
        Vara.entries.forEach { vara ->
            assertThat(HoraCalculator.horaAt(sunrise, vara, sunrise, sunset, nextSunrise).lord).isEqualTo(vara.lord)
        }
    }

    @Test fun `a Sunday runs Surya, Shukra, Budha, Chandra, Shani, Guru, Mangal and round again`() {
        val lords = (0 until 8).map { HoraCalculator.horaAt(sunrise + it * hour + 1, Vara.RAVIVARA, sunrise, sunset, nextSunrise).lord }
        assertThat(lords).containsExactly(
            Graha.SURYA, Graha.SHUKRA, Graha.BUDHA, Graha.CHANDRA, Graha.SHANI, Graha.GURU, Graha.MANGAL, Graha.SURYA,
        ).inOrder()
    }

    @Test fun `the 25th hora would be the next day's lord, so the order is unbroken`() {
        // The last night hora of Sunday is the 24th; one step on from it is Chandra, Monday's lord.
        val last = HoraCalculator.horaAt(nextSunrise - 1, Vara.RAVIVARA, sunrise, sunset, nextSunrise)
        assertThat(last.lord).isEqualTo(Graha.BUDHA)
    }

    @Test fun `night horas split sunset to sunrise into twelve`() {
        val longNight = HoraCalculator.horaAt(sunset, Vara.SHANIVARA, sunrise, sunset - 2 * hour, nextSunrise)
        assertThat(longNight.window.endUtcMillis - longNight.window.startUtcMillis).isEqualTo(14 * hour / 12)
    }

    @Test fun `in Pune the hora at sunrise on a Saturday is Shani's and ends about an hour later`() {
        val day = checkNotNull(PanchangCalculator.forDay(LocalDate.of(2026, 10, 24), india, puneLat, puneLon))
        val hora = checkNotNull(HoraCalculator.horaAt(day.sunriseUtcMillis + 1, india, puneLat, puneLon))
        assertThat(hora.lord).isEqualTo(Graha.SHANI)
        assertThat(hora.window.startUtcMillis).isEqualTo(day.sunriseUtcMillis)
        assertThat(hora.window.endUtcMillis - hora.window.startUtcMillis).isIn(55 * 60_000L..60 * 60_000L)
    }

    @Test fun `before sunrise it is still the previous day's night`() {
        val saturday = checkNotNull(PanchangCalculator.forDay(LocalDate.of(2026, 10, 24), india, puneLat, puneLon))
        val friday = checkNotNull(PanchangCalculator.forDay(LocalDate.of(2026, 10, 23), india, puneLat, puneLon))
        val hora = checkNotNull(HoraCalculator.horaAt(saturday.sunriseUtcMillis - 1, india, puneLat, puneLon))
        // The 24th hora of a Friday (Shukra) is Chandra, one step before Saturday's Shani, and it ends at Saturday's sunrise.
        assertThat(hora.lord).isEqualTo(HoraCalculator.horaAt(saturday.sunriseUtcMillis - 1, Vara.SHUKRAVARA, friday.sunriseUtcMillis, friday.sunsetUtcMillis, saturday.sunriseUtcMillis).lord)
        assertThat(hora.lord).isEqualTo(Graha.CHANDRA)
        assertThat(hora.window.endUtcMillis).isEqualTo(saturday.sunriseUtcMillis)
    }

    @Test fun `there is no hora in a polar summer`() {
        val noon = LocalDate.of(2026, 6, 21).atTime(12, 0).atZone(ZoneId.of("Arctic/Longyearbyen")).toInstant().toEpochMilli()
        assertThat(HoraCalculator.horaAt(noon, ZoneId.of("Arctic/Longyearbyen"), 78.2232, 15.6267)).isNull()
    }
}
