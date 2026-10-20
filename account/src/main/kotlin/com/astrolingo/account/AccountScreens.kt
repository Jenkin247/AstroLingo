// The sign-in offer shown once after onboarding, the updates question after signing in, and the account block for Me.
package com.astrolingo.account

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val Gap = 16.dp
private val SmallGap = 8.dp
private val TouchTarget = 48.dp

/**
 * Offers Google sign-in once, right after onboarding. Everything in the app works without it; signing in adds backup
 * and Sitara. "Maybe later" is always there.
 *
 * @param busy true while Google's sheet or Firebase is working; the buttons wait.
 * @param message a line about the last attempt, from [SignInResult.message].
 * @param sitara draws a line spoken by Sitara, in the app's style.
 */
@Composable
fun SignInScreen(
    busy: Boolean,
    @StringRes message: Int?,
    onContinue: () -> Unit,
    onLater: () -> Unit,
    sitara: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gap),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            sitara(stringResource(R.string.account_sign_in_sitara))
            Text(stringResource(R.string.account_sign_in_title), style = MaterialTheme.typography.headlineMedium)
            Benefit(stringResource(R.string.account_benefit_backup))
            Benefit(stringResource(R.string.account_benefit_sitara))
            Benefit(stringResource(R.string.account_benefit_free))
            Spacer(Modifier.heightIn(min = SmallGap))
            if (message != null) {
                Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            ContinueWithGoogle(busy = busy, onClick = onContinue)
            TextButton(
                onClick = onLater,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = TouchTarget),
            ) { Text(stringResource(R.string.account_maybe_later)) }
            Text(
                stringResource(R.string.account_sign_in_privacy),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = SmallGap))
    }
}

@Composable
private fun ContinueWithGoogle(busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = !busy, modifier = modifier.fillMaxWidth().heightIn(min = TouchTarget)) {
        if (busy) {
            CircularProgressIndicator(Modifier.padding(end = SmallGap).heightIn(max = 20.dp), strokeWidth = 2.dp)
        }
        Text(stringResource(R.string.account_continue_google))
    }
}

/**
 * Asks, once after signing in, which updates the user would like. Every switch starts off; "Not now" saves nothing on.
 */
@Composable
fun UpdatesScreen(
    initial: UpdatesConsent,
    onSave: (UpdatesConsent) -> Unit,
    onNotNow: () -> Unit,
    sitara: @Composable (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lessons by rememberSaveable { mutableStateOf(initial.lessons) }
    var insights by rememberSaveable { mutableStateOf(initial.insights) }
    var offers by rememberSaveable { mutableStateOf(initial.offers) }
    var newsletter by rememberSaveable { mutableStateOf(initial.newsletter) }
    val chosen = UpdatesConsent(lessons, insights, offers, newsletter)
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Gap),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            sitara(stringResource(R.string.account_updates_sitara))
            Text(stringResource(R.string.account_updates_title), style = MaterialTheme.typography.headlineMedium)
            UpdatesSwitches(consent = chosen, onChange = {
                lessons = it.lessons
                insights = it.insights
                offers = it.offers
                newsletter = it.newsletter
            })
            Spacer(Modifier.heightIn(min = SmallGap))
            Button(onClick = { onSave(chosen) }, modifier = Modifier.fillMaxWidth().heightIn(min = TouchTarget)) {
                Text(stringResource(R.string.account_updates_save))
            }
            TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth().heightIn(min = TouchTarget)) {
                Text(stringResource(R.string.account_updates_not_now))
            }
            Text(
                stringResource(R.string.account_updates_note),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The four updates as switches, each with a line on what it sends. Used after sign-in and in Me. */
@Composable
fun UpdatesSwitches(consent: UpdatesConsent, onChange: (UpdatesConsent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SmallGap)) {
        UpdateSwitch(R.string.account_updates_lessons, R.string.account_updates_lessons_detail, consent.lessons) {
            onChange(consent.copy(lessons = it))
        }
        UpdateSwitch(R.string.account_updates_insights, R.string.account_updates_insights_detail, consent.insights) {
            onChange(consent.copy(insights = it))
        }
        UpdateSwitch(R.string.account_updates_offers, R.string.account_updates_offers_detail, consent.offers) {
            onChange(consent.copy(offers = it))
        }
        UpdateSwitch(R.string.account_updates_newsletter, R.string.account_updates_newsletter_detail, consent.newsletter) {
            onChange(consent.copy(newsletter = it))
        }
    }
}

@Composable
private fun UpdateSwitch(@StringRes title: Int, @StringRes detail: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = TouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(detail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(start = SmallGap))
    }
}

/**
 * The account block in Me: sign in or out, and the updates choice while signed in.
 */
@Composable
fun AccountSection(
    state: AccountState,
    busy: Boolean,
    @StringRes message: Int?,
    consent: UpdatesConsent,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onConsent: (UpdatesConsent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SmallGap)) {
        when (state) {
            AccountState.SignedOut -> {
                Text(stringResource(R.string.account_me_signed_out), style = MaterialTheme.typography.bodyMedium)
                if (message != null) {
                    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                ContinueWithGoogle(busy = busy, onClick = onSignIn)
            }
            is AccountState.SignedIn -> {
                Text(stringResource(R.string.account_me_signed_in), style = MaterialTheme.typography.bodyLarge)
                HorizontalDivider()
                Text(stringResource(R.string.account_updates_title), style = MaterialTheme.typography.titleSmall)
                UpdatesSwitches(consent = consent, onChange = onConsent)
                HorizontalDivider()
                OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.heightIn(min = TouchTarget)) {
                    Text(stringResource(R.string.account_sign_out))
                }
            }
        }
    }
}
