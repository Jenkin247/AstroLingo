// What the learner has done in Learn: lessons finished, chapters skipped by the knowledge check, saved cards
// and where to resume. Not birth data, but kept on the phone only and wiped with "Delete my details".
package com.astrolingo.learn.progress

/**
 * A saved card.
 *
 * @property cardId the card's stable id from the content.
 * @property savedAtMillis when it was saved, epoch milliseconds; the Saved list shows newest first.
 */
data class Bookmark(val cardId: String, val savedAtMillis: Long)

/**
 * Where the learner left a lesson, so "Continue" opens the same card.
 *
 * @property cardIndex 0-based position of the card that was on screen.
 */
data class ResumePoint(val lessonId: String, val cardIndex: Int)

/**
 * Learning progress. Immutable: every change returns a new copy for the UI to show and the store to save.
 *
 * @property completedLessons ids of lessons finished to the end.
 * @property knownChapters ids of chapters the learner showed they know in the knowledge check; skipped on the path.
 * @property checkAnswered true once the learner has taken or declined the first-time knowledge check.
 * @property bookmarks saved cards, newest first.
 * @property resume the lesson left part-way, if any.
 * @property studySeconds learning time per day, keyed by epoch day on the phone's clock; only the last
 *   [StudyTime.DAYS_KEPT] days are kept.
 */
data class LearnProgress(
    val completedLessons: Set<String> = emptySet(),
    val knownChapters: Set<String> = emptySet(),
    val checkAnswered: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val resume: ResumePoint? = null,
    val studySeconds: Map<Long, Int> = emptyMap(),
) {
    fun isBookmarked(cardId: String): Boolean = bookmarks.any { it.cardId == cardId }

    /** Saves the card, or removes it when it is already saved. */
    fun toggleBookmark(cardId: String, nowMillis: Long): LearnProgress = copy(
        bookmarks = if (isBookmarked(cardId)) {
            bookmarks.filterNot { it.cardId == cardId }
        } else {
            listOf(Bookmark(cardId, nowMillis)) + bookmarks
        },
    )

    /** Marks a lesson finished and clears its resume point. */
    fun completeLesson(lessonId: String): LearnProgress =
        copy(completedLessons = completedLessons + lessonId, resume = resume?.takeIf { it.lessonId != lessonId })

    /** Remembers the card on screen in an unfinished lesson. */
    fun leaveLesson(lessonId: String, cardIndex: Int): LearnProgress =
        if (lessonId in completedLessons) this else copy(resume = ResumePoint(lessonId, cardIndex.coerceAtLeast(0)))

    /** Records the outcome of a knowledge check: [passed] chapters are skipped from now on. */
    fun recordCheck(passed: Set<String>): LearnProgress = copy(knownChapters = knownChapters + passed, checkAnswered = true)

    /** The learner declined the first-time check; they can still use "I know this" on any chapter. */
    fun skipCheck(): LearnProgress = copy(checkAnswered = true)

    /** Learning time on [epochDay]. */
    fun secondsOn(epochDay: Long): Int = studySeconds[epochDay] ?: 0

    /** Adds [seconds] of learning to [epochDay] and drops days older than [StudyTime.DAYS_KEPT]. */
    fun addStudyTime(epochDay: Long, seconds: Int): LearnProgress {
        if (seconds <= 0) return this
        val updated = studySeconds + (epochDay to secondsOn(epochDay) + seconds)
        return copy(studySeconds = updated.filterKeys { it > epochDay - StudyTime.DAYS_KEPT })
    }
}
