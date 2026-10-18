// What the learner has done in Learn: lessons finished, chapters skipped by the knowledge check, saved cards
// and where to resume. Not birth data, but kept on the phone only and wiped with "Delete my details".
package com.astrolingo.learn.progress

import com.astrolingo.learn.review.Leitner
import com.astrolingo.learn.review.ReviewItem

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
 * @property review cards waiting in the daily Review, by card id.
 * @property reviewDay with [reviewCount]: how many Review cards were answered on that epoch day, for the daily cap.
 */
data class LearnProgress(
    val completedLessons: Set<String> = emptySet(),
    val knownChapters: Set<String> = emptySet(),
    val checkAnswered: Boolean = false,
    val bookmarks: List<Bookmark> = emptyList(),
    val resume: ResumePoint? = null,
    val studySeconds: Map<Long, Int> = emptyMap(),
    val review: Map<String, ReviewItem> = emptyMap(),
    val reviewDay: Long? = null,
    val reviewCount: Int = 0,
) {
    fun isBookmarked(cardId: String): Boolean = bookmarks.any { it.cardId == cardId }

    /**
     * Saves the card, or removes it when it is already saved. With [epochDay], a saved card also joins Review in
     * box 1 and an unsaved one leaves it (only teaching cards can be saved, so this never drops a missed quiz).
     */
    fun toggleBookmark(cardId: String, nowMillis: Long, epochDay: Long? = null): LearnProgress {
        val saving = !isBookmarked(cardId)
        val toggled = copy(
            bookmarks = if (saving) listOf(Bookmark(cardId, nowMillis)) + bookmarks else bookmarks.filterNot { it.cardId == cardId },
        )
        return when {
            epochDay == null -> toggled
            saving -> toggled.copy(review = review + (cardId to (review[cardId] ?: Leitner.item(cardId, 1, epochDay))))
            else -> toggled.copy(review = review - cardId)
        }
    }

    /**
     * A quiz answered in a lesson joins Review: box 1 when missed, box 2 when right. A card already in Review keeps
     * its place, so going through a lesson again doesn't reset it.
     */
    fun recordQuiz(cardId: String, correct: Boolean, epochDay: Long): LearnProgress =
        if (cardId in review) this else copy(review = review + (cardId to Leitner.item(cardId, if (correct) 2 else 1, epochDay)))

    /** Review cards answered on [epochDay]. */
    fun reviewsOn(epochDay: Long): Int = if (reviewDay == epochDay) reviewCount else 0

    /** Moves a card after a review answer and counts it toward today's cap. */
    fun answerReview(cardId: String, remembered: Boolean, epochDay: Long): LearnProgress {
        val item = review[cardId] ?: return this
        val moved = Leitner.after(item, remembered, epochDay)
        return copy(
            review = if (moved == null) review - cardId else review + (cardId to moved),
            reviewDay = epochDay,
            reviewCount = reviewsOn(epochDay) + 1,
        )
    }

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
