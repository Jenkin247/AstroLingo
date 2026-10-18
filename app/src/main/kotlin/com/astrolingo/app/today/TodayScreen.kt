// Today tab: the daily card from fixed Vedic rules, worked out on the phone (docs/spec/daily-card-rules.md).
package com.astrolingo.app.today

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.astrolingo.app.learn.ReviewOffer
import com.astrolingo.app.ui.components.FactRow
import com.astrolingo.app.ui.components.InfoCard
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
import java.time.LocalTime
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
 * @param placeGiven true when the user told us where they live; the greeting then follows the local time there,
 *   otherwise it stays generic.
 * @param onPlaceChange called with the place the user picked, or null to go back to the birthplace.
 * @param reviewDue Review cards due today; when there are any, a small card offers [onReview] (LS-5).
 * @param onLearn opens the lesson behind a part of the card (LS-6); null hides the "Learn why" links.
 */
@Composable
fun TodayScreen(
    kundli: Kundli,
    place: City,
    showDeity: Boolean,
    modifier: Modifier = Modifier,
    placeGiven: Boolean = false,
    onPlaceChange: (City?) -> Unit = {},
    reviewDue: Int = 0,
    onReview: () -> Unit = {},
    onLearn: ((TodayTopic) -> Unit)? = null,
) {
    var choosingPlace by remember { mutableStateOf(false) }
    val today = LocalDate.now(place.zone)
    val card = remember(kundli, today, place) { TodayCard.build(kundli, today, place) }
    val facts = card.facts
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column {
            // The greeting follows the phone's own clock: that is the time where the user actually is.
            Text(
                stringResource(if (placeGiven) greeting(LocalTime.now()) else R.string.today_greeting),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                card.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PlaceRow(place, onChange = { choosingPlace = true })
        if (!placeGiven) {
            // Profiles saved before onboarding asked where the user lives: ask once here instead.
            InfoCard(title = stringResource(R.string.today_ask_place_title), icon = Icons.Filled.LocationOn) {
                Text(stringResource(R.string.today_ask_place), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { choosingPlace = true }) { Text(stringResource(R.string.today_ask_place_button)) }
            }
        }
        HeroCard(card, place.zone, onLearn)
        if (reviewDue > 0) ReviewOffer(due = reviewDue, onStart = onReview)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.height(IntrinsicSize.Min)) {
            InfoCard(modifier = Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.today_label_colour)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Spacer(
                        Modifier
                            .size(28.dp)
                            .background(card.colour.swatch, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                    Text(stringResource(card.colour.label), style = MaterialTheme.typography.titleMedium)
                }
            }
            if (showDeity) {
                InfoCard(modifier = Modifier.weight(1f).fillMaxHeight(), title = stringResource(R.string.today_label_deity)) {
                    Text(card.deity.displayName(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        LearnLink(R.string.today_why_colour, TodayTopic.VARA, onLearn)
        if (facts != null) {
            TimingsCard(facts, place.zone, onLearn)
            MoonCard(facts, place.zone, onLearn)
        } else {
            InfoCard { Text(stringResource(R.string.today_no_sunrise), style = MaterialTheme.typography.bodyMedium) }
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

/** Where the day's times are worked out, with a way to change it. */
@Composable
private fun PlaceRow(place: City, onChange: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            stringResource(R.string.today_times_for, place.label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onChange) { Text(stringResource(R.string.today_change_place)) }
    }
}

/** The day's message: strength, theme, the star lines and the weekday line, on the Today pastel. */
@Composable
private fun HeroCard(card: TodayCard, zone: ZoneId, onLearn: ((TodayTopic) -> Unit)?) {
    val ink = AstroLingoTheme.tabColors.onPastel
    val facts = card.facts
    InfoCard(container = AstroLingoTheme.tabColors.today) {
        if (facts != null) {
            Surface(shape = MaterialTheme.shapes.small, color = ink.copy(alpha = PILL_ALPHA)) {
                Text(
                    stringResource(facts.strength.label),
                    style = MaterialTheme.typography.labelLarge,
                    color = ink,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
            }
            Text(
                stringArrayResource(facts.strength.themes)[facts.gocharHouse - 1],
                style = MaterialTheme.typography.titleLarge,
                color = ink,
            )
            HorizontalDivider(color = ink.copy(alpha = PILL_ALPHA))
            val taraLines = stringArrayResource(R.array.daily_tara)
            val change = facts.panchang.nakshatraChange
            val after = facts.taraAfterChange
            if (change != null && after != null) {
                val at = time(change.atUtcMillis, zone)
                StarLine(stringResource(R.string.today_until_label, at), taraLines[facts.taraAtSunrise.ordinal], ink)
                StarLine(stringResource(R.string.today_from_label, at), taraLines[after.ordinal], ink)
            } else {
                Text(taraLines[facts.tara.ordinal], style = MaterialTheme.typography.bodyLarge, color = ink)
            }
        }
        Text(stringArrayResource(R.array.daily_vara)[card.vara.ordinal], style = MaterialTheme.typography.bodyLarge, color = ink)
        if (facts != null) LearnLink(R.string.today_why_day, TodayTopic.STRENGTH, onLearn, color = ink)
    }
}

/** "Why this colour?" and the like: opens the lesson that explains that part of the card. */
@Composable
private fun LearnLink(@StringRes label: Int, topic: TodayTopic, onLearn: ((TodayTopic) -> Unit)?, color: Color = MaterialTheme.colorScheme.primary) {
    if (onLearn == null) return
    TextButton(onClick = { onLearn(topic) }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(Spacing.xs))
        Text(stringResource(label), color = color)
    }
}

/** "Until 12:49 PM" on its own line in bold, the message under it, so the two halves of the day are easy to tell apart. */
@Composable
private fun StarLine(whenLabel: String, message: String, ink: Color) {
    Column {
        Text(whenLabel, style = MaterialTheme.typography.labelLarge, color = ink)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = ink)
    }
}

@Composable
private fun TimingsCard(facts: DailyCardFacts, zone: ZoneId, onLearn: ((TodayTopic) -> Unit)?) {
    val p = facts.panchang
    InfoCard(title = stringResource(R.string.today_block_timings), icon = Icons.Filled.DateRange) {
        FactRow(
            label = stringResource(R.string.today_label_rahu),
            value = stringResource(R.string.today_time_range, time(p.rahuKaal.startUtcMillis, zone), time(p.rahuKaal.endUtcMillis, zone)),
            note = stringArrayResource(R.array.daily_rahu_tip)[facts.strength.ordinal],
        )
        FactRow(stringResource(R.string.today_label_sunrise), time(p.sunriseUtcMillis, zone))
        FactRow(stringResource(R.string.today_label_sunset), time(p.sunsetUtcMillis, zone))
        LearnLink(R.string.today_why_rahu, TodayTopic.RAHU_KAAL, onLearn)
    }
}

@Composable
private fun MoonCard(facts: DailyCardFacts, zone: ZoneId, onLearn: ((TodayTopic) -> Unit)?) {
    val p = facts.panchang
    InfoCard(title = stringResource(R.string.today_block_moon), icon = Icons.Filled.Star) {
        FactRow(stringResource(R.string.today_label_tithi), tithiName(p.tithi))
        val rashi = stringResource(R.string.rashi_with_english, p.moonRashi.displayName(), p.moonRashi.englishName)
        FactRow(
            label = stringResource(R.string.today_label_moon),
            value = rashi,
            note = p.moonRashiChange?.let { stringResource(R.string.today_change_at, time(it.atUtcMillis, zone), it.next.displayName()) },
        )
        FactRow(
            label = stringResource(R.string.today_label_star),
            value = p.nakshatra.nakshatra.displayName(),
            note = p.nakshatraChange?.let { stringResource(R.string.today_change_at, time(it.atUtcMillis, zone), it.next.displayName()) },
        )
        LearnLink(R.string.today_why_tithi, TodayTopic.TITHI, onLearn)
        LearnLink(R.string.today_why_star, TodayTopic.NAKSHATRA, onLearn)
    }
}

/** Good morning from 5 am, afternoon from noon, evening from 5 pm; in the small hours just "Namaste!". */
private fun greeting(now: LocalTime): Int = when (now.hour) {
    in MORNING_FROM until NOON -> R.string.today_greeting_morning
    in NOON until EVENING_FROM -> R.string.today_greeting_afternoon
    in EVENING_FROM..LAST_HOUR -> R.string.today_greeting_evening
    else -> R.string.today_greeting
}

private const val MORNING_FROM = 5
private const val LAST_HOUR = 23
private const val PILL_ALPHA = 0.12f
private const val NOON = 12
private const val EVENING_FROM = 17

@Composable
private fun tithiName(tithi: Tithi): String = when {
    tithi.isPurnima -> stringResource(R.string.tithi_purnima)
    tithi.isAmavasya -> stringResource(R.string.tithi_amavasya)
    else -> stringResource(
        if (tithi.isShukla) R.string.tithi_shukla else R.string.tithi_krishna,
        stringArrayResource(R.array.tithi_names)[tithi.numberInPaksha - 1],
    )
}

/** Rounded to the nearest minute, so 09:46:57 reads 9:47 as in a printed panchang. */
private fun time(utcMillis: Long, zone: ZoneId): String =
    Instant.ofEpochMilli(utcMillis + HALF_MINUTE_MILLIS).atZone(zone).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private const val HALF_MINUTE_MILLIS = 30_000L

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
    AstroLingoTheme { Surface { TodayScreen(SampleChart.kundli, pune, showDeity = true, placeGiven = true) } }
}
