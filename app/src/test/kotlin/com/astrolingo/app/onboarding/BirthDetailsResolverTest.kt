package com.astrolingo.app.onboarding

import com.astrolingo.engine.jyotish.BirthTimeNote
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.engine.places.HistoricCityClock
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Test

class BirthDetailsResolverTest {
    private val index = CityIndex.bundled()
    private val pune = index.search("Pune").first()
    private val mumbai = index.search("Mumbai").first()
    private val navsari = index.search("Navsari").first()
    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun `known time in Pune converts with IST`() {
        val r = BirthDetailsResolver.resolve(BirthDetails(LocalDate.of(1990, 8, 15), LocalTime.of(6, 30), pune), index)
        assertThat(r.moment.utcEpochMillis).isEqualTo(utc("1990-08-15T01:00:00Z"))
        assertThat(r.moment.latitudeDegrees).isEqualTo(pune.latitudeDegrees)
        assertThat(r.timeKnown).isTrue()
    }

    @Test
    fun `unknown time uses local noon and says so`() {
        val r = BirthDetailsResolver.resolve(BirthDetails(LocalDate.of(1990, 8, 15), null, pune), index)
        assertThat(r.moment.utcEpochMillis).isEqualTo(utc("1990-08-15T06:30:00Z"))
        assertThat(r.timeKnown).isFalse()
    }

    @Test
    fun `old Bombay record asks about the city clock and applies it when chosen`() {
        val details = BirthDetails(LocalDate.of(1950, 3, 1), LocalTime.of(10, 0), mumbai)
        assertThat(BirthDetailsResolver.cityClockToAsk(details, index)).isEqualTo(HistoricCityClock.BOMBAY_TIME)
        val ist = BirthDetailsResolver.resolve(details, index)
        val bombay = BirthDetailsResolver.resolve(details.copy(useCityClock = true), index)
        assertThat(ist.moment.utcEpochMillis).isEqualTo(utc("1950-03-01T04:30:00Z"))
        assertThat(bombay.moment.utcEpochMillis).isEqualTo(utc("1950-03-01T05:09:00Z"))
    }

    @Test
    fun `no city clock question without a birth time`() {
        val details = BirthDetails(LocalDate.of(1950, 3, 1), null, mumbai)
        assertThat(BirthDetailsResolver.cityClockToAsk(details, index)).isNull()
    }

    @Test
    fun `war time birth carries its note through`() {
        val r = BirthDetailsResolver.resolve(BirthDetails(LocalDate.of(1943, 6, 1), LocalTime.NOON, pune), index)
        assertThat(r.notes).contains(BirthTimeNote.CLOCKS_AHEAD)
    }

    // Test users, round 2: a fictional person born 14 Sep 1981 in Navsari with no known time. The Moon moved
    // from Kumbha to Meena that afternoon, so noon alone gave the wrong Moon sign.
    @Test
    fun `unknown time on a day the Moon changes sign asks for the part of the day`() {
        val details = BirthDetails(LocalDate.of(1981, 9, 14), null, navsari)
        val byPart = checkNotNull(BirthDetailsResolver.moonByDayPart(details, index))
        assertThat(byPart.getValue(DayPart.MORNING).map { it.rashi }.distinct()).containsExactly(Rashi.KUMBHA)
        assertThat(byPart.getValue(DayPart.AFTERNOON).map { it.rashi }.distinct()).containsExactly(Rashi.KUMBHA, Rashi.MEENA)
        assertThat(byPart.getValue(DayPart.EVENING).map { it.rashi }.distinct()).containsExactly(Rashi.MEENA)
        assertThat(BirthDetailsResolver.resolve(details, index).moonUncertain).isTrue()

        val evening = BirthDetailsResolver.resolve(details.copy(dayPart = DayPart.EVENING), index)
        // The nakshatra can still change within the evening; then the chart says so.
        assertThat(evening.moonUncertain).isEqualTo(byPart.getValue(DayPart.EVENING).size > 1)
        assertThat(evening.moment.utcEpochMillis).isEqualTo(utc("1981-09-14T15:30:00Z"))
        assertThat(KundliCalculator.calculate(evening.moment).moonSign).isEqualTo(Rashi.MEENA)
    }

    @Test
    fun `known time never asks about the Moon`() {
        val details = BirthDetails(LocalDate.of(1981, 9, 14), LocalTime.of(19, 0), navsari)
        assertThat(BirthDetailsResolver.moonByDayPart(details, index)).isNull()
        assertThat(BirthDetailsResolver.resolve(details, index).moonUncertain).isFalse()
    }

    @Test
    fun `a day when the Moon stays put asks nothing`() {
        // The Moon spends about a day in each nakshatra, so some days have no change at all.
        val quiet = (1..30).map { LocalDate.of(1990, 8, it) }
            .first { BirthDetailsResolver.moonByDayPart(BirthDetails(it, null, pune), index) == null }
        assertThat(BirthDetailsResolver.resolve(BirthDetails(quiet, null, pune), index).moonUncertain).isFalse()
    }
}
