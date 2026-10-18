package com.astrolingo.learn

import com.astrolingo.learn.progress.Bookmark
import com.astrolingo.learn.progress.LearnProgress
import com.astrolingo.learn.progress.LearnProgressCodec
import com.astrolingo.learn.progress.ResumePoint
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
}
