package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiraVoiceListener
import com.example.ai.OpenAIVoiceClient
import com.example.audio.AiraVoiceSynthesizer
import com.example.audio.AudioRecorder
import com.example.bridge.AndroidActionBridge
import com.example.data.model.ActionBadge
import com.example.data.model.ActionResult
import com.example.data.model.AssistantVoiceState
import com.example.data.model.ChatMessage
import com.example.data.model.ContactInfo
import com.example.data.model.MessageSender
import com.example.data.preference.AiraPreferences
import com.example.service.AiraWakeWordService
import com.example.wake.WakeWordDetector
import com.example.wake.WakeWordListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class AuraViewModel(application: Application) : AndroidViewModel(application), AiraVoiceListener, WakeWordListener {

    companion object {
        private const val TAG = "AuraViewModel"
    }

    val preferences = AiraPreferences(application.applicationContext)
    private val actionBridge = AndroidActionBridge(application.applicationContext)
    private val openAIClient = OpenAIVoiceClient(actionBridge, this)
    private val wakeWordDetector = WakeWordDetector(application.applicationContext, this)

    private var voiceSynthesizer: AiraVoiceSynthesizer? = null
    private var liveSpeechRecognizer: SpeechRecognizer? = null
    private var isLiveRecognizerActive = false
    private var recognitionRestartJob: Job? = null

    private val _voiceState = MutableStateFlow(AssistantVoiceState.DISCONNECTED)
    val voiceState: StateFlow<AssistantVoiceState> = _voiceState.asStateFlow()

    private val _visualizerAmplitude = MutableStateFlow(0f)
    val visualizerAmplitude: StateFlow<Float> = _visualizerAmplitude.asStateFlow()

    private val _isAiraActivated = MutableStateFlow(false)
    val isAiraActivated: StateFlow<Boolean> = _isAiraActivated.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AIRA,
                text = "Namaste! I'm Aira, your real-time AI assistant developed by Rauf. Speak naturally in Hindi, English, Hinglish, or any language. Say 'Hi Aira' or ask any question, open apps, make calls, and talk freely!"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentAiraText = MutableStateFlow("")
    val currentAiraText: StateFlow<String> = _currentAiraText.asStateFlow()

    val currentArushiText: StateFlow<String> get() = currentAiraText

    private val _currentUserText = MutableStateFlow("")
    val currentUserText: StateFlow<String> = _currentUserText.asStateFlow()

    private val _activeToolBadge = MutableStateFlow<ActionBadge?>(null)
    val activeToolBadge: StateFlow<ActionBadge?> = _activeToolBadge.asStateFlow()

    private val _clarificationContacts = MutableStateFlow<List<ContactInfo>?>(null)
    val clarificationContacts: StateFlow<List<ContactInfo>?> = _clarificationContacts.asStateFlow()

    private val _isMicEnabled = MutableStateFlow(true)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var audioRecorder: AudioRecorder? = null
    private var activationTimeoutJob: Job? = null

    private val wakeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AiraWakeWordService.BROADCAST_WAKE_DETECTED) {
                val phrase = intent.getStringExtra(AiraWakeWordService.EXTRA_WAKE_PHRASE) ?: "Hi Aira"
                onWakeWordDetected(phrase)
            }
        }
    }

    init {
        initVoiceSynthesizer()
        initAudio()
        initWakeService()
        registerWakeReceiver()
    }

    private fun initVoiceSynthesizer() {
        voiceSynthesizer = AiraVoiceSynthesizer(
            context = getApplication(),
            onPlaybackStarted = {
                _voiceState.value = AssistantVoiceState.SPEAKING
                _isAiraActivated.value = true
                stopLiveSpeechRecognition() // Pause speech input while speaking to prevent echo
            },
            onPlaybackFinished = {
                if (_voiceState.value == AssistantVoiceState.SPEAKING) {
                    _voiceState.value = AssistantVoiceState.LISTENING
                    _statusBanner.value = "Listening to you..."
                    // Automatically resume live voice listening for next question
                    startLiveSpeechRecognition()
                }
            },
            onAmplitudeChanged = { amp ->
                if (_voiceState.value == AssistantVoiceState.SPEAKING) {
                    _visualizerAmplitude.value = amp
                }
            }
        )
    }

    private fun initAudio() {
        audioRecorder = AudioRecorder(
            onAudioChunk = { chunk ->
                if (_voiceState.value != AssistantVoiceState.DISCONNECTED && _isMicEnabled.value) {
                    openAIClient.sendAudioChunk(chunk)
                }
            },
            onAmplitudeChanged = { amp ->
                if (_voiceState.value == AssistantVoiceState.LISTENING) {
                    _visualizerAmplitude.value = amp
                }
            }
        )
    }

    private fun initWakeService() {
        wakeWordDetector.updateWakePhrase(preferences.wakePhrase.value)
        if (preferences.isWakeWordEnabled.value) {
            wakeWordDetector.startListening()
        }
    }

    private fun registerWakeReceiver() {
        val filter = IntentFilter(AiraWakeWordService.BROADCAST_WAKE_DETECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<Application>().registerReceiver(
                wakeBroadcastReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            getApplication<Application>().registerReceiver(wakeBroadcastReceiver, filter)
        }
    }

    fun startLiveSession() {
        val hasRecordPerm = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasRecordPerm) {
            _statusBanner.value = "Microphone permission required for Live Session."
            return
        }

        val apiKey = preferences.getEffectiveApiKey()
        _voiceState.value = AssistantVoiceState.CONNECTING
        _statusBanner.value = "Connecting to Aira Realtime Engine..."
        triggerActivationAnimation()

        // Temporarily pause background wake word during active live session
        wakeWordDetector.pause()

        viewModelScope.launch(Dispatchers.IO) {
            openAIClient.connect(apiKey, preferences.voice.value)
        }

        // Start live continuous speech recognition
        startLiveSpeechRecognition()
    }

    fun stopLiveSession() {
        stopLiveSpeechRecognition()
        audioRecorder?.stop()
        voiceSynthesizer?.stop()
        openAIClient.disconnect()
        _voiceState.value = AssistantVoiceState.DISCONNECTED
        _statusBanner.value = "Aira in standby • Say \"${preferences.wakePhrase.value}\" to wake"
        _visualizerAmplitude.value = 0f
        _isAiraActivated.value = false

        if (preferences.isWakeWordEnabled.value) {
            wakeWordDetector.resume()
        }
    }

    fun toggleSession() {
        if (_voiceState.value == AssistantVoiceState.DISCONNECTED || _voiceState.value == AssistantVoiceState.ERROR) {
            startLiveSession()
        } else {
            stopLiveSession()
        }
    }

    fun triggerActivationAnimation() {
        _isAiraActivated.value = true
        activationTimeoutJob?.cancel()
        activationTimeoutJob = viewModelScope.launch {
            delay(12000)
            if (_voiceState.value == AssistantVoiceState.DISCONNECTED) {
                _isAiraActivated.value = false
            }
        }
    }

    fun toggleMic() {
        val current = _isMicEnabled.value
        _isMicEnabled.value = !current
        if (!current) {
            if (_voiceState.value == AssistantVoiceState.LISTENING) {
                startLiveSpeechRecognition()
                audioRecorder?.start()
            }
        } else {
            stopLiveSpeechRecognition()
            audioRecorder?.stop()
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _currentUserText.value = trimmed
        _voiceState.value = AssistantVoiceState.THINKING
        _statusBanner.value = "Aira is thinking..."
        triggerActivationAnimation()

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )
        _messages.value = _messages.value + userMsg

        processUserQuery(trimmed)
    }

    private fun processUserQuery(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            openAIClient.processPrompt(query) { answer, actionResult ->
                viewModelScope.launch(Dispatchers.Main) {
                    val airaMsg = ChatMessage(
                        sender = MessageSender.AIRA,
                        text = answer,
                        actionDetails = actionResult?.let { ActionBadge(it.actionName, it.message, it.success) }
                    )
                    _messages.value = _messages.value + airaMsg
                    _currentAiraText.value = ""

                    // Speak the answer aloud
                    val apiKey = preferences.getEffectiveApiKey()
                    _voiceState.value = AssistantVoiceState.SPEAKING
                    _statusBanner.value = "Aira speaking..."
                    voiceSynthesizer?.speak(answer, apiKey, preferences.voice.value)
                }
            }
        }
    }

    fun executeQuickCommand(command: String) {
        sendText(command)
    }

    // --- Continuous Live Speech Recognition Engine ---

    private fun startLiveSpeechRecognition() {
        if (!_isMicEnabled.value || _voiceState.value == AssistantVoiceState.DISCONNECTED) return

        viewModelScope.launch(Dispatchers.Main) {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                    Log.w(TAG, "SpeechRecognizer not available on device")
                    return@launch
                }

                liveSpeechRecognizer?.destroy()
                liveSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                    setRecognitionListener(createLiveRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplication<Application>().packageName)
                }

                liveSpeechRecognizer?.startListening(intent)
                isLiveRecognizerActive = true
                Log.d(TAG, "Live speech recognition listening started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start live speech recognition", e)
            }
        }
    }

    private fun stopLiveSpeechRecognition() {
        isLiveRecognizerActive = false
        recognitionRestartJob?.cancel()
        recognitionRestartJob = null
        viewModelScope.launch(Dispatchers.Main) {
            try {
                liveSpeechRecognizer?.stopListening()
                liveSpeechRecognizer?.cancel()
                liveSpeechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping live recognizer", e)
            }
            liveSpeechRecognizer = null
        }
    }

    private fun createLiveRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Live STT Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "User started speaking in Live Session")
            if (_voiceState.value == AssistantVoiceState.SPEAKING) {
                // Interruption
                voiceSynthesizer?.stop()
                openAIClient.interrupt()
            }
            _voiceState.value = AssistantVoiceState.LISTENING
            _statusBanner.value = "Listening to you..."
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (_voiceState.value == AssistantVoiceState.LISTENING) {
                val normalizedAmp = (rmsdB / 10f).coerceIn(0.1f, 1f)
                _visualizerAmplitude.value = normalizedAmp
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            Log.d(TAG, "User finished speech utterance")
        }

        override fun onError(error: Int) {
            Log.d(TAG, "Live STT error code: $error")
            scheduleLiveRecognizerRestart()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val query = matches?.firstOrNull()?.trim()

            if (!query.isNullOrBlank()) {
                Log.d(TAG, "Live STT Result: '$query'")
                sendText(query)
            } else {
                scheduleLiveRecognizerRestart()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull()?.trim()
            if (!partial.isNullOrBlank()) {
                _currentUserText.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun scheduleLiveRecognizerRestart() {
        if (!isLiveRecognizerActive || _voiceState.value == AssistantVoiceState.DISCONNECTED || _voiceState.value == AssistantVoiceState.SPEAKING) {
            return
        }

        recognitionRestartJob?.cancel()
        recognitionRestartJob = viewModelScope.launch(Dispatchers.Main) {
            delay(350)
            if (isLiveRecognizerActive && _voiceState.value != AssistantVoiceState.SPEAKING) {
                startLiveSpeechRecognition()
            }
        }
    }

    // --- Settings & Bridge Handlers ---

    fun updateWakePhrase(newPhrase: String) {
        preferences.setWakePhrase(newPhrase)
        wakeWordDetector.updateWakePhrase(newPhrase)
        if (preferences.isWakeWordEnabled.value) {
            AiraWakeWordService.start(getApplication(), newPhrase)
        }
        _statusBanner.value = "Wake phrase set to: \"$newPhrase\""
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        preferences.setWakeWordEnabled(enabled)
        if (enabled) {
            wakeWordDetector.startListening()
            if (preferences.isBackgroundAssistantEnabled.value) {
                AiraWakeWordService.start(getApplication(), preferences.wakePhrase.value)
            }
            _statusBanner.value = "Wake word listening active"
        } else {
            wakeWordDetector.stopListening()
            AiraWakeWordService.stop(getApplication())
            _statusBanner.value = "Wake word listening disabled"
        }
    }

    fun setBackgroundAssistantEnabled(enabled: Boolean) {
        preferences.setBackgroundAssistantEnabled(enabled)
        if (enabled && preferences.isWakeWordEnabled.value) {
            AiraWakeWordService.start(getApplication(), preferences.wakePhrase.value)
            _statusBanner.value = "Background Assistant enabled"
        } else {
            AiraWakeWordService.stop(getApplication())
            _statusBanner.value = "Background Assistant disabled"
        }
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        preferences.setAutoStartEnabled(enabled)
        _statusBanner.value = if (enabled) "Auto Start on Boot enabled" else "Auto Start on Boot disabled"
    }

    fun setVoice(voiceName: String) {
        preferences.setVoice(voiceName)
        _statusBanner.value = "Voice changed to ${voiceName.replaceFirstChar { it.uppercase() }}"
    }

    fun setCustomApiKey(key: String) {
        preferences.setCustomApiKey(key)
        _statusBanner.value = "OpenAI API key configured"
    }

    fun onContactSelected(contact: ContactInfo) {
        _clarificationContacts.value = null
        val result = actionBridge.makeCall(contact.phoneNumber)
        onToolCompleted("callContact", result)
        sendText("Calling ${contact.name}")
    }

    fun dismissClarification() {
        _clarificationContacts.value = null
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    // --- WakeWordListener Implementation ---

    override fun onWakeWordDetected(phrase: String) {
        Log.d(TAG, "Wake word detected: $phrase")
        viewModelScope.launch(Dispatchers.Main) {
            triggerActivationAnimation()
            _statusBanner.value = "Heard \"$phrase\" • Aira Activated"
            if (_voiceState.value == AssistantVoiceState.DISCONNECTED || _voiceState.value == AssistantVoiceState.ERROR) {
                startLiveSession()
            }
        }
    }

    override fun onWakeListeningStateChanged(isListening: Boolean) {
        Log.d(TAG, "Wake word detector listening: $isListening")
    }

    // --- AiraVoiceListener Implementation ---

    override fun onConnected() {
        viewModelScope.launch(Dispatchers.Main) {
            _voiceState.value = AssistantVoiceState.LISTENING
            _statusBanner.value = "Aira Live Session • Listening"
            _isAiraActivated.value = true
            if (_isMicEnabled.value) {
                audioRecorder?.start()
                startLiveSpeechRecognition()
            }
        }
    }

    override fun onDisconnected(reason: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _voiceState.value = AssistantVoiceState.DISCONNECTED
            _statusBanner.value = "Aira in standby • Say \"${preferences.wakePhrase.value}\" to wake"
            stopLiveSpeechRecognition()
            audioRecorder?.stop()
            if (preferences.isWakeWordEnabled.value) {
                wakeWordDetector.resume()
            }
        }
    }

    override fun onError(errorMessage: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _statusBanner.value = "Aira Notice: $errorMessage"
        }
    }

    override fun onAiraTranscript(text: String, isComplete: Boolean) {
        viewModelScope.launch(Dispatchers.Main) {
            if (text.isNotEmpty()) {
                _currentAiraText.value = text
            }
        }
    }

    override fun onUserTranscript(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _currentUserText.value = text
        }
    }

    override fun onAudioChunkReceived(pcmData: ByteArray, sampleRate: Int) {
        // Realtime PCM chunk received from WebSocket
        _voiceState.value = AssistantVoiceState.SPEAKING
    }

    override fun onInterrupted() {
        viewModelScope.launch(Dispatchers.Main) {
            voiceSynthesizer?.stop()
            openAIClient.interrupt()
            _voiceState.value = AssistantVoiceState.LISTENING
            _statusBanner.value = "Listening to you..."
            _visualizerAmplitude.value = 0f
            startLiveSpeechRecognition()
        }
    }

    override fun onToolCall(toolName: String, summary: String, arguments: JSONObject) {
        viewModelScope.launch(Dispatchers.Main) {
            _voiceState.value = AssistantVoiceState.EXECUTING_ACTION
            val badge = ActionBadge(toolName, summary, true)
            _activeToolBadge.value = badge
            _statusBanner.value = summary
        }
    }

    override fun onToolCompleted(toolName: String, result: ActionResult) {
        viewModelScope.launch(Dispatchers.Main) {
            val badge = ActionBadge(toolName, result.message, result.success)
            _activeToolBadge.value = badge
            _statusBanner.value = result.message

            if (result.contactsList.size > 1) {
                _clarificationContacts.value = result.contactsList
            }

            val msg = ChatMessage(
                sender = MessageSender.SYSTEM,
                text = result.message,
                actionDetails = badge
            )
            _messages.value = _messages.value + msg

            if (_voiceState.value == AssistantVoiceState.EXECUTING_ACTION) {
                _voiceState.value = AssistantVoiceState.LISTENING
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<Application>().unregisterReceiver(wakeBroadcastReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        stopLiveSpeechRecognition()
        audioRecorder?.stop()
        voiceSynthesizer?.release()
        openAIClient.disconnect()
        wakeWordDetector.stopListening()
    }
}
