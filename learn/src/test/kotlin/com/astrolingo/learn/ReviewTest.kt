package com.astrolingo.learn

import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.progress.LearnProgressCodec
import com.astrolingo.learn.review.Leitner
import com.astrolingo.learn.review.ReviewItem
import com.astrolingo.learn.review.ReviewQueue
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReviewTest {
    private val course = Fixtures.course
    private val today = 20_000L

    @Test
    fun `a missed quiz joins box 1 and a right one box 2`() {
        val progress = LearnProgress().recordQuiz("a.l1.2", correct = false, today).recordQuiz("a.l2.2", correct = true, today)
        assertThat(progress.review["a.l1.2"]).isEqualTo(ReviewItem("a.l1.2", 1, today + 1))
        assertThat(progress.review["a.l2.2"]).isEqualTo(ReviewItem("a.l2.2", 2, today + 3))
    }

    @Test
    fun `answering a quiz again in a lesson keeps its review place`() {
        val progress = LearnProgress().recordQuiz("a.l1.2", correct = true, today).recordQuiz("a.l1.2", correct = false, today + 5)
        assertThat(progress.review["a.l1.2"]).isEqualTo(ReviewItem("a.l1.2", 2, today + 3))
    }

    @Test
    fun `a saved card joins box 1 and leaves when unsaved`() {
        val saved = LearnProgress().toggleBookmark("a.l1.1", 100, today)
        assertThat(saved.review["a.l1.1"]).isEqualTo(ReviewItem("a.l1.1", 1, today + 1))
        assertThat(saved.toggleBookmark("a.l1.1", 200, today).review).isEmpty()
    }

    @Test
    fun `saving without a day leaves Review alone`() {
        assertThat(LearnProgress().toggleBookmark("a.l1.1", 100).review).isEmpty()
    }

    @Test
    fun `remembered moves up a box with the longer wait`() {
        var item = ReviewItem("x", 1, today)
        val waits = mutableListOf<Long>()
        repeat(Leitner.BOXES - 1) {
            item = Leitner.after(item, remembered = true, today)!!
            waits += item.dueDay - today
        }
        assertThat(item.box).isEqualTo(5)
        assertThat(waits).containsExactly(3L, 7L, 16L, 35L).inOrder()
    }

    @Test
    fun `remembered in box 5 leaves Review`() {
        assertThat(Leitner.after(ReviewItem("x", 5, today), remembered = true, today)).isNull()
    }

    @Test
    fun `forgotten goes back to box 1`() {
        assertThat(Leitner.after(ReviewItem("x", 4, today), remembered = false, today)).isEqualTo(ReviewItem("x", 1, today + 1))
    }

    @Test
    fun `due cards come lowest box first, then longest overdue, and not before they are due`() {
        val progress = LearnProgress(
            review = listOf(
                ReviewItem("a.l1.2", 3, today - 1),
                ReviewItem("a.l2.2", 1, today),
                ReviewItem("b.l1.2", 1, today - 4),
                ReviewItem("b.l2.2", 2, today + 1),
            ).associateBy { it.cardId },
        )
        assertThat(ReviewQueue.dueToday(progress, course, today).map { it.card.id })
            .containsExactly("b.l1.2", "a.l2.2", "a.l1.2").inOrder()
    }

    @Test
    fun `at most 5 a day, counting those already answered today`() {
        val ids = course.chapters.flatMap { c -> c.lessons.map { "${it.id}.2" } }
        val progress = LearnProgress(review = ids.associateWith { ReviewItem(it, 1, today) })
        assertThat(ids.size).isGreaterThan(5)
        assertThat(ReviewQueue.dueToday(progress, course, today)).hasSize(5)
        val answered = progress.answerReview(ids[0], remembered = true, today).answerReview(ids[1], remembered = false, today)
        assertThat(answered.reviewsOn(today)).isEqualTo(2)
        assertThat(ReviewQueue.dueToday(answered, course, today)).hasSize(3)
        assertThat(answered.reviewsOn(today + 1)).isEqualTo(0)
    }

    @Test
    fun `cards no longer in the content are left out`() {
        val progress = LearnProgress(review = mapOf("gone.card" to ReviewItem("gone.card", 1, today)))
        assertThat(ReviewQueue.dueToday(progress, course, today)).isEmpty()
    }

    @Test
    fun `days to the next review`() {
        val progress = LearnProgress(review = mapOf("a.l1.2" to ReviewItem("a.l1.2", 2, today + 3)))
        assertThat(ReviewQueue.daysToNext(progress, today)).isEqualTo(3)
        assertThat(ReviewQueue.daysToNext(LearnProgress(), today)).isNull()
    }

    @Test
    fun `over seventy days of daily review a card always remembered leaves after five reviews`() {
        var progress = LearnProgress().recordQuiz("a.l1.2", correct = false, today)
        var reviews = 0
        for (day in today..today + 70) {
            ReviewQueue.dueToday(progress, course, day).forEach {
                progress = progress.answerReview(it.card.id, remembered = true, day)
                reviews++
            }
        }
        assertThat(reviews).isEqualTo(5)
        assertThat(progress.review).isEmpty()
    }

    @Test
    fun `review survives encoding and decoding`() {
        val progress = LearnProgress(
            review = mapOf("a.l1.2" to ReviewItem("a.l1.2", 3, today + 7), "a.l1.1" to ReviewItem("a.l1.1", 1, today)),
            reviewDay = today,
            reviewCount = 2,
        )
        assertThat(LearnProgressCodec.decode(LearnProgressCodec.encode(progress))).isEqualTo(progress)
    }
}
