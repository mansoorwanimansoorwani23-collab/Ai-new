package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.preference.AiraPreferences
import com.example.service.AiraWakeWordService

/**
 * Automatically starts the Aira Wake Word Foreground Service on device boot
 * if enabled in Aira Preferences.
 */
class AiraBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AiraBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED == action || "android.intent.action.QUICKBOOT_POWERON" == action) {
            Log.d(TAG, "Device boot completed, checking Aira preferences...")
            val prefs = AiraPreferences(context)
            if (prefs.isBackgroundAssistantEnabled.value && prefs.isAutoStartEnabled.value) {
                Log.d(TAG, "Starting Aira Wake Word Foreground Service on boot with phrase: ${prefs.wakePhrase.value}")
                AiraWakeWordService.start(context, prefs.wakePhrase.value)
            } else {
                Log.d(TAG, "Background assistant or auto-start is disabled in preferences.")
            }
        }
    }
}
