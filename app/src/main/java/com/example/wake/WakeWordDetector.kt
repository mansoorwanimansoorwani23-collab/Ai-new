package com.example.wake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

interface WakeWordListener {
    fun onWakeWordDetected(phrase: String)
    fun onWakeListeningStateChanged(isListening: Boolean)
}

/**
 * On-device real-time wake phrase detection engine.
 * Supports configurable phrases (e.g. "Hi Aira", "Hey Aira", "Aira", "Namaste Aira").
 * Resilient against background silence timeouts with automatic instant restarting.
 */
class WakeWordDetector(
    private val context: Context,
    private val listener: WakeWordListener
) {
    companion object {
        private const val TAG = "WakeWordDetector"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isPaused = false
    private var targetWakePhrase = "Hi Aira"
    private var restartJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun updateWakePhrase(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isNotEmpty()) {
            targetWakePhrase = trimmed
            Log.d(TAG, "Wake phrase updated to: '$targetWakePhrase'")
        }
    }

    fun startListening(): Boolean {
        if (isListening) return true

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer is not available on this device")
            return false
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            isPaused = false
            listener.onWakeListeningStateChanged(true)
            Log.d(TAG, "Wake word listening started for '$targetWakePhrase'")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start wake word detector", e)
            isListening = false
            listener.onWakeListeningStateChanged(false)
            return false
        }
    }

    fun pause() {
        isPaused = true
        stopListening()
    }

    fun resume() {
        isPaused = false
        startListening()
    }

    fun stopListening() {
        isListening = false
        restartJob?.cancel()
        restartJob = null
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        }
        speechRecognizer = null
        listener.onWakeListeningStateChanged(false)
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            Log.d(TAG, "SpeechRecognizer error: $error (restarting wake listener)")
            scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            checkMatches(matches)
            scheduleRestart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            checkMatches(matches)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun checkMatches(matches: List<String>?) {
        if (matches == null || isPaused) return

        val normalizedTarget = targetWakePhrase.lowercase().trim()
        val targetKeywords = normalizedTarget.split("\\s+".toRegex()).filter { it.isNotBlank() }

        for (match in matches) {
            val text = match.lowercase().trim()
            Log.d(TAG, "Partial wake recognition: '$text'")

            // 1. Direct or substring match
            if (text.contains(normalizedTarget) || normalizedTarget.contains(text) && text.length > 3) {
                triggerWake(match)
                return
            }

            // 2. Keyword presence (e.g. contains "aira")
            if (text.contains("aira") || text.contains("ira") || text.contains("ayra")) {
                triggerWake(match)
                return
            }

            // 3. Multi-word match
            if (targetKeywords.all { kw -> text.contains(kw) }) {
                triggerWake(match)
                return
            }
        }
    }

    private fun triggerWake(detectedText: String) {
        Log.d(TAG, "WAKE WORD DETECTED: '$detectedText' -> Target: '$targetWakePhrase'")
        listener.onWakeWordDetected(targetWakePhrase)
    }

    private fun scheduleRestart() {
        if (!isListening || isPaused) return

        restartJob?.cancel()
        restartJob = scope.launch {
            delay(300)
            if (isActive && isListening && !isPaused) {
                try {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }

                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    }
                    speechRecognizer?.startListening(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error restarting SpeechRecognizer", e)
                }
            }
        }
    }
}
