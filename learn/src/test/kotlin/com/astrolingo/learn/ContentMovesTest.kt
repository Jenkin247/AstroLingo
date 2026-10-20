package com.astrolingo.learn

import com.astrolingo.learn.content.CourseLoader
import com.astrolingo.learn.progress.Bookmark
import com.astrolingo.learn.progress.ContentMoves
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.progress.LearnProgressCodec
import com.astrolingo.learn.progress.ResumePoint
import com.astrolingo.learn.review.ReviewItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContentMovesTest {
    private val course = CourseLoader.bundled()

    @Test
    fun `every move points at a lesson or card that exists`() {
        ContentMoves.LESSONS.values.flatten().forEach { assertThat(course.lesson(it)).isNotNull() }
        ContentMoves.CARDS.values.forEach { assertThat(course.locate(it)).isNotNull() }
        ContentMoves.LESSONS.keys.forEach { assertThat(course.lesson(it)).isNull() }
    }

    @Test
    fun `a learner who finished the old grahas lessons keeps the chapter finished`() {
        val old = LearnProgress(completedLessons = ContentMoves.LESSONS.keys + "rashis.what")
        val moved = ContentMoves.apply(old)
        assertThat(moved.completedLessons).containsAtLeastElementsIn(course.chapter("grahas")!!.lessons.map { it.id })
        assertThat(moved.completedLessons).contains("rashis.what")
        assertThat(moved.completedLessons.none { it in ContentMoves.LESSONS }).isTrue()
    }

    @Test
    fun `saved and review cards follow their card and a stale resume point is dropped`() {
        val old = LearnProgress(
            bookmarks = listOf(Bookmark("grahas.sun-moon.2", 200), Bookmark("rashis.what.1", 100)),
            resume = ResumePoint("grahas.venus-saturn", 3),
            review = listOf(ReviewItem("grahas.nature.8", 2, 10), ReviewItem("grahas.sun-moon.9", 1, 11)).associateBy { it.cardId },
        )
        val moved = ContentMoves.apply(old)
        assertThat(moved.bookmarks).containsExactly(Bookmark("grahas.surya.1", 200), Bookmark("rashis.what.1", 100)).inOrder()
        assertThat(moved.resume).isNull()
        // A card with no successor stays as it is; Review leaves it out because the course no longer has it.
        assertThat(moved.review.keys).containsExactly("grahas.sangraha.8", "grahas.sun-moon.9")
        assertThat(moved.review.getValue("grahas.sangraha.8")).isEqualTo(ReviewItem("grahas.sangraha.8", 2, 10))
    }

    @Test
    fun `progress saved before the move is moved when it is read back`() {
        val saved = LearnProgressCodec.encode(LearnProgress(completedLessons = setOf("grahas.rahu-ketu")))
        assertThat(LearnProgressCodec.decode(saved).completedLessons).containsExactly("grahas.rahu", "grahas.ketu")
    }

    @Test
    fun `progress with nothing to move is returned unchanged`() {
        val progress = LearnProgress(completedLessons = setOf("grahas.surya"), resume = ResumePoint("grahas.guru", 2))
        assertThat(ContentMoves.apply(progress)).isSameInstanceAs(progress)
    }
}
