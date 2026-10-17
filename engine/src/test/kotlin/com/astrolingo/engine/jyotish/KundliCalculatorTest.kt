package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * Smoke tests for the kundli pipeline. Reference values are independent published figures where
 * noted; the Astro Guru's 50-chart golden set (checked against Jagannatha Hora) replaces these
 * as the accuracy gate in sprint 1. Test people are fictional.
 */
class KundliCalculatorTest {
    private val delhi = BirthMoment(millis("2000-01-01T12:00:00Z"), latitudeDegrees = 28.6139, longitudeDegrees = 77.2090)
    private val pune = BirthMoment(0, latitudeDegrees = 18.5204, longitudeDegrees = 73.8567)
    private val oneArcminute = 1.0 / 60.0

    @Test
    fun `J2000 chart matches published tropical positions minus ayanamsa`() {
        val kundli = KundliCalculator.calculate(delhi)
        // Apparent tropical longitudes at 2000-01-01 12:00 (Meeus / JPL), minus Lahiri 23.8571°.
        // Only the Sun and Moon are asserted tightly here; planets join with the golden set.
        assertThat(kundli.of(Graha.SURYA).siderealLongitude).isWithin(2 * oneArcminute).of(280.3689 - 23.8571)
        assertThat(kundli.of(Graha.SURYA).rashi).isEqualTo(Rashi.DHANU)
        assertThat(kundli.of(Graha.CHANDRA).rashi).isEqualTo(Rashi.TULA)
        assertThat(kundli.of(Graha.GURU).rashi).isEqualTo(Rashi.MESHA)
        assertThat(kundli.of(Graha.SHANI).rashi).isEqualTo(Rashi.MESHA)
    }

    @Test
    fun `Sun enters sidereal Makara on Makar Sankranti, 14 January 2026`() {
        // Published panchangs give the Sankranti moment at about 15:05 to 15:15 IST (09:35 to 09:45 UTC).
        val before = KundliCalculator.calculate(pune.copy(utcEpochMillis = millis("2026-01-14T09:00:00Z")))
        val after = KundliCalculator.calculate(pune.copy(utcEpochMillis = millis("2026-01-14T10:30:00Z")))
        assertThat(before.of(Graha.SURYA).rashi).isEqualTo(Rashi.DHANU)
        assertThat(after.of(Graha.SURYA).rashi).isEqualTo(Rashi.MAKARA)
    }

    @Test
    fun `Ketu is always opposite Rahu`() {
        val kundli = KundliCalculator.calculate(delhi)
        val gap = normalizeDegrees(kundli.of(Graha.KETU).siderealLongitude - kundli.of(Graha.RAHU).siderealLongitude)
        assertThat(gap).isWithin(1e-9).of(180.0)
        assertThat(kundli.of(Graha.KETU).house).isEqualTo((kundli.of(Graha.RAHU).house + 5) % 12 + 1)
    }

    @Test
    fun `the lagna sign is house 1 and houses count signs from it`() {
        val kundli = KundliCalculator.calculate(delhi)
        assertThat(KundliCalculator.wholeSignHouse(kundli.lagna, kundli.lagna)).isEqualTo(1)
        assertThat(KundliCalculator.wholeSignHouse(Rashi.MEENA, Rashi.MESHA)).isEqualTo(12)
        assertThat(KundliCalculator.wholeSignHouse(Rashi.MESHA, Rashi.MEENA)).isEqualTo(2)
        kundli.grahas.forEach { position ->
            assertThat(position.house).isEqualTo(KundliCalculator.wholeSignHouse(position.rashi, kundli.lagna))
        }
    }

    @Test
    fun `every graha has a consistent rashi and nakshatra`() {
        val kundli = KundliCalculator.calculate(delhi)
        assertThat(kundli.grahas.map { it.graha }).containsExactlyElementsIn(Graha.entries).inOrder()
        kundli.grahas.forEach { position ->
            assertThat(position.siderealLongitude).isAtLeast(0.0)
            assertThat(position.siderealLongitude).isLessThan(360.0)
            assertThat(position.rashi).isEqualTo(Rashi.of(position.siderealLongitude))
            assertThat(position.nakshatra).isEqualTo(Nakshatra.positionOf(position.siderealLongitude))
        }
        assertThat(kundli.moonSign).isEqualTo(kundli.of(Graha.CHANDRA).rashi)
    }

    @Test
    fun `a kundli takes well under one second`() {
        val start = System.nanoTime()
        KundliCalculator.calculate(pune.copy(utcEpochMillis = millis("1990-05-05T05:05:00Z")))
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000
        assertThat(elapsedMillis).isLessThan(1000L)
    }

    @Test
    fun `Moon chart puts the Moon's sign in house 1 and keeps every graha's sign`() {
        val kundli = KundliCalculator.calculate(delhi)
        val moon = kundli.fromMoon()
        assertThat(moon.lagna).isEqualTo(Rashi.TULA)
        assertThat(moon.of(Graha.CHANDRA).house).isEqualTo(1)
        // Guru in Mesha is the 7th sign from Tula.
        assertThat(moon.of(Graha.GURU).house).isEqualTo(7)
        assertThat(moon.grahas.map { it.rashi }).isEqualTo(kundli.grahas.map { it.rashi })
        assertThat(moon.fromMoon()).isEqualTo(moon)
    }

    @Test
    fun `Western Sun sign is one sign ahead for most births`() {
        // 1 January 2000: tropical Sun 280° is Capricorn; sidereal 256° is Dhanu.
        assertThat(KundliCalculator.calculate(delhi).westernSunSign).isEqualTo(Rashi.MAKARA)
    }

    @Test
    fun `Western Sun on a cusp names the neighbouring sign`() {
        // Tropical Sun 270.5°: half a degree into Capricorn, so Sagittarius is next door.
        val kundli = KundliCalculator.calculate(delhi)
        val sun = kundli.of(Graha.SURYA)
        fun withTropicalSun(tropical: Double) = kundli.copy(
            grahas = kundli.grahas.map { if (it.graha == Graha.SURYA) sun.copy(siderealLongitude = tropical - kundli.ayanamsa) else it },
        )
        assertThat(withTropicalSun(270.5).westernSunCuspNeighbour()).isEqualTo(Rashi.DHANU)
        assertThat(withTropicalSun(275.0).westernSunCuspNeighbour()).isNull()
    }

    @Test
    fun `lagna near a sign edge names the neighbouring sign`() {
        val kundli = KundliCalculator.calculate(delhi)
        fun at(longitude: Double) = kundli.copy(lagnaLongitude = longitude, lagna = Rashi.of(longitude))
        assertThat(at(30.5).lagnaNeighbourIfNearEdge()).isEqualTo(Rashi.MESHA)
        assertThat(at(59.0).lagnaNeighbourIfNearEdge()).isEqualTo(Rashi.MITHUNA)
        assertThat(at(359.5).lagnaNeighbourIfNearEdge()).isEqualTo(Rashi.MESHA)
        assertThat(at(0.5).lagnaNeighbourIfNearEdge()).isEqualTo(Rashi.MEENA)
        assertThat(at(45.0).lagnaNeighbourIfNearEdge()).isNull()
    }

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()
}
