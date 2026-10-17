// Draws a kundli from the engine's ChartLayout. All geometry comes from the engine; this file only scales and paints.
package com.astrolingo.app.ui.kundli

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.engine.chart.ChartCell
import com.astrolingo.engine.chart.ChartLayout
import com.astrolingo.engine.chart.ChartStyle
import com.astrolingo.engine.jyotish.Kundli
import kotlin.math.min

/**
 * A square kundli chart.
 *
 * @param kundli the chart to draw.
 * @param style North Indian (houses fixed) or South Indian (signs fixed).
 * @param description spoken by TalkBack in place of the drawing; the planet list below the chart carries the detail.
 * @param markLagna false for a Moon chart, whose first house is the Moon's sign, not the lagna: no "La" mark.
 */
@Composable
fun KundliChart(
    kundli: Kundli,
    style: ChartStyle,
    description: String,
    modifier: Modifier = Modifier,
    markLagna: Boolean = true,
) {
    val cells = remember(kundli, style) { ChartLayout.layout(kundli, style) }
    val textMeasurer = rememberTextMeasurer()
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val lagnaFill = AstroLingoTheme.tabColors.kundli.copy(alpha = 0.45f)
    // Chart labels grow with the phone's font size only up to 1.3x, so they stay inside their houses.
    // The planet list below the chart (and TalkBack) carries the same detail at any size.
    val labelScale = min(1f, MAX_LABEL_FONT_SCALE / LocalDensity.current.fontScale)
    val grahaStyle = MaterialTheme.typography.labelLarge.let {
        it.copy(color = ink, fontWeight = FontWeight.SemiBold, fontSize = it.fontSize * labelScale)
    }
    val signStyle = MaterialTheme.typography.labelSmall.let { it.copy(color = muted, fontSize = it.fontSize * labelScale) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = description },
    ) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        cells.forEach { cell ->
            val path = cellPath(cell)
            if (cell.isLagna) drawPath(path, lagnaFill)
            drawPath(path, line, style = stroke)
            drawLabels(cell, style, markLagna, textMeasurer, grahaStyle, signStyle)
        }
        if (style == ChartStyle.NORTH_INDIAN) {
            drawRect(line, style = stroke)
        }
    }
}

private fun DrawScope.cellPath(cell: ChartCell): Path = Path().apply {
    cell.outline.forEachIndexed { i, p ->
        val x = (p.x * size.width).toFloat()
        val y = (p.y * size.height).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.drawLabels(
    cell: ChartCell,
    style: ChartStyle,
    markLagna: Boolean,
    textMeasurer: TextMeasurer,
    grahaStyle: TextStyle,
    signStyle: TextStyle,
) {
    val centre = Offset((cell.anchor.x * size.width).toFloat(), (cell.anchor.y * size.height).toFloat())
    // North Indian prints the sign number in each house; South Indian's boxes are fixed signs, so they're named.
    val signLabel = when (style) {
        ChartStyle.NORTH_INDIAN -> cell.rashi.number.toString()
        ChartStyle.SOUTH_INDIAN -> cell.rashi.englishName.take(SIGN_LETTERS) + if (cell.isLagna && markLagna) " · La" else ""
    }
    val grahas = cell.grahas.joinToString(" ") { ChartLayout.abbreviation(it) }
    val lines = listOf(signLabel to signStyle, grahas to grahaStyle).filter { it.first.isNotEmpty() }
    // Wrap within the cell: a house holding four grahas breaks onto two lines instead of spilling out.
    val xs = cell.outline.map { it.x }
    val cellWidth = ((xs.max() - xs.min()) * size.width * CELL_TEXT_WIDTH).toInt().coerceAtLeast(1)
    val measured = lines.map { (text, textStyle) ->
        textMeasurer.measure(text, textStyle.copy(textAlign = TextAlign.Center), constraints = Constraints(maxWidth = cellWidth))
    }
    var y = centre.y - measured.sumOf { it.size.height } / 2f
    measured.forEach { layout ->
        drawText(layout, topLeft = Offset(centre.x - layout.size.width / 2f, y))
        y += layout.size.height
    }
}

private const val MAX_LABEL_FONT_SCALE = 1.3f
private const val SIGN_LETTERS = 3
/** Share of a cell's width that text may use; North Indian triangles narrow towards their tips. */
private const val CELL_TEXT_WIDTH = 0.6
