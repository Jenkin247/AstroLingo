package com.astrolingo.engine.chart

import com.astrolingo.engine.jyotish.BirthMoment
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.jyotish.Rashi
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class ChartLayoutTest {
    // Fictional: Asha Testwal, 1990-08-15 01:00 UTC, Pune.
    private val kundli = KundliCalculator.calculate(
        BirthMoment(Instant.parse("1990-08-15T01:00:00Z").toEpochMilli(), 18.5204, 73.8567),
    )

    private fun area(points: List<ChartPoint>): Double =
        points.indices.sumOf { i ->
            val p = points[i]
            val q = points[(i + 1) % points.size]
            p.x * q.y - q.x * p.y
        }.let { kotlin.math.abs(it) / 2 }

    @Test
    fun `both styles have twelve cells with every graha placed once`() {
        ChartStyle.entries.forEach { style ->
            val cells = ChartLayout.layout(kundli, style)
            assertThat(cells.map { it.house }).containsExactlyElementsIn(1..12).inOrder()
            assertThat(cells.flatMap { it.grahas }).containsExactlyElementsIn(Graha.entries)
        }
    }

    @Test
    fun `house 1 holds the lagna sign and signs follow in order`() {
        val cells = ChartLayout.layout(kundli, ChartStyle.NORTH_INDIAN)
        assertThat(cells.first().rashi).isEqualTo(kundli.lagna)
        assertThat(cells.first().isLagna).isTrue()
        cells.zipWithNext().forEach { (x, y) ->
            assertThat(y.rashi.ordinal).isEqualTo((x.rashi.ordinal + 1) % 12)
        }
    }

    @Test
    fun `north Indian cells tile the whole square`() {
        val cells = ChartLayout.layout(kundli, ChartStyle.NORTH_INDIAN)
        assertThat(cells.sumOf { area(it.outline) }).isWithin(1e-9).of(1.0)
        assertThat(cells.first().anchor.y).isLessThan(0.5) // lagna diamond is at the top
    }

    @Test
    fun `south Indian keeps signs fixed with Mesha second in the top row`() {
        val cells = ChartLayout.layout(kundli, ChartStyle.SOUTH_INDIAN)
        val mesha = cells.first { it.rashi == Rashi.MESHA }
        assertThat(mesha.outline.first()).isEqualTo(ChartPoint(0.25, 0.0))
        // Twelve border cells of a 4 x 4 grid cover 12/16 of the square, leaving the centre for a title.
        assertThat(cells.sumOf { area(it.outline) }).isWithin(1e-9).of(0.75)
    }

    @Test
    fun `every graha has a two-letter label`() {
        Graha.entries.forEach { assertThat(ChartLayout.abbreviation(it)).hasLength(2) }
    }
}
