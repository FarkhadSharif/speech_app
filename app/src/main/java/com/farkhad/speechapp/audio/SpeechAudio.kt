package com.farkhad.speechapp.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

class SpeechAudio(context: Context) {
    private data class SpeechRequest(
        val text: String,
        val slower: Boolean,
        val attempt: Int = 0,
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val kazakh = Locale.forLanguageTag("kk-KZ")
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    private var engine: TextToSpeech? = null
    private var ttsReady = false
    private var deviceSupportsKazakh = false
    private var pendingTts: SpeechRequest? = null
    private var activeTts: Pair<String, SpeechRequest>? = null
    private var currentPlayer: MediaPlayer? = null
    private var retrySpeech: Runnable? = null
    private var closed = false
    private var playbackErrorShown = false

    // The current course audio is bundled in the APK, so listening is available
    // immediately and does not depend on an installed Android TTS voice.
    var ready by mutableStateOf(true)
        private set

    var supportsKazakh by mutableStateOf(true)
        private set

    init {
        val created = TextToSpeech(appContext) { status ->
            mainHandler.post { finishTtsInitialization(status) }
        }
        engine = created
    }

    private fun finishTtsInitialization(status: Int) {
        if (closed) return

        val tts = engine
        if (status != TextToSpeech.SUCCESS || tts == null) {
            ttsReady = false
            deviceSupportsKazakh = false
            pendingTts = null
            return
        }

        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
        )

        val selectedVoice = selectKazakhVoice(tts)
        val languageResult = tts.setLanguage(kazakh)
        deviceSupportsKazakh = selectedVoice != null || languageResult >= TextToSpeech.LANG_AVAILABLE
        if (selectedVoice != null) {
            tts.voice = selectedVoice
        }

        tts.setPitch(1.0f)
        tts.setSpeechRate(NORMAL_SPEECH_RATE)
        tts.setOnUtteranceProgressListener(createProgressListener())
        ttsReady = true

        pendingTts?.also {
            pendingTts = null
            speakWithDeviceVoice(it)
        }
    }

    private fun selectKazakhVoice(tts: TextToSpeech): Voice? {
        return tts.voices
            ?.asSequence()
            ?.filter { voice ->
                voice.locale.language.equals(kazakh.language, ignoreCase = true) &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features
            }
            ?.maxWithOrNull(
                compareBy<Voice>(
                    { it.locale.toLanguageTag().equals(kazakh.toLanguageTag(), ignoreCase = true) },
                    { !it.isNetworkConnectionRequired },
                    { it.quality },
                    { -it.latency },
                ),
            )
    }

    fun speak(text: String, slower: Boolean = false) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return

        mainHandler.post {
            if (closed) return@post

            stopSpeech()
            val request = SpeechRequest(normalizedText, slower)
            if (!playBundledSpeech(request)) {
                speakWithDeviceVoice(request)
            }
        }
    }

    private fun playBundledSpeech(request: SpeechRequest): Boolean {
        val player = MediaPlayer()
        return try {
            appContext.assets.openFd("speech/${assetNameFor(request.text)}").use { descriptor ->
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                )
                player.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }

            currentPlayer = player
            player.setOnPreparedListener { prepared ->
                if (closed || currentPlayer !== prepared) {
                    releasePlayer(prepared)
                    return@setOnPreparedListener
                }

                try {
                    if (request.slower) {
                        prepared.playbackParams = PlaybackParams()
                            .setSpeed(SLOW_PLAYBACK_SPEED)
                            .setPitch(1.0f)
                    }
                    prepared.start()
                } catch (_: Exception) {
                    handleBundledPlaybackError(prepared, request)
                }
            }
            player.setOnCompletionListener(::releasePlayer)
            player.setOnErrorListener { failed, _, _ ->
                handleBundledPlaybackError(failed, request)
                true
            }
            player.prepareAsync()
            true
        } catch (_: FileNotFoundException) {
            releasePlayer(player)
            false
        } catch (_: Exception) {
            releasePlayer(player)
            false
        }
    }

    private fun handleBundledPlaybackError(player: MediaPlayer, request: SpeechRequest) {
        if (currentPlayer !== player) {
            releasePlayer(player)
            return
        }

        releasePlayer(player)
        if (closed) return

        if (request.attempt < MAX_BUNDLED_RETRIES) {
            val retryRequest = request.copy(attempt = request.attempt + 1)
            val retry = Runnable {
                retrySpeech = null
                if (!closed && !playBundledSpeech(retryRequest)) {
                    speakWithDeviceVoice(retryRequest)
                }
            }
            retrySpeech = retry
            mainHandler.postDelayed(retry, RETRY_DELAY_MS)
        } else {
            speakWithDeviceVoice(request)
        }
    }

    private fun speakWithDeviceVoice(request: SpeechRequest) {
        val tts = engine
        if (!ttsReady) {
            if (tts != null) {
                pendingTts = request
            } else {
                showPlaybackError()
            }
            return
        }
        if (!deviceSupportsKazakh || tts == null) {
            showPlaybackError()
            return
        }

        tts.setSpeechRate(if (request.slower) SLOW_SPEECH_RATE else NORMAL_SPEECH_RATE)
        val utteranceId = "speech-${System.nanoTime()}"
        activeTts = utteranceId to request
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        if (tts.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, utteranceId) == TextToSpeech.ERROR) {
            showPlaybackError()
        }
    }

    private fun createProgressListener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            mainHandler.post {
                if (activeTts?.first == utteranceId) {
                    activeTts = null
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = handleTtsError(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = handleTtsError(utteranceId)
    }

    private fun handleTtsError(utteranceId: String?) {
        mainHandler.post {
            if (activeTts?.first == utteranceId) {
                activeTts = null
                showPlaybackError()
            }
        }
    }

    private fun showPlaybackError() {
        if (playbackErrorShown || closed) return
        playbackErrorShown = true
        Toast.makeText(
            appContext,
            "Дыбысты ойнату мүмкін болмады / Не удалось воспроизвести звук",
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun assetNameFor(text: String): String {
        val canonical = Normalizer.normalize(text.trim(), Normalizer.Form.NFC)
            .replace(WHITESPACE, " ")
            .lowercase(kazakh)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        val prefix = digest.take(HASH_BYTES).joinToString(separator = "") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
        return "speech_$prefix.mp3"
    }

    private fun releasePlayer(player: MediaPlayer) {
        if (currentPlayer === player) {
            currentPlayer = null
        }
        try {
            player.release()
        } catch (_: Exception) {
        }
    }

    private fun stopSpeech() {
        retrySpeech?.let(mainHandler::removeCallbacks)
        retrySpeech = null
        pendingTts = null
        activeTts = null
        currentPlayer?.let(::releasePlayer)
        try {
            engine?.stop()
        } catch (_: Exception) {
        }
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
        closed = true
        stopSpeech()

        try {
            engine?.shutdown()
        } catch (_: Exception) {
        }
        engine = null

        try {
            tone.release()
        } catch (_: Exception) {
        }
        ready = false
        supportsKazakh = false
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        const val HASH_BYTES = 10
        const val NORMAL_SPEECH_RATE = 0.9f
        const val SLOW_SPEECH_RATE = 0.72f
        const val SLOW_PLAYBACK_SPEED = 0.82f
        const val MAX_BUNDLED_RETRIES = 1
        const val RETRY_DELAY_MS = 250L
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
