// Read-aloud with the phone's own text-to-speech (ADR 0008, LS-7): free, offline, and nothing leaves the phone.
// Each card is spoken part by part, so the sentence being read can be highlighted.
package com.astrolingo.app.learn

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.astrolingo.learn.audio.SpokenPart
import com.astrolingo.learn.content.Language
import java.util.Locale

/** Whether the phone can speak a language. */
enum class VoiceStatus { CHECKING, INSTALLED, MISSING }

/** Why a card couldn't be read aloud, shown to the learner as a short dialog. */
enum class VoiceProblem {
    /** The phone has no text-to-speech engine at all, or it failed to start. */
    NO_ENGINE,

    /** The card is in Hindi and the phone has no Hindi voice. English can be read instead. */
    NO_HINDI,
}

/**
 * Speaks cards with Android's [TextToSpeech]. The engine starts only when first needed, because binding to it
 * takes a moment and most visits to Learn never use it.
 *
 * State is Compose state, so the card's speaker button and highlight follow it.
 */
@Stable
class ReadAloud internal constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var engineReady = false
    private var engineFailed = false
    private var pending: (() -> Unit)? = null

    // Every new reading gets a new number, so callbacks from a reading that was stopped are ignored.
    private var generation = 0

    /** The key of the card being read, or null when silent. */
    var speakingKey by mutableStateOf<String?>(null)
        private set

    /** The part of [speakingKey] being read now, or null between parts. */
    var speakingPart by mutableStateOf<SpokenPart?>(null)
        private set

    /** A problem to show the learner, or null. Cleared by [dismissProblem]. */
    var problem by mutableStateOf<VoiceProblem?>(null)
        private set

    /** Increases each time the engine finishes starting, so screens can check the voices again. */
    var readyCount by mutableIntStateOf(0)
        private set

    /** Listen mode: each card is read as soon as it opens. Off by default (e-learning: a choice, never auto-on). */
    var listenMode by mutableStateOf(prefs.getBoolean(KEY_LISTEN, false))
        private set

    // What NO_HINDI's "Use English" reads.
    private var englishFallback: Pair<String, List<SpokenPart>>? = null

    // Set once the learner chose English over a missing Hindi voice, so Listen mode doesn't ask on every card.
    private var englishInstead = false

    fun updateListenMode(on: Boolean) {
        listenMode = on
        prefs.edit().putBoolean(KEY_LISTEN, on).apply()
        if (!on) stop()
    }

    /**
     * Reads [parts] in [language], replacing whatever is being read. [english] gives the same card in English, for
     * when the phone has no Hindi voice and the learner chooses to hear English instead.
     */
    fun speak(key: String, parts: List<SpokenPart>, language: Language, english: () -> List<SpokenPart>) {
        stop()
        whenReady {
            val engine = tts
            if (engine == null || engineFailed) {
                problem = VoiceProblem.NO_ENGINE
                // Without any voice, Listen mode would only repeat this message on every card.
                if (listenMode) updateListenMode(false)
                return@whenReady
            }
            val locale = localeFor(engine, language)
            if (locale == null && englishInstead) {
                start(engine, key, english(), localeFor(engine, Language.ENGLISH) ?: Locale.getDefault())
            } else if (locale == null) {
                englishFallback = key to english()
                problem = VoiceProblem.NO_HINDI
            } else {
                start(engine, key, parts, locale)
            }
        }
    }

    /** Stops reading at once. Safe to call when nothing is being read or the engine never started. */
    fun stop() {
        generation++
        pending = null
        tts?.stop()
        speakingKey = null
        speakingPart = null
    }

    /** Reads in English what couldn't be read in Hindi. */
    fun useEnglish() {
        val (key, parts) = englishFallback ?: return dismissProblem()
        dismissProblem()
        englishInstead = true
        val engine = tts ?: return
        start(engine, key, parts, localeFor(engine, Language.ENGLISH) ?: Locale.getDefault())
    }

    fun dismissProblem() {
        problem = null
        englishFallback = null
    }

    /** Whether [language] can be spoken. Starts the engine if needed; [readyCount] changes once it is ready. */
    fun voiceStatus(language: Language): VoiceStatus {
        val engine = tts
        if (engine == null) {
            whenReady {}
            return VoiceStatus.CHECKING
        }
        if (!engineReady) return if (engineFailed) VoiceStatus.MISSING else VoiceStatus.CHECKING
        return if (engineFailed || localeFor(engine, language) == null) VoiceStatus.MISSING else VoiceStatus.INSTALLED
    }

    /** Opens the phone's text-to-speech settings, where voices are downloaded. */
    fun openVoiceSettings() {
        dismissProblem()
        val intents = listOf(
            Intent(TTS_SETTINGS),
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
        )
        intents.firstOrNull { intent ->
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (e: ActivityNotFoundException) {
                false
            } catch (e: SecurityException) {
                false
            }
        }
    }

    internal fun release() {
        stop()
        tts?.shutdown()
        tts = null
        engineReady = false
    }

    private fun whenReady(action: () -> Unit) {
        if (engineReady) {
            action()
            return
        }
        pending = action
        if (tts != null) return
        tts = try {
            TextToSpeech(context) { status ->
                main.post {
                    engineReady = true
                    engineFailed = status != TextToSpeech.SUCCESS
                    tts?.setOnUtteranceProgressListener(listener)
                    readyCount++
                    pending?.invoke()
                    pending = null
                }
            }
        } catch (e: RuntimeException) {
            engineReady = true
            engineFailed = true
            null
        }
        if (tts == null) {
            pending?.invoke()
            pending = null
        }
    }

    private fun start(engine: TextToSpeech, key: String, parts: List<SpokenPart>, locale: Locale) {
        if (parts.isEmpty()) return
        val reading = ++generation
        engine.setLanguage(locale)
        speakingKey = key
        currentParts = parts
        parts.forEachIndexed { index, part ->
            val mode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(part.text, mode, null, "$reading$SEPARATOR$index")
        }
    }

    private var currentParts: List<SpokenPart> = emptyList()

    /** Indian English when installed, else the phone's English; Hindi only when a Hindi voice is installed. */
    private fun localeFor(engine: TextToSpeech, language: Language): Locale? {
        fun available(locale: Locale) = runCatching { engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)
        return when (language) {
            Language.HINDI -> HINDI.takeIf(::available)
            Language.ENGLISH -> listOf(INDIAN_ENGLISH, Locale.ENGLISH).firstOrNull(::available) ?: Locale.getDefault()
        }
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) = on(utteranceId) { index ->
            speakingPart = currentParts.getOrNull(index)
        }

        override fun onDone(utteranceId: String) = on(utteranceId) { index ->
            if (index == currentParts.lastIndex) {
                speakingKey = null
                speakingPart = null
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String) = on(utteranceId) {
            speakingKey = null
            speakingPart = null
        }

        private fun on(utteranceId: String, block: (Int) -> Unit) {
            val reading = utteranceId.substringBefore(SEPARATOR).toIntOrNull() ?: return
            val index = utteranceId.substringAfter(SEPARATOR).toIntOrNull() ?: return
            main.post { if (reading == generation) block(index) }
        }
    }

    private companion object {
        const val PREFS = "astrolingo_settings"
        const val KEY_LISTEN = "listen_mode"
        const val SEPARATOR = ':'
        const val TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
        val INDIAN_ENGLISH: Locale = Locale.forLanguageTag("en-IN")
        val HINDI: Locale = Locale.forLanguageTag("hi-IN")
    }
}

/** A [ReadAloud] for this screen, stopped and released when the screen goes away. */
@Composable
fun rememberReadAloud(): ReadAloud {
    val context = LocalContext.current.applicationContext
    val readAloud = remember(context) { ReadAloud(context) }
    DisposableEffect(readAloud) { onDispose { readAloud.release() } }
    return readAloud
}
