// Learn tab root: loads the course and the learner's progress, then shows the path or a lesson.
// Logic lives in the pure-Kotlin learn/ build (ADR 0011); this package is only the Compose UI.
package com.astrolingo.app.learn

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.astrolingo.engine.jyotish.Graha
import com.astrolingo.engine.jyotish.Kundli
import com.astrolingo.engine.jyotish.Rashi
import com.astrolingo.learn.content.ChartFact
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.CourseLoader
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.path.CoursePath
import com.astrolingo.learn.progress.LearnProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The bundled course, parsed once per process. */
internal object BundledCourse {
    val course: Course by lazy { CourseLoader.bundled() }
}

/** Where the Learn tab is. Saved as a short string so it survives rotation. */
internal sealed interface LearnRoute {
    data object Home : LearnRoute

    /** A lesson opened at [start], the card index. */
    data class Lesson(val id: String, val start: Int) : LearnRoute

    fun encode(): String = when (this) {
        Home -> HOME
        is Lesson -> "$LESSON|$id|$start"
    }

    companion object {
        private const val HOME = "home"
        private const val LESSON = "lesson"

        fun decode(value: String): LearnRoute {
            val parts = value.split('|')
            return when (parts.first()) {
                LESSON -> Lesson(parts[1], parts[2].toInt())
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
 */
@Composable
fun LearnTab(
    kundli: Kundli,
    timeKnown: Boolean,
    store: LearnProgressStore,
    modifier: Modifier = Modifier,
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
    val language = Language.fromTag(LocalConfiguration.current.locales[0].language)
    val facts = rememberChartFacts(kundli, timeKnown)
    val open: (LearnRoute) -> Unit = { routeValue = it.encode() }

    when (val route = LearnRoute.decode(routeValue)) {
        LearnRoute.Home -> LearnHome(
            course = loadedCourse,
            path = CoursePath.of(loadedCourse, loadedProgress),
            language = language,
            onOpenLesson = { lesson, start -> open(LearnRoute.Lesson(lesson.id, start)) },
            completedLessons = loadedProgress.completedLessons,
            modifier = modifier,
        )
        is LearnRoute.Lesson -> {
            val lesson = loadedCourse.lesson(route.id)
            val chapter = loadedCourse.chapterOf(route.id)
            if (lesson == null || chapter == null) {
                // A saved route to a lesson that no longer exists (content changed): go back to the path.
                LaunchedEffect(route) { open(LearnRoute.Home) }
                return
            }
            BackHandler { open(LearnRoute.Home) }
            // A new lesson gets a fresh pager and quiz answers.
            key(route.id) {
                LessonPlayer(
                    chapter = chapter,
                    lesson = lesson,
                    startIndex = route.start,
                    language = language,
                    facts = facts,
                    progress = loadedProgress,
                    onProgress = update,
                    onClose = { open(LearnRoute.Home) },
                    onOpenLesson = { next -> open(LearnRoute.Lesson(next.id, 0)) },
                    nextLesson = { done -> CoursePath.of(loadedCourse, done).next },
                    modifier = modifier,
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
