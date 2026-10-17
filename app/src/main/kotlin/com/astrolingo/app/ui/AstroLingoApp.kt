// App shell: onboarding until a profile is saved, then the four tabs (Today, Learn, Kundli, Me).
// Each feature replaces its placeholder in the sprint named on screen.
package com.astrolingo.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.onboarding.OnboardingFlow
import com.astrolingo.app.onboarding.BirthDetailsResolver
import com.astrolingo.app.onboarding.OnboardingAnswers
import com.astrolingo.app.onboarding.ResolvedBirth
import com.astrolingo.app.profile.InMemoryProfileRepository
import com.astrolingo.app.profile.ProfileRepository
import com.astrolingo.app.ui.kundli.KundliScreen
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
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
    class Ready(val birth: ResolvedBirth, val kundli: Kundli, val reveal: Boolean = false) : ProfileState
}

/** Resolves saved answers to a chart. Loads the city list only for the rare old Bombay/Calcutta clock case. */
private fun ready(answers: OnboardingAnswers): ProfileState.Ready {
    val index = if (answers.birth.useCityClock) CityIndex.bundled() else CityIndex.load(ByteArrayInputStream(ByteArray(0)))
    val birth = BirthDetailsResolver.resolve(answers.birth, index)
    return ProfileState.Ready(birth, KundliCalculator.calculate(birth.moment))
}

/**
 * Root composable: onboarding until a profile exists, then bottom navigation plus the selected tab.
 * The profile is read from and saved to [profiles] off the main thread.
 */
@Composable
fun AstroLingoApp(profiles: ProfileRepository) {
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
                profile = ProfileState.Ready(resolved, KundliCalculator.calculate(resolved.moment), reveal = true)
                selected = Tab.KUNDLI
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
            onRevealed = { profile = ProfileState.Ready(current.birth, current.kundli) },
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
            Tab.ME -> MeScreen(onDelete = onDelete, modifier = Modifier.padding(padding))
            else -> PlaceholderScreen(tab = selected, modifier = Modifier.padding(padding))
        }
    }
}

/** Me tab for now: what is stored and a way to delete it. The full profile screen comes in sprint 5. */
@Composable
private fun MeScreen(onDelete: () -> Unit, modifier: Modifier = Modifier) {
    var confirming by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(stringResource(R.string.tab_me), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.me_privacy), style = MaterialTheme.typography.bodyLarge)
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
