package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * High-fidelity, resilient Speech Synthesizer for Aira.
 * Combines OpenAI Studio Neural TTS (alloy, echo, shimmer, etc.) with
 * instant zero-latency on-device Android TextToSpeech as an infallible dual engine.
 */
class AiraVoiceSynthesizer(
    private val context: Context,
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackFinished: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AiraVoiceSynthesizer"
        private const val OPENAI_TTS_URL = "https://api.openai.com/v1/audio/speech"
        private const val SAMPLE_RATE_OPENAI = 24000
    }

    private var androidTts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var activeSpeechJob: Job? = null
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isSpeaking = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        try {
            androidTts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Android TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            androidTts?.language = Locale.ENGLISH
            androidTts?.setSpeechRate(1.05f)
            androidTts?.setPitch(1.0f)
            setupUtteranceListener()
            Log.d(TAG, "Android TextToSpeech initialized successfully")
        } else {
            Log.w(TAG, "Android TextToSpeech initialization returned status: $status")
        }
    }

    private fun setupUtteranceListener() {
        androidTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                scope.launch(Dispatchers.Main) {
                    onPlaybackStarted()
                }
                startSimulatedAmplitude()
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                stopAmplitude()
                scope.launch(Dispatchers.Main) {
                    onPlaybackFinished()
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                stopAmplitude()
                scope.launch(Dispatchers.Main) {
                    onPlaybackFinished()
                }
            }
        })
    }

    /**
     * Speaks the given text using OpenAI TTS if API key is provided and available,
     * or smoothly falls back to Android native TextToSpeech engine.
     */
    fun speak(text: String, apiKey: String, voiceName: String = "alloy") {
        stop()

        val cleanText = text.replace(Regex("[*#`_~]"), "").trim()
        if (cleanText.isEmpty()) {
            onPlaybackFinished()
            return
        }

        activeSpeechJob = scope.launch {
            var openAiSuccess = false

            if (apiKey.isNotBlank() && !apiKey.startsWith("your_")) {
                try {
                    openAiSuccess = synthesizeAndPlayOpenAI(cleanText, apiKey, voiceName)
                } catch (e: Exception) {
                    Log.w(TAG, "OpenAI TTS failed, falling back to Android TTS: ${e.message}")
                    openAiSuccess = false
                }
            }

            if (!openAiSuccess) {
                // Fallback to Android Native TextToSpeech
                withContext(Dispatchers.Main) {
                    speakWithAndroidTts(cleanText)
                }
            }
        }
    }

    private suspend fun synthesizeAndPlayOpenAI(text: String, apiKey: String, voice: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("model", "tts-1")
                    put("input", text)
                    put("voice", voice.lowercase().ifBlank { "alloy" })
                    put("response_format", "pcm")
                    put("speed", 1.05)
                }

                val request = Request.Builder()
                    .url(OPENAI_TTS_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(json.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.w(TAG, "OpenAI TTS returned HTTP ${response.code}")
                    return@withContext false
                }

                val pcmBytes = response.body?.bytes()
                if (pcmBytes == null || pcmBytes.isEmpty()) {
                    return@withContext false
                }

                // Play PCM directly through AudioTrack
                playPcmAudio(pcmBytes)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error in synthesizeAndPlayOpenAI", e)
                false
            }
        }
    }

    private suspend fun playPcmAudio(pcmData: ByteArray) {
        withContext(Dispatchers.IO) {
            var audioTrack: AudioTrack? = null
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE_OPENAI,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE_OPENAI)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                isSpeaking = true

                withContext(Dispatchers.Main) {
                    onPlaybackStarted()
                }

                val chunkSize = 2048
                var offset = 0
                while (offset < pcmData.size && isActive && isSpeaking) {
                    val length = Math.min(chunkSize, pcmData.size - offset)
                    val chunk = pcmData.copyOfRange(offset, offset + length)
                    audioTrack.write(chunk, 0, length)

                    // Compute real amplitude
                    val amp = calculateRms(chunk)
                    withContext(Dispatchers.Main) {
                        onAmplitudeChanged(amp)
                    }

                    offset += length
                    delay(20)
                }

                // Wait slightly for audio buffer to drain
                delay(300)
            } catch (e: Exception) {
                Log.e(TAG, "Error during AudioTrack playback", e)
            } finally {
                try {
                    audioTrack?.stop()
                    audioTrack?.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping AudioTrack", e)
                }
                isSpeaking = false
                withContext(Dispatchers.Main) {
                    onAmplitudeChanged(0f)
                    onPlaybackFinished()
                }
            }
        }
    }

    private fun speakWithAndroidTts(text: String) {
        if (!isTtsInitialized || androidTts == null) {
            Log.w(TAG, "Android TTS not ready, calling playback finished")
            onPlaybackFinished()
            return
        }

        val utteranceId = "aira_${UUID.randomUUID()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        // Auto-detect Hindi/English
        val hasHindi = text.any { it.code in 0x0900..0x097F }
        if (hasHindi) {
            androidTts?.language = Locale("hi", "IN")
        } else {
            androidTts?.language = Locale.ENGLISH
        }

        androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun startSimulatedAmplitude() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (isActive && isSpeaking) {
                val simulatedAmp = (0.25f + (Math.random().toFloat() * 0.65f))
                withContext(Dispatchers.Main) {
                    onAmplitudeChanged(simulatedAmp)
                }
                delay(100)
            }
            withContext(Dispatchers.Main) {
                onAmplitudeChanged(0f)
            }
        }
    }

    private fun stopAmplitude() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        scope.launch(Dispatchers.Main) {
            onAmplitudeChanged(0f)
        }
    }

    private fun calculateRms(pcmData: ByteArray): Float {
        if (pcmData.size < 2) return 0f
        val shortBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        var sumSquares = 0.0
        val count = shortBuffer.remaining()
        if (count == 0) return 0f

        while (shortBuffer.hasRemaining()) {
            val sample = shortBuffer.get() / 32768.0
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / count).toFloat()
        return (rms * 4.0f).coerceIn(0f, 1f)
    }

    fun stop() {
        isSpeaking = false
        activeSpeechJob?.cancel()
        activeSpeechJob = null
        stopAmplitude()
        try {
            androidTts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Android TTS", e)
        }
    }

    fun release() {
        stop()
        try {
            androidTts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down Android TTS", e)
        }
        androidTts = null
        isTtsInitialized = false
    }
}
