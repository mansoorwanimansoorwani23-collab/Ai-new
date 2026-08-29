package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.ai.AiraVoiceListener
import com.example.ai.OpenAIVoiceClient
import com.example.audio.AiraVoiceSynthesizer
import com.example.bridge.AndroidActionBridge
import com.example.data.preference.AiraPreferences
import com.example.wake.WakeWordDetector
import com.example.wake.WakeWordListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class AiraWakeWordService : Service(), WakeWordListener, AiraVoiceListener {

    companion object {
        private const val TAG = "AiraWakeWordService"
        const val CHANNEL_ID = "aira_wake_word_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        const val ACTION_UPDATE_PHRASE = "com.example.service.action.UPDATE_PHRASE"
        const val BROADCAST_WAKE_DETECTED = "com.example.service.broadcast.WAKE_DETECTED"
        const val EXTRA_WAKE_PHRASE = "extra_wake_phrase"

        fun start(context: Context, wakePhrase: String) {
            val intent = Intent(context, AiraWakeWordService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_WAKE_PHRASE, wakePhrase)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AiraWakeWordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var detector: WakeWordDetector? = null
    private lateinit var preferences: AiraPreferences
    private lateinit var actionBridge: AndroidActionBridge
    private var openAIClient: OpenAIVoiceClient? = null
    private var voiceSynthesizer: AiraVoiceSynthesizer? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentPhrase = "Hi Aira"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isProcessingVoiceSession = false

    override fun onCreate() {
        super.onCreate()
        preferences = AiraPreferences(this)
        actionBridge = AndroidActionBridge(this)
        openAIClient = OpenAIVoiceClient(actionBridge, this)
        detector = WakeWordDetector(this, this)
        initVoiceSynthesizer()
        createNotificationChannel()
    }

    private fun initVoiceSynthesizer() {
        voiceSynthesizer = AiraVoiceSynthesizer(
            context = this,
            onPlaybackStarted = {
                AiraOverlayManager.updateState("Speaking...")
            },
            onPlaybackFinished = {
                serviceScope.launch {
                    delay(800)
                    finishBackgroundVoiceSession()
                }
            },
            onAmplitudeChanged = { amp ->
                AiraOverlayManager.updateAmplitude(amp)
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!preferences.isBackgroundAssistantEnabled.value || !preferences.isWakeWordEnabled.value) {
                    Log.d(TAG, "Background Assistant or Wake Word is disabled; skipping service start.")
                    stopSelf()
                    return START_NOT_STICKY
                }
                val phrase = intent.getStringExtra(EXTRA_WAKE_PHRASE) ?: preferences.wakePhrase.value
                currentPhrase = phrase
                detector?.updateWakePhrase(phrase)
                startForegroundWithNotification(phrase)
                detector?.startListening()
            }
            ACTION_STOP -> {
                detector?.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_PHRASE -> {
                val phrase = intent.getStringExtra(EXTRA_WAKE_PHRASE) ?: preferences.wakePhrase.value
                currentPhrase = phrase
                detector?.updateWakePhrase(phrase)
                updateNotification(phrase)
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification(phrase: String) {
        val notification = buildNotification(phrase)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(phrase: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aira Voice Assistant Active")
            .setContentText("Listening for wake phrase: \"$phrase\"")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(phrase: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(phrase))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aira Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listening service for Aira wake-up phrase"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onWakeWordDetected(phrase: String) {
        Log.d(TAG, "Wake word detected in background service: $phrase")

        // 1. Broadcast wake event to any active foreground activity / viewmodel
        val broadcastIntent = Intent(BROADCAST_WAKE_DETECTED).apply {
            putExtra(EXTRA_WAKE_PHRASE, phrase)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)

        if (isProcessingVoiceSession) return
        isProcessingVoiceSession = true

        // 2. Pause detector while processing user voice command
        detector?.pause()

        // 3. Show fully transparent, non-blocking wave overlay across the phone display
        AiraOverlayManager.show(this, "Listening...", timeoutMs = 12000)

        // 4. Start listening to the user's spoken command in background
        startBackgroundSpeechRecognition()
    }

    private fun startBackgroundSpeechRecognition() {
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    Log.w(TAG, "SpeechRecognizer is not available on device")
                    finishBackgroundVoiceSession()
                    return@post
                }

                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            AiraOverlayManager.updateState("Listening...")
                        }

                        override fun onBeginningOfSpeech() {
                            AiraOverlayManager.updateState("Listening...")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val normAmp = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            AiraOverlayManager.updateAmplitude(normAmp)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            AiraOverlayManager.updateState("Thinking...")
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "Background SpeechRecognizer error: $error")
                            finishBackgroundVoiceSession()
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val userText = matches?.firstOrNull()?.trim() ?: ""
                            if (userText.isNotEmpty()) {
                                handleUserVoicePrompt(userText)
                            } else {
                                finishBackgroundVoiceSession()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start background speech recognizer", e)
                finishBackgroundVoiceSession()
            }
        }
    }

    private fun handleUserVoicePrompt(prompt: String) {
        Log.d(TAG, "Processing background user prompt: '$prompt'")
        AiraOverlayManager.updateState("Thinking...")

        val apiKey = preferences.getEffectiveApiKey()
        openAIClient?.updateApiKey(apiKey)

        serviceScope.launch(Dispatchers.IO) {
            openAIClient?.processPrompt(prompt, apiKey) { answer, actionResult ->
                serviceScope.launch(Dispatchers.Main) {
                    AiraOverlayManager.updateState("Speaking...")
                    voiceSynthesizer?.speak(answer, apiKey, preferences.voice.value)
                }
            }
        }
    }

    private fun finishBackgroundVoiceSession() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up speech recognizer", e)
            }
            speechRecognizer = null

            AiraOverlayManager.hide()
            isProcessingVoiceSession = false

            if (preferences.isWakeWordEnabled.value) {
                detector?.resume()
            }
        }
    }

    override fun onWakeListeningStateChanged(isListening: Boolean) {
        Log.d(TAG, "Wake listening state changed: $isListening")
    }

    // --- AiraVoiceListener Implementation ---
    override fun onConnected() {}
    override fun onDisconnected(reason: String) {}
    override fun onError(errorMessage: String) {
        Log.e(TAG, "OpenAIVoiceClient error: $errorMessage")
        finishBackgroundVoiceSession()
    }
    override fun onAiraTranscript(text: String, isComplete: Boolean) {}
    override fun onUserTranscript(text: String) {}
    override fun onAudioChunkReceived(pcmData: ByteArray, sampleRate: Int) {}
    override fun onInterrupted() {}
    override fun onToolCall(toolName: String, summary: String, arguments: org.json.JSONObject) {}
    override fun onToolCompleted(toolName: String, result: com.example.data.model.ActionResult) {}

    override fun onDestroy() {
        detector?.stopListening()
        detector = null
        AiraOverlayManager.hide()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer on destroy", e)
        }
        speechRecognizer = null
        voiceSynthesizer?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
