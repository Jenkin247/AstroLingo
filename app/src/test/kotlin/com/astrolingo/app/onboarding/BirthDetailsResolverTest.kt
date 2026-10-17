package com.astrolingo.app.onboarding

import com.astrolingo.engine.jyotish.BirthTimeNote
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
}
