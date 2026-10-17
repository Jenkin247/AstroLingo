package com.astrolingo.learn

import com.astrolingo.learn.check.KnowledgeCheck
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KnowledgeCheckTest {
    private val course = Fixtures.course

    @Test
    fun `questions come chapter by chapter in course order`() {
        val check = KnowledgeCheck.of(course, listOf("c", "a"))
        assertThat(check.questions.map { it.quiz.id }).containsExactly("a.check.1", "a.check.2", "c.check.1", "c.check.2").inOrder()
    }

    @Test
    fun `both answers right marks a chapter known, one wrong keeps it on the path`() {
        // Right answers in the fixture: check.1 is option 0, check.2 is option 2.
        val check = KnowledgeCheck.of(course, listOf("a", "b")).answer(0).answer(2).answer(0).answer(1)
        assertThat(check.isFinished).isTrue()
        assertThat(check.passedChapters()).containsExactly("a")
        assertThat(check.refresherChapters()).containsExactly("b")
    }

    @Test
    fun `stopping early counts unanswered chapters as not known`() {
        val check = KnowledgeCheck.of(course, listOf("a", "b")).answer(0).answer(2).answer(0)
        assertThat(check.isFinished).isFalse()
        assertThat(check.current?.quiz?.id).isEqualTo("b.check.2")
        assertThat(check.passedChapters()).containsExactly("a")
        assertThat(check.refresherChapters()).containsExactly("b")
    }

    @Test
    fun `an empty check passes nothing`() {
        val check = KnowledgeCheck.of(course, emptyList())
        assertThat(check.isFinished).isTrue()
        assertThat(check.passedChapters()).isEmpty()
    }

    @Test(expected = IllegalStateException::class)
    fun `answering after the end is an error`() {
        KnowledgeCheck.of(course, listOf("a")).answer(0).answer(2).answer(0)
    }
}
