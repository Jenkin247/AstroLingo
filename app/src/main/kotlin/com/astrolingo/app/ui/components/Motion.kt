// Shared motion pieces (ADR 0007): a twinkling star field behind Today and Learn, a staggered rise for blocks as a
// screen opens, a burst of stars for a right answer, and haptic taps. All of them stay still when animations are off.
package com.astrolingo.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.astrolingo.app.learn.rememberReduceMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

/** One star of the field, placed as fractions of the screen so it fits any size. */
private class FieldStar(val x: Float, val y: Float, val radiusDp: Float, val phase: Float, val sparkle: Boolean, val tint: Int)

// The same sky every time: a fixed seed, so stars don't jump about when the screen is rebuilt.
private val fieldStars: List<FieldStar> = Random(FIELD_SEED).let { random ->
    List(FIELD_STAR_COUNT) { i ->
        FieldStar(
            x = random.nextFloat(),
            y = random.nextFloat(),
            radiusDp = 0.8f + random.nextFloat() * 1.6f,
            phase = random.nextFloat(),
            sparkle = i % 7 == 0,
            tint = i % 3,
        )
    }
}

/**
 * A quiet night sky drawn behind a screen: small stars that slowly twinkle, with a few four-point sparkles. Only the
 * drawing repeats, so the screen above it is not recomposed. With animations off the stars hold still.
 */
@Composable
fun StarField(modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val time: State<Float> = if (reduceMotion) {
        remember { mutableFloatStateOf(0.25f) }
    } else {
        rememberInfiniteTransition(label = "star field")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(TWINKLE_MILLIS, easing = LinearEasing)), label = "twinkle")
    }
    // Stronger on the dark theme, where a sky looks natural; faint on the light one so text stays easy to read.
    val strength = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.7f else 0.35f
    val colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondary)
    Canvas(modifier) {
        val t = time.value
        fieldStars.forEach { star ->
            val wave = sin(2 * PI * (t + star.phase)).toFloat()
            val alpha = strength * (0.55f + 0.45f * wave)
            val at = Offset(star.x * size.width, star.y * size.height)
            val color = colors[star.tint]
            if (star.sparkle) {
                drawSparkle(color, at, (star.radiusDp + 2.5f).dp.toPx() * (0.85f + 0.15f * wave), alpha)
            } else {
                drawCircle(color, star.radiusDp.dp.toPx(), at, alpha = alpha)
            }
        }
    }
}

/** A four-point sparkle, like light catching a star. */
private fun DrawScope.drawSparkle(color: Color, at: Offset, radius: Float, alpha: Float) {
    val path = Path()
    for (k in 0 until 8) {
        val r = if (k % 2 == 0) radius else radius * 0.28f
        val angle = -PI / 2 + k * PI / 4
        val x = at.x + (r * cos(angle)).toFloat()
        val y = at.y + (r * sin(angle)).toFloat()
        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, alpha = alpha.coerceIn(0f, 1f))
}

/**
 * A block that rises gently into place as its screen opens, [order] steps after the first, so a screen fills in from
 * the top down. Shows at once when animations are off.
 */
@Composable
fun Modifier.riseIn(order: Int): Modifier {
    val reduceMotion = rememberReduceMotion()
    val shown = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (shown.value < 1f) {
            delay(order * RISE_STAGGER_MILLIS)
            shown.animateTo(1f, tween(RISE_MILLIS, easing = FastOutSlowInEasing))
        }
    }
    return graphicsLayer {
        alpha = shown.value
        translationY = (1f - shown.value) * RISE_OFFSET_DP * density
    }
}

/**
 * Stars bursting out from the middle of this element, for a right answer. [progress] runs from 0 (nothing) to 1 (the
 * burst has faded); hold it at 0 to show nothing.
 */
fun Modifier.starBurst(progress: () -> Float, colors: List<Color>): Modifier = drawWithContent {
    drawContent()
    val p = progress()
    if (p <= 0f || p >= 1f) return@drawWithContent
    val centre = Offset(size.width / 2, size.height / 2)
    val reach = maxOf(size.width, size.height) * 0.55f
    repeat(BURST_STARS) { i ->
        val angle = 2 * PI * i / BURST_STARS + (if (i % 2 == 0) 0.0 else 0.2)
        val distance = reach * (0.35f + 0.65f * p) * (if (i % 2 == 0) 1f else 0.7f)
        val at = Offset(centre.x + (distance * cos(angle)).toFloat(), centre.y + (distance * sin(angle)).toFloat() * 0.6f)
        drawSparkle(colors[i % colors.size], at, (5f + 3f * (1f - p)).dp.toPx(), 1f - p)
    }
}

/** A short "that worked" tap, such as for a right answer. */
fun HapticFeedback.confirm() = performHapticFeedback(HapticFeedbackType.Confirm)

/** A soft "not quite" tap, for a wrong answer. Never harsh: wrong answers are not punished. */
fun HapticFeedback.reject() = performHapticFeedback(HapticFeedbackType.Reject)

/** A light tick, such as for a card turning over. */
fun HapticFeedback.tick() = performHapticFeedback(HapticFeedbackType.SegmentTick)

/** How long a right-answer burst takes. */
const val BURST_MILLIS = 700

private const val FIELD_SEED = 27
private const val FIELD_STAR_COUNT = 42
private const val TWINKLE_MILLIS = 6000
private const val RISE_STAGGER_MILLIS = 70L
private const val RISE_MILLIS = 380
private const val RISE_OFFSET_DP = 18
private const val BURST_STARS = 10
