// The chart bubble on Learn cards (LS-9): a floating circle that opens a sheet showing where the card's graha sits in
// the learner's chart and in up to three saved people's charts. Only what the learner has studied is shown.
// Rules are in ChartLens; this file is only the Compose UI. Charts are calculated on the phone and never leave it.
package com.astrolingo.app.learn

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.people.PersonAvatar
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.ui.kundli.KundliChart
import com.astrolingo.app.ui.kundli.displayName
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.chart.ChartStyle
import com.astrolingo.engine.jyotish.Dignity
import com.astrolingo.engine.jyotish.Element
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.engine.jyotish.element
import com.astrolingo.learn.content.Card as LessonCard
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.progress.LearnProgress
import kotlin.math.roundToInt

/**
 * The bubble and its sheet for the card on screen. Shows nothing on quizzes, on cards without a graha, and until the
 * learner has finished the Grahas chapter.
 *
 * @param people the learner first, then saved people.
 * @param onAddPerson opens Me's "Add a person" form.
 */
@Composable
internal fun BoxScope.ChartLensOverlay(
    card: LessonCard?,
    people: List<ChartPerson>,
    course: Course,
    progress: LearnProgress,
    language: Language,
    onAddPerson: () -> Unit,
) {
    val graha = card?.let(ChartLens::grahaOf) ?: return
    if (people.isEmpty() || !ChartLens.chapterFinished(course, progress, ChartLens.GRAHAS_CHAPTER)) return
    var open by remember(card) { mutableStateOf(false) }
    ChartBubble(count = people.size, onOpen = { open = true })
    if (open) {
        ChartLensSheet(
            graha = graha,
            people = people,
            course = course,
            progress = progress,
            language = language,
            onAddPerson = {
                open = false
                onAddPerson()
            },
            onDismiss = { open = false },
        )
    }
}

/**
 * The floating circle. Drag it up or down, or across to the other side; the side is remembered. TalkBack users get a
 * "Move to the other side" action instead of dragging.
 */
@Composable
internal fun BoxScope.ChartBubble(count: Int, onOpen: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var left by remember { mutableStateOf(prefs.getBoolean(KEY_LEFT, false)) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    // How far above its resting place the learner moved it, in pixels (negative is up).
    var lift by rememberSaveable { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val flipAt = with(density) { FLIP_DISTANCE.toPx() }
    val maxLift = with(density) { MAX_LIFT.toPx() }
    val moveSide = {
        left = !left
        prefs.edit().putBoolean(KEY_LEFT, left).apply()
    }
    val label = pluralStringResource(R.plurals.lens_bubble, count, count)
    val moveLabel = stringResource(R.string.lens_bubble_move)
    Box(
        Modifier
            .align(if (left) Alignment.BottomStart else Alignment.BottomEnd)
            .padding(start = Spacing.md, end = Spacing.md, bottom = BOTTOM_CLEARANCE)
            .offset { IntOffset(drag.x.roundToInt(), (lift + drag.y).roundToInt()) }
            .size(BUBBLE_SIZE)
            .shadow(6.dp, CircleShape, clip = false)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .pointerInput(left) {
                detectDragGestures(
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount
                    },
                    onDragEnd = {
                        val across = if (left) drag.x > flipAt else drag.x < -flipAt
                        lift = (lift + drag.y).coerceIn(-maxLift, 0f)
                        drag = Offset.Zero
                        if (across) moveSide()
                    },
                    onDragCancel = { drag = Offset.Zero },
                )
            }
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics {
                contentDescription = label
                customActions = listOf(CustomAccessibilityAction(moveLabel) { moveSide(); true })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Badge(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).clearAndSetSemantics {}) { Text(count.toString()) }
    }
}

/** Where the card's graha sits in each saved chart, a pattern to spot, and a quick self-check. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChartLensSheet(
    graha: Graha,
    people: List<ChartPerson>,
    course: Course,
    progress: LearnProgress,
    language: Language,
    onAddPerson: () -> Unit,
    onDismiss: () -> Unit,
) {
    val layers = remember(course, progress) { ChartLens.unlocked(course, progress) }
    val readings = remember(people, graha, layers) { people.map { ChartLens.read(it, graha, layers) } }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val shown = readings.indexOfFirst { it.person.id == detail }
            if (shown >= 0) {
                PersonReading(readings[shown], index = shown, layers = layers, onBack = { detail = null })
            } else {
                Overview(graha, readings, layers, course, language, onOpen = { detail = it.id }, onAddPerson = onAddPerson)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Overview(
    graha: Graha,
    readings: List<GrahaReading>,
    layers: Set<LensLayer>,
    course: Course,
    language: Language,
    onOpen: (ChartPerson) -> Unit,
    onAddPerson: () -> Unit,
) {
    val grahaLabel = grahaLabel(graha)
    val subject = remember(readings) { ChartLens.checkSubject(readings) }
    var revealed by rememberSaveable(graha) { mutableStateOf(false) }
    Text(
        stringResource(R.string.lens_title, grahaLabel),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
    readings.forEachIndexed { index, reading ->
        PersonRow(reading, index, layers, hidden = reading == subject && !revealed, onClick = { onOpen(reading.person) })
    }
    // What opens later, so the learner knows the sheet grows as they learn.
    LensLayer.entries.filterNot { it in layers }.forEach { layer ->
        val chapter = course.chapter(layer.chapterId)?.title?.get(language) ?: return@forEach
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(Spacing.sm))
            Text(
                stringResource(R.string.lens_locked, layerLabel(layer), chapter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    ChartLens.sharedElement(readings)?.let { shared ->
        Surface(color = AstroLingoTheme.tabColors.learn, contentColor = AstroLingoTheme.tabColors.onPastel, shape = RoundedCornerShape(12.dp)) {
            Text(
                stringResource(R.string.lens_pattern, shared.count, shared.total, grahaLabel, elementLabel(shared.element)),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
    if (subject != null) {
        CheckYourself(subject, grahaLabel, revealed, onReveal = { revealed = true })
    }
    val others = readings.count { !it.person.isYou }
    if (others < SavedPerson.MAX_OTHERS) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(pluralStringResource(R.plurals.lens_add_more, SavedPerson.MAX_OTHERS - others, SavedPerson.MAX_OTHERS - others), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.lens_add_more_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onAddPerson) { Text(stringResource(R.string.lens_add)) }
            }
        }
    }
    NotAPrediction()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonRow(reading: GrahaReading, index: Int, layers: Set<LensLayer>, hidden: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = Spacing.touchTarget)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PersonAvatar(reading.person.name, index)
        Spacer(Modifier.size(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(reading.person.name, style = MaterialTheme.typography.titleMedium)
            if (hidden) {
                Text(
                    stringResource(R.string.lens_check_hidden),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    facts(reading, layers).forEach { Chip(it) }
                }
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The short facts on a row: sign, house, nakshatra and strength, as far as they are unlocked. */
@Composable
private fun facts(reading: GrahaReading, layers: Set<LensLayer>): List<String> = listOfNotNull(
    reading.rashi?.let { rashiLabel(it) },
    when {
        reading.house != null -> stringResource(R.string.lens_house, reading.house)
        LensLayer.HOUSE in layers && !reading.person.timeKnown -> stringResource(R.string.lens_time_unknown)
        else -> null
    },
    reading.nakshatra?.nakshatra?.displayName(),
    reading.dignity?.let { dignityLabel(it) },
)

@Composable
private fun Chip(text: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp))
    }
}

/** Find the graha in a chart, then check. Not scored: it is practice in reading a chart. */
@Composable
private fun CheckYourself(subject: GrahaReading, grahaLabel: String, revealed: Boolean, onReveal: () -> Unit) {
    val person = subject.person
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(stringResource(R.string.lens_check_title), style = MaterialTheme.typography.titleMedium)
            Text(
                if (person.isYou) {
                    stringResource(R.string.lens_check_question_you, grahaLabel)
                } else {
                    stringResource(R.string.lens_check_question, grahaLabel, person.name)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            KundliChart(
                kundli = if (person.timeKnown) person.kundli else person.kundli.fromMoon(),
                style = ChartStyle.NORTH_INDIAN,
                description = stringResource(R.string.lens_chart_description, person.name, grahaLabel),
                modifier = Modifier
                    .widthIn(max = CHECK_CHART_WIDTH)
                    .align(Alignment.CenterHorizontally),
                markLagna = person.timeKnown,
                highlight = subject.graha.takeIf { revealed },
            )
            if (revealed) {
                Text(
                    listOfNotNull(subject.rashi?.let { rashiLabel(it) }, subject.house?.let { stringResource(R.string.lens_house, it) }).joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Button(onClick = onReveal) { Text(stringResource(R.string.lens_check_show)) }
            }
            Text(
                stringResource(R.string.lens_check_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One person's chart with the graha marked, and what each unlocked detail means. */
@Composable
private fun ColumnScope.PersonReading(reading: GrahaReading, index: Int, layers: Set<LensLayer>, onBack: () -> Unit) {
    val person = reading.person
    val grahaLabel = grahaLabel(reading.graha)
    TextButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(Spacing.xs))
        Text(stringResource(R.string.lens_back))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        PersonAvatar(person.name, index)
        Spacer(Modifier.size(Spacing.md))
        Text(person.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
    KundliChart(
        kundli = if (person.timeKnown) person.kundli else person.kundli.fromMoon(),
        style = ChartStyle.NORTH_INDIAN,
        description = stringResource(R.string.lens_chart_description, person.name, grahaLabel),
        modifier = Modifier
            .widthIn(max = DETAIL_CHART_WIDTH)
            .align(Alignment.CenterHorizontally),
        markLagna = person.timeKnown,
        highlight = reading.graha,
    )
    HorizontalDivider()
    meaningLines(reading, layers).forEach { line ->
        Row {
            Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(Spacing.sm))
            Text(line, style = MaterialTheme.typography.bodyLarge)
        }
    }
    NotAPrediction()
}

/** Short sentences from the course's own teaching: what the graha stands for, then each unlocked detail. */
@Composable
private fun meaningLines(reading: GrahaReading, layers: Set<LensLayer>): List<String> {
    val keywords = stringArrayResource(R.array.lens_graha_keywords)
    val themes = stringArrayResource(R.array.lens_house_themes)
    return listOfNotNull(
        stringResource(R.string.lens_line_graha, grahaLabel(reading.graha), keywords[reading.graha.ordinal]),
        reading.rashi?.let { stringResource(R.string.lens_line_sign, rashiLabel(it), elementLabel(it.element)) },
        when {
            reading.house != null -> stringResource(R.string.lens_line_house, reading.house, themes[reading.house - 1])
            LensLayer.HOUSE in layers && !reading.person.timeKnown -> stringResource(R.string.lens_line_house_unknown)
            else -> null
        },
        reading.nakshatra?.let { stringResource(R.string.lens_line_nakshatra, it.nakshatra.displayName(), grahaLabel(it.nakshatra.lord)) },
        reading.dignity?.let { stringResource(dignityLineRes(it)) },
    )
}

@Composable
private fun NotAPrediction() {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(Spacing.sm))
        Text(
            stringResource(R.string.lens_not_prediction),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun grahaLabel(graha: Graha): String = stringResource(R.string.rashi_with_english, graha.displayName(), graha.englishName)

@Composable
private fun rashiLabel(rashi: Rashi): String = stringResource(R.string.rashi_with_english, rashi.displayName(), rashi.englishName)

@Composable
private fun layerLabel(layer: LensLayer): String = stringResource(
    when (layer) {
        LensLayer.SIGN -> R.string.lens_layer_sign
        LensLayer.HOUSE -> R.string.lens_layer_house
        LensLayer.NAKSHATRA -> R.string.lens_layer_nakshatra
        LensLayer.STRENGTH -> R.string.lens_layer_strength
    },
)

@Composable
private fun elementLabel(element: Element): String = stringResource(
    when (element) {
        Element.FIRE -> R.string.lens_element_fire
        Element.EARTH -> R.string.lens_element_earth
        Element.AIR -> R.string.lens_element_air
        Element.WATER -> R.string.lens_element_water
    },
)

@Composable
private fun dignityLabel(dignity: Dignity): String = stringResource(
    when (dignity) {
        Dignity.EXALTED -> R.string.lens_dignity_exalted
        Dignity.OWN_SIGN -> R.string.lens_dignity_own
        Dignity.FRIEND -> R.string.lens_dignity_friend
        Dignity.NEUTRAL -> R.string.lens_dignity_neutral
        Dignity.ENEMY -> R.string.lens_dignity_enemy
        Dignity.DEBILITATED -> R.string.lens_dignity_debilitated
    },
)

private fun dignityLineRes(dignity: Dignity): Int = when (dignity) {
    Dignity.EXALTED -> R.string.lens_dignity_exalted_line
    Dignity.OWN_SIGN -> R.string.lens_dignity_own_line
    Dignity.FRIEND -> R.string.lens_dignity_friend_line
    Dignity.NEUTRAL -> R.string.lens_dignity_neutral_line
    Dignity.ENEMY -> R.string.lens_dignity_enemy_line
    Dignity.DEBILITATED -> R.string.lens_dignity_debilitated_line
}

private const val PREFS = "astrolingo_settings"
private const val KEY_LEFT = "bubble_left"
private val BUBBLE_SIZE = 56.dp
// Keeps the bubble clear of the card's Next button.
private val BOTTOM_CLEARANCE = 96.dp
private val FLIP_DISTANCE = 120.dp
private val MAX_LIFT = 480.dp
private val CHECK_CHART_WIDTH = 220.dp
private val DETAIL_CHART_WIDTH = 300.dp
