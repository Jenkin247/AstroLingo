// AstroLingo's Compose theme: Material 3 colour schemes built from the brand palette,
// plus the per-tab pastel colours (Today butter, Learn lavender, Kundli sky, Me peach).
package com.astrolingo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours that belong to one tab. Each tab has its own pastel so the app feels varied but calm.
 *
 * @property today Today tab (butter).
 * @property learn Learn tab (lavender).
 * @property kundli Kundli tab (sky).
 * @property me Me tab (peach).
 * @property onPastel text colour that reads on any of the pastels.
 */
@Immutable
data class TabColors(
    val today: Color,
    val learn: Color,
    val kundli: Color,
    val me: Color,
    val onPastel: Color,
)

private val LightTabColors = TabColors(
    today = Palette.Butter,
    learn = Palette.Lavender,
    kundli = Palette.Sky,
    me = Palette.Peach,
    onPastel = Palette.DeepSlate,
)

private val DarkTabColors = TabColors(
    today = Palette.NightButter,
    learn = Palette.NightLavender,
    kundli = Palette.NightSky,
    me = Palette.NightPeach,
    onPastel = Palette.OnPastelDark,
)

private val LightScheme = lightColorScheme(
    primary = Palette.Lagoon,
    onPrimary = Palette.Mist,
    secondary = Palette.SeaGlass,
    onSecondary = Palette.DeepSlate,
    tertiary = Palette.Rose,
    background = Palette.Mist,
    onBackground = Palette.DeepSlate,
    surface = Color.White,
    onSurface = Palette.DeepSlate,
    onSurfaceVariant = Palette.Muted,
)

private val DarkScheme = darkColorScheme(
    primary = Palette.NightLagoon,
    onPrimary = Palette.NightBackground,
    secondary = Palette.NightSeaGlass,
    onSecondary = Palette.OnPastelDark,
    tertiary = Palette.NightRose,
    background = Palette.NightBackground,
    onBackground = Palette.NightInk,
    surface = Palette.NightSurface,
    onSurface = Palette.NightInk,
    onSurfaceVariant = Palette.NightMuted,
)

private val LocalTabColors = staticCompositionLocalOf { LightTabColors }

/** Access point for AstroLingo-specific tokens that Material 3 has no slot for. */
object AstroLingoTheme {
    /** The per-tab pastel colours for the current light or dark theme. */
    val tabColors: TabColors
        @Composable get() = LocalTabColors.current
}

/**
 * Wraps content in the AstroLingo theme.
 *
 * @param darkTheme whether to use the dark palette; follows the system setting by default.
 */
@Composable
fun AstroLingoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTabColors provides if (darkTheme) DarkTabColors else LightTabColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = AstroLingoTypography,
            shapes = AstroLingoShapes,
            content = content,
        )
    }
}
