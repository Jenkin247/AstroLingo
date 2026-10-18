package com.astrolingo.learn

import com.astrolingo.learn.check.KnowledgeCheck
import com.astrolingo.learn.content.Card
import com.astrolingo.learn.content.CourseLoader
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.LocalizedText
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.content.TeachingCard
import com.astrolingo.learn.content.VisualKind
import com.astrolingo.learn.content.YourChartCard
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/** The Astro Guru's course, checked against every rule in docs/spec/learn-content.md. */
class CourseContentTest {
    private val course = CourseLoader.bundled()
    private val chapters = course.chapters
    private val lessons = chapters.flatMap { it.lessons }
    private val cards: List<Card> = lessons.flatMap { it.cards } + chapters.flatMap { it.check }

    private val grahas = setOf("SURYA", "CHANDRA", "MANGAL", "BUDHA", "GURU", "SHUKRA", "SHANI", "RAHU", "KETU")

    @Test
    fun `the course has 14 chapters in teaching order`() {
        assertThat(chapters.map { it.id }).containsExactly(
            "welcome", "grahas", "rashis", "bhavas", "nakshatras", "reading", "drishti",
            "dignity", "vargas", "dasha", "gochar", "yogas", "panchang", "matching",
        ).inOrder()
    }

    @Test
    fun `every id is unique and nested under its parent`() {
        val ids = chapters.map { it.id } + lessons.map { it.id } + cards.map { it.id }
        assertThat(ids).containsNoDuplicates()
        chapters.forEach { chapter ->
            chapter.check.forEach { assertThat(it.id).startsWith("${chapter.id}.check.") }
            chapter.lessons.forEach { lesson ->
                assertThat(lesson.id).startsWith("${chapter.id}.")
                lesson.cards.forEach { assertThat(it.id).startsWith("${lesson.id}.") }
            }
        }
    }

    @Test
    fun `each chapter has 3 to 5 lessons and 2 check questions`() {
        chapters.forEach { chapter ->
            assertWithMessage(chapter.id).that(chapter.lessons.size).isIn(3..5)
            assertWithMessage(chapter.id).that(chapter.check).hasSize(2)
        }
    }

    @Test
    fun `each lesson teaches 5 to 7 cards then ends with 3 quiz cards`() {
        lessons.forEach { lesson ->
            assertWithMessage(lesson.id).that(lesson.minutes).isIn(3..5)
            assertWithMessage(lesson.id).that(lesson.cards.count { it is TeachingCard }).isIn(5..7)
            assertWithMessage(lesson.id).that(lesson.quizzes).hasSize(3)
            assertWithMessage(lesson.id).that(lesson.cards.takeLast(3).all { it is QuizCard }).isTrue()
        }
    }

    @Test
    fun `quiz answers are inside their options and options differ`() {
        cards.filterIsInstance<QuizCard>().forEach { quiz ->
            assertWithMessage(quiz.id).that(quiz.options.size).isIn(3..4)
            assertWithMessage(quiz.id).that(quiz.answer).isIn(quiz.options.indices)
            assertWithMessage(quiz.id).that(quiz.options.map { it.en }).containsNoDuplicates()
        }
    }

    @Test
    fun `english text is present and within the length limits`() {
        fun check(id: String, text: LocalizedText?, limit: Int) {
            assertWithMessage(id).that(text?.en.orEmpty()).isNotEmpty()
            assertWithMessage("$id is ${text?.en?.length} characters").that(text!!.en.length).isAtMost(limit)
        }
        chapters.forEach {
            check(it.id, it.title, 40)
            check(it.id, it.summary, 320)
        }
        lessons.forEach { check(it.id, it.title, 40) }
        cards.forEach { card ->
            when (card) {
                is LearnCard -> {
                    check(card.id, card.title, 40)
                    check(card.id, card.body, 320)
                }
                is YourChartCard -> {
                    check(card.id, card.title, 40)
                    check(card.id, card.body, 320)
                }
                is QuizCard -> {
                    check(card.id, card.question, 140)
                    check(card.id, card.explanation, 260)
                    card.options.forEach { check(card.id, it, 60) }
                }
            }
        }
    }

    @Test
    fun `your chart cards have a value and say what to do without a birth time`() {
        cards.filterIsInstance<YourChartCard>().forEach { card ->
            assertWithMessage(card.id).that(card.body.en).contains(YourChartCard.PLACEHOLDER)
            if (card.fact.needsBirthTime) assertWithMessage(card.id).that(card.unknownTime?.en.orEmpty()).isNotEmpty()
        }
    }

    @Test
    fun `visuals are in range and only learn cards with a picture flip`() {
        cards.filterIsInstance<LearnCard>().forEach { card ->
            val visual = card.visual
            if (card.flip) assertWithMessage(card.id).that(visual).isNotNull()
            when (visual?.kind) {
                VisualKind.GLYPH -> assertWithMessage(card.id).that(visual.value.length).isIn(1..2)
                VisualKind.ZODIAC, VisualKind.HOUSES -> assertWithMessage(card.id).that(visual.number).isIn(0..12)
                VisualKind.NAKSHATRAS -> assertWithMessage(card.id).that(visual.number).isIn(0..27)
                VisualKind.GRAHA -> assertWithMessage(card.id).that(visual.value).isIn(grahas)
                null -> Unit
            }
        }
    }

    @Test
    fun `every graha has a flip card in the grahas chapter`() {
        val flipped = course.chapter("grahas")!!.lessons.flatMap { it.cards }.filterIsInstance<LearnCard>()
            .filter { it.flip }.map { it.visual?.value }
        assertThat(flipped).containsExactlyElementsIn(grahas)
    }

    @Test
    fun `answering every check question right marks every chapter known`() {
        val check = chapters.map { it.id }.let { KnowledgeCheck.of(course, it) }
        val done = check.questions.fold(check) { state, question -> state.answer(question.quiz.answer) }
        assertThat(done.passedChapters()).containsExactlyElementsIn(chapters.map { it.id })
    }
}
