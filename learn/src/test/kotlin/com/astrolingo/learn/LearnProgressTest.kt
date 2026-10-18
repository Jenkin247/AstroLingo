package com.astrolingo.learn

import com.astrolingo.learn.progress.Bookmark
import com.astrolingo.learn.progress.GoalProgress
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.progress.LearnProgressCodec
import com.astrolingo.learn.progress.ResumePoint
import com.astrolingo.learn.progress.StudyTime
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnProgressTest {
    @Test
    fun `bookmarks toggle and list newest first`() {
        val progress = LearnProgress().toggleBookmark("a.l1.1", 100).toggleBookmark("b.l1.1", 200)
        assertThat(progress.bookmarks.map { it.cardId }).containsExactly("b.l1.1", "a.l1.1").inOrder()
        assertThat(progress.isBookmarked("a.l1.1")).isTrue()
        val removed = progress.toggleBookmark("a.l1.1", 300)
        assertThat(removed.isBookmarked("a.l1.1")).isFalse()
        assertThat(removed.bookmarks).containsExactly(Bookmark("b.l1.1", 200))
    }

    @Test
    fun `finishing a lesson clears its resume point but not another lesson's`() {
        assertThat(LearnProgress().leaveLesson("a.l1", 3).completeLesson("a.l1").resume).isNull()
        assertThat(LearnProgress().leaveLesson("a.l2", 3).completeLesson("a.l1").resume).isEqualTo(ResumePoint("a.l2", 3))
    }

    @Test
    fun `leaving a finished lesson again doesn't set a resume point`() {
        assertThat(LearnProgress().completeLesson("a.l1").leaveLesson("a.l1", 2).resume).isNull()
    }

    @Test
    fun `recording the check adds known chapters and remembers it was answered`() {
        val progress = LearnProgress(knownChapters = setOf("a")).recordCheck(setOf("b"))
        assertThat(progress.knownChapters).containsExactly("a", "b")
        assertThat(progress.checkAnswered).isTrue()
        assertThat(LearnProgress().skipCheck().checkAnswered).isTrue()
    }

    @Test
    fun `progress survives encoding and decoding`() {
        val progress = LearnProgress(
            completedLessons = setOf("a.l1", "a.l2"),
            knownChapters = setOf("b"),
            checkAnswered = true,
            bookmarks = listOf(Bookmark("c.l1.2", 1_700_000_000_000), Bookmark("a.l1.1", 1_600_000_000_000)),
            resume = ResumePoint("c.l1", 4),
            studySeconds = mapOf(20_000L to 300, 20_001L to 45),
        )
        assertThat(LearnProgressCodec.decode(LearnProgressCodec.encode(progress))).isEqualTo(progress)
    }

    @Test
    fun `empty progress survives encoding and decoding`() {
        assertThat(LearnProgressCodec.decode(LearnProgressCodec.encode(LearnProgress()))).isEqualTo(LearnProgress())
    }

    @Test
    fun `nothing saved or unreadable values give empty progress`() {
        assertThat(LearnProgressCodec.decode(emptyMap())).isEqualTo(LearnProgress())
        assertThat(LearnProgressCodec.decode(mapOf("version" to "1", "bookmarks" to "no-tab-here"))).isEqualTo(LearnProgress())
        assertThat(LearnProgressCodec.decode(mapOf("version" to "99"))).isEqualTo(LearnProgress())
    }

    @Test
    fun `version 1 progress is read without losing anything`() {
        val v1 = mapOf(
            "version" to "1",
            "completed_lessons" to "a.l1",
            "known_chapters" to "b",
            "check_answered" to "true",
            "bookmarks" to "a.l1.1\t100",
            "resume" to "a.l2\t3",
        )
        assertThat(LearnProgressCodec.decode(v1)).isEqualTo(
            LearnProgress(
                completedLessons = setOf("a.l1"),
                knownChapters = setOf("b"),
                checkAnswered = true,
                bookmarks = listOf(Bookmark("a.l1.1", 100)),
                resume = ResumePoint("a.l2", 3),
            ),
        )
    }

    @Test
    fun `study time adds up per day`() {
        val progress = LearnProgress().addStudyTime(100, 60).addStudyTime(100, 30).addStudyTime(101, 10)
        assertThat(progress.secondsOn(100)).isEqualTo(90)
        assertThat(progress.secondsOn(101)).isEqualTo(10)
        assertThat(progress.secondsOn(99)).isEqualTo(0)
    }

    @Test
    fun `nothing is added for no time`() {
        assertThat(LearnProgress().addStudyTime(100, 0)).isEqualTo(LearnProgress())
    }

    @Test
    fun `only the last 30 days are kept`() {
        var progress = LearnProgress()
        (1L..40L).forEach { progress = progress.addStudyTime(it, 60) }
        assertThat(progress.studySeconds.keys).containsExactlyElementsIn(11L..40L)
    }

    @Test
    fun `a card counts at most two minutes`() {
        assertThat(StudyTime.credit(45_900)).isEqualTo(45)
        assertThat(StudyTime.credit(10 * 60_000)).isEqualTo(120)
        assertThat(StudyTime.credit(-5)).isEqualTo(0)
    }

    @Test
    fun `the goal ring fills and is met at the goal`() {
        assertThat(GoalProgress(10, 240).fraction).isWithin(0.001f).of(0.4f)
        assertThat(GoalProgress(10, 240).minutes).isEqualTo(4)
        assertThat(GoalProgress(10, 240).met).isFalse()
        assertThat(GoalProgress(10, 600).met).isTrue()
        assertThat(GoalProgress(10, 900).fraction).isEqualTo(1f)
    }
}
