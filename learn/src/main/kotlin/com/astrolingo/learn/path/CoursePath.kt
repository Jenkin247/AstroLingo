// The learning path: where each chapter stands for this learner and which lesson comes next.
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

    /** Nothing finished yet. */
    NOT_STARTED,
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
 */
data class CoursePath(
    val chapters: List<ChapterProgress>,
    val next: Lesson?,
    val resumeAt: Int,
) {
    /** True when every chapter is done or known. */
    val isComplete: Boolean get() = next == null

    companion object {
        /**
         * Works out the path. The next lesson is the unfinished one the learner left part-way, if any; otherwise
         * the first unfinished lesson, in course order, of a chapter that isn't known.
         */
        fun of(course: Course, progress: LearnProgress): CoursePath {
            val resume = progress.resume?.takeIf { it.lessonId !in progress.completedLessons }
            val resumed = resume?.let { course.lesson(it.lessonId) }
            val next = resumed ?: course.chapters
                .filterNot { it.id in progress.knownChapters }
                .firstNotNullOfOrNull { chapter -> chapter.lessons.firstOrNull { it.id !in progress.completedLessons } }
            val nextChapter = next?.let { course.chapterOf(it.id) }
            val chapters = course.chapters.map { chapter ->
                val done = chapter.lessons.count { it.id in progress.completedLessons }
                val state = when {
                    chapter.id in progress.knownChapters -> ChapterState.KNOWN
                    done == chapter.lessons.size -> ChapterState.DONE
                    done > 0 || chapter.lessons.any { it.id == resume?.lessonId } -> ChapterState.IN_PROGRESS
                    else -> ChapterState.NOT_STARTED
                }
                ChapterProgress(chapter, state, done, isNext = chapter == nextChapter)
            }
            val resumeAt = if (resumed != null) resume.cardIndex.coerceIn(0, resumed.cards.lastIndex) else 0
            return CoursePath(chapters, next, resumeAt)
        }
    }
}
