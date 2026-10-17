// Spacing and shape tokens. Screens use these instead of raw dp values.
package com.astrolingo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing on a 4dp grid. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp

    /** Smallest touch target (Android accessibility guideline). */
    val touchTarget = 48.dp
}

/** Soft, rounded shapes: chips small, cards medium, sheets and the daily card large. */
internal val AstroLingoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)
