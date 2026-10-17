// Where each house and graha goes in a drawn kundli, in a unit square (0..1, y down).
// Pure geometry, shared by Android now and iOS later; the UI only scales and draws it.
package com.astrolingo.engine.chart

import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.Rashi

/** A point in the unit square; (0, 0) is top-left. */
data class ChartPoint(val x: Double, val y: Double)

/**
 * One box of the chart.
 *
 * @property house whole-sign house, 1 to 12.
 * @property rashi the sign in that house.
 * @property outline polygon to stroke, in drawing order.
 * @property anchor where to centre the labels.
 * @property grahas grahas in this house, in [Graha] order.
 * @property isLagna true for house 1.
 */
data class ChartCell(
    val house: Int,
    val rashi: Rashi,
    val outline: List<ChartPoint>,
    val anchor: ChartPoint,
    val grahas: List<Graha>,
) {
    val isLagna: Boolean get() = house == 1
}

/** The two chart styles users know. North Indian fixes the houses; South Indian fixes the signs. */
enum class ChartStyle { NORTH_INDIAN, SOUTH_INDIAN }

/** Lays out a kundli for drawing. Source for both layouts: the Astro Guru's chart spec (Kundli tab story). */
object ChartLayout {
    private val a = ChartPoint(0.0, 0.0)
    private val b = ChartPoint(1.0, 0.0)
    private val c = ChartPoint(1.0, 1.0)
    private val d = ChartPoint(0.0, 1.0)
    private val top = ChartPoint(0.5, 0.0)
    private val right = ChartPoint(1.0, 0.5)
    private val bottom = ChartPoint(0.5, 1.0)
    private val left = ChartPoint(0.0, 0.5)
    private val centre = ChartPoint(0.5, 0.5)
    private val p1 = ChartPoint(0.25, 0.25)
    private val p2 = ChartPoint(0.75, 0.25)
    private val p3 = ChartPoint(0.75, 0.75)
    private val p4 = ChartPoint(0.25, 0.75)

    /** North Indian: house 1 is the top diamond, houses run anticlockwise. */
    private val northOutlines: List<List<ChartPoint>> = listOf(
        listOf(top, p2, centre, p1),
        listOf(a, top, p1),
        listOf(a, p1, left),
        listOf(left, p1, centre, p4),
        listOf(left, p4, d),
        listOf(d, p4, bottom),
        listOf(bottom, p4, centre, p3),
        listOf(bottom, p3, c),
        listOf(c, p3, right),
        listOf(right, p3, centre, p2),
        listOf(right, p2, b),
        listOf(b, p2, top),
    )

    /** South Indian: each sign's cell in a 4 x 4 grid (column, row), Meena top-left, running clockwise. */
    private val southCells: Map<Rashi, Pair<Int, Int>> = mapOf(
        Rashi.MEENA to (0 to 0), Rashi.MESHA to (1 to 0), Rashi.VRISHABHA to (2 to 0), Rashi.MITHUNA to (3 to 0),
        Rashi.KARKA to (3 to 1), Rashi.SIMHA to (3 to 2), Rashi.KANYA to (3 to 3), Rashi.TULA to (2 to 3),
        Rashi.VRISHCHIKA to (1 to 3), Rashi.DHANU to (0 to 3), Rashi.MAKARA to (0 to 2), Rashi.KUMBHA to (0 to 1),
    )
    private const val GRID = 4

    /** The 12 cells for a kundli in the chosen style, house 1 first. */
    fun layout(kundli: Kundli, style: ChartStyle): List<ChartCell> = (1..12).map { house ->
        val rashi = Rashi.entries[(kundli.lagna.ordinal + house - 1) % Rashi.entries.size]
        val outline = when (style) {
            ChartStyle.NORTH_INDIAN -> northOutlines[house - 1]
            ChartStyle.SOUTH_INDIAN -> southOutline(rashi)
        }
        ChartCell(
            house = house,
            rashi = rashi,
            outline = outline,
            anchor = centroid(outline),
            grahas = kundli.grahas.filter { it.house == house }.map { it.graha },
        )
    }

    private fun southOutline(rashi: Rashi): List<ChartPoint> {
        val (col, row) = southCells.getValue(rashi)
        val x0 = col.toDouble() / GRID
        val y0 = row.toDouble() / GRID
        val s = 1.0 / GRID
        return listOf(ChartPoint(x0, y0), ChartPoint(x0 + s, y0), ChartPoint(x0 + s, y0 + s), ChartPoint(x0, y0 + s))
    }

    private fun centroid(points: List<ChartPoint>): ChartPoint =
        ChartPoint(points.sumOf { it.x } / points.size, points.sumOf { it.y } / points.size)

    /** Short label for a graha on the chart, as printed kundlis use. */
    fun abbreviation(graha: Graha): String = when (graha) {
        Graha.SURYA -> "Su"
        Graha.CHANDRA -> "Mo"
        Graha.MANGAL -> "Ma"
        Graha.BUDHA -> "Me"
        Graha.GURU -> "Ju"
        Graha.SHUKRA -> "Ve"
        Graha.SHANI -> "Sa"
        Graha.RAHU -> "Ra"
        Graha.KETU -> "Ke"
    }
}
