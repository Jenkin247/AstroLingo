// Learn tab root: loads the course and the learner's progress, then shows the path or a lesson.
// Logic lives in the pure-Kotlin learn/ build (ADR 0011); this package is only the Compose UI.
package com.astrolingo.app.learn

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.astrolingo.app.R
import com.astrolingo.app.ui.kundli.displayName
import com.astrolingo.app.ui.theme.Spacing
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.learn.content.ChartFact
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.CourseLoader
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.path.CoursePath
import com.astrolingo.learn.progress.GoalProgress
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.review.ReviewQueue
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The bundled course, parsed once per process. */
internal object BundledCourse {
    val course: Course by lazy { CourseLoader.bundled() }
}

/** Something another tab asks Learn to open. */
sealed interface LearnRequest {
    /** Today's Review, from the card on Today. */
    data object Review : LearnRequest

    /** The lesson behind a part of the daily card, from a "Learn why" link on Today (LS-6). */
    data class Lesson(val id: String) : LearnRequest
}

/** Where the Learn tab is. Saved as a short string so it survives rotation. */
internal sealed interface LearnRoute {
    data object Home : LearnRoute

    /** A lesson opened at [start], the card index. */
    data class Lesson(val id: String, val start: Int) : LearnRoute

    /** The saved cards. */
    data object Saved : LearnRoute

    /** The knowledge check, for [chapters] or, when null, for chapters the learner picks. */
    data class Check(val chapters: List<String>?) : LearnRoute

    /** Today's Review, with its [cards] fixed when it opened so answering doesn't reshuffle them. */
    data class Review(val cards: List<String>) : LearnRoute

    fun encode(): String = when (this) {
        Home -> HOME
        is Lesson -> "$LESSON|$id|$start"
        Saved -> SAVED
        is Check -> listOfNotNull(CHECK, chapters?.joinToString(",")).joinToString("|")
        is Review -> "$REVIEW|${cards.joinToString(",")}"
    }

    companion object {
        private const val HOME = "home"
        private const val LESSON = "lesson"
        private const val SAVED = "saved"
        private const val CHECK = "check"
        private const val REVIEW = "review"

        fun decode(value: String): LearnRoute {
            val parts = value.split('|')
            return when (parts.first()) {
                LESSON -> Lesson(parts[1], parts[2].toInt())
                SAVED -> Saved
                CHECK -> Check(parts.getOrNull(1)?.split(','))
                REVIEW -> Review(parts.getOrNull(1)?.split(',')?.filter { it.isNotEmpty() }.orEmpty())
                else -> Home
            }
        }
    }
}

/**
 * The Learn tab.
 *
 * @param kundli the user's chart, used only on screen to fill "your chart" cards.
 * @param timeKnown false when the birth time is unknown; cards that need the lagna then say so.
 * @param store where progress and bookmarks are kept.
 * @param knowsSome true when the learner said in onboarding they know some astrology; the knowledge check then
 *   opens the first time they visit Learn. Others are offered it on Learn home.
 * @param goalMinutes the daily goal chosen in onboarding, shown as a ring on Learn home; null shows no ring.
 * @param request something another tab asked Learn to open; [onRequestHandled] is called once it is open.
 * @param onReturn called when a lesson opened from Today is closed, so the app can go back there.
 * @param onImmersive called with true while a lesson, the check or the saved cards fill the screen, so the app can
 *   hide its bottom tabs (LS-3), and with false back on Learn home.
 * @param people the learner's chart and saved people's, for the chart bubble on cards (LS-9).
 * @param onAddPerson opens Me's "Add a person" form, from the chart bubble's sheet.
 */
@Composable
fun LearnTab(
    kundli: Kundli,
    timeKnown: Boolean,
    store: LearnProgressStore,
    modifier: Modifier = Modifier,
    knowsSome: Boolean = false,
    goalMinutes: Int? = null,
    request: LearnRequest? = null,
    onRequestHandled: () -> Unit = {},
    onReturn: () -> Unit = {},
    onImmersive: (Boolean) -> Unit = {},
    people: List<ChartPerson> = emptyList(),
    onAddPerson: () -> Unit = {},
) {
    val course by produceState<Course?>(null) { value = withContext(Dispatchers.IO) { BundledCourse.course } }
    var progress by remember(store) { mutableStateOf<LearnProgress?>(null) }
    LaunchedEffect(store) {
        progress = withContext(Dispatchers.IO) { runCatching { store.load() }.getOrDefault(LearnProgress()) }
    }
    var routeValue by rememberSaveable { mutableStateOf(LearnRoute.Home.encode()) }
    val loadedCourse = course
    val loadedProgress = progress
    if (loadedCourse == null || loadedProgress == null) {
        // Loading takes a moment once per launch; an empty screen avoids a flash of spinner.
        Box(modifier.fillMaxSize())
        return
    }
    val update: (LearnProgress) -> Unit = { changed ->
        progress = changed
        store.save(changed)
    }
    // Applies a change to the progress as it is now, so two changes in the same moment both count.
    val change: (LearnProgress.() -> LearnProgress) -> Unit = { edit -> progress?.let { update(it.edit()) } }
    val language = Language.fromTag(LocalConfiguration.current.locales[0].language)
    val facts = rememberChartFacts(kundli, timeKnown)
    val readAloud = rememberReadAloud()
    val open: (LearnRoute) -> Unit = { routeValue = it.encode() }

    // First visit for someone who knows some astrology: find what to skip before showing the path.
    LaunchedEffect(Unit) {
        if (knowsSome && !loadedProgress.checkAnswered && LearnRoute.decode(routeValue) == LearnRoute.Home) {
            open(LearnRoute.Check(null))
        }
    }

    val today = LocalDate.now().toEpochDay()
    val openReview = { open(LearnRoute.Review(ReviewQueue.dueToday(loadedProgress, loadedCourse, today).map { it.card.id })) }
    // True while a lesson opened from Today is on screen: closing it goes back to Today, not Learn home.
    var fromToday by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(request) {
        when (request) {
            LearnRequest.Review -> openReview()
            is LearnRequest.Lesson -> {
                fromToday = true
                open(LearnRoute.Lesson(request.id, 0))
            }
            null -> return@LaunchedEffect
        }
        onRequestHandled()
    }
    val closeLesson = {
        open(LearnRoute.Home)
        if (fromToday) {
            fromToday = false
            onReturn()
        }
    }

    val immersive = LearnRoute.decode(routeValue) != LearnRoute.Home
    LaunchedEffect(immersive) { onImmersive(immersive) }
    // Leaving the Learn tab, or deleting the profile, always brings the tabs back.
    DisposableEffect(Unit) { onDispose { onImmersive(false) } }

    when (val route = LearnRoute.decode(routeValue)) {
        LearnRoute.Home -> LearnHome(
            course = loadedCourse,
            path = CoursePath.of(loadedCourse, loadedProgress),
            language = language,
            onOpenLesson = { lesson, start -> open(LearnRoute.Lesson(lesson.id, start)) },
            completedLessons = loadedProgress.completedLessons,
            modifier = modifier,
            actions = { SavedButton(onClick = { open(LearnRoute.Saved) }) },
            banner = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    goalMinutes?.let { minutes -> GoalCard(GoalProgress(minutes, loadedProgress.secondsOn(today))) }
                    val due = ReviewQueue.dueToday(loadedProgress, loadedCourse, today).size
                    if (due > 0) ReviewOffer(due = due, onStart = openReview)
                    if (!loadedProgress.checkAnswered && !knowsSome) {
                        CheckOffer(onTake = { open(LearnRoute.Check(null)) }, onLater = { update(loadedProgress.skipCheck()) })
                    }
                }
            },
            chapterActions = { chapter, dismiss ->
                if (chapter.id !in loadedProgress.knownChapters) {
                    KnowThisButton {
                        dismiss()
                        open(LearnRoute.Check(listOf(chapter.id)))
                    }
                }
            },
        )
        is LearnRoute.Review -> {
            BackHandler { open(LearnRoute.Home) }
            ReviewScreen(
                cards = route.cards.mapNotNull(loadedCourse::locate),
                language = language,
                facts = facts,
                onProgress = change,
                daysToNext = { progress?.let { ReviewQueue.daysToNext(it, LocalDate.now().toEpochDay()) } },
                onClose = { open(LearnRoute.Home) },
                modifier = modifier,
            )
        }
        is LearnRoute.Check -> {
            BackHandler { open(LearnRoute.Home) }
            KnowledgeCheckScreen(
                course = loadedCourse,
                chapterIds = route.chapters,
                language = language,
                onDone = { passed ->
                    update(loadedProgress.recordCheck(passed))
                    open(LearnRoute.Home)
                },
                onSkip = {
                    update(loadedProgress.skipCheck())
                    open(LearnRoute.Home)
                },
                modifier = modifier,
            )
        }
        LearnRoute.Saved -> {
            BackHandler { open(LearnRoute.Home) }
            SavedCards(
                course = loadedCourse,
                progress = loadedProgress,
                language = language,
                facts = facts,
                onOpen = { location -> open(LearnRoute.Lesson(location.lesson.id, location.index)) },
                onToggle = { cardId -> change { toggleBookmark(cardId, System.currentTimeMillis(), LocalDate.now().toEpochDay()) } },
                onBack = { open(LearnRoute.Home) },
                modifier = modifier,
            )
        }
        is LearnRoute.Lesson -> {
            val lesson = loadedCourse.lesson(route.id)
            val chapter = loadedCourse.chapterOf(route.id)
            if (lesson == null || chapter == null) {
                // A saved route to a lesson that no longer exists (content changed): go back to the path.
                LaunchedEffect(route) { open(LearnRoute.Home) }
                return
            }
            BackHandler(onBack = closeLesson)
            // A new lesson gets a fresh pager and quiz answers.
            key(route.id) {
                // Fixed when the lesson opens: a lesson peeked at from Today doesn't become "Continue".
                val keepResume = remember { !fromToday }
                LessonPlayer(
                    chapter = chapter,
                    lesson = lesson,
                    startIndex = route.start,
                    language = language,
                    facts = facts,
                    progress = loadedProgress,
                    onProgress = change,
                    onClose = closeLesson,
                    onOpenLesson = { next ->
                        // Going on to the next lesson means the learner stays in Learn.
                        fromToday = false
                        open(LearnRoute.Lesson(next.id, 0))
                    },
                    nextLesson = { done -> CoursePath.of(loadedCourse, done).next },
                    keepResume = keepResume,
                    readAloud = readAloud,
                    overlay = { card -> ChartLensOverlay(card, people, loadedCourse, loadedProgress, language, onAddPerson) },
                    modifier = modifier,
                    cardActions = { card ->
                        BookmarkButton(saved = loadedProgress.isBookmarked(card.id)) {
                            change { toggleBookmark(card.id, System.currentTimeMillis(), LocalDate.now().toEpochDay()) }
                        }
                    },
                )
            }
        }
    }
}

/**
 * Turns a [ChartFact] into the words shown on a "your chart" card, such as "Simha (Leo)".
 * Returns null for facts that need the lagna when the birth time is unknown.
 */
internal fun interface ChartFacts {
    fun valueOf(fact: ChartFact): String?
}

@Composable
internal fun rememberChartFacts(kundli: Kundli, timeKnown: Boolean): ChartFacts {
    val withEnglish = stringResource(R.string.rashi_with_english)
    val padaFormat = stringResource(R.string.learn_nakshatra_pada)
    return remember(kundli, timeKnown, withEnglish, padaFormat) {
        fun rashi(rashi: Rashi) = String.format(withEnglish, rashi.displayName(), rashi.englishName)
        fun graha(graha: Graha) = String.format(withEnglish, graha.displayName(), graha.englishName)
        ChartFacts { fact ->
            if (fact.needsBirthTime && !timeKnown) return@ChartFacts null
            when (fact) {
                ChartFact.SUN_RASHI -> rashi(kundli.of(Graha.SURYA).rashi)
                ChartFact.MOON_RASHI -> rashi(kundli.moonSign)
                ChartFact.MOON_NAKSHATRA -> String.format(padaFormat, kundli.birthNakshatra.nakshatra.displayName(), kundli.birthNakshatra.pada)
                ChartFact.MOON_NAKSHATRA_LORD -> graha(kundli.birthNakshatra.nakshatra.lord)
                ChartFact.LAGNA -> rashi(kundli.lagna)
                ChartFact.LAGNA_LORD -> graha(kundli.lagna.lord)
                ChartFact.SUN_HOUSE -> kundli.of(Graha.SURYA).house.toString()
                ChartFact.MOON_HOUSE -> kundli.of(Graha.CHANDRA).house.toString()
                ChartFact.GURU_HOUSE -> kundli.of(Graha.GURU).house.toString()
                ChartFact.SHANI_HOUSE -> kundli.of(Graha.SHANI).house.toString()
            }
        }
    }
}

/** True when the phone's "Remove animations" setting is on; motion is then replaced by a fade (ADR 0007). */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
