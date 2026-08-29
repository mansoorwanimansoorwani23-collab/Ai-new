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
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.preference.AiraPreferences
import com.example.wake.WakeWordDetector
import com.example.wake.WakeWordListener

class AiraWakeWordService : Service(), WakeWordListener {

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
    private var currentPhrase = "Hi Aira"

    override fun onCreate() {
        super.onCreate()
        preferences = AiraPreferences(this)
        detector = WakeWordDetector(this, this)
        createNotificationChannel()
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
        Log.d(TAG, "Wake word detected in foreground service: $phrase")
        val broadcastIntent = Intent(BROADCAST_WAKE_DETECTED).apply {
            putExtra(EXTRA_WAKE_PHRASE, phrase)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)

        // Bring MainActivity to foreground
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_AUTO_WAKE", true)
        }
        startActivity(launchIntent)
    }

    override fun onWakeListeningStateChanged(isListening: Boolean) {
        Log.d(TAG, "Wake listening state changed: $isListening")
    }

    override fun onDestroy() {
        detector?.stopListening()
        detector = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
