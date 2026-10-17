package com.astrolingo.learn

import com.astrolingo.learn.content.ChartFact
import com.astrolingo.learn.content.CourseLoader
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.LocalizedText
import com.astrolingo.learn.content.QuizCard
import com.astrolingo.learn.content.VisualKind
import com.astrolingo.learn.content.YourChartCard
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CourseLoaderTest {
    private val chapter = """
        {
          "schemaVersion": 1, "contentVersion": 3, "id": "grahas", "glyph": "☉",
          "title": { "en": "The nine grahas", "hi": "नौ ग्रह" },
          "summary": { "en": "Meet the grahas." },
          "check": [
            { "id": "grahas.check.1", "type": "quiz", "question": { "en": "Q1" }, "options": [ { "en": "a" }, { "en": "b" }, { "en": "c" } ], "answer": 2, "explanation": { "en": "e" } }
          ],
          "lessons": [
            {
              "id": "grahas.sun", "title": { "en": "Sun" }, "minutes": 4, "futureField": true,
              "cards": [
                { "id": "grahas.sun.1", "type": "learn", "title": { "en": "Surya" }, "body": { "en": "The soul.", "hi": "" },
                  "term": { "devanagari": "सूर्य", "roman": "Surya" }, "visual": { "kind": "glyph", "value": "☉" } },
                { "id": "grahas.sun.2", "type": "learn", "title": { "en": "Wheel" }, "body": { "en": "Simha." }, "visual": { "kind": "zodiac", "value": 5 } },
                { "id": "grahas.sun.3", "type": "yourChart", "fact": "SUN_HOUSE", "title": { "en": "Your Sun" },
                  "body": { "en": "Your Sun is in house {value}." }, "unknownTime": { "en": "Add your birth time to see this." } },
                { "id": "grahas.sun.4", "type": "quiz", "question": { "en": "Q" }, "options": [ { "en": "x" }, { "en": "y" }, { "en": "z" } ], "answer": 1, "explanation": { "en": "e" } }
              ]
            }
          ]
        }
    """.trimIndent()

    private val files = mapOf(
        "course.json" to """{ "schemaVersion": 1, "contentVersion": 3, "chapters": ["02-grahas.json"] }""",
        "02-grahas.json" to chapter,
    )

    private val course = CourseLoader.load { name -> files[name]?.byteInputStream() }

    @Test
    fun `reads chapters, lessons and every card type`() {
        assertThat(course.contentVersion).isEqualTo(3)
        val lesson = course.chapters.single().lessons.single()
        assertThat(lesson.cards.map { it::class }).containsExactly(LearnCard::class, LearnCard::class, YourChartCard::class, QuizCard::class).inOrder()
        val sun = lesson.cards[0] as LearnCard
        assertThat(sun.term?.devanagari).isEqualTo("सूर्य")
        assertThat(sun.visual?.kind).isEqualTo(VisualKind.GLYPH)
        assertThat(sun.visual?.value).isEqualTo("☉")
        assertThat((lesson.cards[1] as LearnCard).visual?.number).isEqualTo(5)
        assertThat((lesson.cards[2] as YourChartCard).fact).isEqualTo(ChartFact.SUN_HOUSE)
        assertThat((lesson.cards[3] as QuizCard).isCorrect(1)).isTrue()
        assertThat(lesson.quizzes).hasSize(1)
    }

    @Test
    fun `hindi falls back to english when missing or blank`() {
        val chapter = course.chapters.single()
        assertThat(chapter.title.get(Language.HINDI)).isEqualTo("नौ ग्रह")
        assertThat(chapter.summary.get(Language.HINDI)).isEqualTo("Meet the grahas.")
        assertThat((chapter.lessons[0].cards[0] as LearnCard).body.get(Language.HINDI)).isEqualTo("The soul.")
        assertThat(LocalizedText("x", "य").get(Language.ENGLISH)).isEqualTo("x")
    }

    @Test
    fun `language comes from the locale tag`() {
        assertThat(Language.fromTag("hi")).isEqualTo(Language.HINDI)
        assertThat(Language.fromTag("hi-IN")).isEqualTo(Language.HINDI)
        assertThat(Language.fromTag("en-GB")).isEqualTo(Language.ENGLISH)
        assertThat(Language.fromTag("nl-BE")).isEqualTo(Language.ENGLISH)
    }

    @Test
    fun `your chart text fills in the value or says the time is needed`() {
        val card = course.chapters.single().lessons.single().cards[2] as YourChartCard
        assertThat(card.text(Language.ENGLISH, "10")).isEqualTo("Your Sun is in house 10.")
        assertThat(card.text(Language.ENGLISH, null)).isEqualTo("Add your birth time to see this.")
    }

    @Test
    fun `cards are found by id for bookmarks`() {
        val location = course.locate("grahas.sun.3")!!
        assertThat(location.chapter.id).isEqualTo("grahas")
        assertThat(location.index).isEqualTo(2)
        assertThat(location.card.id).isEqualTo("grahas.sun.3")
        assertThat(course.locate("nope")).isNull()
        assertThat(course.chapterOf("grahas.sun")?.id).isEqualTo("grahas")
    }

    @Test(expected = IllegalStateException::class)
    fun `a missing chapter file is an error`() {
        CourseLoader.load { name -> if (name == "course.json") files.getValue(name).byteInputStream() else null }
    }

    @Test(expected = IllegalStateException::class)
    fun `an unknown schema version is an error`() {
        CourseLoader.load { name -> files[name]?.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")?.byteInputStream() }
    }
}
