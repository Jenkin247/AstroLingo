// What a card says when it is read aloud (LS-7). The app hands these parts to the phone's own voice one after
// another (ADR 0008), so the sentence being read can be highlighted on the card.
package com.astrolingo.learn.audio

import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.QuizCard

/**
 * One piece read aloud.
 *
 * @property bodyRange where this piece sits in the card's body text, for highlighting; null for the title, quiz
 *   options and anything else that isn't part of the body.
 */
data class SpokenPart(val text: String, val bodyRange: IntRange? = null)

object CardSpeech {
    /** Sentence ends: full stop, question and exclamation marks, and the Devanagari danda. */
    private val SENTENCE_END = Regex("[.!?।]+[\"'”’)]*(?=\\s|$)")

    /**
     * The sentences of [text] as ranges, each ending at its punctuation and without surrounding spaces.
     * Text with no sentence end is one sentence.
     */
    fun sentences(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var start = 0
        fun add(endExclusive: Int) {
            var from = start
            var to = endExclusive
            while (from < to && text[from].isWhitespace()) from++
            while (to > from && text[to - 1].isWhitespace()) to--
            if (from < to) ranges += from until to
        }
        SENTENCE_END.findAll(text).forEach { match ->
            add(match.range.last + 1)
            start = match.range.last + 1
        }
        add(text.length)
        return ranges
    }

    /** A teaching card: its title, then its body sentence by sentence. */
    fun teaching(title: String, body: String): List<SpokenPart> =
        listOf(SpokenPart(title)) + sentences(body).map { SpokenPart(body.substring(it), it) }

    /** A learn card in [language]. */
    fun learn(card: LearnCard, language: Language): List<SpokenPart> = teaching(card.title.get(language), card.body.get(language))

    /**
     * A quiz card: the question and the numbered options, and once [answered], the explanation too.
     * Nothing says which option is right before the learner answers.
     */
    fun quiz(card: QuizCard, language: Language, answered: Boolean): List<SpokenPart> = buildList {
        add(SpokenPart(card.question.get(language)))
        card.options.forEachIndexed { index, option -> add(SpokenPart("${index + 1}. ${option.get(language)}")) }
        if (answered) add(SpokenPart(card.explanation.get(language)))
    }
}
