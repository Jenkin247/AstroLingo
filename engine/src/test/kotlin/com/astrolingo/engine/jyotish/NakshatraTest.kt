package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NakshatraTest {
    @Test
    fun `zero degrees is Ashwini pada 1`() {
        assertThat(Nakshatra.positionOf(0.0)).isEqualTo(NakshatraPosition(Nakshatra.ASHWINI, 1))
    }

    @Test
    fun `Rohini spans 40 to 53 degrees 20 minutes`() {
        // Worked example from the proposal: a birth Moon in Rohini, the 4th nakshatra.
        assertThat(Nakshatra.positionOf(40.0).nakshatra).isEqualTo(Nakshatra.ROHINI)
        assertThat(Nakshatra.positionOf(53.33).nakshatra).isEqualTo(Nakshatra.ROHINI)
        assertThat(Nakshatra.positionOf(53.34).nakshatra).isEqualTo(Nakshatra.MRIGASHIRA)
        assertThat(Nakshatra.ROHINI.number).isEqualTo(4)
    }

    @Test
    fun `padas split each nakshatra into four quarters of 3 degrees 20 minutes`() {
        assertThat(Nakshatra.positionOf(3.3).pada).isEqualTo(1)
        assertThat(Nakshatra.positionOf(3.34).pada).isEqualTo(2)
        assertThat(Nakshatra.positionOf(10.01).pada).isEqualTo(4)
    }

    @Test
    fun `Hasta is the 13th nakshatra in Kanya`() {
        val hastaMiddle = 12 * Nakshatra.SPAN_DEGREES + Nakshatra.SPAN_DEGREES / 2
        assertThat(Nakshatra.positionOf(hastaMiddle).nakshatra).isEqualTo(Nakshatra.HASTA)
        assertThat(Rashi.of(hastaMiddle)).isEqualTo(Rashi.KANYA)
    }

    @Test
    fun `the last degree of the zodiac is Revati pada 4`() {
        assertThat(Nakshatra.positionOf(359.999)).isEqualTo(NakshatraPosition(Nakshatra.REVATI, 4))
    }

    @Test
    fun `dasha lords repeat every nine nakshatras`() {
        assertThat(Nakshatra.MAGHA.lord).isEqualTo(Nakshatra.ASHWINI.lord)
        assertThat(Nakshatra.MULA.lord).isEqualTo(Graha.KETU)
        assertThat(Nakshatra.REVATI.lord).isEqualTo(Graha.BUDHA)
    }
}
