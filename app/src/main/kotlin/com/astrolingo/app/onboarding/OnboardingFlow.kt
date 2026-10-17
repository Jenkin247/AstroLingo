// Onboarding: purpose, daily goal (learners), level, then birth date, time and place, then a check of what we'll use.
// Sign-in (sprint 3) and the reminder time (sprint 4) slot in after the kundli reveal.
package com.astrolingo.app.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.kundli.displayName
import com.astrolingo.app.ui.places.CitySearch
import com.astrolingo.app.ui.sitara.SitaraSays
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.jyotish.BirthTimeNote
import com.astrolingo.engine.places.City
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.engine.places.HistoricCityClock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Step { PURPOSE, GOAL, LEVEL, DATE, TIME, PLACE, CHECK }

/**
 * The whole onboarding flow. State survives rotation; nothing is stored until [onFinished].
 *
 * @param onFinished called with the answers and the resolved birth moment when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(onFinished: (OnboardingAnswers, ResolvedBirth) -> Unit, modifier: Modifier = Modifier) {
    val index by produceState<CityIndex?>(initialValue = null) {
        value = withContext(Dispatchers.Default) { CityIndex.bundled() }
    }
    var step by rememberSaveable { mutableStateOf(Step.PURPOSE) }
    var purpose by rememberSaveable { mutableStateOf<Purpose?>(null) }
    var goal by rememberSaveable { mutableStateOf<DailyGoal?>(null) }
    var level by rememberSaveable { mutableStateOf<Level?>(null) }
    var timeUnknown by rememberSaveable { mutableStateOf(false) }
    // Test users: an untouched picker must not become a real birth time, so Next waits for a choice.
    var timeChosen by rememberSaveable { mutableStateOf(false) }
    var dayPart by rememberSaveable { mutableStateOf<DayPart?>(null) }
    var cityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var useCityClock by rememberSaveable { mutableStateOf(false) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = null,
        // Open on 1990 rather than this month, so older users scroll less to reach their year.
        initialDisplayedMonthMillis = LocalDate.of(INITIAL_YEAR, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = 1900..LocalDate.now().year,
    )
    val timeState = rememberTimePickerState(initialHour = INITIAL_HOUR, initialMinute = 0, is24Hour = false)

    val date: LocalDate? = dateState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val city: City? = cityId?.let { id -> index?.byId(id) }
    val details: BirthDetails? = if (date != null && city != null) {
        BirthDetails(
            date = date,
            time = if (timeUnknown) null else LocalTime.of(timeState.hour, timeState.minute),
            city = city,
            useCityClock = useCityClock,
            dayPart = if (timeUnknown) dayPart else null,
        )
    } else {
        null
    }

    fun back() {
        step = when (step) {
            Step.PURPOSE -> Step.PURPOSE
            Step.GOAL -> Step.PURPOSE
            Step.LEVEL -> if (purpose == Purpose.LEARN) Step.GOAL else Step.PURPOSE
            Step.DATE -> Step.LEVEL
            Step.TIME -> Step.DATE
            Step.PLACE -> Step.TIME
            Step.CHECK -> Step.PLACE
        }
    }

    Surface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (step != Step.PURPOSE) {
                TextButton(onClick = ::back) { Text(stringResource(R.string.onboarding_back)) }
            }
            when (step) {
                Step.PURPOSE -> ChoiceStep(
                    title = stringResource(R.string.onboarding_purpose_title),
                    options = Purpose.entries,
                    label = { stringResource(if (it == Purpose.LEARN) R.string.purpose_learn else R.string.purpose_guidance) },
                    selected = purpose,
                    onSelect = { purpose = it },
                    onNext = { step = if (purpose == Purpose.LEARN) Step.GOAL else Step.LEVEL },
                    sitara = stringResource(R.string.onboarding_purpose_sitara),
                )
                Step.GOAL -> ChoiceStep(
                    title = stringResource(R.string.onboarding_goal_title),
                    options = DailyGoal.entries,
                    label = { stringResource(R.string.goal_minutes, it.minutes) },
                    selected = goal,
                    onSelect = { goal = it },
                    onNext = { step = Step.LEVEL },
                )
                Step.LEVEL -> ChoiceStep(
                    title = stringResource(R.string.onboarding_level_title),
                    options = Level.entries,
                    label = {
                        stringResource(
                            when (it) {
                                Level.NEW -> R.string.level_new
                                Level.SOME -> R.string.level_some
                                Level.EXPERIENCED -> R.string.level_experienced
                            },
                        )
                    },
                    selected = level,
                    onSelect = { level = it },
                    onNext = { step = Step.DATE },
                )
                Step.DATE -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    StepTitle(stringResource(R.string.onboarding_date_title))
                    Text(stringResource(R.string.onboarding_private_note), style = MaterialTheme.typography.bodyMedium)
                    DatePicker(state = dateState, title = null, showModeToggle = true)
                    NextButton(enabled = date != null) { step = Step.TIME }
                }
                Step.TIME -> Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    StepTitle(stringResource(R.string.onboarding_time_title))
                    if (!timeUnknown) TimePicker(state = timeState)
                    LaunchedEffect(timeState.hour, timeState.minute) {
                        if (timeState.hour != INITIAL_HOUR || timeState.minute != 0) timeChosen = true
                    }
                    if (!timeUnknown && !timeChosen) {
                        OutlinedButton(onClick = { timeChosen = true }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                            Text(stringResource(R.string.onboarding_time_is_initial, formatTime(LocalTime.of(INITIAL_HOUR, 0))))
                        }
                    }
                    if (!timeUnknown && timeState.hour < AFTER_MIDNIGHT_HOURS) {
                        // Test users: families often call 12:20 am on the 8th "the night of the 7th".
                        SitaraSays(stringResource(R.string.onboarding_time_after_midnight))
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { timeUnknown = !timeUnknown }.heightIn(min = Spacing.touchTarget),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = timeUnknown, onCheckedChange = null)
                        Text(stringResource(R.string.onboarding_time_unknown), Modifier.padding(start = Spacing.sm))
                    }
                    if (timeUnknown) {
                        SitaraSays(stringResource(R.string.onboarding_time_unknown_sitara))
                    }
                    NextButton(enabled = timeUnknown || timeChosen) { step = Step.PLACE }
                }
                Step.PLACE -> PlaceStep(
                    index = index,
                    selected = city,
                    onSelect = { cityId = it.id; useCityClock = false },
                    onNext = { step = Step.CHECK },
                    modifier = Modifier.weight(1f),
                )
                Step.CHECK -> if (details != null && index != null) {
                    CheckStep(
                        details = details,
                        index = index!!,
                        onUseCityClock = { useCityClock = it },
                        onDayPart = { dayPart = it },
                        onConfirm = { resolved ->
                            onFinished(
                                OnboardingAnswers(checkNotNull(purpose), goal, checkNotNull(level), details),
                                resolved,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun NextButton(enabled: Boolean, label: String = stringResource(R.string.onboarding_next), onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
        Text(label)
    }
}

@Composable
private fun <T> ChoiceStep(
    title: String,
    options: List<T>,
    label: @Composable (T) -> String,
    selected: T?,
    onSelect: (T) -> Unit,
    onNext: () -> Unit,
    sitara: String? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (sitara != null) SitaraSays(sitara)
        StepTitle(title)
        options.forEach { option ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = if (option == selected) 4.dp else 0.dp,
                color = if (option == selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) },
            ) {
                Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = option == selected, onClick = null)
                    Text(label(option), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = Spacing.sm))
                }
            }
        }
        NextButton(enabled = selected != null, onClick = onNext)
    }
}

@Composable
private fun PlaceStep(
    index: CityIndex?,
    selected: City?,
    onSelect: (City) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StepTitle(stringResource(R.string.onboarding_place_title))
        CitySearch(index = index, selected = selected, onSelect = onSelect, modifier = Modifier.weight(1f))
        NextButton(enabled = selected != null, onClick = onNext)
    }
}

@Composable
private fun CheckStep(
    details: BirthDetails,
    index: CityIndex,
    onUseCityClock: (Boolean) -> Unit,
    onDayPart: (DayPart?) -> Unit,
    onConfirm: (ResolvedBirth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = remember(details.date, details.time, details.city) { BirthDetailsResolver.cityClockToAsk(details, index) }
    val resolved = remember(details) { BirthDetailsResolver.resolve(details, index) }
    val moonByPart = remember(details.date, details.time, details.city, details.useCityClock) {
        BirthDetailsResolver.moonByDayPart(details, index)
    }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StepTitle(stringResource(R.string.onboarding_check_title))
        Text(details.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)), style = MaterialTheme.typography.titleMedium)
        Text(
            details.time?.let(::formatTime) ?: stringResource(R.string.onboarding_check_time_unknown),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(details.city.label, style = MaterialTheme.typography.titleMedium)

        if (clock != null) {
            Text(
                stringResource(
                    if (clock == HistoricCityClock.BOMBAY_TIME) R.string.city_clock_bombay else R.string.city_clock_calcutta,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            ClockOption(stringResource(R.string.city_clock_ist), selected = !details.useCityClock) { onUseCityClock(false) }
            ClockOption(stringResource(R.string.city_clock_local), selected = details.useCityClock) { onUseCityClock(true) }
        }
        if (moonByPart != null) {
            SitaraSays(stringResource(R.string.day_part_question))
            DayPart.entries.forEach { part ->
                val moon = moonByPart.getValue(part).map { it.label() }.joinToString(stringResource(R.string.day_part_or))
                ClockOption(
                    stringResource(R.string.day_part_option, stringResource(part.label), moon),
                    selected = details.dayPart == part,
                ) { onDayPart(part) }
            }
            ClockOption(stringResource(R.string.day_part_not_sure), selected = details.dayPart == null) { onDayPart(null) }
        }
        resolved.notes.forEach { note ->
            val text = when (note) {
                BirthTimeNote.CLOCKS_AHEAD -> R.string.note_clocks_ahead
                BirthTimeNote.SKIPPED_BY_CLOCK_CHANGE -> R.string.note_skipped
                BirthTimeNote.REPEATED_BY_CLOCK_CHANGE -> R.string.note_repeated
                BirthTimeNote.LOCAL_MEAN_TIME_ERA -> R.string.note_local_mean_time
            }
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.heightIn(min = Spacing.sm))
        NextButton(enabled = true, label = stringResource(R.string.onboarding_show_kundli)) { onConfirm(resolved) }
        Text(
            stringResource(R.string.onboarding_private_note),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ClockOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick).heightIn(min = Spacing.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = Spacing.sm))
    }
}

/** Birth times before 4 am get a reminder that the date is the calendar date, not "the night of". */
private const val AFTER_MIDNIGHT_HOURS = 4

private const val INITIAL_HOUR = 6
private const val INITIAL_YEAR = 1990

/** "5:45 am" or "05:45", following the phone's language. */
private fun formatTime(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private val DayPart.label: Int
    get() = when (this) {
        DayPart.NIGHT -> R.string.day_part_night
        DayPart.MORNING -> R.string.day_part_morning
        DayPart.AFTERNOON -> R.string.day_part_afternoon
        DayPart.EVENING -> R.string.day_part_evening
    }

/** "Meena (Pisces), Revati". */
@Composable
private fun MoonPlace.label(): String =
    stringResource(R.string.rashi_with_english, rashi.displayName(), rashi.englishName) + ", " + nakshatra.displayName()
