// App shell: onboarding until a profile is saved, then the four tabs (Today, Learn, Kundli, Me).
// Each feature replaces its placeholder in the sprint named on screen.
package com.astrolingo.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.learn.BundledCourse
import com.astrolingo.app.learn.ChartPerson
import com.astrolingo.app.learn.InMemoryLearnStore
import com.astrolingo.app.learn.LearnProgressStore
import com.astrolingo.app.learn.LearnRequest
import com.astrolingo.app.learn.LearnTab
import com.astrolingo.app.notes.NoteOffer
import com.astrolingo.app.notes.NoteSettingsCard
import com.astrolingo.app.notes.rememberNotePermission
import com.astrolingo.app.onboarding.BirthDetails
import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.onboarding.Level
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.OnboardingFlow
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.onboarding.ResolvedBirth
import com.astrolingo.app.people.KundliPicker
import com.astrolingo.app.people.PersonForm
import com.astrolingo.app.people.SavedPeopleCard
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.ProfileRepository
import com.astrolingo.app.profile.SavedPerson
import com.astrolingo.app.settings.AppLanguage
import com.astrolingo.app.today.DayPerson
import com.astrolingo.app.today.TodayScreen
import com.astrolingo.app.ui.components.InfoCard
import com.astrolingo.app.ui.components.StarField
import com.astrolingo.app.ui.kundli.KundliScreen
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.app.ui.theme.ThemeMode
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.CityIndex
import com.astrolingo.learn.review.ReviewQueue
import com.astrolingo.notify.InMemoryNoteSettings
import com.astrolingo.notify.NoteSettings
import java.io.ByteArrayInputStream
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The four top-level destinations, in bottom-bar order. */
enum class Tab(@StringRes val label: Int, @StringRes val placeholder: Int, val icon: ImageVector) {
    TODAY(R.string.tab_today, R.string.placeholder_today, Icons.Filled.Home),
    LEARN(R.string.tab_learn, R.string.placeholder_learn, Icons.AutoMirrored.Filled.List),
    KUNDLI(R.string.tab_kundli, R.string.placeholder_kundli, Icons.Filled.Star),
    ME(R.string.tab_me, R.string.placeholder_me, Icons.Filled.Person),
}

/**
 * The tab the app opens on once a profile is saved: learners go straight to Learn's next lesson, guidance users to
 * the daily card (learning review, gap 3). Day one is different: learners first see their kundli revealed.
 */
internal fun startTab(purpose: Purpose): Tab = if (purpose == Purpose.LEARN) Tab.LEARN else Tab.TODAY

/** What the app knows about the user's profile. */
private sealed interface ProfileState {
    data object Loading : ProfileState
    data object Missing : ProfileState
    /** @property reveal true right after onboarding, until the Kundli tab has played its reveal. */
    data class Ready(
        val answers: OnboardingAnswers,
        val birth: ResolvedBirth,
        val kundli: Kundli,
        val reveal: Boolean = false,
    ) : ProfileState
}

/** Resolves saved answers to a chart. Loads the city list only for the rare old Bombay/Calcutta clock case. */
private fun ready(answers: OnboardingAnswers): ProfileState.Ready {
    val index = if (answers.birth.useCityClock) CityIndex.bundled() else CityIndex.load(ByteArrayInputStream(ByteArray(0)))
    val birth = BirthDetailsResolver.resolve(answers.birth, index)
    return ProfileState.Ready(answers, birth, KundliCalculator.calculate(birth.moment))
}

/** A saved person's chart, or null if it can't be worked out. Calculated on the phone, like the user's own. */
private fun chartOf(person: SavedPerson): ChartPerson? = runCatching {
    val index = if (person.birth.useCityClock) CityIndex.bundled() else CityIndex.load(ByteArrayInputStream(ByteArray(0)))
    val birth = BirthDetailsResolver.resolve(person.birth, index)
    ChartPerson(person.id, person.name, KundliCalculator.calculate(birth.moment), birth.timeKnown, isYou = false)
}.getOrNull()

private const val YOU_ID = "you"

/**
 * Root composable: onboarding until a profile exists, then bottom navigation plus the selected tab.
 * The profile is read from and saved to [profiles] off the main thread.
 *
 * @param learnStore Learn progress and bookmarks; wiped with the profile by "Delete my details".
 * @param themeMode the light or dark choice shown in Me; [onThemeMode] is called when the user changes it.
 * @param language the app's language, also changeable in Me through [onLanguage].
 * @param notes the daily note's settings, offered on Today and changed in Me.
 * @param resumed goes up each time the app comes back to the front, so the note settings are read again (the user
 *   may have allowed notifications in the phone's settings meanwhile).
 */
@Composable
fun AstroLingoApp(
    profiles: ProfileRepository,
    learnStore: LearnProgressStore = remember { InMemoryLearnStore() },
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeMode: (ThemeMode) -> Unit = {},
    language: AppLanguage = AppLanguage.ENGLISH,
    onLanguage: (AppLanguage) -> Unit = {},
    notes: NoteSettings = remember { InMemoryNoteSettings() },
    resumed: Int = 0,
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }
    // People saved to learn from (LS-8), oldest first.
    var people by remember { mutableStateOf<List<SavedPerson>>(emptyList()) }
    var selected by rememberSaveable { mutableStateOf(Tab.TODAY) }
    // True once the start tab was picked, so turning the phone keeps the tab the user is on.
    var startPicked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(profiles) {
        val loaded = withContext(Dispatchers.IO) {
            runCatching { profiles.load()?.let(::ready) }.getOrNull() ?: ProfileState.Missing
        }
        people = withContext(Dispatchers.IO) { runCatching { profiles.loadPeople() }.getOrDefault(emptyList()) }
        if (!startPicked && loaded is ProfileState.Ready) selected = startTab(loaded.answers.purpose)
        startPicked = true
        profile = loaded
    }
    when (val current = profile) {
        ProfileState.Loading -> Box(Modifier.fillMaxSize())
        ProfileState.Missing -> OnboardingFlow(
            onFinished = { answers, resolved ->
                profile = ProfileState.Ready(answers, resolved, KundliCalculator.calculate(resolved.moment), reveal = true)
                // Guidance users came for the card; learners see their kundli revealed first. The reveal still plays
                // the first time a guidance user opens Kundli.
                selected = if (answers.purpose == Purpose.LEARN) Tab.KUNDLI else Tab.TODAY
                scope.launch(Dispatchers.IO) {
                    // If saving fails the chart still shows; onboarding is asked again on next launch.
                    runCatching { profiles.save(answers) }
                }
            },
            modifier = Modifier.safeDrawingPadding(),
        )
        is ProfileState.Ready -> Tabs(
            profile = current,
            learnStore = learnStore,
            selected = selected,
            onSelect = { selected = it },
            onRevealed = { profile = current.copy(reveal = false) },
            themeMode = themeMode,
            onThemeMode = onThemeMode,
            language = language,
            onLanguage = onLanguage,
            notes = notes,
            resumed = resumed,
            onAnswersChange = { answers ->
                profile = current.copy(answers = answers)
                scope.launch(Dispatchers.IO) { runCatching { profiles.save(answers) } }
            },
            people = people,
            onSavePerson = { person ->
                people = (people.filterNot { it.id == person.id } + person).sortedBy { it.createdAt }
                scope.launch(Dispatchers.IO) { runCatching { profiles.savePerson(person) } }
            },
            onBirthChange = { birth ->
                val answers = current.answers.copy(birth = birth)
                scope.launch {
                    // A new chart for the new details; Learn progress and saved people stay as they are.
                    profile = withContext(Dispatchers.Default) { ready(answers) }
                    withContext(Dispatchers.IO) { runCatching { profiles.save(answers) } }
                }
            },
            onDeletePerson = { person ->
                people = people.filterNot { it.id == person.id }
                scope.launch(Dispatchers.IO) { runCatching { profiles.deletePerson(person.id) } }
            },
            onDelete = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { profiles.deleteAll() }
                        runCatching { learnStore.deleteAll() }
                    }
                    selected = Tab.TODAY
                    people = emptyList()
                    profile = ProfileState.Missing
                }
            },
        )
    }
}

@Composable
private fun Tabs(
    profile: ProfileState.Ready,
    learnStore: LearnProgressStore,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onRevealed: () -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    onAnswersChange: (OnboardingAnswers) -> Unit,
    onDelete: () -> Unit,
    notes: NoteSettings = InMemoryNoteSettings(),
    resumed: Int = 0,
    people: List<SavedPerson> = emptyList(),
    onSavePerson: (SavedPerson) -> Unit = {},
    onDeletePerson: (SavedPerson) -> Unit = {},
    onBirthChange: (BirthDetails) -> Unit = {},
) {
    // The person being added ("new") or edited (their id, or "me" for the user) in Me, or null.
    var personForm by rememberSaveable { mutableStateOf<String?>(null) }
    // Whose kundli the Kundli tab shows: the user's, or a saved person's id.
    var kundliOf by rememberSaveable { mutableStateOf(YOU_ID) }
    // Charts for the bubble on Learn cards (LS-9): the user's at once, saved people's once calculated.
    val youName = stringResource(R.string.people_you)
    val chartPeople by produceState(emptyList<ChartPerson>(), profile.kundli, profile.birth.timeKnown, people, youName) {
        val you = ChartPerson(YOU_ID, youName, profile.kundli, profile.birth.timeKnown, isYou = true)
        value = listOf(you)
        value = listOf(you) + withContext(Dispatchers.Default) { people.mapNotNull(::chartOf) }
    }
    // Whose day Today shows (LS-10): a saved person's id, or null for the user's own. Not stored with the profile.
    var todayPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    val dayPeople by produceState(emptyList<DayPerson>(), people) {
        value = withContext(Dispatchers.Default) { people.mapNotNull(DayPerson::of) }
    }
    // The daily note's settings, read again on each tab change and each return to the app.
    var noteState by remember(notes) { mutableStateOf(notes.state()) }
    LaunchedEffect(notes, selected, resumed) { noteState = notes.state() }
    val refreshNotes = { noteState = notes.state() }
    val acceptOffer = rememberNotePermission(notes) { allowed ->
        if (allowed) notes.turnOn() else notes.declineOffer()
        refreshNotes()
    }
    // From Me the note is turned on even if Android said no, so Me can show the way to the phone's settings.
    val turnOnInMe = rememberNotePermission(notes) {
        notes.turnOn()
        refreshNotes()
    }
    // True while Learn shows a lesson, the check or saved cards: the tabs then step aside so nothing distracts.
    var immersive by remember { mutableStateOf(false) }
    // A Review asked for from Today, handed to Learn once it opens.
    var learnRequest by remember { mutableStateOf<LearnRequest?>(null) }
    // Due Review cards, for the small Review card on Today. Read again each time the tab changes.
    val reviewDue by produceState(0, selected) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val progress = learnStore.load()
                if (progress.review.isEmpty()) 0 else ReviewQueue.dueToday(progress, BundledCourse.course, LocalDate.now().toEpochDay()).size
            }.getOrDefault(0)
        }
    }
    Scaffold(
        bottomBar = {
            if (!(immersive && selected == Tab.LEARN)) NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { onSelect(tab) },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        // A quiet twinkling sky behind the daily card and the learning path.
        if (selected == Tab.TODAY || (selected == Tab.LEARN && !immersive)) StarField(Modifier.fillMaxSize().padding(padding))
        when (selected) {
            Tab.KUNDLI -> {
                // The user's own kundli unless they picked a saved person; a deleted person falls back to the user.
                val other = chartPeople.firstOrNull { it.id == kundliOf && !it.isYou }
                val picker: @Composable () -> Unit = {
                    // Day one's reveal is about the user's own chart alone.
                    if (!profile.reveal) {
                        KundliPicker(
                            people = chartPeople,
                            selectedId = other?.id ?: YOU_ID,
                            onSelect = { kundliOf = it.id },
                            onAdd = {
                                personForm = NEW_PERSON
                                onSelect(Tab.ME)
                            },
                        )
                    }
                }
                if (other == null) {
                    KundliScreen(
                        kundli = profile.kundli,
                        title = stringResource(R.string.your_kundli_title),
                        note = when {
                            profile.birth.timeKnown -> null
                            profile.birth.moonUncertain ->
                                stringResource(R.string.your_kundli_time_unknown) + " " + stringResource(R.string.your_kundli_moon_uncertain)
                            else -> stringResource(R.string.your_kundli_time_unknown)
                        },
                        modifier = Modifier.padding(padding),
                        timeKnown = profile.birth.timeKnown,
                        reveal = profile.reveal,
                        onRevealed = onRevealed,
                        top = picker,
                    )
                } else {
                    KundliScreen(
                        kundli = other.kundli,
                        title = stringResource(R.string.kundli_of, other.name),
                        note = if (other.timeKnown) null else stringResource(R.string.kundli_other_time_unknown, other.name),
                        modifier = Modifier.padding(padding),
                        timeKnown = other.timeKnown,
                        top = picker,
                    )
                }
            }
            Tab.TODAY -> TodayScreen(
                kundli = profile.kundli,
                place = profile.answers.todayPlace,
                showDeity = profile.answers.showDeity,
                placeGiven = profile.answers.currentPlace != null,
                modifier = Modifier.padding(padding),
                // "Use my birthplace" is an answer too, so it is stored as the birthplace rather than cleared.
                onPlaceChange = { place -> onAnswersChange(profile.answers.copy(currentPlace = place ?: profile.answers.birth.city)) },
                reviewDue = reviewDue,
                onReview = {
                    learnRequest = LearnRequest.Review
                    onSelect(Tab.LEARN)
                },
                onLearn = { topic ->
                    learnRequest = LearnRequest.Lesson(topic.lessonId)
                    onSelect(Tab.LEARN)
                },
                people = dayPeople,
                // Someone deleted in Me drops out of the list, which takes Today back to the user's own day.
                viewing = dayPeople.firstOrNull { it.id == todayPersonId },
                onView = { todayPersonId = it?.id },
                offer = if (noteState.ask) {
                    {
                        NoteOffer(
                            noteState,
                            showDeity = profile.answers.showDeity,
                            onYes = acceptOffer,
                            onNotNow = {
                                notes.declineOffer()
                                refreshNotes()
                            },
                        )
                    }
                } else null,
                onAddPerson = if (people.size < SavedPerson.MAX_OTHERS) {
                    {
                        personForm = NEW_PERSON
                        onSelect(Tab.ME)
                    }
                } else null,
            )
            Tab.LEARN -> LearnTab(
                kundli = profile.kundli,
                timeKnown = profile.birth.timeKnown,
                store = learnStore,
                modifier = Modifier.padding(padding),
                knowsSome = profile.answers.level != Level.NEW,
                goalMinutes = profile.answers.goal?.minutes,
                request = learnRequest,
                onRequestHandled = { learnRequest = null },
                onReturn = { onSelect(Tab.TODAY) },
                onImmersive = { immersive = it },
                people = chartPeople,
                onAddPerson = {
                    personForm = NEW_PERSON
                    onSelect(Tab.ME)
                },
            )
            Tab.ME -> if (personForm == EDIT_ME) {
                PersonForm(
                    editing = SavedPerson(YOU_ID, youName, profile.answers.birth, 0),
                    self = true,
                    onSave = { me ->
                        onBirthChange(keepDetails(profile.answers.birth, me.birth))
                        personForm = null
                    },
                    onCancel = { personForm = null },
                    modifier = Modifier.padding(padding),
                )
            } else if (personForm != null) {
                PersonForm(
                    editing = people.firstOrNull { it.id == personForm },
                    onSave = { person ->
                        onSavePerson(person)
                        personForm = null
                    },
                    onCancel = { personForm = null },
                    modifier = Modifier.padding(padding),
                )
            } else MeScreen(
                showDeity = profile.answers.showDeity,
                onShowDeity = { show -> onAnswersChange(profile.answers.copy(showDeity = show)) },
                themeMode = themeMode,
                onThemeMode = onThemeMode,
                language = language,
                onLanguage = onLanguage,
                onDelete = onDelete,
                modifier = Modifier.padding(padding),
                savedPeople = {
                    SavedPeopleCard(
                        you = profile.answers.birth,
                        people = people,
                        onAdd = { personForm = NEW_PERSON },
                        onEdit = { personForm = it.id },
                        onDelete = onDeletePerson,
                        onEditYou = { personForm = EDIT_ME },
                    )
                },
                hasPeople = people.isNotEmpty(),
                notes = {
                    NoteSettingsCard(
                        noteState,
                        onTurn = { on ->
                            if (on) turnOnInMe() else notes.turnOff()
                            refreshNotes()
                        },
                        onTime = { time ->
                            notes.setTime(time)
                            refreshNotes()
                        },
                    )
                },
            )
            else -> PlaceholderScreen(tab = selected, modifier = Modifier.padding(padding))
        }
    }
}

/** Me tab for now: privacy, settings and a way to delete the birth details. The full profile screen comes in sprint 5. */
@OptIn(ExperimentalMaterial3Api::class) // segmented buttons; stable in recent Material 3, harmless otherwise
@Composable
private fun MeScreen(
    showDeity: Boolean,
    onShowDeity: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    savedPeople: @Composable () -> Unit = {},
    hasPeople: Boolean = false,
    notes: @Composable () -> Unit = {},
) {
    var confirming by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(stringResource(R.string.tab_me), style = MaterialTheme.typography.headlineMedium)
        savedPeople()
        notes()
        InfoCard(title = stringResource(R.string.me_block_privacy), icon = Icons.Filled.Lock) {
            Text(stringResource(R.string.me_privacy), style = MaterialTheme.typography.bodyMedium)
        }
        InfoCard(title = stringResource(R.string.me_block_settings), icon = Icons.Filled.Settings) {
            Text(stringResource(R.string.me_language), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AppLanguage.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = language == option,
                        onClick = { onLanguage(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppLanguage.entries.size),
                    ) {
                        // Each language is named in itself, so it can be found whatever the app is showing.
                        Text(stringResource(if (option == AppLanguage.HINDI) R.string.language_hindi else R.string.language_english))
                    }
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.me_appearance), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { onThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) {
                        Text(stringResource(mode.label))
                    }
                }
            }
            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = showDeity, role = Role.Switch, onValueChange = onShowDeity)
                    .heightIn(min = Spacing.touchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.me_show_deity), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = showDeity, onCheckedChange = null)
            }
        }
        OutlinedButton(onClick = { confirming = true }) { Text(stringResource(R.string.me_delete)) }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(stringResource(R.string.me_delete_title)) },
            text = {
                val body = stringResource(R.string.me_delete_body)
                Text(if (hasPeople) body + " " + stringResource(R.string.people_delete_too) else body)
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text(stringResource(R.string.me_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text(stringResource(R.string.me_delete_cancel)) }
            },
        )
    }
}

/** [Tabs]' person form value for a new person rather than an edit. */
private const val NEW_PERSON = "new"
private const val EDIT_ME = "me"

/**
 * The user's edited birth details, keeping onboarding's extra answers where they still apply: the part of the day for
 * an unknown time on the same date, and the old city clock for the same moment and place.
 */
internal fun keepDetails(before: BirthDetails, after: BirthDetails): BirthDetails = after.copy(
    dayPart = before.dayPart.takeIf { after.time == null && after.date == before.date },
    useCityClock = before.useCityClock && after.date == before.date && after.time == before.time && after.city.id == before.city.id,
)

private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.me_appearance_system
        ThemeMode.LIGHT -> R.string.me_appearance_light
        ThemeMode.DARK -> R.string.me_appearance_dark
    }

/** Temporary screen for a tab, tinted with that tab's pastel colour. */
@Composable
private fun PlaceholderScreen(tab: Tab, modifier: Modifier = Modifier) {
    val colors = AstroLingoTheme.tabColors
    val tint: Color = when (tab) {
        Tab.TODAY -> colors.today
        Tab.LEARN -> colors.learn
        Tab.KUNDLI -> colors.kundli
        Tab.ME -> colors.me
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .padding(Spacing.lg)
                .fillMaxWidth()
                .background(tint, MaterialTheme.shapes.medium)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (tab == Tab.LEARN) SitaraAvatar(size = 96.dp)
            Text(
                text = stringResource(tab.label),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onPastel,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPastel,
            )
            Text(
                text = stringResource(tab.placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onPastel,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AstroLingoAppPreview() {
    AstroLingoTheme { AstroLingoApp(InMemoryProfileRepository()) }
}
