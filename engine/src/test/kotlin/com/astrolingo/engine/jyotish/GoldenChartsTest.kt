package com.astrolingo.engine.jyotish

import com.google.common.truth.Truth.assertWithMessage
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Test

/**
 * The Astro Guru's golden set: 50 fictional people checked against an independent reference
 * (JPL DE421 and ERFA, see tools/golden/generate_golden.py). Every graha, Rahu (true node) and the lagna
 * must agree within 1 arcminute; Ketu is checked as Rahu's opposite.
 */
class GoldenChartsTest {
    private data class GoldenChart(
        val name: String,
        val local: LocalDateTime,
        val zone: ZoneId,
        val latitude: Double,
        val longitude: Double,
        val utcMillis: Long,
        val ayanamsa: Double,
        val lagna: Double,
        val grahas: Map<Graha, Double>,
    )

    private val charts: List<GoldenChart> by lazy {
        val lines = checkNotNull(javaClass.getResourceAsStream("/golden/charts.csv")).bufferedReader().readLines()
            .filterNot { it.startsWith("#") || it.isBlank() }
        val header = lines.first().split(",")
        lines.drop(1).map { line ->
            val cells = header.zip(line.split(",")).toMap()
            GoldenChart(
                name = cells.getValue("name"),
                local = LocalDateTime.parse(cells.getValue("local")),
                zone = ZoneId.of(cells.getValue("zone")),
                latitude = cells.getValue("lat").toDouble(),
                longitude = cells.getValue("lon").toDouble(),
                utcMillis = cells.getValue("utc_millis").toLong(),
                ayanamsa = cells.getValue("ayanamsa").toDouble(),
                lagna = cells.getValue("lagna").toDouble(),
                grahas = Graha.entries.filter { it.name in cells }.associateWith { cells.getValue(it.name).toDouble() },
            )
        }
    }

    private fun arcminutesApart(a: Double, b: Double): Double {
        val d = abs(normalizeDegrees(a - b))
        return minOf(d, FULL_CIRCLE_DEGREES - d) * 60.0
    }

    @Test
    fun `set has fifty charts with seven grahas and Rahu each`() {
        assertWithMessage("charts").that(charts).hasSize(50)
        charts.forEach { assertWithMessage(it.name).that(it.grahas.keys).hasSize(8) }
    }

    @Test
    fun `local birth time converts to the same UTC moment as the reference`() {
        charts.forEach { chart ->
            val resolved = BirthTimeResolver.resolve(chart.local, chart.zone)
            assertWithMessage(chart.name).that(resolved.utcEpochMillis).isEqualTo(chart.utcMillis)
        }
    }

    @Test
    fun `every graha and the lagna agree within one arcminute`() {
        charts.forEach { chart ->
            val kundli = KundliCalculator.calculate(BirthMoment(chart.utcMillis, chart.latitude, chart.longitude))
            assertWithMessage("${chart.name} ayanamsa").that(kundli.ayanamsa).isWithin(1e-5).of(chart.ayanamsa)
            assertWithMessage("${chart.name} lagna")
                .that(arcminutesApart(kundli.lagnaLongitude, chart.lagna)).isAtMost(1.0)
            chart.grahas.forEach { (graha, expected) ->
                assertWithMessage("${chart.name} $graha")
                    .that(arcminutesApart(kundli.of(graha).siderealLongitude, expected)).isAtMost(1.0)
            }
            val ketu = normalizeDegrees(chart.grahas.getValue(Graha.RAHU) + HALF_CIRCLE_DEGREES)
            assertWithMessage("${chart.name} KETU")
                .that(arcminutesApart(kundli.of(Graha.KETU).siderealLongitude, ketu)).isAtMost(1.0)
        }
    }
}

private const val HALF_CIRCLE_DEGREES = 180.0
