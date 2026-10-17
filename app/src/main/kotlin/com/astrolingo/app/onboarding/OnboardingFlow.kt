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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
    var cityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var useCityClock by rememberSaveable { mutableStateOf(false) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = null,
        yearRange = 1900..LocalDate.now().year,
    )
    val timeState = rememberTimePickerState(initialHour = 6, initialMinute = 0, is24Hour = false)

    val date: LocalDate? = dateState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val city: City? = cityId?.let { id -> index?.byId(id) }
    val details: BirthDetails? = if (date != null && city != null) {
        BirthDetails(date, if (timeUnknown) null else LocalTime.of(timeState.hour, timeState.minute), city, useCityClock)
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
                    NextButton(enabled = true) { step = Step.PLACE }
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
    var query by rememberSaveable { mutableStateOf(selected?.label ?: "") }
    val results = remember(query, index) { if (index == null || query == selected?.label) emptyList() else index.search(query) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StepTitle(stringResource(R.string.onboarding_place_title))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.onboarding_place_hint)) },
            singleLine = true,
            enabled = index != null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (index == null) Text(stringResource(R.string.onboarding_place_loading))
        LazyColumn(Modifier.weight(1f)) {
            items(results, key = { it.id }) { city ->
                Text(
                    city.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(city); query = city.label }
                        .heightIn(min = Spacing.touchTarget)
                        .padding(vertical = Spacing.sm),
                )
            }
        }
        if (query.length >= 2 && results.isEmpty() && query != selected?.label) {
            Text(stringResource(R.string.onboarding_place_none), style = MaterialTheme.typography.bodyMedium)
        }
        NextButton(enabled = selected != null && query == selected.label, onClick = onNext)
    }
}

@Composable
private fun CheckStep(
    details: BirthDetails,
    index: CityIndex,
    onUseCityClock: (Boolean) -> Unit,
    onConfirm: (ResolvedBirth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = remember(details.date, details.time, details.city) { BirthDetailsResolver.cityClockToAsk(details, index) }
    val resolved = remember(details) { BirthDetailsResolver.resolve(details, index) }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        StepTitle(stringResource(R.string.onboarding_check_title))
        Text(details.date.toString(), style = MaterialTheme.typography.titleMedium)
        Text(
            details.time?.toString() ?: stringResource(R.string.onboarding_check_time_unknown),
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
