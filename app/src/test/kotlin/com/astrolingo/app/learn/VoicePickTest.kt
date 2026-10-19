package com.astrolingo.app.learn

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoicePickTest {
    private val enIn = Locale.forLanguageTag("en-IN")
    private val enUs = Locale.forLanguageTag("en-US")
    private val hiIn = Locale.forLanguageTag("hi-IN")

    private fun voice(
        name: String,
        locale: Locale,
        quality: Int = Voice.QUALITY_NORMAL,
        network: Boolean = false,
        features: Set<String> = emptySet(),
    ) = Voice(name, locale, quality, Voice.LATENCY_NORMAL, network, features)

    @Test
    fun englishAndHindiEachGetTheirOwnBestVoice() {
        val voices = listOf(
            voice("en-in-plain", enIn),
            voice("en-in-neural", enIn, Voice.QUALITY_VERY_HIGH),
            voice("hi-in-plain", hiIn),
            voice("hi-in-neural", hiIn, Voice.QUALITY_HIGH),
        )
        assertThat(pickVoice(voices, enIn)?.name).isEqualTo("en-in-neural")
        assertThat(pickVoice(voices, hiIn)?.name).isEqualTo("hi-in-neural")
    }

    @Test
    fun indianEnglishBeatsABetterAmericanVoice() {
        val voices = listOf(voice("en-us-neural", enUs, Voice.QUALITY_VERY_HIGH), voice("en-in-plain", enIn))
        assertThat(pickVoice(voices, enIn)?.name).isEqualTo("en-in-plain")
    }

    @Test
    fun otherEnglishIsUsedWhenNoIndianVoice() {
        assertThat(pickVoice(listOf(voice("en-us", enUs)), enIn)?.name).isEqualTo("en-us")
    }

    @Test
    fun neverANetworkOrUndownloadedVoice() {
        val voices = listOf(
            voice("hi-cloud", hiIn, Voice.QUALITY_VERY_HIGH, network = true),
            voice("hi-not-downloaded", hiIn, Voice.QUALITY_VERY_HIGH, features = setOf(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)),
            voice("hi-local", hiIn),
        )
        assertThat(pickVoice(voices, hiIn)?.name).isEqualTo("hi-local")
    }

    @Test
    fun noVoiceForTheLanguageLeavesTheDefault() {
        assertThat(pickVoice(listOf(voice("en-in", enIn)), hiIn)).isNull()
    }
}
