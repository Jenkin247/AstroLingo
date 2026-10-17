package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Test

class PanchangCalculatorTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private val delhiLat = 28.6139
    private val delhiLon = 77.2090
    private val twoMinutes = 2 * 60_000L

    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    private fun delhi(date: String) =
        checkNotNull(PanchangCalculator.forDay(LocalDate.parse(date), india, delhiLat, delhiLon))

    @Test
    fun `Delhi sunrise and sunset on new year 2025 match the reference within two minutes`() {
        // Reference: astropy, upper limb with standard refraction (07:13:30 and 17:35:30 IST).
        val day = delhi("2025-01-01")
        assertThat(abs(day.sunriseUtcMillis - utc("2025-01-01T01:43:30Z"))).isAtMost(twoMinutes)
        assertThat(abs(day.sunsetUtcMillis - utc("2025-01-01T12:05:30Z"))).isAtMost(twoMinutes)
        assertThat(day.vara).isEqualTo(Vara.BUDHAVARA)
    }

    @Test
    fun `London midsummer sunrise is about 04 43 BST`() {
        val day = checkNotNull(
            PanchangCalculator.forDay(LocalDate.parse("2025-06-21"), ZoneId.of("Europe/London"), 51.5072, -0.1276),
        )
        assertThat(abs(day.sunriseUtcMillis - utc("2025-06-21T03:43:00Z"))).isAtMost(twoMinutes)
    }

    @Test
    fun `the morning before the 5 November 2025 full Moon is Purnima`() {
        assertThat(delhi("2025-11-05").tithi.isPurnima).isTrue()
    }

    @Test
    fun `the morning before the 21 October 2025 new Moon is Amavasya`() {
        val tithi = delhi("2025-10-21").tithi
        assertThat(tithi.isAmavasya).isTrue()
        assertThat(tithi.isShukla).isFalse()
    }

    @Test
    fun `tithi is twelve degrees of elongation`() {
        assertThat(Tithi.of(0.0).number).isEqualTo(1)
        assertThat(Tithi.of(11.99).number).isEqualTo(1)
        assertThat(Tithi.of(12.0).number).isEqualTo(2)
        assertThat(Tithi.of(179.9).number).isEqualTo(15)
        assertThat(Tithi.of(180.0).numberInPaksha).isEqualTo(1)
        assertThat(Tithi.of(-0.1).number).isEqualTo(30)
    }

    @Test
    fun `Rahu kaal follows the weekday table`() {
        val rise = 0L
        val set = 8 * 3_600_000L // eight hour day, so each part is one hour
        assertThat(PanchangCalculator.rahuKaal(rise, set, Vara.SOMAVARA)).isEqualTo(TimeWindow(3_600_000L, 7_200_000L))
        assertThat(PanchangCalculator.rahuKaal(rise, set, Vara.RAVIVARA)).isEqualTo(TimeWindow(7 * 3_600_000L, set))
        assertThat(PanchangCalculator.rahuKaal(rise, set, Vara.SHANIVARA).startUtcMillis).isEqualTo(2 * 3_600_000L)
    }

    @Test
    fun `Rahu kaal sits inside the day`() {
        val day = delhi("2025-01-01")
        assertThat(day.rahuKaal.startUtcMillis).isAtLeast(day.sunriseUtcMillis)
        assertThat(day.rahuKaal.endUtcMillis).isAtMost(day.sunsetUtcMillis)
    }

    @Test
    fun `Moon sign and nakshatra at sunrise agree`() {
        val day = delhi("2025-03-14")
        val signOfStar = Rashi.entries[(day.nakshatra.nakshatra.ordinal * 4 + day.nakshatra.pada - 1) / 9]
        assertThat(day.moonRashi).isEqualTo(signOfStar)
    }

    @Test
    fun `the Moon entering Purva Ashadha at 9 47 am in Delhi is listed and holds for most of the day`() {
        // Reference: a scratch Swiss Ephemeris run (Lahiri), never committed: Moon crosses 253°20' at 04:17:17 UTC.
        val day = delhi("2026-10-17")
        assertThat(day.nakshatra.nakshatra).isEqualTo(Nakshatra.MULA)
        val change = checkNotNull(day.nakshatraChange)
        assertThat(change.next).isEqualTo(Nakshatra.PURVA_ASHADHA)
        assertThat(abs(change.atUtcMillis - utc("2026-10-17T04:17:17Z"))).isAtMost(60_000L)
        assertThat(day.daytimeNakshatra).isEqualTo(Nakshatra.PURVA_ASHADHA)
        assertThat(day.moonRashiChange).isNull()
        assertThat(day.daytimeMoonRashi).isEqualTo(Rashi.DHANU)
    }

    @Test
    fun `a sign change after midday keeps the sunrise sign for the card`() {
        // Reference: Moon enters Meena at 09:04:58 UTC, after Delhi's midday.
        val day = delhi("2025-01-05")
        assertThat(day.moonRashi).isEqualTo(Rashi.KUMBHA)
        val change = checkNotNull(day.moonRashiChange)
        assertThat(change.next).isEqualTo(Rashi.MEENA)
        assertThat(abs(change.atUtcMillis - utc("2025-01-05T09:04:58Z"))).isAtMost(60_000L)
        assertThat(day.daytimeMoonRashi).isEqualTo(Rashi.KUMBHA)
    }

    @Test
    fun `a day without a change lists none`() {
        val day = delhi("2025-01-01")
        assertThat(day.nakshatraChange).isNull()
        assertThat(day.moonRashiChange).isNull()
        assertThat(day.daytimeNakshatra).isEqualTo(day.nakshatra.nakshatra)
    }

    @Test
    fun `no sunrise in the polar night gives no panchang`() {
        val day = PanchangCalculator.forDay(LocalDate.parse("2025-12-21"), ZoneId.of("Europe/Oslo"), 78.22, 15.65)
        assertThat(day).isNull()
    }
}
