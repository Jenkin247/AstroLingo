// Kundli tab: the chart in the user's preferred style and the list of grahas below it.
package com.astrolingo.app.ui.kundli

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.chart.ChartStyle
import com.astrolingo.engine.jyotish.GrahaPosition
import com.astrolingo.engine.jyotish.Kundli
import kotlin.math.floor

/**
 * The Kundli tab.
 *
 * @param kundli the chart to show.
 * @param title who the chart belongs to, for example "Your kundli" or "Sample chart".
 * @param note optional line under the title.
 */
@OptIn(ExperimentalMaterial3Api::class) // segmented buttons; stable in recent Material 3, harmless otherwise
@Composable
fun KundliScreen(kundli: Kundli, title: String, note: String?, modifier: Modifier = Modifier) {
    var style by rememberSaveable { mutableStateOf(ChartStyle.NORTH_INDIAN) }
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
        KundliChart(
            kundli = kundli,
            style = style,
            description = stringResource(R.string.chart_description, kundli.lagna.displayName(), kundli.moonSign.displayName()),
        )
        Text(stringResource(R.string.grahas_heading), style = MaterialTheme.typography.titleLarge)
        kundli.grahas.forEach { position ->
            GrahaRow(position)
            HorizontalDivider()
        }
    }
}

@Composable
private fun GrahaRow(position: GrahaPosition) {
    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Column(Modifier.weight(1f)) {
            Text(position.graha.displayName(), style = MaterialTheme.typography.titleMedium)
            Text(position.graha.englishName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1.4f)) {
            Text(
                "${position.rashi.displayName()} ${formatDegrees(position.degreeInRashi)}",
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
    val whole = floor(degrees).toInt()
    val minutes = floor((degrees - whole) * 60).toInt()
    return "$whole°${minutes.toString().padStart(2, '0')}′"
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun KundliScreenPreview() {
    AstroLingoTheme {
        Surface { KundliScreen(SampleChart.kundli, title = "Sample chart", note = "Asha Testwal (fictional)") }
    }
}
