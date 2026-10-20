// The daily note's screens: a one-time offer on Today and its settings in Me. The engine is the notify module.
package com.astrolingo.app.notes

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.astrolingo.app.R
import com.astrolingo.app.ui.components.InfoCard
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.notify.NoteSettings
import com.astrolingo.notify.NoteState
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Asks Android for permission to post notifications when it needs asking (Android 13 and up), then calls [onAnswer]
 * with true if notes can be shown. On older phones it answers true at once.
 */
@Composable
fun rememberNotePermission(settings: NoteSettings, onAnswer: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onAnswer)
    return {
        if (settings.needsPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onAnswer(true)
        }
    }
}

/**
 * The offer on Today, shown once from the user's second day: one calm note each morning. "Not now" hides it for good;
 * Me keeps the switch.
 */
@Composable
fun NoteOffer(state: NoteState, showDeity: Boolean, onYes: () -> Unit, onNotNow: () -> Unit) {
    InfoCard(title = stringResource(R.string.notes_offer_title), icon = Icons.Filled.Notifications) {
        Text(
            stringResource(if (showDeity) R.string.notes_offer_body_deity else R.string.notes_offer_body, time(state.time)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = onYes, modifier = Modifier.heightIn(min = Spacing.touchTarget)) { Text(stringResource(R.string.notes_offer_yes)) }
            TextButton(onClick = onNotNow, modifier = Modifier.heightIn(min = Spacing.touchTarget)) { Text(stringResource(R.string.notes_offer_no)) }
        }
    }
}

/** The daily note in Me: on or off, its time, and a way to the phone's settings when Android blocks it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSettingsCard(state: NoteState, onTurn: (Boolean) -> Unit, onTime: (LocalTime) -> Unit) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    InfoCard(title = stringResource(R.string.notes_settings_title), icon = Icons.Filled.Notifications) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = state.on, role = Role.Switch, onValueChange = onTurn)
                .heightIn(min = Spacing.touchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.notes_settings_switch), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = state.on, onCheckedChange = null)
        }
        Text(stringResource(R.string.notes_settings_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.on) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.notes_settings_time, time(state.time)), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { picking = true }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                    Text(stringResource(R.string.notes_settings_change))
                }
            }
            if (state.blocked) {
                Text(stringResource(R.string.notes_settings_blocked), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }) { Text(stringResource(R.string.notes_settings_open)) }
            }
        }
    }
    if (picking) {
        val picker = rememberTimePickerState(state.time.hour, state.time.minute)
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(stringResource(R.string.notes_time_title)) },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    onTime(LocalTime.of(picker.hour, picker.minute))
                }) { Text(stringResource(R.string.notes_time_save)) }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.me_delete_cancel)) } },
        )
    }
}

/** "7:30 AM", in the app's language. */
private fun time(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
