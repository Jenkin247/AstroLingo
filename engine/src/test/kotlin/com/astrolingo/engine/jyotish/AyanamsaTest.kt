package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AyanamsaTest {
    @Test
    fun `Lahiri ayanamsa at J2000 is about 23 degrees 51 minutes`() {
        // Swiss Ephemeris gives 23.8571° for SE_SIDM_LAHIRI at J2000.0.
        assertThat(Ayanamsa.lahiri(0.0)).isWithin(0.001).of(23.8571)
    }

    @Test
    fun `Lahiri ayanamsa equals its defining value on 21 March 1956`() {
        val epochCenturies = (2435553.5 - 2451545.0) / 36525.0
        assertThat(Ayanamsa.lahiri(epochCenturies)).isWithin(1e-9).of(23.245524743)
    }

    @Test
    fun `ayanamsa grows by about 50 arcseconds a year`() {
        val perYear = (Ayanamsa.lahiri(0.01) - Ayanamsa.lahiri(0.0)) * 3600
        assertThat(perYear).isWithin(0.1).of(50.29)
    }
}
