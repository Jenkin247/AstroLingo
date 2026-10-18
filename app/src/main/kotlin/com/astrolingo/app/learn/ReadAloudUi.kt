// The read-aloud controls in the lesson player (LS-7): the speaker on each card, the Listening sheet with Listen mode,
// and the dialogs for a missing voice.
package com.astrolingo.app.learn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.content.Language

/** The speaker on a card: reads it aloud, and while reading turns into a Stop button. */
@Composable
internal fun SpeakerButton(speaking: Boolean, onSpeak: () -> Unit, onStop: () -> Unit) {
    if (speaking) {
        FilledTonalButton(onClick = onStop, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Icon(painterResource(R.drawable.ic_stop), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(Spacing.xs))
            Text(stringResource(R.string.audio_stop))
        }
    } else {
        IconButton(onClick = onSpeak) {
            Icon(
                painterResource(R.drawable.ic_volume_up),
                contentDescription = stringResource(R.string.audio_read_aloud),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The headphones button in the player's top bar, opening [ListeningSheet]. */
@Composable
internal fun ListeningButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.ic_headphones),
            contentDescription = stringResource(R.string.audio_listening_open),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Listen mode and which voices the phone has. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListeningSheet(readAloud: ReadAloud, onDismiss: () -> Unit) {
    // Read again whenever the engine finishes starting.
    val ready = readAloud.readyCount
    val english = remember(ready) { readAloud.voiceStatus(Language.ENGLISH) }
    val hindi = remember(ready) { readAloud.voiceStatus(Language.HINDI) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.lg)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_headphones), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.audio_listening), style = MaterialTheme.typography.titleLarge)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = readAloud.listenMode, role = Role.Switch, onValueChange = readAloud::updateListenMode)
                    .heightIn(min = Spacing.touchTarget),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.audio_listen_mode), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.audio_listen_mode_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = readAloud.listenMode, onCheckedChange = null)
            }
            HorizontalDivider()
            VoiceRow(stringResource(R.string.audio_voice_english), english, readAloud::openVoiceSettings)
            VoiceRow(stringResource(R.string.audio_voice_hindi), hindi, readAloud::openVoiceSettings)
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    stringResource(R.string.audio_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VoiceRow(name: String, status: VoiceStatus, onAdd: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = status == VoiceStatus.MISSING, onClick = onAdd)
            .heightIn(min = Spacing.touchTarget)
            .padding(vertical = Spacing.xs),
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        Text(
            stringResource(
                when (status) {
                    VoiceStatus.CHECKING -> R.string.audio_voice_checking
                    VoiceStatus.INSTALLED -> R.string.audio_voice_installed
                    VoiceStatus.MISSING -> R.string.audio_voice_missing
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (status == VoiceStatus.MISSING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Says plainly why a card can't be read, and offers the phone's speech settings (or English, for Hindi). */
@Composable
internal fun VoiceProblemDialog(readAloud: ReadAloud) {
    val problem = readAloud.problem ?: return
    val noHindi = problem == VoiceProblem.NO_HINDI
    AlertDialog(
        onDismissRequest = readAloud::dismissProblem,
        icon = { Icon(painterResource(R.drawable.ic_volume_up), contentDescription = null) },
        title = { Text(stringResource(if (noHindi) R.string.audio_no_hindi_title else R.string.audio_no_engine_title)) },
        text = { Text(stringResource(if (noHindi) R.string.audio_no_hindi_body else R.string.audio_no_engine_body)) },
        confirmButton = {
            TextButton(onClick = readAloud::openVoiceSettings) { Text(stringResource(R.string.audio_open_settings)) }
        },
        dismissButton = {
            if (noHindi) {
                TextButton(onClick = readAloud::useEnglish) { Text(stringResource(R.string.audio_use_english)) }
            } else {
                TextButton(onClick = readAloud::dismissProblem) { Text(stringResource(R.string.audio_close)) }
            }
        },
    )
}

/** [text] with the part in [range] marked, for the sentence being read aloud. */
@Composable
internal fun highlighted(text: String, range: IntRange?): AnnotatedString {
    if (range == null || range.first < 0 || range.last >= text.length) return AnnotatedString(text)
    val mark = AstroLingoTheme.tabColors.today
    val ink = AstroLingoTheme.tabColors.onPastel
    return buildAnnotatedString {
        append(text.substring(0, range.first))
        withStyle(SpanStyle(background = mark, color = ink)) { append(text.substring(range)) }
        append(text.substring(range.last + 1))
    }
}
