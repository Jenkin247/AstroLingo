package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DailyCardRulesTest {
    private fun panchang(moonRashi: Rashi, nakshatra: Nakshatra, vara: Vara) = DayPanchang(
        date = LocalDate.parse("2025-01-01"),
        sunriseUtcMillis = 0L,
        sunsetUtcMillis = 1L,
        vara = vara,
        tithi = Tithi(1),
        moonRashi = moonRashi,
        nakshatra = NakshatraPosition(nakshatra, 1),
        rahuKaal = TimeWindow(0L, 1L),
    )

    @Test
    fun `tara counts from the birth star in cycles of nine`() {
        assertThat(Tara.of(Nakshatra.ASHWINI, Nakshatra.ASHWINI)).isEqualTo(Tara.JANMA)
        assertThat(Tara.of(Nakshatra.ASHWINI, Nakshatra.BHARANI)).isEqualTo(Tara.SAMPAT)
        assertThat(Tara.of(Nakshatra.ASHWINI, Nakshatra.ASHLESHA)).isEqualTo(Tara.PARAMA_MITRA)
        assertThat(Tara.of(Nakshatra.ASHWINI, Nakshatra.MAGHA)).isEqualTo(Tara.JANMA)
        // Wraps past Revati: from Revati, Ashwini is the 2nd star.
        assertThat(Tara.of(Nakshatra.REVATI, Nakshatra.ASHWINI)).isEqualTo(Tara.SAMPAT)
    }

    @Test
    fun `gochar houses lean the classical way`() {
        listOf(1, 3, 6, 7, 10, 11).forEach { assertThat(DailyCardRules.gocharLeaning(it)).isEqualTo(Leaning.FAVOURABLE) }
        listOf(2, 5, 9).forEach { assertThat(DailyCardRules.gocharLeaning(it)).isEqualTo(Leaning.MIXED) }
        listOf(4, 8, 12).forEach { assertThat(DailyCardRules.gocharLeaning(it)).isEqualTo(Leaning.CHALLENGING) }
    }

    @Test
    fun `strength combines gochar and tara`() {
        assertThat(DailyCardRules.strength(Leaning.FAVOURABLE, Leaning.FAVOURABLE)).isEqualTo(DayStrength.STRONG)
        assertThat(DailyCardRules.strength(Leaning.FAVOURABLE, Leaning.MIXED)).isEqualTo(DayStrength.STEADY)
        assertThat(DailyCardRules.strength(Leaning.FAVOURABLE, Leaning.CHALLENGING)).isEqualTo(DayStrength.STEADY)
        assertThat(DailyCardRules.strength(Leaning.MIXED, Leaning.CHALLENGING)).isEqualTo(DayStrength.GENTLE)
        assertThat(DailyCardRules.strength(Leaning.CHALLENGING, Leaning.CHALLENGING)).isEqualTo(DayStrength.GENTLE)
    }

    @Test
    fun `a Thursday with the Moon in the 10th and a Kshema tara is strong in yellow for Vishnu`() {
        // Fictional user: Moon in Mesha, born under Ashwini. Today's Moon in Makara (10th), in Shravana.
        val facts = DailyCardRules.facts(
            Rashi.MESHA,
            Nakshatra.ASHWINI,
            panchang(Rashi.MAKARA, Nakshatra.SHRAVANA, Vara.GURUVARA),
        )
        assertThat(facts.gocharHouse).isEqualTo(10)
        assertThat(facts.tara).isEqualTo(Tara.KSHEMA)
        assertThat(facts.strength).isEqualTo(DayStrength.STRONG)
        assertThat(facts.colour).isEqualTo(DayColour.YELLOW)
        assertThat(facts.deity).isEqualTo(Deity.VISHNU)
    }

    @Test
    fun `the Moon in the 8th on a Naidhana day is gentle`() {
        val facts = DailyCardRules.facts(
            Rashi.MESHA,
            Nakshatra.ASHWINI,
            panchang(Rashi.VRISHCHIKA, Nakshatra.VISHAKHA, Vara.SHANIVARA),
        )
        assertThat(facts.gocharHouse).isEqualTo(8)
        assertThat(facts.tara).isEqualTo(Tara.NAIDHANA)
        assertThat(facts.strength).isEqualTo(DayStrength.GENTLE)
        assertThat(facts.colour).isEqualTo(DayColour.DARK_BLUE)
    }

    @Test
    fun `a star change before midday sets the headline and both taras are kept`() {
        // Born under Ashwini. Sunrise in Mula, the 19th star, so Janma (19 is 1 in the third cycle of nine);
        // from the morning the Moon is in Purva Ashadha, the 20th star, so Sampat.
        val day = panchang(Rashi.DHANU, Nakshatra.MULA, Vara.SHANIVARA).copy(
            sunriseUtcMillis = 0L,
            sunsetUtcMillis = 12 * 3_600_000L,
            nakshatraChange = MoonChange(3 * 3_600_000L, Nakshatra.PURVA_ASHADHA),
        )
        val facts = DailyCardRules.facts(Rashi.MESHA, Nakshatra.ASHWINI, day)
        assertThat(facts.taraAtSunrise).isEqualTo(Tara.JANMA)
        assertThat(facts.taraAfterChange).isEqualTo(Tara.SAMPAT)
        assertThat(facts.tara).isEqualTo(Tara.SAMPAT)
        assertThat(facts.strength).isEqualTo(DayStrength.STEADY) // Dhanu is the 9th (mixed), Sampat favourable
    }

    @Test
    fun `a star change after midday leaves the headline on the sunrise star`() {
        val day = panchang(Rashi.DHANU, Nakshatra.MULA, Vara.SHANIVARA).copy(
            sunriseUtcMillis = 0L,
            sunsetUtcMillis = 12 * 3_600_000L,
            nakshatraChange = MoonChange(9 * 3_600_000L, Nakshatra.PURVA_ASHADHA),
        )
        val facts = DailyCardRules.facts(Rashi.MESHA, Nakshatra.ASHWINI, day)
        assertThat(facts.tara).isEqualTo(Tara.JANMA)
        assertThat(facts.taraAfterChange).isEqualTo(Tara.SAMPAT)
    }

    @Test
    fun `every weekday lord has one colour and one deity`() {
        Vara.entries.forEach {
            assertThat(DayColour.of(it.lord).graha).isEqualTo(it.lord)
            assertThat(Deity.of(it.lord).graha).isEqualTo(it.lord)
        }
    }
}
