// The living sky at the top of Today: it follows the time of day where the user is, with the Sun by day and tonight's
// Moon by night, and carries the greeting and the day's headline. Cards scroll up over it.
package com.astrolingo.app.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrolingo.app.learn.rememberReduceMotion
import com.astrolingo.app.ui.components.StarField
import com.astrolingo.app.ui.theme.MoonLight
import com.astrolingo.app.ui.theme.SkyInk
import com.astrolingo.app.ui.theme.SkyPhase
import com.astrolingo.app.ui.theme.Spacing
import kotlin.math.abs

/**
 * The sky for [phase], with [content] (greeting, date, headline) in white over it. While the page scrolls, the sky
 * moves away more slowly than the cards and the words fade, so the cards seem to slide up over the sky.
 *
 * @param moonLit how much of tonight's Moon is lit, 0 to 1; [waxing] puts the light on the right, as seen from India.
 * @param overlap how far the first card below rises over the bottom of the sky.
 */
@Composable
internal fun SkyHeader(
    phase: SkyPhase,
    moonLit: Float,
    waxing: Boolean,
    scroll: ScrollState,
    overlap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(Brush.verticalGradient(phase.colors)),
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { if (!reduceMotion) translationY = scroll.value * SKY_PARALLAX },
        ) {
            StarField(Modifier.fillMaxSize(), tint = SkyInk, strength = phase.starStrength)
            SkyBody(
                phase,
                moonLit,
                waxing,
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Spacing.lg, end = Spacing.lg)
                    .size(BODY_SIZE),
            )
        }
        CompositionLocalProvider(LocalContentColor provides SkyInk) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.md, end = Spacing.md, top = TEXT_TOP, bottom = Spacing.lg + overlap)
                    .graphicsLayer {
                        if (!reduceMotion) {
                            translationY = scroll.value * TEXT_PARALLAX
                            alpha = (1f - scroll.value / (size.height * FADE_SHARE)).coerceIn(0f, 1f)
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                content = content,
            )
        }
    }
}

/** The Sun with a soft glow, or tonight's Moon in its real phase, growing to it as Today opens. */
@Composable
private fun SkyBody(phase: SkyPhase, moonLit: Float, waxing: Boolean, modifier: Modifier) {
    val reduceMotion = rememberReduceMotion()
    val lit = remember { Animatable(if (reduceMotion) moonLit else MOON_START) }
    LaunchedEffect(moonLit) { lit.animateTo(moonLit, tween(MOON_MILLIS, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        val r = size.minDimension / 2
        val centre = Offset(size.width / 2, size.height / 2)
        if (phase.sunUp) {
            drawCircle(
                Brush.radialGradient(
                    0f to phase.sunCore,
                    SUN_CORE_STOP to phase.sunGlow,
                    1f to Color.Transparent,
                    center = centre,
                    radius = r,
                ),
                radius = r,
                center = centre,
            )
        } else {
            val moonR = r * MOON_SHARE
            drawCircle(SkyInk.copy(alpha = MOON_SHADOW_ALPHA), moonR, centre)
            // The lit right half, closed by the terminator: an ellipse that bulges left past half full.
            val k = lit.value
            val rx = moonR * abs(1 - 2 * k)
            val path = Path().apply {
                moveTo(centre.x, centre.y - moonR)
                arcTo(Rect(centre, moonR), -QUARTER, HALF_TURN, false)
                arcTo(Rect(centre.x - rx, centre.y - moonR, centre.x + rx, centre.y + moonR), QUARTER, if (k > 0.5f) HALF_TURN else -HALF_TURN, false)
                close()
            }
            // A waning Moon is lit on the left: the same shape, mirrored.
            scale(if (waxing) 1f else -1f, 1f, centre) { drawPath(path, MoonLight) }
        }
    }
}

private val BODY_SIZE = 76.dp
private val TEXT_TOP = 96.dp
private const val SKY_PARALLAX = 0.4f
private const val TEXT_PARALLAX = 0.2f
private const val FADE_SHARE = 0.7f
private const val SUN_CORE_STOP = 0.55f
private const val MOON_SHARE = 0.6f
private const val MOON_SHADOW_ALPHA = 0.14f
private const val MOON_START = 0.03f
private const val MOON_MILLIS = 1400
private const val QUARTER = 90f
private const val HALF_TURN = 180f
