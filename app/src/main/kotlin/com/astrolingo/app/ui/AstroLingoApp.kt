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
import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.OnboardingFlow
import com.astrolingo.app.onboarding.Purpose
import com.astrolingo.app.onboarding.ResolvedBirth
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.ProfileRepository
import com.astrolingo.app.today.TodayScreen
import com.astrolingo.app.ui.components.InfoCard
import com.astrolingo.app.ui.kundli.KundliScreen
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.app.ui.theme.ThemeMode
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.KundliCalculator
import com.astrolingo.engine.places.CityIndex
import java.io.ByteArrayInputStream
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

/**
 * Root composable: onboarding until a profile exists, then bottom navigation plus the selected tab.
 * The profile is read from and saved to [profiles] off the main thread.
 *
 * @param themeMode the light or dark choice shown in Me; [onThemeMode] is called when the user changes it.
 */
@Composable
fun AstroLingoApp(
    profiles: ProfileRepository,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeMode: (ThemeMode) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ProfileState>(ProfileState.Loading) }
    var selected by rememberSaveable { mutableStateOf(Tab.TODAY) }
    LaunchedEffect(profiles) {
        profile = withContext(Dispatchers.IO) {
            runCatching { profiles.load()?.let(::ready) }.getOrNull() ?: ProfileState.Missing
        }
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
            selected = selected,
            onSelect = { selected = it },
            onRevealed = { profile = current.copy(reveal = false) },
            themeMode = themeMode,
            onThemeMode = onThemeMode,
            onAnswersChange = { answers ->
                profile = current.copy(answers = answers)
                scope.launch(Dispatchers.IO) { runCatching { profiles.save(answers) } }
            },
            onDelete = {
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { profiles.deleteAll() } }
                    selected = Tab.TODAY
                    profile = ProfileState.Missing
                }
            },
        )
    }
}

@Composable
private fun Tabs(
    profile: ProfileState.Ready,
    selected: Tab,
    onSelect: (Tab) -> Unit,
    onRevealed: () -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    onAnswersChange: (OnboardingAnswers) -> Unit,
    onDelete: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
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
        when (selected) {
            Tab.KUNDLI -> KundliScreen(
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
            )
            Tab.TODAY -> TodayScreen(
                kundli = profile.kundli,
                place = profile.answers.todayPlace,
                showDeity = profile.answers.showDeity,
                placeGiven = profile.answers.currentPlace != null,
                modifier = Modifier.padding(padding),
                // "Use my birthplace" is an answer too, so it is stored as the birthplace rather than cleared.
                onPlaceChange = { place -> onAnswersChange(profile.answers.copy(currentPlace = place ?: profile.answers.birth.city)) },
            )
            Tab.ME -> MeScreen(
                showDeity = profile.answers.showDeity,
                onShowDeity = { show -> onAnswersChange(profile.answers.copy(showDeity = show)) },
                themeMode = themeMode,
                onThemeMode = onThemeMode,
                onDelete = onDelete,
                modifier = Modifier.padding(padding),
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
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
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
        InfoCard(title = stringResource(R.string.me_block_privacy), icon = Icons.Filled.Lock) {
            Text(stringResource(R.string.me_privacy), style = MaterialTheme.typography.bodyMedium)
        }
        InfoCard(title = stringResource(R.string.me_block_settings), icon = Icons.Filled.Settings) {
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
            text = { Text(stringResource(R.string.me_delete_body)) },
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
