// Daily Review (LS-5): spaced retrieval in simple Leitner boxes. Quiz answers from lessons and saved cards come back
// after 1, 3, 7, 16 and 35 days; a right answer moves a card up a box, a miss sends it back to box 1.
package com.astrolingo.learn.review

import com.astrolingo.learn.content.CardLocation
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.progress.LearnProgress

/**
 * A card waiting in Review.
 *
 * @property box 1 to [Leitner.BOXES]; higher boxes come back less often.
 * @property dueDay the epoch day, on the phone's clock, from which it is due.
 */
data class ReviewItem(val cardId: String, val box: Int, val dueDay: Long)

object Leitner {
    /** Days until a card in box 1, 2, 3, 4 or 5 is due again. */
    val INTERVAL_DAYS = listOf(1, 3, 7, 16, 35)

    val BOXES: Int get() = INTERVAL_DAYS.size

    /** At most this many cards a day, so Review stays a two-minute habit. */
    const val DAILY_CAP = 5

    fun item(cardId: String, box: Int, today: Long): ReviewItem = ReviewItem(cardId, box, today + INTERVAL_DAYS[box - 1])

    /** Where a card goes after a review: up a box when [remembered], back to box 1 otherwise; null when it leaves Review. */
    fun after(item: ReviewItem, remembered: Boolean, today: Long): ReviewItem? = when {
        !remembered -> item(item.cardId, 1, today)
        item.box == BOXES -> null
        else -> item(item.cardId, item.box + 1, today)
    }
}

object ReviewQueue {
    /**
     * Today's Review: due cards, lowest box first, then the longest overdue, up to what is left of [Leitner.DAILY_CAP]
     * today. Cards no longer in the content (renamed by a content update) are left out.
     */
    fun dueToday(progress: LearnProgress, course: Course, today: Long): List<CardLocation> {
        val left = (Leitner.DAILY_CAP - progress.reviewsOn(today)).coerceAtLeast(0)
        return progress.review.values
            .filter { it.dueDay <= today }
            .sortedWith(compareBy<ReviewItem> { it.box }.thenBy { it.dueDay }.thenBy { it.cardId })
            .mapNotNull { course.locate(it.cardId) }
            .take(left)
    }

    /** Days from [today] until the next card is due, or null when Review is empty. 0 means some are due today. */
    fun daysToNext(progress: LearnProgress, today: Long): Long? =
        progress.review.values.minOfOrNull { it.dueDay }?.let { (it - today).coerceAtLeast(0) }
}
