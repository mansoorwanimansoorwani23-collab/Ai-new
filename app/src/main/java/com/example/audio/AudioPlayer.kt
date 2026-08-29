package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class AudioPlayer(
    private val onPlaybackStateChanged: (Boolean) -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) {
    companion object {
        private const val TAG = "AudioPlayer"
        const val DEFAULT_SAMPLE_RATE = 24000 // Gemini Live output sample rate
    }

    private var audioTrack: AudioTrack? = null
    private val audioChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isPlaying = false
    private var currentSampleRate = DEFAULT_SAMPLE_RATE

    init {
        startPlaybackLoop()
    }

    private fun initAudioTrack(sampleRate: Int) {
        if (audioTrack != null && currentSampleRate == sampleRate) return

        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing old AudioTrack", e)
        }

        currentSampleRate = sampleRate
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
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
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    private var idleJob: Job? = null

    private fun startPlaybackLoop() {
        playbackJob = scope.launch {
            for (pcmChunk in audioChannel) {
                if (!isActive) break

                if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                    initAudioTrack(currentSampleRate)
                }

                idleJob?.cancel()

                if (!isPlaying) {
                    isPlaying = true
                    onPlaybackStateChanged(true)
                }

                // Compute amplitude for playback visualization
                val amp = calculateRms(pcmChunk)
                onAmplitudeChanged(amp)

                try {
                    audioTrack?.write(pcmChunk, 0, pcmChunk.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing to AudioTrack", e)
                }

                // Launch idle timeout to detect end of audio stream
                idleJob = scope.launch {
                    kotlinx.coroutines.delay(400)
                    if (isPlaying) {
                        isPlaying = false
                        onPlaybackStateChanged(false)
                        onAmplitudeChanged(0f)
                    }
                }
            }
        }
    }

    fun playChunk(pcmChunk: ByteArray, sampleRate: Int = DEFAULT_SAMPLE_RATE) {
        if (currentSampleRate != sampleRate) {
            initAudioTrack(sampleRate)
        }
        audioChannel.trySend(pcmChunk)
    }

    /**
     * Instantly stops playback and empties queued audio on interruption
     */
    fun interrupt() {
        idleJob?.cancel()
        idleJob = null
        // Drain channel
        while (true) {
            val polled = audioChannel.tryReceive().getOrNull() ?: break
        }

        try {
            audioTrack?.apply {
                if (state == AudioTrack.STATE_INITIALIZED) {
                    pause()
                    flush()
                    play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error flushing AudioTrack on interrupt", e)
        }

        isPlaying = false
        onPlaybackStateChanged(false)
        onAmplitudeChanged(0f)
    }

    fun release() {
        interrupt()
        playbackJob?.cancel()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioTrack", e)
        }
        audioTrack = null
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
        return (rms * 3.5f).coerceIn(0f, 1f)
    }
}
