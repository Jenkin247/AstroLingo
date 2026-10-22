// Learn home: the "Up next" card and the path of 14 chapters, each with its progress ring.
// Tapping a chapter opens a sheet with its lessons (docs/design/learn-module.md, UX/UI). Lessons open in order, so
// chapters and lessons not reached yet show a lock and what opens them.
package com.astrolingo.app.learn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.sitara.SitaraAvatar
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.content.Chapter
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.Lesson
import com.astrolingo.learn.path.ChapterProgress
import com.astrolingo.learn.path.ChapterState
import com.astrolingo.learn.path.CoursePath
import kotlinx.coroutines.delay

/**
 * The Learn tab's home screen.
 *
 * @param onOpenLesson opens a lesson at a card index (0 to start).
 * @param actions extra buttons in the header row, such as Saved.
 * @param banner shown between the header and the "Up next" card, such as the knowledge check offer.
 * @param chapterActions extra buttons at the foot of a chapter's sheet, such as "I know this".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LearnHome(
    course: Course,
    path: CoursePath,
    language: Language,
    onOpenLesson: (Lesson, Int) -> Unit,
    modifier: Modifier = Modifier,
    completedLessons: Set<String> = path.chapters.flatMap { progress ->
        progress.chapter.lessons.take(progress.lessonsDone).map { it.id }
    }.toSet(),
    actions: @Composable () -> Unit = {},
    banner: @Composable () -> Unit = {},
    chapterActions: @Composable (Chapter, dismiss: () -> Unit) -> Unit = { _, _ -> },
) {
    var sheetChapter by rememberSaveable { mutableStateOf<String?>(null) }
    val reduceMotion = rememberReduceMotion()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.learn_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                actions()
            }
        }
        item(key = "banner") { banner() }
        item(key = "next") {
            UpNextCard(course, path, language, onStart = { lesson -> onOpenLesson(lesson, path.resumeAt) })
        }
        itemsIndexed(path.chapters, key = { _, item -> item.chapter.id }) { index, item ->
            ChapterTile(
                number = index + 1,
                item = item,
                language = language,
                first = index == 0,
                last = index == path.chapters.lastIndex,
                appearDelay = if (reduceMotion) null else index * STAGGER_MILLIS,
                onClick = { sheetChapter = item.chapter.id },
            )
        }
    }
    val open = sheetChapter?.let(course::chapter)
    if (open != null) {
        ModalBottomSheet(onDismissRequest = { sheetChapter = null }) {
            ChapterSheet(
                chapter = open,
                number = course.chapters.indexOf(open) + 1,
                completedLessons = completedLessons,
                unlocked = path.unlocked,
                language = language,
                onOpenLesson = { lesson ->
                    sheetChapter = null
                    onOpenLesson(lesson, 0)
                },
                actions = { chapterActions(open) { sheetChapter = null } },
            )
        }
    }
}

/** The lavender "Up next" card with Sitara, or a well-done note when the course is complete. */
@Composable
private fun UpNextCard(course: Course, path: CoursePath, language: Language, onStart: (Lesson) -> Unit) {
    val colors = AstroLingoTheme.tabColors
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.learn,
        contentColor = colors.onPastel,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                SitaraAvatar(size = 72.dp)
                Column(Modifier.weight(1f)) {
                    val next = path.next
                    if (next == null) {
                        Text(stringResource(R.string.learn_course_done_title), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.learn_course_done_body), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        val chapter = course.chapterOf(next.id)
                        Text(stringResource(R.string.learn_up_next), style = MaterialTheme.typography.labelLarge)
                        if (chapter != null) Text(chapter.title.get(language), style = MaterialTheme.typography.bodyMedium)
                        Text(next.title.get(language), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.learn_minutes, next.minutes), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            path.next?.let { next ->
                Button(onClick = { onStart(next) }, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget)) {
                    Text(stringResource(if (path.resumeAt > 0) R.string.learn_continue else R.string.learn_start))
                }
            }
        }
    }
}

/** One chapter on the path: progress ring with the chapter's glyph, title and state, joined to its neighbours by a line. */
@Composable
private fun ChapterTile(
    number: Int,
    item: ChapterProgress,
    language: Language,
    first: Boolean,
    last: Boolean,
    appearDelay: Long?,
    onClick: () -> Unit,
) {
    val appear = remember { Animatable(if (appearDelay == null) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (appearDelay != null) {
            delay(appearDelay)
            appear.animateTo(1f, tween(APPEAR_MILLIS))
        }
    }
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val highlight = if (item.isNext) AstroLingoTheme.tabColors.learn.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * APPEAR_OFFSET_DP * density
            }
            .drawBehind {
                // The path line runs through the centre of the rings, from the tile above to the tile below.
                val x = Spacing.md.toPx() + RING_SIZE.toPx() / 2
                val top = if (first) size.height / 2 else -Spacing.sm.toPx()
                val bottom = if (last) size.height / 2 else size.height + Spacing.sm.toPx()
                drawLine(lineColor, Offset(x, top), Offset(x, bottom), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            .background(highlight, MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val locked = item.state == ChapterState.LOCKED
        ProgressRing(fraction = item.fraction, glyph = item.chapter.glyph, known = item.state == ChapterState.KNOWN, locked = locked)
        Column(Modifier.weight(1f).graphicsLayer { alpha = if (locked) LOCKED_ALPHA else 1f }) {
            Text(
                stringResource(R.string.learn_chapter_number, number),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(item.chapter.title.get(language), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.learn_chapter_outcome, item.chapter.outcome.get(language)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                when (item.state) {
                    ChapterState.KNOWN -> stringResource(R.string.learn_state_known)
                    ChapterState.DONE -> stringResource(R.string.learn_state_done)
                    ChapterState.IN_PROGRESS -> stringResource(R.string.learn_state_progress, item.lessonsDone, item.lessonsTotal)
                    ChapterState.NOT_STARTED -> stringResource(R.string.learn_state_new, item.lessonsTotal)
                    ChapterState.LOCKED -> stringResource(R.string.learn_state_locked, number - 1)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.isNext) {
            Text(
                stringResource(R.string.learn_up_next),
                style = MaterialTheme.typography.labelMedium,
                color = AstroLingoTheme.tabColors.onPastel,
                modifier = Modifier
                    .background(AstroLingoTheme.tabColors.learn, CircleShape)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }
    }
}

/** A ring that fills as lessons are finished, with the chapter's glyph inside, or a lock while the chapter is closed. */
@Composable
private fun ProgressRing(fraction: Float, glyph: String, known: Boolean, locked: Boolean = false) {
    val animated by animateFloatAsState(fraction, tween(RING_MILLIS), label = "chapter progress")
    val track = MaterialTheme.colorScheme.outlineVariant
    val fill = if (known) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    Box(Modifier.size(RING_SIZE), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawCircle(surface)
            drawCircle(track, style = Stroke(stroke))
            drawArc(fill, -90f, 360f * animated, useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (locked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = stringResource(R.string.learn_lesson_locked_description),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(textGlyph(glyph), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        }
    }
}

/** A chapter's summary and its lessons, ticked when done, locked until the one before is passed. */
@Composable
private fun ChapterSheet(
    chapter: Chapter,
    number: Int,
    completedLessons: Set<String>,
    unlocked: Set<String>,
    language: Language,
    onOpenLesson: (Lesson) -> Unit,
    actions: @Composable () -> Unit,
) {
    // Scrolls: Grahas has a lesson per graha, more than a sheet shows at once.
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg)
            .padding(bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            stringResource(R.string.learn_chapter_number, number),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(chapter.title.get(language), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Text(chapter.summary.get(language), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.size(Spacing.xs))
        chapter.lessons.forEachIndexed { index, lesson ->
            if (index > 0) HorizontalDivider()
            val done = lesson.id in completedLessons
            val locked = lesson.id !in unlocked
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !locked, role = Role.Button) { onOpenLesson(lesson) }
                    .heightIn(min = Spacing.touchTarget)
                    .padding(vertical = Spacing.sm)
                    .graphicsLayer { alpha = if (locked) LOCKED_ALPHA else 1f },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        done -> Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.learn_lesson_done_description),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        locked -> Icon(
                            Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.learn_lesson_locked_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        else -> Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(lesson.title.get(language), style = MaterialTheme.typography.titleMedium)
                    if (locked) {
                        Text(
                            if (index > 0) {
                                stringResource(R.string.learn_lesson_locked, chapter.lessons[index - 1].title.get(language))
                            } else {
                                stringResource(R.string.learn_state_locked, number - 1)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    stringResource(R.string.learn_minutes, lesson.minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
        actions()
    }
}

/**
 * Some zodiac and planet symbols draw as colourful emoji on Android. The text variation selector asks
 * for the plain symbol so it takes the theme's text colour.
 */
internal fun textGlyph(glyph: String): String = if (glyph.endsWith(TEXT_STYLE)) glyph else glyph + TEXT_STYLE

private const val TEXT_STYLE = "︎"
private val RING_SIZE = 56.dp
private const val STAGGER_MILLIS = 40L
private const val APPEAR_MILLIS = 320
private const val APPEAR_OFFSET_DP = 24f
private const val RING_MILLIS = 700

// Locked chapters and lessons are drawn faded, so the open path stands out.
private const val LOCKED_ALPHA = 0.5f
