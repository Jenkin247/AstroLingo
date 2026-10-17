// Today tab: the daily card from fixed Vedic rules, worked out on the phone (docs/spec/daily-card-rules.md).
package com.astrolingo.app.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astrolingo.app.R
import com.astrolingo.app.ui.kundli.SampleChart
import com.astrolingo.app.ui.kundli.displayName
import com.astrolingo.app.ui.places.CitySearch
import com.astrolingo.app.ui.places.UseMyLocation
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.jyotish.DailyCardFacts
import com.astrolingo.engine.jyotish.DayColour
import com.astrolingo.engine.jyotish.DayStrength
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.Tithi
import com.astrolingo.engine.places.City
import com.astrolingo.engine.places.CityIndex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Today tab.
 *
 * @param kundli the user's chart; only the birth Moon is used.
 * @param place where sunrise and Rahu kaal are worked out.
 * @param showDeity false when the user turned deities off; then no deity is ever shown.
 * @param onPlaceChange called with the place the user picked, or null to go back to the birthplace.
 */
@Composable
fun TodayScreen(
    kundli: Kundli,
    place: City,
    showDeity: Boolean,
    modifier: Modifier = Modifier,
    onPlaceChange: (City?) -> Unit = {},
) {
    var choosingPlace by remember { mutableStateOf(false) }
    val today = LocalDate.now(place.zone)
    val card = remember(kundli, today, place) { TodayCard.build(kundli, today, place) }
    val tint = AstroLingoTheme.tabColors.today
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(card.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)), style = MaterialTheme.typography.titleMedium)
        val facts = card.facts
        Column(
            Modifier
                .fillMaxWidth()
                .background(tint, MaterialTheme.shapes.large)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (facts != null) {
                Text(stringResource(facts.strength.label), style = MaterialTheme.typography.labelLarge, color = AstroLingoTheme.tabColors.onPastel)
                Text(
                    stringArrayResource(facts.strength.themes)[facts.gocharHouse - 1],
                    style = MaterialTheme.typography.headlineSmall,
                    color = AstroLingoTheme.tabColors.onPastel,
                )
                val taraLines = stringArrayResource(R.array.daily_tara)
                val change = facts.panchang.nakshatraChange
                val after = facts.taraAfterChange
                val starLines = if (change != null && after != null) {
                    val at = time(change.atUtcMillis, place.zone)
                    listOf(
                        stringResource(R.string.today_until, at, taraLines[facts.taraAtSunrise.ordinal]),
                        stringResource(R.string.today_from, at, taraLines[after.ordinal]),
                    )
                } else {
                    listOf(taraLines[facts.tara.ordinal])
                }
                starLines.forEach {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = AstroLingoTheme.tabColors.onPastel)
                }
            }
            Text(
                stringArrayResource(R.array.daily_vara)[card.vara.ordinal],
                style = MaterialTheme.typography.bodyLarge,
                color = AstroLingoTheme.tabColors.onPastel,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Spacer(
                Modifier
                    .size(28.dp)
                    .background(card.colour.swatch, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Text(stringResource(R.string.today_colour, stringResource(card.colour.label)), style = MaterialTheme.typography.bodyLarge)
        }
        if (showDeity) {
            Text(stringResource(R.string.today_deity, card.deity.displayName()), style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider()
        if (facts != null) {
            PanchangDetails(facts, place.zone)
        } else {
            Text(stringResource(R.string.today_no_sunrise), style = MaterialTheme.typography.bodyMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.today_times_for, place.label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { choosingPlace = true }) { Text(stringResource(R.string.today_change_place)) }
        }
    }
    if (choosingPlace) {
        PlaceDialog(
            current = place,
            onDismiss = { choosingPlace = false },
            onPick = {
                choosingPlace = false
                onPlaceChange(it)
            },
        )
    }
}

/** Full-screen place search, so sunrise and Rahu kaal follow where the user lives now. */
@Composable
private fun PlaceDialog(current: City, onDismiss: () -> Unit, onPick: (City?) -> Unit) {
    val index by produceState<CityIndex?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { CityIndex.bundled() }
    }
    var picked by remember { mutableStateOf<City?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(stringResource(R.string.today_place_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.today_place_note), style = MaterialTheme.typography.bodyMedium)
                UseMyLocation(index = index, onFound = { picked = it })
                CitySearch(index = index, selected = picked, onSelect = { picked = it }, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.today_place_birthplace)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.me_delete_cancel)) }
                    Button(onClick = { onPick(picked) }, enabled = picked != null && picked != current) {
                        Text(stringResource(R.string.today_place_use))
                    }
                }
            }
        }
    }
}

@Composable
private fun PanchangDetails(facts: DailyCardFacts, zone: ZoneId) {
    val p = facts.panchang
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            stringResource(R.string.today_rahu_kaal, time(p.rahuKaal.startUtcMillis, zone), time(p.rahuKaal.endUtcMillis, zone)),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringArrayResource(R.array.daily_rahu_tip)[facts.strength.ordinal], style = MaterialTheme.typography.bodyMedium)
        Text(tithiName(p.tithi), style = MaterialTheme.typography.bodyLarge)
        val rashi = stringResource(R.string.rashi_with_english, p.moonRashi.displayName(), p.moonRashi.englishName)
        val star = p.nakshatra.nakshatra.displayName()
        Text(
            stringResource(
                R.string.today_moon,
                p.moonRashiChange?.let { stringResource(R.string.today_moon_change, rashi, time(it.atUtcMillis, zone), it.next.displayName()) }
                    ?: rashi,
                p.nakshatraChange?.let { stringResource(R.string.today_moon_change, star, time(it.atUtcMillis, zone), it.next.displayName()) }
                    ?: star,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            stringResource(R.string.today_sun, time(p.sunriseUtcMillis, zone), time(p.sunsetUtcMillis, zone)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun tithiName(tithi: Tithi): String = when {
    tithi.isPurnima -> stringResource(R.string.tithi_purnima)
    tithi.isAmavasya -> stringResource(R.string.tithi_amavasya)
    else -> stringResource(
        if (tithi.isShukla) R.string.tithi_shukla else R.string.tithi_krishna,
        stringArrayResource(R.array.tithi_names)[tithi.numberInPaksha - 1],
    )
}

private fun time(utcMillis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(utcMillis).atZone(zone).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private val DayStrength.label: Int
    get() = when (this) {
        DayStrength.STRONG -> R.string.today_strong
        DayStrength.STEADY -> R.string.today_steady
        DayStrength.GENTLE -> R.string.today_gentle
    }

private val DayStrength.themes: Int
    get() = when (this) {
        DayStrength.STRONG -> R.array.daily_theme_strong
        DayStrength.STEADY -> R.array.daily_theme_steady
        DayStrength.GENTLE -> R.array.daily_theme_gentle
    }

private val DayColour.label: Int
    get() = when (this) {
        DayColour.ORANGE -> R.string.colour_orange
        DayColour.WHITE -> R.string.colour_white
        DayColour.RED -> R.string.colour_red
        DayColour.GREEN -> R.string.colour_green
        DayColour.YELLOW -> R.string.colour_yellow
        DayColour.PINK -> R.string.colour_pink
        DayColour.DARK_BLUE -> R.string.colour_dark_blue
    }

private val DayColour.swatch: Color
    get() = when (this) {
        DayColour.ORANGE -> Color(0xFFF28C28)
        DayColour.WHITE -> Color(0xFFFFFFFF)
        DayColour.RED -> Color(0xFFD32F2F)
        DayColour.GREEN -> Color(0xFF2E7D32)
        DayColour.YELLOW -> Color(0xFFFBC02D)
        DayColour.PINK -> Color(0xFFEC407A)
        DayColour.DARK_BLUE -> Color(0xFF1A237E)
    }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun TodayScreenPreview() {
    val pune = City(1259229, "Pune", "Maharashtra", "IN", 18.51957, 73.85535, ZoneId.of("Asia/Kolkata"), 0, emptyList())
    AstroLingoTheme { Surface { TodayScreen(SampleChart.kundli, pune, showDeity = true) } }
}
