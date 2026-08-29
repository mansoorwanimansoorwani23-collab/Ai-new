package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Pure OpenAI Neural Realtime Voice Synthesizer for Aira.
 * Streams natural, high-fidelity neural audio directly via AudioTrack at 24kHz.
 * Zero reliance on robotic device/phone TTS.
 */
class AiraVoiceSynthesizer(
    private val context: Context,
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackFinished: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) {

    companion object {
        private const val TAG = "AiraVoiceSynthesizer"
        private const val OPENAI_TTS_URL = "https://api.openai.com/v1/audio/speech"
        private const val SAMPLE_RATE_OPENAI = 24000
    }

    private var activeSpeechJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isSpeaking = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Speaks text using OpenAI Studio Neural Voice (alloy, echo, shimmer, nova, coral, etc.)
     * streamed directly to the device audio hardware.
     */
    fun speak(text: String, apiKey: String, voiceName: String = "alloy") {
        stop()

        val cleanText = text.replace(Regex("[*#`_~]"), "").trim()
        if (cleanText.isEmpty()) {
            onPlaybackFinished()
            return
        }

        activeSpeechJob = scope.launch {
            if (apiKey.isNotBlank() && !apiKey.startsWith("your_")) {
                try {
                    val success = synthesizeAndPlayOpenAI(cleanText, apiKey, voiceName)
                    if (!success) {
                        withContext(Dispatchers.Main) {
                            onPlaybackFinished()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "OpenAI voice synthesis stream failed: ${e.message}")
                    withContext(Dispatchers.Main) {
                        onPlaybackFinished()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    onPlaybackFinished()
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

                // Play raw PCM audio directly through AudioTrack
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
                val totalFrames = pcmData.size / 2 // 16-bit mono = 2 bytes per frame
                val minBufferSize = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE_OPENAI,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize * 4, 32768)

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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                isSpeaking = true

                withContext(Dispatchers.Main) {
                    onPlaybackStarted()
                }

                val chunkSize = 4096
                var offset = 0
                while (offset < pcmData.size && isActive && isSpeaking) {
                    val length = Math.min(chunkSize, pcmData.size - offset)
                    val written = audioTrack.write(pcmData, offset, length, AudioTrack.WRITE_BLOCKING)
                    if (written > 0) {
                        val chunk = pcmData.copyOfRange(offset, offset + written)
                        val amp = calculateRms(chunk)
                        withContext(Dispatchers.Main) {
                            onAmplitudeChanged(amp)
                        }
                        offset += written
                    } else {
                        break
                    }
                }

                // Wait until the hardware finishes playing all buffered audio frames
                var waitCount = 0
                while (isActive && isSpeaking && waitCount < 300) {
                    val head = audioTrack.playbackHeadPosition
                    if (head >= totalFrames) {
                        break
                    }
                    delay(30)
                    waitCount++
                }
                delay(80)
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
        scope.launch(Dispatchers.Main) {
            onAmplitudeChanged(0f)
        }
    }

    fun release() {
        stop()
    }
}
