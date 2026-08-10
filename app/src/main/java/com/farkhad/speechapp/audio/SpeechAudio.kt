package com.farkhad.speechapp.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class SpeechAudio(context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val kazakh = Locale("kk", "KZ")
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    private var engine: TextToSpeech? = null

    var ready by mutableStateOf(false)
        private set

    var supportsKazakh by mutableStateOf(true)
        private set

    init {
        val created = TextToSpeech(context.applicationContext) { status ->
            mainHandler.post {
                val tts = engine
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    val availability = tts.isLanguageAvailable(kazakh)
                    supportsKazakh = availability >= TextToSpeech.LANG_AVAILABLE
                    if (supportsKazakh) {
                        tts.language = kazakh
                    } else {
                        tts.language = Locale.getDefault()
                    }
                    tts.setSpeechRate(0.82f)
                    tts.setPitch(1.02f)
                    ready = true
                }
            }
        }
        engine = created
    }

    fun speak(text: String, slower: Boolean = false) {
        if (!ready || text.isBlank()) return
        engine?.setSpeechRate(if (slower) 0.68f else 0.82f)
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "speech-${System.nanoTime()}")
    }

    fun playCorrect() {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 150)
    }

    fun playTryAgain() {
        tone.startTone(ToneGenerator.TONE_PROP_PROMPT, 110)
    }

    fun playCelebration() {
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 230)
    }

    fun close() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        tone.release()
        ready = false
    }
}

@Composable
fun rememberSpeechAudio(): SpeechAudio {
    val context = LocalContext.current
    val audio = remember(context.applicationContext) { SpeechAudio(context.applicationContext) }
    DisposableEffect(audio) {
        onDispose { audio.close() }
    }
    return audio
}

