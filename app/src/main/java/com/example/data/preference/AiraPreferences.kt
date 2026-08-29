package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AiraPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "aira_assistant_preferences"
        private const val KEY_WAKE_PHRASE = "wake_phrase"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_BACKGROUND_ASSISTANT_ENABLED = "background_assistant_enabled"
        private const val KEY_AUTO_START_ENABLED = "auto_start_enabled"
        private const val KEY_FIRST_SETUP_COMPLETED = "first_setup_completed"
        private const val KEY_CUSTOM_API_KEY = "custom_openai_api_key"
        private const val KEY_VOICE = "openai_voice"
        private const val KEY_AUTO_LISTEN_ON_WAKE = "auto_listen_on_wake"
        private const val KEY_LANGUAGE_MODE = "language_mode"
        
        const val DEFAULT_WAKE_PHRASE = "Hi Aira"
        const val DEFAULT_VOICE = "alloy"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _wakePhrase = MutableStateFlow(
        prefs.getString(KEY_WAKE_PHRASE, DEFAULT_WAKE_PHRASE) ?: DEFAULT_WAKE_PHRASE
    )
    val wakePhrase: StateFlow<String> = _wakePhrase.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
    )
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _isBackgroundAssistantEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_BACKGROUND_ASSISTANT_ENABLED, true)
    )
    val isBackgroundAssistantEnabled: StateFlow<Boolean> = _isBackgroundAssistantEnabled.asStateFlow()

    private val _isAutoStartEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_START_ENABLED, true)
    )
    val isAutoStartEnabled: StateFlow<Boolean> = _isAutoStartEnabled.asStateFlow()

    private val _isFirstSetupCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_FIRST_SETUP_COMPLETED, false)
    )
    val isFirstSetupCompleted: StateFlow<Boolean> = _isFirstSetupCompleted.asStateFlow()

    private val _customApiKey = MutableStateFlow(
        prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
    )
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _voice = MutableStateFlow(
        prefs.getString(KEY_VOICE, DEFAULT_VOICE) ?: DEFAULT_VOICE
    )
    val voice: StateFlow<String> = _voice.asStateFlow()

    private val _autoListenOnWake = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_LISTEN_ON_WAKE, true)
    )
    val autoListenOnWake: StateFlow<Boolean> = _autoListenOnWake.asStateFlow()

    fun setWakePhrase(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isNotEmpty()) {
            prefs.edit().putString(KEY_WAKE_PHRASE, trimmed).apply()
            _wakePhrase.value = trimmed
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, enabled).apply()
        _isWakeWordEnabled.value = enabled
    }

    fun setBackgroundAssistantEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_ASSISTANT_ENABLED, enabled).apply()
        _isBackgroundAssistantEnabled.value = enabled
    }

    fun setAutoStartEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_START_ENABLED, enabled).apply()
        _isAutoStartEnabled.value = enabled
    }

    fun setFirstSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_SETUP_COMPLETED, completed).apply()
        _isFirstSetupCompleted.value = completed
    }

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
        _customApiKey.value = trimmed
    }

    fun setVoice(voiceName: String) {
        val trimmed = voiceName.trim().lowercase()
        prefs.edit().putString(KEY_VOICE, trimmed).apply()
        _voice.value = trimmed
    }

    fun setAutoListenOnWake(autoListen: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LISTEN_ON_WAKE, autoListen).apply()
        _autoListenOnWake.value = autoListen
    }

    /**
     * Resolves the effective OpenAI API Key, preferring user custom key if set,
     * otherwise falling back to injected BuildConfig key.
     */
    fun getEffectiveApiKey(): String {
        val custom = _customApiKey.value
        if (custom.isNotBlank()) return custom

        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("OPENAI_API_KEY")
            val value = field.get(null) as? String ?: ""
            if (value.startsWith("MY_") || value.startsWith("your_")) "" else value
        } catch (e: Exception) {
            ""
        }
    }
}
