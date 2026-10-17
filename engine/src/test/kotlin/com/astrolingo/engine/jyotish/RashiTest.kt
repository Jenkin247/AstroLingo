package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RashiTest {
    @Test
    fun `zero degrees is Mesha`() {
        assertThat(Rashi.of(0.0)).isEqualTo(Rashi.MESHA)
    }

    @Test
    fun `sign boundaries belong to the next sign`() {
        assertThat(Rashi.of(29.9999)).isEqualTo(Rashi.MESHA)
        assertThat(Rashi.of(30.0)).isEqualTo(Rashi.VRISHABHA)
        assertThat(Rashi.of(359.9999)).isEqualTo(Rashi.MEENA)
    }

    @Test
    fun `longitudes outside 0 to 360 are normalised`() {
        assertThat(Rashi.of(360.0)).isEqualTo(Rashi.MESHA)
        assertThat(Rashi.of(-15.0)).isEqualTo(Rashi.MEENA)
        assertThat(Rashi.of(725.0)).isEqualTo(Rashi.MESHA)
    }

    @Test
    fun `sign numbers and lords follow the classical order`() {
        assertThat(Rashi.KANYA.number).isEqualTo(6)
        assertThat(Rashi.SIMHA.lord).isEqualTo(Graha.SURYA)
        assertThat(Rashi.KUMBHA.lord).isEqualTo(Graha.SHANI)
    }
}
