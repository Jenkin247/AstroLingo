// Kundli tab: the chart in the user's preferred style and the list of grahas below it.
package com.astrolingo.app.ui.kundli

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraSays
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.chart.ChartStyle
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.GrahaPosition
import com.astrolingo.engine.jyotish.Kundli
import kotlin.math.floor

/**
 * The Kundli tab.
 *
 * @param kundli the chart to show.
 * @param title who the chart belongs to, for example "Your kundli" or "Sample chart".
 * @param note optional line under the title.
 * @param timeKnown false when the birth time is unknown: only the Moon chart is shown, since the lagna can't be trusted.
 * @param reveal true the first time the user sees their own chart: it fades and turns into place, and Sitara says hello.
 * @param onRevealed called when the user leaves the screen after the reveal, so it plays only once.
 */
@OptIn(ExperimentalMaterial3Api::class) // segmented buttons; stable in recent Material 3, harmless otherwise
@Composable
fun KundliScreen(
    kundli: Kundli,
    title: String,
    note: String?,
    modifier: Modifier = Modifier,
    timeKnown: Boolean = true,
    reveal: Boolean = false,
    onRevealed: () -> Unit = {},
) {
    var style by rememberSaveable { mutableStateOf(ChartStyle.NORTH_INDIAN) }
    var fromMoon by rememberSaveable(timeKnown) { mutableStateOf(!timeKnown) }
    val shown = remember(kundli, fromMoon) { if (fromMoon) kundli.fromMoon() else kundli }
    var revealed by remember { mutableStateOf(!reveal) }
    val progress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = REVEAL_MILLIS, easing = FastOutSlowInEasing),
        label = "reveal",
    )
    LaunchedEffect(Unit) { revealed = true }
    // The reveal and Sitara's hello stay for this visit; leaving the tab marks them as seen.
    val latestOnRevealed by rememberUpdatedState(onRevealed)
    DisposableEffect(reveal) { onDispose { if (reveal) latestOnRevealed() } }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (note != null) {
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ChartStyle.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = style == option,
                    onClick = { style = option },
                    shape = SegmentedButtonDefaults.itemShape(index, ChartStyle.entries.size),
                ) {
                    Text(stringResource(if (option == ChartStyle.NORTH_INDIAN) R.string.chart_north else R.string.chart_south))
                }
            }
        }
        if (timeKnown) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(false, true).forEachIndexed { index, moon ->
                    SegmentedButton(
                        selected = fromMoon == moon,
                        onClick = { fromMoon = moon },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) {
                        Text(stringResource(if (moon) R.string.chart_from_moon else R.string.chart_from_lagna))
                    }
                }
            }
        }
        KundliChart(
            kundli = shown,
            style = style,
            description = if (fromMoon) {
                stringResource(R.string.chart_description_moon, shown.moonSign.displayName())
            } else {
                stringResource(R.string.chart_description, shown.lagna.displayName(), shown.moonSign.displayName())
            },
            markLagna = !fromMoon,
            modifier = Modifier.graphicsLayer {
                alpha = progress
                scaleX = REVEAL_START_SCALE + (1f - REVEAL_START_SCALE) * progress
                scaleY = scaleX
                rotationZ = REVEAL_START_DEGREES * (1f - progress)
            },
        )
        if (reveal) {
            SitaraSays(
                text = stringResource(
                    R.string.kundli_reveal_sitara,
                    stringResource(R.string.rashi_with_english, kundli.moonSign.displayName(), kundli.moonSign.englishName),
                ),
            modifier = Modifier.graphicsLayer { alpha = progress },
            )
        }
        val neighbour = if (timeKnown && !fromMoon) shown.lagnaNeighbourIfNearEdge() else null
        if (neighbour != null) {
            Text(
                stringResource(R.string.lagna_near_edge, shown.lagna.displayName(), neighbour.displayName()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        WesternSignNote(kundli)
        Text(stringResource(R.string.grahas_heading), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.grahas_glossary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        shown.grahas.forEach { position ->
            GrahaRow(position)
            HorizontalDivider()
        }
    }
}

/** The question every Western-astrology user asks first, answered with their own Sun. */
@Composable
private fun WesternSignNote(kundli: Kundli) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = !open }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(R.string.western_sign_question))
        }
        if (open) {
            val sun = kundli.of(Graha.SURYA).rashi
            Text(
                stringResource(
                    R.string.western_sign_answer,
                    stringResource(R.string.rashi_with_english, sun.displayName(), sun.englishName),
                    kundli.westernSunSign.englishName,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            kundli.westernSunCuspNeighbour()?.let { neighbour ->
                Text(stringResource(R.string.western_sign_cusp, neighbour.englishName), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private const val REVEAL_MILLIS = 1400
private const val REVEAL_START_SCALE = 0.6f
private const val REVEAL_START_DEGREES = -30f

@Composable
private fun GrahaRow(position: GrahaPosition) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Column(Modifier.weight(1f)) {
            Text(position.graha.displayName(), style = MaterialTheme.typography.titleMedium)
            Text(position.graha.englishName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1.4f)) {
            Text(
                "${stringResource(R.string.rashi_with_english, position.rashi.displayName(), position.rashi.englishName)} " +
                    formatDegrees(position.degreeInRashi),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(
                    R.string.graha_detail,
                    position.nakshatra.nakshatra.displayName(),
                    position.nakshatra.pada,
                    position.house,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "PURVA_PHALGUNI" becomes "Purva Phalguni". Sanskrit names stay untranslated in every language. */
internal fun Enum<*>.displayName(): String =
    name.lowercase().split('_').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

/** 28.2 becomes "28°12′". */
internal fun formatDegrees(degrees: Double): String {
    // Work in whole arcminutes; the tiny epsilon stops 28.2 from becoming 28°11.99′ in binary floating point.
    val totalMinutes = floor(degrees * 60 + 1e-9).toInt()
    val whole = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "$whole°${minutes.toString().padStart(2, '0')}′"
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun KundliScreenPreview() {
    AstroLingoTheme {
        Surface { KundliScreen(SampleChart.kundli, title = "Sample chart", note = "Asha Testwal (fictional)") }
    }
}
