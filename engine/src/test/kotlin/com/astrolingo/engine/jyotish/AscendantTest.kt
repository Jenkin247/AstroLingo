package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AscendantTest {
    private val obliquity = 23.4393

    @Test
    fun `on the equator with sidereal time 0 the tropical ascendant is 90 degrees`() {
        assertThat(Ascendant.tropical(0.0, 0.0, 0.0, obliquity)).isWithin(1e-9).of(90.0)
    }

    @Test
    fun `on the equator with sidereal time 6h the tropical ascendant is 180 degrees`() {
        assertThat(Ascendant.tropical(6.0, 0.0, 0.0, obliquity)).isWithin(1e-9).of(180.0)
    }

    @Test
    fun `east longitude adds to local sidereal time`() {
        val fromLongitude = Ascendant.tropical(0.0, 90.0, 18.5, obliquity)
        val fromTime = Ascendant.tropical(6.0, 0.0, 18.5, obliquity)
        assertThat(fromLongitude).isWithin(1e-9).of(fromTime)
    }

    @Test
    fun `mean obliquity at J2000 is 23 degrees 26 minutes 21 seconds`() {
        assertThat(Ascendant.meanObliquity(0.0)).isWithin(1e-6).of(84381.406 / 3600)
    }
}
