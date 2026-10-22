// The lesson player: full-screen cards, swipe up for the next one (vertical pager). A quiz must be answered
// before the cards after it appear. Ends with a "Lesson complete" card (docs/design/learn-module.md, UX/UI), or, with
// fewer than 2 of 3 right, an offer to take the quiz again: only a pass opens the next lesson.
package com.astrolingo.app.learn

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.astrolingo.app.R
import com.astrolingo.app.ui.theme.AstroLingoTheme
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.learn.audio.CardSpeech
import com.astrolingo.learn.audio.SpokenPart
import com.astrolingo.learn.content.Card
import com.astrolingo.learn.content.Chapter
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.Lesson
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.content.TeachingCard
import com.astrolingo.learn.content.YourChartCard
import com.astrolingo.learn.path.QuizPass
import com.astrolingo.learn.progress.LearnProgress
import java.time.LocalDate
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * Plays one lesson.
 *
 * @param startIndex the card to open at, such as a resume point or a saved card.
 * @param onProgress called with a change to progress: the lesson finished or left part-way, or learning time added.
 *   A change rather than a new value, so changes made in the same moment don't overwrite each other.
 * @param nextLesson the lesson to offer after this one, given the progress with this one finished.
 * @param cardActions extra controls on each teaching card, such as the bookmark.
 * @param keepResume false for a lesson peeked at from Today, so leaving it part-way doesn't replace "Continue".
 * @param readAloud reads cards aloud (LS-7); null hides the speaker and Listening buttons.
 * @param overlay drawn over the cards, given the card that has settled on screen (null on the done card), such as the
 *   chart bubble (LS-9).
 * @param unlocked false for a lesson whose turn on the path hasn't come (peeked at from Today): it can be read, but
 *   finishing it doesn't count and leaving it doesn't replace "Continue".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LessonPlayer(
    chapter: Chapter,
    lesson: Lesson,
    startIndex: Int,
    language: Language,
    facts: ChartFacts,
    progress: LearnProgress,
    onProgress: (LearnProgress.() -> LearnProgress) -> Unit,
    onClose: () -> Unit,
    onOpenLesson: (Lesson) -> Unit,
    nextLesson: (LearnProgress) -> Lesson?,
    modifier: Modifier = Modifier,
    cardActions: @Composable (TeachingCard) -> Unit = {},
    keepResume: Boolean = true,
    readAloud: ReadAloud? = null,
    overlay: @Composable BoxScope.(Card?) -> Unit = {},
    unlocked: Boolean = true,
) {
    val cards = lesson.cards
    // Chosen option per quiz card index. Kept for this visit only: reopening a lesson asks the quizzes again.
    val answers = remember(lesson.id) { mutableStateMapOf<Int, Int>() }
    // Pages run up to the first unanswered quiz, then the done card once every quiz is answered.
    val gate = cards.indices.firstOrNull { cards[it] is QuizCard && it !in answers }
    val pageCount = if (gate == null) cards.size + 1 else gate + 1
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, pageCount - 1)) { pageCount }
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()

    val latestProgress by rememberUpdatedState(progress)
    val latestPage by rememberUpdatedState(pager.currentPage)
    val finishedNow = pager.currentPage == cards.size
    val right = cards.withIndex().count { (index, card) -> card is QuizCard && answers[index]?.let(card::isCorrect) == true }
    val passed = QuizPass.passed(right, lesson.quizzes.size)
    // Stars the pass on screen earned, worked out before it is recorded so the done card can show them.
    var starsEarned by remember(lesson.id) { mutableIntStateOf(0) }
    LaunchedEffect(finishedNow) {
        // Only a pass in an open lesson counts: it opens the next lesson and earns Stars the first time.
        if (finishedNow && unlocked && passed && lesson.id !in latestProgress.completedLessons) {
            val today = LocalDate.now().toEpochDay()
            val perfect = right == lesson.quizzes.size
            starsEarned = latestProgress.starsForPassing(lesson.id, perfect, today)
            onProgress { passLesson(lesson.id, perfect, today) }
        }
    }
    // Leaving part-way (close, back or another tab) remembers the card for "Continue".
    DisposableEffect(lesson.id) {
        onDispose {
            if (keepResume && unlocked && latestPage < cards.size) onProgress { leaveLesson(lesson.id, latestPage) }
        }
    }
    // Back to the first quiz with every answer cleared. Scrolls first: clearing the answers shortens the pager.
    val retry: () -> Unit = {
        scope.launch {
            val firstQuiz = cards.indexOfFirst { it is QuizCard }.coerceAtLeast(0)
            pager.scrollToPage(firstQuiz)
            answers.clear()
        }
    }
    // What each card says aloud (LS-7). Quiz explanations are read only once the quiz is answered.
    fun speechOf(card: Card, page: Int, speechLanguage: Language): List<SpokenPart> = when (card) {
        is LearnCard -> CardSpeech.learn(card, speechLanguage)
        is YourChartCard -> CardSpeech.teaching(card.title.get(speechLanguage), card.text(speechLanguage, facts.valueOf(card.fact)))
        is QuizCard -> CardSpeech.quiz(card, speechLanguage, answered = page in answers)
    }
    fun speak(card: Card, page: Int) {
        readAloud?.speak(card.id, speechOf(card, page, language), language) { speechOf(card, page, Language.ENGLISH) }
    }
    if (readAloud != null) {
        // Swiping stops the reading; in Listen mode the card that arrives is read instead.
        LaunchedEffect(pager.settledPage, readAloud.listenMode) {
            val card = cards.getOrNull(pager.settledPage)
            if (readAloud.listenMode && card != null) speak(card, pager.settledPage) else readAloud.stop()
        }
        DisposableEffect(readAloud) { onDispose { readAloud.stop() } }
        VoiceProblemDialog(readAloud)
    }
    var listening by remember { mutableStateOf(false) }
    if (listening && readAloud != null) ListeningSheet(readAloud, onDismiss = { listening = false })
    val speaker: @Composable (Card, Int) -> Unit = { card, page ->
        if (readAloud != null) {
            SpeakerButton(speaking = readAloud.speakingKey == card.id, onSpeak = { speak(card, page) }, onStop = readAloud::stop)
        }
    }
    // The sentence being read on [card], to highlight it.
    fun highlightOn(card: Card): IntRange? = readAloud?.takeIf { it.speakingKey == card.id }?.speakingPart?.bodyRange

    // Time on each card counts toward today's goal.
    StudyTimer(key = pager.settledPage) { seconds -> onProgress { addStudyTime(LocalDate.now().toEpochDay(), seconds) } }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.learn_close))
            }
            val fraction by animateFloatAsState((pager.currentPage + 1f) / (cards.size + 1f), label = "lesson progress")
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                trackColor = AstroLingoTheme.tabColors.learn.copy(alpha = 0.4f),
            )
            Text(
                "${(pager.currentPage + 1).coerceAtMost(cards.size)}/${cards.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(end = if (readAloud == null) Spacing.sm else 0.dp),
            )
            if (readAloud != null) ListeningButton(onClick = { listening = true })
        }
        Box(Modifier.weight(1f)) {
            VerticalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
                // A soft spring, so a card lands with a little give instead of stopping dead.
                flingBehavior = PagerDefaults.flingBehavior(pager, snapAnimationSpec = if (reduceMotion) snap() else cardSpring),
                key = { page -> cards.getOrNull(page)?.id ?: DONE_KEY },
            ) { page ->
                val toNext: () -> Unit = {
                    scope.launch {
                        pager.animateScrollToPage((page + 1).coerceAtMost(pageCount - 1), animationSpec = if (reduceMotion) snap() else cardSpring)
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .graphicsLayer {
                            // Cards move like a deck: the leaving card tips back and the incoming card tips up, growing
                            // and brightening as it arrives.
                            val signed = ((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                            val offset = signed.absoluteValue
                            alpha = lerp(1f, 0.35f, offset)
                            if (!reduceMotion) {
                                val scale = lerp(1f, 0.9f, offset)
                                scaleX = scale
                                scaleY = scale
                                rotationX = signed * CARD_TILT_DEGREES
                                cameraDistance = 12f * density
                            }
                        },
                ) {
                    val active = pager.settledPage == page
                    val position = stringResource(R.string.learn_card_position, page + 1, cards.size)
                    when (val card = cards.getOrNull(page)) {
                        is LearnCard -> LearnCardPage(
                            card = card,
                            language = language,
                            active = active,
                            position = position,
                            onNext = toNext,
                            actions = {
                                speaker(card, page)
                                cardActions(card)
                            },
                            highlight = highlightOn(card),
                        )
                        is YourChartCard -> YourChartCardPage(
                            card = card,
                            text = card.text(language, facts.valueOf(card.fact)),
                            language = language,
                            position = position,
                            onNext = toNext,
                            actions = {
                                speaker(card, page)
                                cardActions(card)
                            },
                            highlight = highlightOn(card),
                        )
                        is QuizCard -> QuizCardPage(
                            quiz = card,
                            language = language,
                            chosen = answers[page],
                            position = position,
                            onChoose = { choice ->
                                answers[page] = choice
                                // Every quiz answered joins the daily Review: missed ones come back tomorrow (LS-5).
                                onProgress { recordQuiz(card.id, card.isCorrect(choice), LocalDate.now().toEpochDay()) }
                            },
                            onNext = toNext,
                            actions = { speaker(card, page) },
                        )
                        null -> LessonDonePage(
                            chapter = chapter,
                            lesson = lesson,
                            language = language,
                            right = right,
                            total = lesson.quizzes.size,
                            active = active,
                            next = nextLesson(progress.completeLesson(lesson.id)),
                            onNext = onOpenLesson,
                            onClose = onClose,
                            chapterDone = progress.completeLesson(lesson.id).let { done -> chapter.lessons.all { it.id in done.completedLessons } },
                            passed = passed,
                            unlocked = unlocked,
                            starsEarned = starsEarned,
                            onRetry = retry,
                        )
                    }
                }
            }
            overlay(cards.getOrNull(pager.settledPage))
        }
    }
}

private const val DONE_KEY = "lesson-done"
private const val CARD_TILT_DEGREES = 10f
private val cardSpring = spring<Float>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
