// Saved people in Me (LS-8): the user plus up to three others, kept only on this phone, to compare charts in Learn.
package com.astrolingo.app.people

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.ui.components.InfoCard
import com.astrolingo.app.ui.places.CitySearch
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.places.City
import com.astrolingo.engine.places.CityIndex
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The pastel behind each person's initial, in list order (you first). Shared with the compare sheet in Learn. */
@Composable
fun personColor(index: Int): Color {
    val colors = AstroLingoTheme.tabColors
    return listOf(colors.today, colors.me, colors.kundli, colors.learn)[index % 4]
}

/** A round initial for a person, such as "A" for Asha. */
@Composable
fun PersonAvatar(name: String, index: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .background(personColor(index), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = AstroLingoTheme.tabColors.onPastel,
        )
    }
}

/** "15 Aug 1990 · 06:30 · Pune", or with "time unknown". */
@Composable
internal fun birthSummary(birth: BirthDetails): String {
    val date = birth.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val time = birth.time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: stringResource(R.string.people_time_unknown)
    return listOf(date, time, birth.city.name).joinToString(" · ")
}

/**
 * The Saved people block in Me.
 *
 * @param you the user's own birth details, always first.
 */
@Composable
fun SavedPeopleCard(
    you: BirthDetails,
    people: List<SavedPerson>,
    onAdd: () -> Unit,
    onEdit: (SavedPerson) -> Unit,
    onDelete: (SavedPerson) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleting by remember { mutableStateOf<SavedPerson?>(null) }
    InfoCard(modifier = modifier, title = stringResource(R.string.people_title), icon = Icons.Filled.Person) {
        Text(
            stringResource(R.string.people_count, people.size + 1, SavedPerson.MAX_OTHERS + 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PersonRow(name = stringResource(R.string.people_you), index = 0, summary = birthSummary(you))
        people.forEachIndexed { i, person ->
            HorizontalDivider()
            PersonRow(name = person.name, index = i + 1, summary = birthSummary(person.birth)) {
                IconButton(onClick = { onEdit(person) }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.people_edit, person.name))
                }
                IconButton(onClick = { deleting = person }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.people_delete, person.name))
                }
            }
        }
        HorizontalDivider()
        val left = SavedPerson.MAX_OTHERS - people.size
        if (left > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onAdd)
                    .heightIn(min = Spacing.touchTarget)
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(stringResource(R.string.people_add), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.people_places_left, left),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Text(stringResource(R.string.people_full), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.people_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    deleting?.let { person ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.people_delete_title, person.name)) },
            text = { Text(stringResource(R.string.people_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    onDelete(person)
                }) { Text(stringResource(R.string.me_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.people_cancel)) } },
        )
    }
}

@Composable
private fun PersonRow(name: String, index: Int, summary: String, actions: @Composable () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PersonAvatar(name, index)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        actions()
    }
}

/**
 * Adds a person, or edits [editing]. Save stays off until there is a name, a date, a time (or "I don't know"),
 * a place and the consent tick.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonForm(
    editing: SavedPerson?,
    onSave: (SavedPerson) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onCancel)
    val index by produceState<CityIndex?>(null) { value = withContext(Dispatchers.Default) { CityIndex.bundled() } }
    var name by rememberSaveable { mutableStateOf(editing?.name.orEmpty()) }
    var dateEpochDay by rememberSaveable { mutableStateOf(editing?.birth?.date?.toEpochDay()) }
    var minuteOfDay by rememberSaveable { mutableStateOf(editing?.birth?.time?.let { it.hour * 60 + it.minute }) }
    var timeUnknown by rememberSaveable { mutableStateOf(editing != null && editing.birth.time == null) }
    var cityId by rememberSaveable { mutableStateOf(editing?.birth?.city?.id) }
    // An edit keeps the stored place even before the city list has loaded.
    val city: City? = cityId?.let { id -> index?.byId(id) ?: editing?.birth?.city?.takeIf { it.id == id } }
    var consent by rememberSaveable { mutableStateOf(editing != null) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }

    val date = dateEpochDay?.let(LocalDate::ofEpochDay)
    val time = minuteOfDay?.let { LocalTime.of(it / 60, it % 60) }
    val cleanName = SavedPerson.cleanName(name)
    val ready = cleanName.isNotEmpty() && date != null && (timeUnknown || time != null) && city != null && consent

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.people_back))
            }
            Text(
                if (editing == null) stringResource(R.string.people_add_title) else stringResource(R.string.people_edit_title, editing.name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= SavedPerson.MAX_NAME_LENGTH) name = it },
            label = { Text(stringResource(R.string.people_name)) },
            supportingText = { Text(stringResource(R.string.people_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.people_date), style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Text(date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) ?: stringResource(R.string.people_date_choose))
        }
        Text(stringResource(R.string.people_time), style = MaterialTheme.typography.titleSmall)
        if (!timeUnknown) {
            OutlinedButton(onClick = { pickingTime = true }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                Text(time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: stringResource(R.string.people_time_choose))
            }
        }
        CheckRow(checked = timeUnknown, text = stringResource(R.string.people_time_unknown_check)) { timeUnknown = it }
        Text(stringResource(R.string.people_place), style = MaterialTheme.typography.titleSmall)
        CitySearch(index = index, selected = city, onSelect = { cityId = it.id }, modifier = Modifier.heightIn(max = 320.dp))
        CheckRow(
            checked = consent,
            text = if (cleanName.isEmpty()) stringResource(R.string.people_consent_someone) else stringResource(R.string.people_consent, cleanName),
        ) { consent = it }
        Button(
            onClick = {
                onSave(
                    SavedPerson(
                        id = editing?.id ?: UUID.randomUUID().toString(),
                        name = cleanName,
                        birth = BirthDetails(date = checkNotNull(date), time = if (timeUnknown) null else time, city = checkNotNull(city)),
                        createdAt = editing?.createdAt ?: System.currentTimeMillis(),
                    ),
                )
            },
            enabled = ready,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.touchTarget),
        ) {
            Text(stringResource(R.string.people_save))
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            initialDisplayedMonthMillis = (date ?: LocalDate.of(INITIAL_YEAR, 1, 1)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1900..LocalDate.now().year,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateEpochDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                    pickingDate = false
                }) { Text(stringResource(R.string.people_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.people_cancel)) } },
        ) {
            DatePicker(state = state, title = null, showModeToggle = true)
        }
    }
    if (pickingTime) {
        val state = rememberTimePickerState(initialHour = time?.hour ?: INITIAL_HOUR, initialMinute = time?.minute ?: 0, is24Hour = false)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            confirmButton = {
                TextButton(onClick = {
                    minuteOfDay = state.hour * 60 + state.minute
                    pickingTime = false
                }) { Text(stringResource(R.string.people_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.people_cancel)) } },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun CheckRow(checked: Boolean, text: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onChange(!checked) }
            .heightIn(min = Spacing.touchTarget)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text, Modifier.padding(start = Spacing.sm), style = MaterialTheme.typography.bodyMedium)
    }
}

private const val INITIAL_YEAR = 1990
private const val INITIAL_HOUR = 12
