// The pages of the lesson player: learn cards (some flip over on a tap), "your chart" cards, quiz cards and
// the lesson-done card. Motion follows ADR 0007 and becomes a plain change when animations are off.
package com.astrolingo.app.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.content.Chapter
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.Lesson
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.content.Term
import com.astrolingo.learn.content.YourChartCard
import kotlin.math.cos
import kotlin.math.sin

/**
 * The frame every card sits in: a rounded sheet with a small label, optional actions (bookmark) at the top right,
 * scrolling content for large fonts, and a "Next card" button for people who can't swipe.
 */
@Composable
private fun CardFrame(
    label: String,
    position: String,
    color: Color,
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = color,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxSize()
            .semantics { stateDescription = position },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                actions()
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
                content = content,
            )
            footer()
            if (onNext != null) {
                TextButton(
                    onClick = onNext,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = Spacing.sm)
                        .heightIn(min = Spacing.touchTarget),
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                    Spacer(Modifier.size(Spacing.xs))
                    Text(stringResource(R.string.learn_next_card))
                }
            }
        }
    }
}

@Composable
private fun TermChip(term: Term) {
    Text(
        "${term.devanagari}  ·  ${term.roman}",
        style = MaterialTheme.typography.titleMedium,
        color = AstroLingoTheme.tabColors.onPastel,
        modifier = Modifier
            .border(1.dp, AstroLingoTheme.tabColors.learn, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    )
}

/** "Swipe up for the next card", with a gently bobbing arrow on the first card of a lesson. */
@Composable
private fun SwipeHint() {
    val reduceMotion = rememberReduceMotion()
    val bob = if (reduceMotion) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "swipe hint")
        val value by transition.animateFloat(0f, -8f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "bob")
        value
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer { translationY = bob * density },
        )
        Text(stringResource(R.string.learn_swipe_hint), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * A learn card. A flip card (graha cards) first shows only the picture and title; a tap turns it over to the text,
 * and another tap turns it back.
 */
@Composable
internal fun LearnCardPage(
    card: LearnCard,
    language: Language,
    active: Boolean,
    position: String,
    showSwipeHint: Boolean,
    onNext: () -> Unit,
    actions: @Composable () -> Unit,
) {
    if (!card.flip || card.visual == null) {
        CardFrame(
            label = position,
            position = position,
            color = MaterialTheme.colorScheme.surface,
            onNext = onNext,
            actions = actions,
            footer = { if (showSwipeHint) SwipeHint() },
        ) {
            card.visual?.let { CardVisual(it, active) }
            Text(card.title.get(language), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
            card.term?.let { TermChip(it) }
            Text(card.body.get(language), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    var flipped by rememberSaveable(card.id) { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, tween(if (reduceMotion) 0 else FLIP_MILLIS), label = "flip")
    val showBack = rotation > 90f
    val turnLabel = stringResource(R.string.learn_flip_card)
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(role = Role.Button, onClickLabel = turnLabel) { flipped = !flipped },
    ) {
        // The back is drawn mirrored so it reads the right way round once the card has turned.
        val content = Modifier.graphicsLayer { rotationY = if (showBack) 180f else 0f }
        if (!showBack) {
            CardFrame(
                label = position,
                position = position,
                color = AstroLingoTheme.tabColors.learn.copy(alpha = 0.45f),
                onNext = null,
                modifier = content,
                actions = actions,
                footer = { if (showSwipeHint) SwipeHint() },
            ) {
                CardVisual(card.visual!!, active, size = 240.dp)
                Text(
                    card.title.get(language),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                )
                card.term?.let { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TermChip(it) } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(Spacing.xs))
                    Text(stringResource(R.string.learn_tap_to_flip), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            CardFrame(
                label = position,
                position = position,
                color = MaterialTheme.colorScheme.surface,
                onNext = onNext,
                modifier = content,
                actions = actions,
            ) {
                grahaOf(card.visual!!.value)?.let {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { GrahaPicture(it, Modifier.size(96.dp), animate = false) }
                }
                Text(card.title.get(language), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                card.term?.let { TermChip(it) }
                Text(card.body.get(language), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** The lesson's idea in the learner's own kundli, tinted with the Kundli tab's sky blue. */
@Composable
internal fun YourChartCardPage(
    card: YourChartCard,
    text: String,
    language: Language,
    position: String,
    onNext: () -> Unit,
    actions: @Composable () -> Unit,
) {
    CardFrame(
        label = stringResource(R.string.learn_your_chart),
        position = position,
        color = AstroLingoTheme.tabColors.kundli.copy(alpha = 0.45f),
        onNext = onNext,
        actions = actions,
    ) {
        Text("✦", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(card.title.get(language), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A quiz question. Answering shows the right answer and why; a wrong choice shakes gently and is never punished.
 *
 * @param chosen the option picked, or null before answering.
 */
@Composable
internal fun QuizCardPage(
    quiz: QuizCard,
    language: Language,
    chosen: Int?,
    position: String,
    onChoose: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    val shake = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(chosen) {
        if (chosen == null || reduceMotion) return@LaunchedEffect
        if (quiz.isCorrect(chosen)) {
            pulse.animateTo(1f, keyframes { durationMillis = 320; 1.06f at 140 })
        } else {
            shake.animateTo(0f, keyframes { durationMillis = 420; -12f at 60; 12f at 140; -8f at 220; 6f at 300 })
        }
    }
    CardFrame(
        label = stringResource(R.string.learn_quiz),
        position = position,
        color = MaterialTheme.colorScheme.surface,
        onNext = if (chosen != null) onNext else null,
    ) {
        Text(quiz.question.get(language), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        quiz.options.forEachIndexed { index, option ->
            val answered = chosen != null
            val isAnswer = index == quiz.answer
            val isChosen = index == chosen
            val tint = when {
                answered && isAnswer -> MaterialTheme.colorScheme.primary
                answered && isChosen -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (isChosen) {
                            translationX = shake.value * density
                            scaleX = pulse.value
                            scaleY = pulse.value
                        }
                    }
                    .border(2.dp, tint, MaterialTheme.shapes.medium)
                    .clickable(enabled = !answered, role = Role.Button) { onChoose(index) }
                    .heightIn(min = Spacing.touchTarget)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(option.get(language), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (answered && isAnswer) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.learn_quiz_right), tint = tint)
                if (answered && isChosen && !isAnswer) Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.learn_quiz_wrong), tint = tint)
            }
        }
        AnimatedVisibility(visible = chosen != null, enter = fadeIn() + expandVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                val right = chosen != null && quiz.isCorrect(chosen)
                Text(
                    stringResource(if (right) R.string.learn_quiz_right else R.string.learn_quiz_wrong),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (right) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
                Text(quiz.explanation.get(language), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (chosen == null) {
            Text(
                stringResource(R.string.learn_quiz_answer_first),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Sitara celebrates the finished lesson, with stars drifting up, and offers the next lesson. */
@Composable
internal fun LessonDonePage(
    chapter: Chapter,
    lesson: Lesson,
    language: Language,
    right: Int,
    total: Int,
    active: Boolean,
    next: Lesson?,
    onNext: (Lesson) -> Unit,
    onClose: () -> Unit,
    chapterDone: Boolean = false,
) {
    val reduceMotion = rememberReduceMotion()
    CardFrame(
        label = chapter.title.get(language),
        position = lesson.title.get(language),
        color = AstroLingoTheme.tabColors.learn,
        onNext = null,
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 160.dp), contentAlignment = Alignment.Center) {
            if (active && !reduceMotion) DriftingStars(Modifier.matchParentSize())
            SitaraAvatar(size = 120.dp)
        }
        Text(
            stringResource(if (chapterDone) R.string.learn_chapter_complete else R.string.learn_lesson_complete),
            style = MaterialTheme.typography.displaySmall,
            color = AstroLingoTheme.tabColors.onPastel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Text(
            stringResource(R.string.learn_lesson_score, right, total),
            style = MaterialTheme.typography.titleLarge,
            color = AstroLingoTheme.tabColors.onPastel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(if (right == total) R.string.learn_lesson_sitara_great else R.string.learn_lesson_sitara_good),
            style = MaterialTheme.typography.bodyLarge,
            color = AstroLingoTheme.tabColors.onPastel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (chapterDone) {
            // Names what the learner can now do, so finishing a chapter feels like a skill gained (LS-2).
            Text(
                stringResource(R.string.learn_chapter_can_now, chapter.outcome.get(language)),
                style = MaterialTheme.typography.bodyLarge,
                color = AstroLingoTheme.tabColors.onPastel,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (chapter.id == READ_CHART_CHAPTER) {
                Text(
                    stringResource(R.string.learn_milestone_read_chart),
                    style = MaterialTheme.typography.titleMedium,
                    color = AstroLingoTheme.tabColors.onPastel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (next != null) {
            Button(onClick = { onNext(next) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                Text(stringResource(R.string.learn_next_lesson, next.title.get(language)))
            }
        }
        TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(R.string.learn_back_to_path), color = AstroLingoTheme.tabColors.onPastel)
        }
    }
}

/** A handful of small stars rising and fading around Sitara. */
@Composable
private fun DriftingStars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stars")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(STARS_MILLIS)), label = "rise")
    val star = MaterialTheme.colorScheme.primary
    val rose = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        repeat(STAR_COUNT) { i ->
            val phase = (t + i / STAR_COUNT.toFloat()) % 1f
            val x = size.width * (0.15f + 0.7f * ((i * 37) % 100) / 100f) + 10.dp.toPx() * sin(phase * 6f + i)
            val y = size.height * (1f - phase)
            val radius = (3 + i % 3).dp.toPx()
            val alpha = 1f - phase
            drawStar(if (i % 2 == 0) star else rose, Offset(x, y), radius, alpha)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(color: Color, at: Offset, radius: Float, alpha: Float) {
    val path = androidx.compose.ui.graphics.Path()
    for (k in 0 until 10) {
        val r = if (k % 2 == 0) radius * 2 else radius * 0.8f
        val angle = -Math.PI / 2 + k * Math.PI / 5
        val point = Offset(at.x + (r * cos(angle)).toFloat(), at.y + (r * sin(angle)).toFloat())
        if (k == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color, alpha = alpha)
}

private const val FLIP_MILLIS = 450

/** Chapter 6, Reading a kundli: finishing it is the milestone "You can read your own chart". */
private const val READ_CHART_CHAPTER = "reading"
private const val STARS_MILLIS = 3200
private const val STAR_COUNT = 9
