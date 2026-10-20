// The richer parts of a learn card (PO, 20 Oct 2026): a graha's nature badge, short fact rows, chip groups, and the
// Navagraha Sangraha, all nine grahas on one page that opens full screen with an AstroLingo watermark.
package com.astrolingo.app.learn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.content.Badge
import com.astrolingo.learn.content.ChipGroup
import com.astrolingo.learn.content.FactRow
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.Nature
import com.astrolingo.learn.content.SheetEntry

/** The badge, rows and chips of [card], in that order, shown between its title and its body. */
@Composable
internal fun CardDetails(card: LearnCard, language: Language) {
    card.badge?.let { NatureBadge(it, language) }
    if (card.rows.isNotEmpty()) FactRows(card.rows, language)
    if (card.chips.isNotEmpty()) ChipGroups(card.chips, language)
}

/** A graha's nature as a coloured pill: green-blue benefic, peach malefic, grey neutral, lavender shadow. */
@Composable
internal fun NatureBadge(badge: Badge, language: Language, modifier: Modifier = Modifier) {
    val (container, content) = natureColors(badge.kind)
    Surface(shape = MaterialTheme.shapes.extraSmall, color = container, contentColor = content, modifier = modifier) {
        Text(
            "●  ${badge.text.get(language)}",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
    }
}

@Composable
private fun natureColors(nature: Nature): Pair<Color, Color> = when (nature) {
    Nature.BENEFIC -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
    Nature.MALEFIC -> AstroLingoTheme.tabColors.me to AstroLingoTheme.tabColors.onPastel
    Nature.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    Nature.SHADOW -> AstroLingoTheme.tabColors.learn to AstroLingoTheme.tabColors.onPastel
}

/** "Label ... value" lines with a hairline between them; TalkBack reads each line as one. */
@Composable
internal fun FactRows(rows: List<FactRow>, language: Language) {
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {}
                    .padding(vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    row.label.get(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f),
                )
                Text(
                    row.value.get(language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.58f),
                )
            }
        }
    }
}

/** Each group's label, then its words as soft lavender chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipGroups(groups: List<ChipGroup>, language: Language) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        groups.forEach { group ->
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(group.label.get(language), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    group.items(language).forEach { item ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = AstroLingoTheme.tabColors.learn.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Text(item, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * The one-page sheet on its card: every graha in a short block. A tap anywhere on it, or the button under it, opens
 * the same page full screen.
 */
@Composable
internal fun GrahaSheetOnCard(title: String, entries: List<SheetEntry>, language: Language) {
    var open by rememberSaveable { mutableStateOf(false) }
    val openLabel = stringResource(R.string.learn_sheet_open)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = openLabel) { open = true },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        GrahaSheet(entries, language, pictureSize = 36)
    }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
        Text(openLabel)
    }
    if (open) GrahaSheetFullScreen(title, entries, language, onClose = { open = false })
}

@Composable
private fun GrahaSheet(entries: List<SheetEntry>, language: Language, pictureSize: Int) {
    entries.forEachIndexed { index, entry ->
        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            grahaOf(entry.graha)?.let { GrahaPicture(it, Modifier.size(pictureSize.dp), animate = false) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.name.get(language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                NatureBadge(entry.nature, language)
                Text(stringResource(R.string.learn_sheet_think, entry.think.get(language)), style = MaterialTheme.typography.bodyMedium)
                Text(entry.facts.get(language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The sheet over the whole screen, with "AstroLingo" faintly across the background, as on a printed chart. */
@Composable
internal fun GrahaSheetFullScreen(title: String, entries: List<SheetEntry>, language: Language, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize()) {
                Watermark(Modifier.matchParentSize())
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { heading() },
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.learn_sheet_close))
                        }
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        GrahaSheet(entries, language, pictureSize = 48)
                    }
                }
            }
        }
    }
}

/** Rows of the app's name, tilted and very faint, behind the page. Hidden from TalkBack. */
@Composable
private fun Watermark(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.app_name)
    val ink = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Box(modifier.clipToBounds().clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .requiredSize(WATERMARK_SIZE.dp)
                .rotate(-30f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(WATERMARK_LINES) { line ->
                Text(
                    List(4) { name }.joinToString("      "),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(x = if (line % 2 == 0) 0.dp else 70.dp),
                )
            }
        }
    }
}

private const val WATERMARK_SIZE = 1000
private const val WATERMARK_LINES = 14
