// A tiny made-up course for logic tests, so they don't change when the Guru edits the real content.
package com.astrolingo.learn

import com.astrolingo.learn.content.Chapter
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.Lesson
import com.astrolingo.learn.content.LocalizedText
import com.astrolingo.learn.content.QuizCard

object Fixtures {
    private fun text(en: String) = LocalizedText(en)

    fun quiz(id: String, answer: Int = 0) = QuizCard(id, text("Q $id"), listOf(text("a"), text("b"), text("c")), answer, text("why"))

    private fun lesson(id: String) = Lesson(id, text(id), 4, listOf(LearnCard("$id.1", text("t"), text("b")), quiz("$id.2", 1)))

    private fun chapter(id: String, lessons: Int) = Chapter(
        schemaVersion = 1,
        contentVersion = 1,
        id = id,
        glyph = "★",
        title = text(id),
        summary = text(id),
        outcome = text("do $id"),
        check = listOf(quiz("$id.check.1", 0), quiz("$id.check.2", 2)),
        lessons = (1..lessons).map { lesson("$id.l$it") },
    )

    /** Three chapters: a (2 lessons), b (3 lessons), c (2 lessons). */
    val course = Course(1, listOf(chapter("a", 2), chapter("b", 3), chapter("c", 2)))
}
