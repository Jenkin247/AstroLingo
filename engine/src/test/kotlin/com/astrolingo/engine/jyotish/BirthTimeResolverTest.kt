package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class BirthTimeResolverTest {
    private val india = ZoneId.of("Asia/Kolkata")
    private val newYork = ZoneId.of("America/New_York")

    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun `modern Indian birth uses IST`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("1990-08-15T06:30"), india)
        assertThat(r.utcEpochMillis).isEqualTo(utc("1990-08-15T01:00:00Z"))
        assertThat(r.utcOffsetSeconds).isEqualTo(19_800)
        assertThat(r.notes).isEmpty()
    }

    @Test
    fun `war time birth in India applies plus six thirty and says so`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("1943-06-01T12:00"), india)
        assertThat(r.utcOffsetSeconds).isEqualTo(23_400)
        assertThat(r.utcEpochMillis).isEqualTo(utc("1943-06-01T05:30:00Z"))
        assertThat(r.notes).containsExactly(BirthTimeNote.CLOCKS_AHEAD)
    }

    @Test
    fun `India returned to IST between the two war time spells`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("1942-07-01T12:00"), india)
        assertThat(r.utcOffsetSeconds).isEqualTo(19_800)
        assertThat(r.notes).isEmpty()
    }

    @Test
    fun `India before 1906 is flagged as local mean time`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("1900-01-01T12:00"), india)
        assertThat(r.utcOffsetSeconds).isEqualTo(5 * 3600 + 21 * 60 + 10)
        assertThat(r.notes).contains(BirthTimeNote.LOCAL_MEAN_TIME_ERA)
    }

    @Test
    fun `time skipped by spring forward moves forward by an hour`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("2025-03-09T02:30"), newYork)
        assertThat(r.utcEpochMillis).isEqualTo(utc("2025-03-09T07:30:00Z"))
        assertThat(r.notes).containsExactly(BirthTimeNote.SKIPPED_BY_CLOCK_CHANGE, BirthTimeNote.CLOCKS_AHEAD)
    }

    @Test
    fun `repeated time on fall back uses the first and offers the second`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("2025-11-02T01:30"), newYork)
        assertThat(r.utcEpochMillis).isEqualTo(utc("2025-11-02T05:30:00Z"))
        assertThat(r.laterUtcEpochMillis).isEqualTo(utc("2025-11-02T06:30:00Z"))
        assertThat(r.notes).containsExactly(BirthTimeNote.REPEATED_BY_CLOCK_CHANGE, BirthTimeNote.CLOCKS_AHEAD)
    }

    @Test
    fun `summer time birth abroad is flagged`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("2000-07-04T09:00"), ZoneId.of("Europe/London"))
        assertThat(r.utcEpochMillis).isEqualTo(utc("2000-07-04T08:00:00Z"))
        assertThat(r.notes).containsExactly(BirthTimeNote.CLOCKS_AHEAD)
    }

    @Test
    fun `explicit offset overrides the zone`() {
        val r = BirthTimeResolver.resolve(LocalDateTime.parse("1950-03-01T10:00"), utcOffsetSeconds = 4 * 3600 + 51 * 60)
        assertThat(r.utcEpochMillis).isEqualTo(utc("1950-03-01T05:09:00Z"))
        assertThat(r.notes).isEmpty()
    }
}
