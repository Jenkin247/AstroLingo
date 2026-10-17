// The Learn course as data: chapters, lessons and cards, read from the JSON files described in
// docs/spec/learn-content.md. No Android imports, so the same model can serve iOS later.
package com.astrolingo.learn.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Languages the course can be shown in. English is complete; Hindi falls back to English until written. */
enum class Language(val tag: String) {
    ENGLISH("en"),
    HINDI("hi"),
    ;

    companion object {
        /** The course language for a locale language tag such as "hi" or "en-GB"; anything else is English. */
        fun fromTag(tag: String): Language = entries.firstOrNull { tag.substringBefore('-').equals(it.tag, ignoreCase = true) } ?: ENGLISH
    }
}

/**
 * One piece of user-facing text in every language the course supports (ADR 0006).
 *
 * @property en English, always present.
 * @property hi Hindi in Devanagari; null or blank until translated.
 */
@Serializable
data class LocalizedText(val en: String, val hi: String? = null) {
    /** The text in [language], or English when that translation is missing. */
    fun get(language: Language): String = when (language) {
        Language.HINDI -> hi?.takeIf { it.isNotBlank() } ?: en
        Language.ENGLISH -> en
    }
}

/**
 * A Sanskrit term as introduced on a card.
 *
 * @property devanagari the word in Devanagari, e.g. ग्रह.
 * @property roman the same word in roman letters, e.g. graha.
 */
@Serializable
data class Term(val devanagari: String, val roman: String)

/** The kinds of picture a learn card can show. */
@Serializable
enum class VisualKind {
    /** A large symbol such as ☉. */
    @SerialName("glyph")
    GLYPH,

    /** The zodiac wheel with one rashi (1 to 12) lit, or none for 0. */
    @SerialName("zodiac")
    ZODIAC,

    /** The North Indian house diagram with one bhava (1 to 12) lit, or none for 0. */
    @SerialName("houses")
    HOUSES,

    /** The ring of 27 nakshatras with one (1 to 27) lit, or none for 0. */
    @SerialName("nakshatras")
    NAKSHATRAS,
}

/**
 * The picture on a learn card. [value] is the symbol for [VisualKind.GLYPH] and a number for the others.
 */
@Serializable
data class Visual(val kind: VisualKind, val value: String) {
    /** The lit sign, house or nakshatra, or 0 for none; null for a glyph. */
    val number: Int? get() = if (kind == VisualKind.GLYPH) null else value.toIntOrNull()
}

/**
 * A fact from the user's own kundli that a [YourChartCard] fills in.
 *
 * @property needsBirthTime true when the fact depends on the lagna, which is unreliable without a birth time.
 */
@Serializable
enum class ChartFact(val needsBirthTime: Boolean) {
    SUN_RASHI(false),
    MOON_RASHI(false),
    MOON_NAKSHATRA(false),
    MOON_NAKSHATRA_LORD(false),
    LAGNA(true),
    LAGNA_LORD(true),
    SUN_HOUSE(true),
    MOON_HOUSE(true),
    GURU_HOUSE(true),
    SHANI_HOUSE(true),
}

/** One swipe-up card. Its [id] is stable across content versions because bookmarks and progress refer to it. */
@Serializable
sealed interface Card {
    val id: String
}

/** A card that teaches, as opposed to a quiz. Only these can be bookmarked. */
sealed interface TeachingCard : Card {
    val title: LocalizedText
}

/**
 * Teaches one idea.
 *
 * @property term the Sanskrit word introduced on this card, if any.
 * @property visual the picture shown above the text, if any.
 */
@Serializable
@SerialName("learn")
data class LearnCard(
    override val id: String,
    override val title: LocalizedText,
    val body: LocalizedText,
    val term: Term? = null,
    val visual: Visual? = null,
) : TeachingCard

/**
 * The lesson's idea applied to the user's own kundli.
 *
 * @property body text with `{value}` where the app puts the [fact].
 * @property unknownTime shown instead of [body] when the fact needs a birth time the user doesn't know.
 */
@Serializable
@SerialName("yourChart")
data class YourChartCard(
    override val id: String,
    override val title: LocalizedText,
    val fact: ChartFact,
    val body: LocalizedText,
    val unknownTime: LocalizedText? = null,
    val visual: Visual? = null,
) : TeachingCard {
    /** The text to show, with `{value}` replaced, or the unknown-time text when [value] is null. */
    fun text(language: Language, value: String?): String =
        if (value == null) unknownTime?.get(language).orEmpty() else body.get(language).replace(PLACEHOLDER, value)

    companion object {
        const val PLACEHOLDER = "{value}"
    }
}

/**
 * A multiple-choice question.
 *
 * @property answer 0-based index of the right option.
 * @property explanation why the answer is right, shown after answering.
 */
@Serializable
@SerialName("quiz")
data class QuizCard(
    override val id: String,
    val question: LocalizedText,
    val options: List<LocalizedText>,
    val answer: Int,
    val explanation: LocalizedText,
) : Card {
    fun isCorrect(choice: Int): Boolean = choice == answer
}

/**
 * One idea, 3 to 5 minutes: teaching cards then three quiz cards.
 *
 * @property minutes estimated time, shown on the path.
 */
@Serializable
data class Lesson(
    val id: String,
    val title: LocalizedText,
    val minutes: Int,
    val cards: List<Card>,
) {
    val quizzes: List<QuizCard> get() = cards.filterIsInstance<QuizCard>()
}

/**
 * A chapter of the course.
 *
 * @property glyph the symbol on its tile on the path.
 * @property check the two questions the knowledge check asks to let someone skip this chapter.
 */
@Serializable
data class Chapter(
    val schemaVersion: Int,
    val contentVersion: Int,
    val id: String,
    val glyph: String,
    val title: LocalizedText,
    val summary: LocalizedText,
    val check: List<QuizCard>,
    val lessons: List<Lesson>,
)

/** The whole course in teaching order. */
data class Course(val contentVersion: Int, val chapters: List<Chapter>) {
    private val cardIndex: Map<String, Pair<Chapter, Lesson>> by lazy {
        chapters.flatMap { chapter -> chapter.lessons.flatMap { lesson -> lesson.cards.map { it.id to (chapter to lesson) } } }.toMap()
    }

    fun chapter(id: String): Chapter? = chapters.firstOrNull { it.id == id }

    fun lesson(id: String): Lesson? = chapters.firstNotNullOfOrNull { chapter -> chapter.lessons.firstOrNull { it.id == id } }

    /** The chapter a lesson belongs to. */
    fun chapterOf(lessonId: String): Chapter? = chapters.firstOrNull { chapter -> chapter.lessons.any { it.id == lessonId } }

    /** Where a card sits, for opening a bookmark: its chapter, lesson and the card itself. */
    fun locate(cardId: String): CardLocation? = cardIndex[cardId]?.let { (chapter, lesson) ->
        CardLocation(chapter, lesson, lesson.cards.indexOfFirst { it.id == cardId })
    }
}

/** A card's place in the course. [index] is its position in the lesson. */
data class CardLocation(val chapter: Chapter, val lesson: Lesson, val index: Int) {
    val card: Card get() = lesson.cards[index]
}
