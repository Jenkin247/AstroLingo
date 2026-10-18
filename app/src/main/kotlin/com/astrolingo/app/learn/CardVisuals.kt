// Pictures on learn cards: drawn graha portraits, a large glyph, the zodiac wheel, the North Indian house diagram
// and the ring of 27 nakshatras. All drawn in Compose so they are crisp, tiny in the APK and animate (ADR 0007).
package com.astrolingo.app.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrolingo.app.R
import com.astrolingo.app.ui.kundli.displayName
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.Nakshatra
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.learn.content.Visual
import com.astrolingo.learn.content.VisualKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The picture for a card.
 *
 * @param active true while the card is settled on screen; the picture draws itself in the first time.
 * @param size the picture's width; diagrams are square.
 */
@Composable
internal fun CardVisual(visual: Visual, active: Boolean, modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val reduceMotion = rememberReduceMotion()
    val reveal = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(active) {
        if (active && reveal.value < 1f) reveal.animateTo(1f, tween(REVEAL_MILLIS, easing = FastOutSlowInEasing))
    }
    val progress = reveal.value
    val number = visual.number ?: 0
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        when (visual.kind) {
            VisualKind.GRAHA -> grahaOf(visual.value)?.let { GrahaPicture(it, Modifier.size(size), animate = !reduceMotion) }
            VisualKind.GLYPH -> Text(
                textGlyph(visual.value),
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 72.sp, lineHeight = 84.sp),
                modifier = Modifier.graphicsLayer {
                    alpha = progress
                    scaleX = 0.7f + 0.3f * progress
                    scaleY = 0.7f + 0.3f * progress
                },
            )
            VisualKind.ZODIAC -> {
                ZodiacWheel(lit = number, progress = progress, modifier = Modifier.size(size))
                Rashi.entries.getOrNull(number - 1)?.let { Caption(stringResource(R.string.rashi_with_english, it.displayName(), it.englishName)) }
            }
            VisualKind.HOUSES -> {
                HouseDiagram(lit = number, progress = progress, modifier = Modifier.size(size))
                if (number in 1..12) Caption(stringResource(R.string.learn_house_number, number))
            }
            VisualKind.NAKSHATRAS -> {
                NakshatraRing(lit = number, progress = progress, modifier = Modifier.size(size))
                Nakshatra.entries.getOrNull(number - 1)?.let { Caption(it.displayName()) }
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
}

internal fun grahaOf(name: String): Graha? = Graha.entries.firstOrNull { it.name == name }

/** Colours of the graha portraits. Illustration colours, not UI tokens: they stay the same in light and dark. */
private object GrahaArt {
    val Sky = Color(0xFF1E2340)
    val SkyEdge = Color(0xFF2E3560)
    val Star = Color(0xFFFFF6D8)
}

/**
 * A small night-sky portrait of a graha: Surya glowing with rays, Chandra with craters, Mangal red, Budha small
 * and grey, Guru banded, Shukra bright, Shani with its rings, Rahu as the eclipse shadow, Ketu as a comet.
 *
 * @param animate false when animations are off: the portrait then stays still.
 */
@Composable
internal fun GrahaPicture(graha: Graha, modifier: Modifier = Modifier, animate: Boolean = true) {
    val drift = if (animate) {
        val transition = rememberInfiniteTransition(label = "graha")
        val value by transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(FLOAT_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "graha float",
        )
        value
    } else {
        0f
    }
    val description = stringResource(R.string.rashi_with_english, graha.displayName(), graha.englishName)
    Box(
        modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .semantics { contentDescription = description },
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.radialGradient(listOf(GrahaArt.SkyEdge, GrahaArt.Sky), center = center, radius = size.minDimension / 1.4f))
            drawStars(twinkle = drift)
            val c = center.copy(y = center.y + drift * size.minDimension * 0.015f)
            val r = size.minDimension * 0.27f
            when (graha) {
                Graha.SURYA -> drawSun(c, r, drift)
                Graha.CHANDRA -> drawMoon(c, r)
                Graha.MANGAL -> drawMars(c, r)
                Graha.BUDHA -> drawMercury(c, r * 0.75f)
                Graha.GURU -> drawJupiter(c, r * 1.15f)
                Graha.SHUKRA -> drawVenus(c, r)
                Graha.SHANI -> drawSaturn(c, r * 0.85f)
                Graha.RAHU -> drawRahu(c, r)
                Graha.KETU -> drawKetu(c, r)
            }
        }
    }
}

private fun DrawScope.drawStars(twinkle: Float) {
    // Fixed positions so the sky looks the same every time.
    val points = listOf(0.15f to 0.2f, 0.8f to 0.15f, 0.88f to 0.7f, 0.2f to 0.82f, 0.5f to 0.08f, 0.1f to 0.55f, 0.72f to 0.9f, 0.92f to 0.42f)
    points.forEachIndexed { i, (x, y) ->
        val alpha = 0.55f + 0.35f * (if (i % 2 == 0) twinkle else -twinkle) * 0.5f + 0.1f
        drawCircle(GrahaArt.Star.copy(alpha = alpha.coerceIn(0.2f, 1f)), radius = size.minDimension * (0.006f + 0.003f * (i % 3)), center = Offset(x * size.width, y * size.height))
    }
}

private fun DrawScope.glow(c: Offset, r: Float, color: Color) {
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), Color.Transparent), center = c, radius = r * 1.7f), radius = r * 1.7f, center = c)
}

private fun DrawScope.ball(c: Offset, r: Float, light: Color, dark: Color) {
    drawCircle(
        Brush.radialGradient(listOf(light, dark), center = Offset(c.x - r * 0.35f, c.y - r * 0.35f), radius = r * 1.6f),
        radius = r,
        center = c,
    )
}

private fun DrawScope.drawSun(c: Offset, r: Float, pulse: Float) {
    glow(c, r * (1.1f + 0.05f * pulse), Color(0xFFFFC94D))
    val rays = 12
    repeat(rays) { i ->
        val angle = (i * 2 * PI / rays).toFloat()
        val inner = r * 1.15f
        val outer = r * (1.5f + if (i % 2 == 0) 0.1f else 0f)
        drawLine(
            Color(0xFFFFB627),
            Offset(c.x + inner * cos(angle), c.y + inner * sin(angle)),
            Offset(c.x + outer * cos(angle), c.y + outer * sin(angle)),
            strokeWidth = r * 0.12f,
            cap = StrokeCap.Round,
        )
    }
    ball(c, r, Color(0xFFFFF0B3), Color(0xFFF59E0B))
}

private fun DrawScope.drawMoon(c: Offset, r: Float) {
    glow(c, r, Color(0xFFE8ECF5))
    ball(c, r, Color(0xFFFFFFFF), Color(0xFFC9CFDA))
    val crater = Color(0xFFB4BCC9).copy(alpha = 0.7f)
    drawCircle(crater, r * 0.18f, Offset(c.x - r * 0.3f, c.y - r * 0.2f))
    drawCircle(crater, r * 0.12f, Offset(c.x + r * 0.35f, c.y + r * 0.1f))
    drawCircle(crater, r * 0.09f, Offset(c.x - r * 0.05f, c.y + r * 0.45f))
    drawCircle(crater, r * 0.07f, Offset(c.x + r * 0.15f, c.y - r * 0.5f))
}

private fun DrawScope.drawMars(c: Offset, r: Float) {
    glow(c, r, Color(0xFFE5673A))
    ball(c, r, Color(0xFFF7A072), Color(0xFFB0361A))
    val patch = Color(0xFF8E2A14).copy(alpha = 0.55f)
    drawOval(patch, topLeft = Offset(c.x - r * 0.55f, c.y - r * 0.05f), size = Size(r * 0.6f, r * 0.25f))
    drawOval(patch, topLeft = Offset(c.x + r * 0.05f, c.y + r * 0.35f), size = Size(r * 0.45f, r * 0.18f))
    drawOval(Color.White.copy(alpha = 0.85f), topLeft = Offset(c.x - r * 0.25f, c.y - r * 0.98f), size = Size(r * 0.5f, r * 0.2f))
}

private fun DrawScope.drawMercury(c: Offset, r: Float) {
    glow(c, r, Color(0xFF9FD3C8))
    ball(c, r, Color(0xFFDDE7E4), Color(0xFF6F8A8C))
    val crater = Color(0xFF5E7576).copy(alpha = 0.5f)
    drawCircle(crater, r * 0.16f, Offset(c.x + r * 0.25f, c.y - r * 0.25f))
    drawCircle(crater, r * 0.1f, Offset(c.x - r * 0.35f, c.y + r * 0.2f))
    drawCircle(crater, r * 0.08f, Offset(c.x + r * 0.1f, c.y + r * 0.5f))
}

private fun DrawScope.drawJupiter(c: Offset, r: Float) {
    glow(c, r, Color(0xFFF2C98A))
    val circle = Path().apply { addOval(Rect(c, r)) }
    clipPath(circle) {
        val bands = listOf(Color(0xFFF6E3C4), Color(0xFFD9A06B), Color(0xFFF1D3A6), Color(0xFFC2804E), Color(0xFFF6E3C4), Color(0xFFD7A574), Color(0xFFEFD0A0))
        val h = 2 * r / bands.size
        bands.forEachIndexed { i, color -> drawRect(color, Offset(c.x - r, c.y - r + i * h), Size(2 * r, h + 1f)) }
        drawOval(Color(0xFFC45A3A), topLeft = Offset(c.x + r * 0.15f, c.y + r * 0.2f), size = Size(r * 0.42f, r * 0.24f))
        drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color(0x55000000)), center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.8f), r, c)
    }
}

private fun DrawScope.drawVenus(c: Offset, r: Float) {
    glow(c, r * 1.2f, Color(0xFFFFF1C2))
    ball(c, r, Color(0xFFFFFBEA), Color(0xFFE6C677))
    drawOval(Color.White.copy(alpha = 0.35f), topLeft = Offset(c.x - r * 0.7f, c.y - r * 0.3f), size = Size(r * 1.4f, r * 0.25f))
}

private fun DrawScope.drawSaturn(c: Offset, r: Float) {
    glow(c, r, Color(0xFFE9CF8F))
    rotate(-18f, pivot = c) {
        val ring = Size(r * 3.6f, r * 1.1f)
        val ringTopLeft = Offset(c.x - ring.width / 2, c.y - ring.height / 2)
        val ringColor = Color(0xFFE3C98C)
        // The far half of the rings sits behind the planet, the near half in front.
        drawArc(ringColor, 180f, 180f, useCenter = false, topLeft = ringTopLeft, size = ring, style = Stroke(r * 0.22f))
        ball(c, r, Color(0xFFF8E5B5), Color(0xFFC79F55))
        drawArc(ringColor, 0f, 180f, useCenter = false, topLeft = ringTopLeft, size = ring, style = Stroke(r * 0.22f))
        drawArc(Color(0xFFB8975A), 0f, 180f, useCenter = false, topLeft = ringTopLeft + Offset(r * 0.2f, r * 0.08f), size = Size(ring.width - r * 0.4f, ring.height - r * 0.16f), style = Stroke(r * 0.06f))
    }
}

private fun DrawScope.drawRahu(c: Offset, r: Float) {
    // Rahu, the eclipse point: a dark shadow covering a light, with the corona around it.
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE6A3), Color(0x66F5A623), Color.Transparent), center = c, radius = r * 1.8f), r * 1.8f, c)
    drawCircle(Color(0xFFFFF3CF), r * 1.04f, c)
    drawCircle(Color(0xFF15182B), r, Offset(c.x + r * 0.04f, c.y))
}

private fun DrawScope.drawKetu(c: Offset, r: Float) {
    // Ketu, the tail: a comet with a long, fading tail.
    val head = Offset(c.x + r * 0.6f, c.y + r * 0.6f)
    val tail = Path().apply {
        moveTo(head.x - r * 0.25f, head.y + r * 0.2f)
        lineTo(c.x - r * 1.6f, c.y - r * 1.5f)
        lineTo(head.x + r * 0.2f, head.y - r * 0.25f)
        close()
    }
    drawPath(tail, Brush.linearGradient(listOf(Color(0xCCC3B8F2), Color.Transparent), start = head, end = Offset(c.x - r * 1.6f, c.y - r * 1.5f)))
    glow(head, r * 0.45f, Color(0xFFE6E1FF))
    drawCircle(Color(0xFFF4F1FF), r * 0.28f, head)
}

/** The 12 rashis as a wheel, Mesha at the top and the rest clockwise, with [lit] (1 to 12) highlighted. */
@Composable
private fun ZodiacWheel(lit: Int, progress: Float, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = AstroLingoTheme.tabColors.learn
    val accent = MaterialTheme.colorScheme.primary
    val label = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    Canvas(modifier.widthIn(max = 260.dp).aspectRatio(1f)) {
        val outer = size.minDimension / 2 * 0.95f
        val inner = outer * 0.5f
        val shown = (progress * 12).toInt().coerceIn(0, 12)
        for (i in 0 until 12) {
            if (i >= shown && progress < 1f) continue
            val start = -90f - 15f + i * 30f
            val isLit = i + 1 == lit
            drawArc(
                if (isLit) fill else fill.copy(alpha = 0.18f),
                start, 30f, useCenter = true,
                topLeft = Offset(center.x - outer, center.y - outer), size = Size(outer * 2, outer * 2),
            )
            drawArc(line.copy(alpha = 0.5f), start, 30f, useCenter = true, topLeft = Offset(center.x - outer, center.y - outer), size = Size(outer * 2, outer * 2), style = Stroke(1.dp.toPx()))
            val mid = Math.toRadians((start + 15f).toDouble())
            val at = Offset(center.x + (outer + inner) / 2 * cos(mid).toFloat(), center.y + (outer + inner) / 2 * sin(mid).toFloat())
            drawLabel(measurer, "${i + 1}", at, if (isLit) label.copy(color = accent) else label)
        }
        drawCircle(centreTint(fill), inner, center)
        drawCircle(line.copy(alpha = 0.5f), inner, center, style = Stroke(1.dp.toPx()))
        if (lit in 1..12) drawCircle(accent, inner * 0.12f * progress, center)
    }
}

/** The centre of the wheel: a soft tint so the numbers ring stands out. */
private fun centreTint(fill: Color): Color = fill.copy(alpha = 0.08f)

/**
 * The North Indian chart's 12 houses, house 1 in the top diamond and the rest anticlockwise, with [lit] (1 to 12)
 * highlighted. Corners and midpoints are fractions of the square, as on a hand-drawn kundli.
 */
@Composable
private fun HouseDiagram(lit: Int, progress: Float, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val line = MaterialTheme.colorScheme.onSurface
    val fill = AstroLingoTheme.tabColors.learn
    val label = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    Canvas(modifier.widthIn(max = 260.dp).aspectRatio(1f)) {
        val s = size.minDimension * 0.94f
        val o = Offset((size.width - s) / 2, (size.height - s) / 2)
        fun p(x: Float, y: Float) = Offset(o.x + x * s, o.y + y * s)
        val houses = northIndianHouses.map { points -> points.map { (x, y) -> p(x, y) } }
        houses.forEachIndexed { index, points ->
            if (index + 1 == lit) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, fill.copy(alpha = progress))
            }
        }
        val stroke = 1.5.dp.toPx()
        drawRect(line, topLeft = o, size = Size(s, s), style = Stroke(stroke))
        drawLine(line, p(0f, 0f), p(1f, 1f), stroke)
        drawLine(line, p(1f, 0f), p(0f, 1f), stroke)
        val diamond = Path().apply {
            moveTo(p(0.5f, 0f).x, p(0.5f, 0f).y)
            lineTo(p(1f, 0.5f).x, p(1f, 0.5f).y)
            lineTo(p(0.5f, 1f).x, p(0.5f, 1f).y)
            lineTo(p(0f, 0.5f).x, p(0f, 0.5f).y)
            close()
        }
        drawPath(diamond, line, style = Stroke(stroke))
        houses.forEachIndexed { index, points ->
            val centroid = Offset(points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())
            drawLabel(measurer, "${index + 1}", centroid, label)
        }
    }
}

/** Corners of each house in the North Indian chart, house 1 first, as (x, y) fractions of the square. */
private val northIndianHouses: List<List<Pair<Float, Float>>> = run {
    val tl = 0f to 0f
    val tr = 1f to 0f
    val br = 1f to 1f
    val bl = 0f to 1f
    val t = 0.5f to 0f
    val r = 1f to 0.5f
    val b = 0.5f to 1f
    val l = 0f to 0.5f
    val c = 0.5f to 0.5f
    val p1 = 0.25f to 0.25f
    val p2 = 0.75f to 0.25f
    val p3 = 0.75f to 0.75f
    val p4 = 0.25f to 0.75f
    listOf(
        listOf(t, p2, c, p1), listOf(tl, t, p1), listOf(tl, p1, l), listOf(l, p1, c, p4),
        listOf(l, p4, bl), listOf(bl, p4, b), listOf(b, p4, c, p3), listOf(b, p3, br),
        listOf(br, p3, r), listOf(r, p3, c, p2), listOf(r, p2, tr), listOf(tr, p2, t),
    )
}

/** The 27 nakshatras as a ring of stars, Ashwini at the top and the rest clockwise, with [lit] (1 to 27) highlighted. */
@Composable
private fun NakshatraRing(lit: Int, progress: Float, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val dot = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val ring = AstroLingoTheme.tabColors.learn
    val label = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
    Canvas(modifier.widthIn(max = 260.dp).aspectRatio(1f)) {
        val radius = size.minDimension / 2 * 0.82f
        drawCircle(ring.copy(alpha = 0.35f), radius, center, style = Stroke(10.dp.toPx()))
        val shown = (progress * 27).toInt().coerceIn(0, 27)
        for (i in 0 until 27) {
            if (i >= shown && progress < 1f) continue
            val angle = Math.toRadians((-90.0 + i * 360.0 / 27))
            val at = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
            if (i + 1 == lit) {
                drawCircle(accent, 14.dp.toPx(), at)
                drawLabel(measurer, "${i + 1}", at, label)
            } else {
                drawCircle(dot, 3.5.dp.toPx(), at)
            }
        }
    }
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, at: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

private const val REVEAL_MILLIS = 900
private const val FLOAT_MILLIS = 2600
