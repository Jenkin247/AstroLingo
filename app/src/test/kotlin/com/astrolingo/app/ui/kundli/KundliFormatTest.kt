package com.astrolingo.app.ui.kundli

import com.astrolingo.engine.jyotish.Nakshatra
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KundliFormatTest {
    @Test
    fun `degrees show whole degrees and minutes`() {
        assertThat(formatDegrees(28.2)).isEqualTo("28°12′")
        assertThat(formatDegrees(0.0)).isEqualTo("0°00′")
        assertThat(formatDegrees(29.999)).isEqualTo("29°59′")
    }

    @Test
    fun `enum names read as words`() {
        assertThat(Nakshatra.PURVA_PHALGUNI.displayName()).isEqualTo("Purva Phalguni")
    }

    @Test
    fun `sample chart is calculated`() {
        assertThat(SampleChart.kundli.grahas).hasSize(9)
    }
}
