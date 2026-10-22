// The learning path: where each chapter stands for this learner, which lessons are open and which comes next.
// Lessons open one after another: a lesson opens once the one before it is passed, and a chapter once every chapter
// before it is done or known (PO, 22 Oct 2026).
package com.astrolingo.learn.path

import com.astrolingo.learn.content.Chapter
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.Lesson
import com.astrolingo.learn.progress.LearnProgress

/** How a chapter looks on the path. */
enum class ChapterState {
    /** Skipped: the learner showed they know it. Still open to read. */
    KNOWN,

    /** Every lesson finished. */
    DONE,

    /** Some lessons finished. */
    IN_PROGRESS,

    /** Nothing finished yet, and open. */
    NOT_STARTED,

    /** Not open yet: a chapter before it still has lessons to pass. */
    LOCKED,
}

/**
 * One chapter on the path.
 *
 * @property isNext true for the chapter holding the next lesson.
 */
data class ChapterProgress(
    val chapter: Chapter,
    val state: ChapterState,
    val lessonsDone: Int,
    val isNext: Boolean,
) {
    val lessonsTotal: Int get() = chapter.lessons.size

    /** 0 to 1, for the tile's progress ring. A known chapter shows full. */
    val fraction: Float get() = if (state == ChapterState.KNOWN) 1f else lessonsDone.toFloat() / lessonsTotal
}

/**
 * The path through the course for one learner.
 *
 * @property next the lesson "Continue" opens, or null when everything not skipped is done.
 * @property resumeAt for [next], the card to reopen at (0 for a fresh lesson).
 * @property unlocked ids of the lessons the learner may open: every finished lesson, every lesson of a known chapter,
 *   and the next lesson in order.
 */
data class CoursePath(
    val chapters: List<ChapterProgress>,
    val next: Lesson?,
    val resumeAt: Int,
    val unlocked: Set<String> = emptySet(),
) {
    /** True when every chapter is done or known. */
    val isComplete: Boolean get() = next == null

    fun isUnlocked(lessonId: String): Boolean = lessonId in unlocked

    companion object {
        /**
         * Works out the path. The next lesson is the unfinished one the learner left part-way, if it is open;
         * otherwise the first unfinished, open lesson, in course order, of a chapter that isn't known.
         */
        fun of(course: Course, progress: LearnProgress): CoursePath {
            val unlocked = unlockedLessons(course, progress)
            val resume = progress.resume?.takeIf { it.lessonId !in progress.completedLessons && it.lessonId in unlocked }
            val resumed = resume?.let { course.lesson(it.lessonId) }
            val next = resumed ?: course.chapters
                .filterNot { it.id in progress.knownChapters }
                .firstNotNullOfOrNull { chapter -> chapter.lessons.firstOrNull { it.id !in progress.completedLessons && it.id in unlocked } }
            val nextChapter = next?.let { course.chapterOf(it.id) }
            val chapters = course.chapters.map { chapter ->
                val done = chapter.lessons.count { it.id in progress.completedLessons }
                val state = when {
                    chapter.id in progress.knownChapters -> ChapterState.KNOWN
                    done == chapter.lessons.size -> ChapterState.DONE
                    done > 0 || chapter.lessons.any { it.id == resume?.lessonId } -> ChapterState.IN_PROGRESS
                    chapter.lessons.none { it.id in unlocked } -> ChapterState.LOCKED
                    else -> ChapterState.NOT_STARTED
                }
                ChapterProgress(chapter, state, done, isNext = chapter == nextChapter)
            }
            val resumeAt = if (resumed != null) resume.cardIndex.coerceIn(0, resumed.cards.lastIndex) else 0
            return CoursePath(chapters, next, resumeAt, unlocked)
        }

        /**
         * A chapter is open when every chapter before it is done or known, when it is known itself, or when the
         * learner already finished a lesson in it (progress from before lessons were locked is never taken away).
         * In an open chapter a lesson is open when it is finished or the lesson before it is finished; the first
         * lesson of an open chapter is always open. A known chapter has every lesson open.
         */
        private fun unlockedLessons(course: Course, progress: LearnProgress): Set<String> {
            val done = progress.completedLessons
            val unlocked = mutableSetOf<String>()
            var earlierDone = true
            for (chapter in course.chapters) {
                val known = chapter.id in progress.knownChapters
                if (earlierDone || known || chapter.lessons.any { it.id in done }) {
                    var previousDone = true
                    for (lesson in chapter.lessons) {
                        if (known || previousDone || lesson.id in done) unlocked += lesson.id
                        previousDone = lesson.id in done
                    }
                }
                earlierDone = earlierDone && (known || chapter.lessons.all { it.id in done })
            }
            return unlocked
        }
    }
}
