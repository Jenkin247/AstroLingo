// The knowledge check: the learner picks what they already know, answers two questions per chapter, and chapters
// they really know are skipped on the path (PO, 17 Oct 2026; rules in learn/ KnowledgeCheck, BA and Astro Guru).
package com.astrolingo.app.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraSays
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.check.KnowledgeCheck
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.Language

/**
 * The knowledge check.
 *
 * @param chapterIds chapters to check straight away ("I know this" on one chapter), or null to let the learner pick.
 * @param onDone called with the chapters passed once the learner has seen the result.
 * @param onSkip the learner chose to start from the beginning without a check.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KnowledgeCheckScreen(
    course: Course,
    chapterIds: List<String>?,
    language: Language,
    onDone: (Set<String>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saved as plain lists so the check survives rotation.
    var picked by rememberSaveable { mutableStateOf(chapterIds) }
    var answers by rememberSaveable(picked) { mutableStateOf(listOf<Int>()) }
    var choice by rememberSaveable(picked, answers.size) { mutableStateOf<Int?>(null) }
    var stopped by rememberSaveable(picked) { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSkip) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.check_close)) }
            val chosen = picked
            if (chosen != null) {
                val total = KnowledgeCheck.of(course, chosen).questions.size.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = { answers.size / total.toFloat() },
                    modifier = Modifier.weight(1f).padding(end = Spacing.md),
                    trackColor = AstroLingoTheme.tabColors.learn.copy(alpha = 0.4f),
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            val chosen = picked
            if (chosen == null) {
                PickChapters(course, language, onCheck = { picked = it }, onSkip = onSkip)
                return@Column
            }
            val check = KnowledgeCheck(KnowledgeCheck.of(course, chosen).questions, answers)
            val question = check.current
            if (question == null || stopped) {
                CheckResult(course, check, language, onStart = { onDone(check.passedChapters()) })
                return@Column
            }
            Text(
                stringResource(R.string.check_question_count, answers.size + 1, check.questions.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(question.chapter.title.get(language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(question.quiz.question.get(language), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            val answered = choice
            question.quiz.options.forEachIndexed { index, option ->
                val tint = when {
                    answered != null && index == question.quiz.answer -> MaterialTheme.colorScheme.primary
                    answered == index -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .border(2.dp, tint, MaterialTheme.shapes.medium)
                        .clickable(enabled = answered == null, role = Role.Button) { choice = index }
                        .heightIn(min = Spacing.touchTarget)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(option.get(language), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    if (answered != null && index == question.quiz.answer) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.learn_quiz_right), tint = tint)
                    }
                }
            }
            AnimatedVisibility(visible = answered != null, enter = fadeIn() + expandVertically()) {
                Text(question.quiz.explanation.get(language), style = MaterialTheme.typography.bodyLarge)
            }
            if (answered != null) {
                val last = answers.size + 1 == check.questions.size
                Button(
                    onClick = { answers = answers + answered },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
                ) { Text(stringResource(if (last) R.string.check_finish else R.string.check_next)) }
            }
            if (answers.isNotEmpty()) {
                TextButton(onClick = { stopped = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.check_stop)) }
            }
        }
    }
}

/** Step 1: Sitara asks what the learner knows; a chip per chapter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickChapters(course: Course, language: Language, onCheck: (List<String>) -> Unit, onSkip: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    SitaraSays(stringResource(R.string.check_pick_sitara))
    Text(stringResource(R.string.check_pick_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        course.chapters.forEach { chapter ->
            val on = chapter.id in selected
            FilterChip(
                selected = on,
                onClick = { selected = if (on) selected - chapter.id else selected + chapter.id },
                label = { Text("${textGlyph(chapter.glyph)}  ${chapter.title.get(language)}") },
                leadingIcon = if (on) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
                modifier = Modifier.heightIn(min = Spacing.touchTarget),
            )
        }
    }
    val questions = KnowledgeCheck.of(course, selected).questions.size
    Button(
        onClick = { onCheck(selected) },
        enabled = selected.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget),
    ) { Text(stringResource(R.string.check_start, questions)) }
    OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
        Text(stringResource(R.string.check_from_start))
    }
}

/** Step 3: what was skipped and what stays as a refresher. */
@Composable
private fun CheckResult(course: Course, check: KnowledgeCheck, language: Language, onStart: () -> Unit) {
    val passed = check.passedChapters()
    val refresh = check.refresherChapters()
    Text(stringResource(R.string.check_result_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    if (passed.isEmpty()) {
        SitaraSays(stringResource(R.string.check_result_none))
    } else {
        Text(stringResource(R.string.check_result_known), style = MaterialTheme.typography.bodyLarge)
        ChapterList(course, passed, language, known = true)
    }
    if (refresh.isNotEmpty()) {
        Text(stringResource(R.string.check_result_refresh), style = MaterialTheme.typography.bodyLarge)
        ChapterList(course, refresh, language, known = false)
    }
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
        Text(stringResource(R.string.check_result_start))
    }
}

@Composable
private fun ChapterList(course: Course, ids: Set<String>, language: Language, known: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (known) AstroLingoTheme.tabColors.learn.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            course.chapters.filter { it.id in ids }.forEach { chapter ->
                Text("${textGlyph(chapter.glyph)}  ${chapter.title.get(language)}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** The offer on Learn home for learners who said they are new; they can take the check or not. */
@Composable
internal fun CheckOffer(onTake: () -> Unit, onLater: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = AstroLingoTheme.tabColors.kundli.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.check_offer_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.check_offer_body), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = onTake) { Text(stringResource(R.string.check_offer_take)) }
                TextButton(onClick = onLater) { Text(stringResource(R.string.check_offer_later)) }
            }
        }
    }
}

/** "I know this" at the foot of a chapter's sheet. */
@Composable
internal fun KnowThisButton(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm).heightIn(min = Spacing.touchTarget)) {
        Text(stringResource(R.string.check_know_this))
    }
}
