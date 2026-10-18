// The knowledge check: a few questions per chapter the learner says they know, so chapters they really know
// are skipped (PO, 17 Oct 2026: "we don't want to waste time of the user").
package com.astrolingo.learn.check

import com.astrolingo.learn.content.Chapter
import com.astrolingo.learn.content.Course
import com.astrolingo.learn.content.QuizCard

/** One question in the check and the chapter it is for. */
data class CheckQuestion(val chapter: Chapter, val quiz: QuizCard)

/**
 * A knowledge check in progress. Immutable: [answer] returns the next state.
 *
 * Rule (BA and Astro Guru): a chapter is known only when every one of its check questions is answered right.
 * Questions not reached, because the learner stopped early, don't count as known.
 *
 * @property questions every question, chapter by chapter in course order.
 * @property answers the option chosen for each question answered so far.
 */
data class KnowledgeCheck(val questions: List<CheckQuestion>, val answers: List<Int> = emptyList()) {
    /** The question on screen, or null when all are answered. */
    val current: CheckQuestion? get() = questions.getOrNull(answers.size)

    val isFinished: Boolean get() = answers.size >= questions.size

    /** Records the option chosen for [current]. */
    fun answer(choice: Int): KnowledgeCheck {
        check(!isFinished) { "No question left to answer" }
        return copy(answers = answers + choice)
    }

    /** Ids of chapters whose questions were all answered right. */
    fun passedChapters(): Set<String> = questions
        .withIndex()
        .groupBy({ it.value.chapter.id }, { (index, question) -> answers.getOrNull(index)?.let(question.quiz::isCorrect) == true })
        .filterValues { results -> results.all { it } }
        .keys

    /** Chapters that were checked to the end but not passed: Sitara offers them as a quick refresher. */
    fun refresherChapters(): Set<String> = questions
        .withIndex()
        .filter { it.index < answers.size }
        .map { it.value.chapter.id }
        .toSet() - passedChapters()

    companion object {
        /** A check for [chapterIds], asked in course order whatever order they were picked in. */
        fun of(course: Course, chapterIds: Collection<String>): KnowledgeCheck = KnowledgeCheck(
            course.chapters.filter { it.id in chapterIds }.flatMap { chapter -> chapter.check.map { CheckQuestion(chapter, it) } },
        )
    }
}
