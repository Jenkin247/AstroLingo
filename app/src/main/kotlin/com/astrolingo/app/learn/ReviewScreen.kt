// Daily Review (LS-5): up to five due cards, one at a time. A quiz is asked again; a saved card shows its title and
// asks "Do you remember?" before turning over. Ends with how many were remembered and when the next ones are due.
package com.astrolingo.app.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.content.CardLocation
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.content.YourChartCard
import com.astrolingo.learn.progress.LearnProgress
import java.time.LocalDate

/**
 * The daily Review.
 *
 * @param cards today's due cards, fixed when Review opens so answering doesn't reshuffle them.
 * @param onProgress applies a change to progress: each answer, Review time toward the daily goal, and Stars once
 *   Review is finished.
 * @param daysToNext with the progress after this Review, days until the next card is due, or null when none wait.
 */
@Composable
internal fun ReviewScreen(
    cards: List<CardLocation>,
    language: Language,
    facts: ChartFacts,
    onProgress: (LearnProgress.() -> LearnProgress) -> Unit,
    daysToNext: () -> Long?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var remembered by rememberSaveable { mutableIntStateOf(0) }
    // Review time counts toward the daily goal, like lesson time.
    StudyTimer(key = index) { seconds -> onProgress { addStudyTime(LocalDate.now().toEpochDay(), seconds) } }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.learn_review_close))
            }
            LinearProgressIndicator(
                progress = { if (cards.isEmpty()) 1f else index.coerceAtMost(cards.size) / cards.size.toFloat() },
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = AstroLingoTheme.tabColors.learn.copy(alpha = 0.4f),
            )
            Text(
                "${(index + 1).coerceAtMost(cards.size)}/${cards.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = Spacing.sm),
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            val location = cards.getOrNull(index)
            if (location == null) {
                // Finishing the day's Review earns Stars once a day.
                LaunchedEffect(Unit) { if (cards.isNotEmpty()) onProgress { finishReview(LocalDate.now().toEpochDay()) } }
                ReviewDone(remembered = remembered, total = cards.size, daysToNext = daysToNext(), onClose = onClose)
            } else {
                val position = stringResource(R.string.learn_review_position, index + 1, cards.size)
                val next: (Boolean) -> Unit = { ok ->
                    onProgress { answerReview(location.card.id, ok, LocalDate.now().toEpochDay()) }
                    if (ok) remembered++
                    index++
                }
                // A fresh card each time: its own answer and turned-over state.
                key(location.card.id) {
                    when (val card = location.card) {
                        is QuizCard -> {
                            var chosen by remember { mutableStateOf<Int?>(null) }
                            QuizCardPage(
                                quiz = card,
                                language = language,
                                chosen = chosen,
                                position = position,
                                onChoose = { chosen = it },
                                onNext = { next(chosen?.let(card::isCorrect) == true) },
                            )
                        }
                        is LearnCard -> RecallCard(card.title.get(language), card.body.get(language), position, next)
                        is YourChartCard -> RecallCard(card.title.get(language), card.text(language, facts.valueOf(card.fact)), position, next)
                    }
                }
            }
        }
    }
}

/** A saved card: the title first, "Do you remember?", then the text and an honest "I remembered" or "Not yet". */
@Composable
private fun RecallCard(title: String, body: String, position: String, onAnswer: (Boolean) -> Unit) {
    var shown by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        ) {
            Text(position, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            if (shown) {
                Text(body, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onAnswer(false) }, modifier = Modifier.weight(1f).heightIn(min = Spacing.touchTarget)) {
                        Text(stringResource(R.string.learn_review_no))
                    }
                    Button(onClick = { onAnswer(true) }, modifier = Modifier.weight(1f).heightIn(min = Spacing.touchTarget)) {
                        Text(stringResource(R.string.learn_review_yes))
                    }
                }
            } else {
                Text(stringResource(R.string.learn_review_remember), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { shown = true }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Text(stringResource(R.string.learn_review_show), modifier = Modifier.padding(start = Spacing.xs))
                }
            }
        }
    }
}

@Composable
private fun ReviewDone(remembered: Int, total: Int, daysToNext: Long?, onClose: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = AstroLingoTheme.tabColors.learn, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val ink = AstroLingoTheme.tabColors.onPastel
            SitaraAvatar(size = 120.dp)
            Text(
                stringResource(R.string.learn_review_done_title),
                style = MaterialTheme.typography.displaySmall,
                color = ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (total > 0) {
                Text(stringResource(R.string.learn_review_done_score, remembered, total), style = MaterialTheme.typography.titleLarge, color = ink)
            }
            Text(
                when (daysToNext) {
                    null -> stringResource(R.string.learn_review_none_left)
                    0L, 1L -> stringResource(R.string.learn_review_next_tomorrow)
                    else -> stringResource(R.string.learn_review_next_days, daysToNext.toInt())
                },
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                Text(stringResource(R.string.learn_back_to_path), color = ink)
            }
        }
    }
}

/** The Review entry at the top of Learn home, shown when cards are due. */
@Composable
internal fun ReviewOffer(due: Int, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = AstroLingoTheme.tabColors.today.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.learn_review_due, due), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.learn_review_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onStart, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
                Text(stringResource(R.string.learn_review_start))
            }
        }
    }
}
