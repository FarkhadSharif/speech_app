package com.farkhad.speechapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
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
import java.net.URLEncoder
import java.util.Locale

class SpeechAudio(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val kazakh = Locale("kk", "KZ")
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    private var engine: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

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
                        
                        val bestVoice = tts.voices?.find { 
                            it.locale.language == "kk" && !it.isNetworkConnectionRequired 
                        } ?: tts.voices?.find { 
                            it.locale.language == "kk" 
                        }
                        if (bestVoice != null) tts.voice = bestVoice
                    }
                    tts.setSpeechRate(0.85f)
                    ready = true
                }
            }
        }
        engine = created
    }

    fun speak(text: String, slower: Boolean = false) {
        if (text.isBlank()) return
        
        // Try high-quality Google Neural TTS URL
        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            // This URL is generally more stable for Kazakh
            val url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=kk-KZ&client=tw-ob&q=$encodedText"
            
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) it.stop()
                } catch (e: Exception) {}
                it.reset()
            } ?: run {
                mediaPlayer = MediaPlayer()
            }

            mediaPlayer?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                // Essential headers to prevent 403 Forbidden
                val headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Mobile Safari/537.36",
                    "Referer" to "https://translate.google.com/"
                )
                
                setDataSource(context, Uri.parse(url), headers)
                
                setOnPreparedListener { 
                    it.playbackParams = it.playbackParams.setSpeed(if (slower) 0.8f else 1.0f)
                    it.start() 
                }
                
                setOnErrorListener { _, what, extra ->
                    // Log error and fallback
                    speakNative(text, slower)
                    true
                }
                
                prepareAsync()
            }
        } catch (e: Exception) {
            speakNative(text, slower)
        }
    }

    private fun speakNative(text: String, slower: Boolean) {
        if (!ready) return
        engine?.setSpeechRate(if (slower) 0.7f else 0.85f)
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
        try {
            engine?.stop()
            engine?.shutdown()
        } catch (e: Exception) {}
        engine = null
        
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        
        try {
            tone.release()
        } catch (e: Exception) {}
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
