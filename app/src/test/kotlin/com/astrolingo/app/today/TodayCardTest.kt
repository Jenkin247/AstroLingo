package com.astrolingo.app.today

import com.astrolingo.engine.jyotish.BirthMoment
import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.jyotish.Vara
import com.astrolingo.engine.places.City
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

// Fictional people only.
class TodayCardTest {
    private val pune = City(1259229, "Pune", "Maharashtra", "IN", 18.51957, 73.85535, ZoneId.of("Asia/Kolkata"), 0, emptyList())
    private val longyearbyen = City(2729907, "Longyearbyen", "Svalbard", "SJ", 78.2232, 15.6267, ZoneId.of("Arctic/Longyearbyen"), 0, emptyList())
    private val asha = KundliCalculator.calculate(BirthMoment(650682000000, 18.5196, 73.8554))

    @Test fun `a normal day has the full card`() {
        val card = TodayCard.build(asha, LocalDate.of(2026, 10, 22), pune)
        assertThat(card.vara).isEqualTo(Vara.GURUVARA)
        assertThat(card.colour).isEqualTo(DayColour.YELLOW)
        val facts = checkNotNull(card.facts)
        assertThat(facts.gocharHouse).isIn(1..12)
        assertThat(facts.panchang.rahuKaal.startUtcMillis).isGreaterThan(facts.panchang.sunriseUtcMillis)
    }

    @Test fun `polar summer has colour and deity but no panchang`() {
        val card = TodayCard.build(asha, LocalDate.of(2026, 6, 21), longyearbyen)
        assertThat(card.facts).isNull()
        assertThat(card.vara).isEqualTo(Vara.RAVIVARA)
        assertThat(card.colour).isEqualTo(DayColour.ORANGE)
    }

    @Test fun `wordings rotate by day and never repeat within three days`() {
        val wordings = listOf("a", "b", "c")
        assertThat((10..12).map { wordings.forDay(it) }).containsExactly("b", "c", "a").inOrder()
    }

    @Test fun `card never prints the place`() {
        assertThat(TodayCard.build(asha, LocalDate.of(2026, 10, 22), pune).toString()).doesNotContain("Pune")
    }
}
