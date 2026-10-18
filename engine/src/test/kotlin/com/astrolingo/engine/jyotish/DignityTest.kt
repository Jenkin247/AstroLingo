package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DignityTest {
    @Test
    fun `exaltation and debilitation follow the classics`() {
        assertThat(Dignity.of(Graha.SURYA, Rashi.MESHA)).isEqualTo(Dignity.EXALTED)
        assertThat(Dignity.of(Graha.SURYA, Rashi.TULA)).isEqualTo(Dignity.DEBILITATED)
        assertThat(Dignity.of(Graha.CHANDRA, Rashi.VRISHCHIKA)).isEqualTo(Dignity.DEBILITATED)
        assertThat(Dignity.of(Graha.GURU, Rashi.MAKARA)).isEqualTo(Dignity.DEBILITATED)
        assertThat(Dignity.of(Graha.SHANI, Rashi.MESHA)).isEqualTo(Dignity.DEBILITATED)
    }

    @Test
    fun `exaltation wins over own sign`() {
        assertThat(Dignity.of(Graha.BUDHA, Rashi.KANYA)).isEqualTo(Dignity.EXALTED)
        assertThat(Dignity.of(Graha.BUDHA, Rashi.MITHUNA)).isEqualTo(Dignity.OWN_SIGN)
    }

    @Test
    fun `own, friend, neutral and enemy signs come from the sign lord`() {
        assertThat(Dignity.of(Graha.SURYA, Rashi.SIMHA)).isEqualTo(Dignity.OWN_SIGN)
        assertThat(Dignity.of(Graha.SURYA, Rashi.DHANU)).isEqualTo(Dignity.FRIEND)
        assertThat(Dignity.of(Graha.SURYA, Rashi.MITHUNA)).isEqualTo(Dignity.NEUTRAL)
        assertThat(Dignity.of(Graha.SURYA, Rashi.MAKARA)).isEqualTo(Dignity.ENEMY)
        assertThat(Dignity.of(Graha.CHANDRA, Rashi.MAKARA)).isEqualTo(Dignity.NEUTRAL)
    }

    @Test
    fun `every graha but the nodes has a dignity in every sign`() {
        Graha.entries.forEach { graha ->
            Rashi.entries.forEach { rashi ->
                val dignity = Dignity.of(graha, rashi)
                if (graha == Graha.RAHU || graha == Graha.KETU) assertThat(dignity).isNull() else assertThat(dignity).isNotNull()
            }
        }
    }

    @Test
    fun `each graha is exalted in one sign and debilitated in the opposite one`() {
        Graha.entries.filter { it != Graha.RAHU && it != Graha.KETU }.forEach { graha ->
            assertThat(Rashi.entries.count { Dignity.of(graha, it) == Dignity.EXALTED }).isEqualTo(1)
            assertThat(Rashi.entries.count { Dignity.of(graha, it) == Dignity.DEBILITATED }).isEqualTo(1)
        }
    }

    @Test
    fun `elements repeat fire, earth, air, water`() {
        assertThat(Rashi.MESHA.element).isEqualTo(Element.FIRE)
        assertThat(Rashi.SIMHA.element).isEqualTo(Element.FIRE)
        assertThat(Rashi.DHANU.element).isEqualTo(Element.FIRE)
        assertThat(Rashi.MAKARA.element).isEqualTo(Element.EARTH)
        assertThat(Rashi.TULA.element).isEqualTo(Element.AIR)
        assertThat(Rashi.MEENA.element).isEqualTo(Element.WATER)
    }
}
