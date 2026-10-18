package com.astrolingo.learn

import com.astrolingo.learn.audio.CardSpeech
import com.astrolingo.learn.audio.SpokenPart
import com.astrolingo.learn.content.Language
import com.astrolingo.learn.content.LearnCard
import com.astrolingo.learn.content.LocalizedText
import com.astrolingo.learn.content.QuizCard
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CardSpeechTest {
    private fun split(text: String) = CardSpeech.sentences(text).map(text::substring)

    @Test
    fun `splits English sentences and keeps their punctuation`() {
        assertThat(split("Surya is the atma. It shows confidence!  Like a king?"))
            .containsExactly("Surya is the atma.", "It shows confidence!", "Like a king?").inOrder()
    }

    @Test
    fun `splits Hindi sentences on the danda`() {
        assertThat(split("सूर्य कुंडली की आत्मा है। यह आत्मविश्वास दिखाता है।"))
            .containsExactly("सूर्य कुंडली की आत्मा है।", "यह आत्मविश्वास दिखाता है।").inOrder()
    }

    @Test
    fun `does not split inside numbers or abbreviations without a space`() {
        assertThat(split("The 120-year cycle is 6.5 times longer. Done")).containsExactly("The 120-year cycle is 6.5 times longer.", "Done").inOrder()
    }

    @Test
    fun `text without an ending is one sentence and blank text is none`() {
        assertThat(split("No full stop here")).containsExactly("No full stop here")
        assertThat(CardSpeech.sentences("   ")).isEmpty()
    }

    @Test
    fun `a learn card reads its title then each body sentence with its place in the body`() {
        val card = LearnCard("c", LocalizedText("Surya, the Sun"), LocalizedText("One. Two."))
        assertThat(CardSpeech.learn(card, Language.ENGLISH)).containsExactly(
            SpokenPart("Surya, the Sun"),
            SpokenPart("One.", 0..3),
            SpokenPart("Two.", 5..8),
        ).inOrder()
    }

    @Test
    fun `a quiz is read without its answer until it is answered`() {
        val quiz = QuizCard(
            id = "q",
            question = LocalizedText("Which graha is the soul?"),
            options = listOf(LocalizedText("Chandra"), LocalizedText("Surya")),
            answer = 1,
            explanation = LocalizedText("Surya is the atma."),
        )
        assertThat(CardSpeech.quiz(quiz, Language.ENGLISH, answered = false).map { it.text })
            .containsExactly("Which graha is the soul?", "1. Chandra", "2. Surya").inOrder()
        assertThat(CardSpeech.quiz(quiz, Language.ENGLISH, answered = true).last().text).isEqualTo("Surya is the atma.")
    }
}
