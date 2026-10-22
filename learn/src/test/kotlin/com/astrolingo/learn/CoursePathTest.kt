package com.astrolingo.learn

import com.astrolingo.learn.path.ChapterState
import com.astrolingo.learn.path.CoursePath
import com.astrolingo.learn.progress.LearnProgress
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CoursePathTest {
    private val course = Fixtures.course

    @Test
    fun `a new learner starts at the first lesson of the first chapter`() {
        val path = CoursePath.of(course, LearnProgress())
        assertThat(path.next?.id).isEqualTo("a.l1")
        assertThat(path.resumeAt).isEqualTo(0)
        assertThat(path.chapters.map { it.state }).containsExactly(ChapterState.NOT_STARTED, ChapterState.LOCKED, ChapterState.LOCKED)
        assertThat(path.chapters.map { it.isNext }).containsExactly(true, false, false).inOrder()
    }

    @Test
    fun `finished lessons move the path on and chapters show their state`() {
        val progress = LearnProgress().completeLesson("a.l1").completeLesson("a.l2").completeLesson("b.l1")
        val path = CoursePath.of(course, progress)
        assertThat(path.next?.id).isEqualTo("b.l2")
        assertThat(path.chapters[0].state).isEqualTo(ChapterState.DONE)
        assertThat(path.chapters[1].state).isEqualTo(ChapterState.IN_PROGRESS)
        assertThat(path.chapters[1].lessonsDone).isEqualTo(1)
        assertThat(path.chapters[1].fraction).isWithin(1e-6f).of(1f / 3)
        assertThat(path.chapters[1].isNext).isTrue()
    }

    @Test
    fun `known chapters are skipped but stay on the path`() {
        val path = CoursePath.of(course, LearnProgress(knownChapters = setOf("a", "b")))
        assertThat(path.next?.id).isEqualTo("c.l1")
        assertThat(path.chapters[0].state).isEqualTo(ChapterState.KNOWN)
        assertThat(path.chapters[0].fraction).isEqualTo(1f)
        assertThat(path.chapters).hasSize(3)
    }

    @Test
    fun `a lesson left part-way is continued at the same card`() {
        val progress = LearnProgress().completeLesson("a.l1").completeLesson("a.l2").completeLesson("b.l1").leaveLesson("b.l2", 1)
        val path = CoursePath.of(course, progress)
        assertThat(path.next?.id).isEqualTo("b.l2")
        assertThat(path.resumeAt).isEqualTo(1)
        assertThat(path.chapters[1].state).isEqualTo(ChapterState.IN_PROGRESS)
    }

    @Test
    fun `a new learner can open only the first lesson`() {
        val path = CoursePath.of(course, LearnProgress())
        assertThat(path.unlocked).containsExactly("a.l1")
        assertThat(path.isUnlocked("a.l2")).isFalse()
    }

    @Test
    fun `passing a lesson opens the next one, and finishing a chapter opens the next chapter`() {
        val one = CoursePath.of(course, LearnProgress().completeLesson("a.l1"))
        assertThat(one.unlocked).containsExactly("a.l1", "a.l2")
        assertThat(one.chapters[1].state).isEqualTo(ChapterState.LOCKED)

        val chapterDone = CoursePath.of(course, LearnProgress().completeLesson("a.l1").completeLesson("a.l2"))
        assertThat(chapterDone.unlocked).containsExactly("a.l1", "a.l2", "b.l1")
        assertThat(chapterDone.chapters.map { it.state })
            .containsExactly(ChapterState.DONE, ChapterState.NOT_STARTED, ChapterState.LOCKED).inOrder()
    }

    @Test
    fun `a chapter known from the check is open in full and counts as done for the next chapter`() {
        val path = CoursePath.of(course, LearnProgress(knownChapters = setOf("a")))
        assertThat(path.unlocked).containsExactly("a.l1", "a.l2", "b.l1")
        assertThat(path.next?.id).isEqualTo("b.l1")
    }

    @Test
    fun `a known chapter later in the course is open, but the chapter after it waits for the earlier ones`() {
        val path = CoursePath.of(course, LearnProgress(knownChapters = setOf("b")))
        assertThat(path.unlocked).containsExactly("a.l1", "b.l1", "b.l2", "b.l3")
        assertThat(path.chapters[2].state).isEqualTo(ChapterState.LOCKED)
        assertThat(path.next?.id).isEqualTo("a.l1")
    }

    @Test
    fun `lessons finished before locking stay open, with the one after them`() {
        // From when every lesson was open: b.l2 finished without chapter a.
        val path = CoursePath.of(course, LearnProgress().completeLesson("b.l2"))
        assertThat(path.unlocked).containsExactly("a.l1", "b.l1", "b.l2", "b.l3")
        assertThat(path.next?.id).isEqualTo("a.l1")
    }

    @Test
    fun `a resume point in a locked lesson is ignored`() {
        val path = CoursePath.of(course, LearnProgress().leaveLesson("c.l2", 1))
        assertThat(path.next?.id).isEqualTo("a.l1")
        assertThat(path.resumeAt).isEqualTo(0)
        assertThat(path.chapters[2].state).isEqualTo(ChapterState.LOCKED)
    }

    @Test
    fun `a resume point beyond the lesson is clamped to its last card`() {
        val path = CoursePath.of(course, LearnProgress().leaveLesson("a.l1", 99))
        assertThat(path.resumeAt).isEqualTo(1)
    }

    @Test
    fun `a resume point for a lesson no longer in the course is ignored`() {
        val path = CoursePath.of(course, LearnProgress().leaveLesson("gone.l1", 3))
        assertThat(path.next?.id).isEqualTo("a.l1")
        assertThat(path.resumeAt).isEqualTo(0)
    }

    @Test
    fun `when everything is done or known the course is complete`() {
        val progress = course.chapters.filter { it.id != "a" }.flatMap { it.lessons }
            .fold(LearnProgress(knownChapters = setOf("a"))) { p, lesson -> p.completeLesson(lesson.id) }
        val path = CoursePath.of(course, progress)
        assertThat(path.isComplete).isTrue()
        assertThat(path.chapters.none { it.isNext }).isTrue()
    }
}
