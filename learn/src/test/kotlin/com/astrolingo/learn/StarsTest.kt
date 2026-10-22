package com.astrolingo.learn

import com.astrolingo.learn.path.QuizPass
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.progress.LearnProgressCodec
import com.astrolingo.learn.stars.Stars
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StarsTest {
    private val day = 20_000L

    @Test
    fun `two of three right passes a lesson, one of three doesn't`() {
        assertThat(QuizPass.needed(3)).isEqualTo(2)
        assertThat(QuizPass.passed(2, 3)).isTrue()
        assertThat(QuizPass.passed(1, 3)).isFalse()
        assertThat(QuizPass.passed(0, 0)).isTrue()
    }

    @Test
    fun `the first visit of the day earns 2 Stars, later visits that day none`() {
        val visited = LearnProgress().recordVisit(day).recordVisit(day)
        assertThat(visited.stars.total).isEqualTo(2)
        assertThat(visited.recordVisit(day + 1).stars.total).isEqualTo(4)
    }

    @Test
    fun `passing a lesson earns 3 Stars, 4 when perfect, and only the first time`() {
        val passed = LearnProgress().passLesson("a.l1", perfect = false, epochDay = day)
        assertThat(passed.stars.total).isEqualTo(3)
        assertThat(passed.completedLessons).contains("a.l1")
        assertThat(passed.passLesson("a.l1", perfect = true, epochDay = day).stars.total).isEqualTo(3)
        assertThat(LearnProgress().passLesson("a.l2", perfect = true, epochDay = day).stars.total).isEqualTo(4)
    }

    @Test
    fun `starsForPassing shows what a pass will earn before it is recorded`() {
        val progress = LearnProgress().passLesson("a.l1", perfect = false, epochDay = day)
        assertThat(progress.starsForPassing("a.l2", perfect = true, epochDay = day)).isEqualTo(4)
        assertThat(progress.starsForPassing("a.l1", perfect = true, epochDay = day)).isEqualTo(0)
    }

    @Test
    fun `no more than 9 Stars a day, and a new day starts again`() {
        val busy = (1..5).fold(LearnProgress().recordVisit(day)) { p, n -> p.passLesson("l$n", perfect = true, epochDay = day) }
        assertThat(busy.stars.total).isEqualTo(Stars.DAILY_MAX)
        assertThat(busy.starsForPassing("l9", perfect = true, epochDay = day)).isEqualTo(0)
        assertThat(busy.passLesson("l6", perfect = false, epochDay = day + 1).stars.total).isEqualTo(Stars.DAILY_MAX + 3)
    }

    @Test
    fun `a finished Review earns 1 Star once a day`() {
        val reviewed = LearnProgress().finishReview(day).finishReview(day)
        assertThat(reviewed.stars.total).isEqualTo(1)
    }

    @Test
    fun `Stars survive saving and loading`() {
        val progress = LearnProgress().recordVisit(day).passLesson("a.l1", perfect = true, epochDay = day).finishReview(day)
        assertThat(LearnProgressCodec.decode(LearnProgressCodec.encode(progress))).isEqualTo(progress)
        val none = LearnProgress()
        assertThat(LearnProgressCodec.decode(LearnProgressCodec.encode(none))).isEqualTo(none)
    }

    @Test
    fun `progress saved before Stars existed loads with none`() {
        val old = mapOf("version" to "2", "completed_lessons" to "a.l1")
        assertThat(LearnProgressCodec.decode(old).stars).isEqualTo(Stars())
        assertThat(LearnProgressCodec.decode(old).completedLessons).containsExactly("a.l1")
    }
}
