package com.example.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager

/**
 * Manages the floating translucent Aira wave wake overlay.
 * Uses WindowManager TYPE_APPLICATION_OVERLAY with FLAG_NOT_TOUCHABLE and FLAG_NOT_FOCUSABLE
 * to ensure that the phone's screen, home launcher, icons, widgets, and gestures remain 100% interactive and visible.
 */
object AiraOverlayManager {
    private const val TAG = "AiraOverlayManager"

    private var windowManager: WindowManager? = null
    private var overlayView: AiraWaveOverlayView? = null
    private var isOverlayShowing = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null

    fun show(context: Context, stateText: String = "Listening...", timeoutMs: Long = 10000) {
        mainHandler.post {
            try {
                if (isOverlayShowing && overlayView != null) {
                    overlayView?.updateStateText(stateText)
                    scheduleAutoDismiss(timeoutMs)
                    return@post
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "SYSTEM_ALERT_WINDOW permission not granted; skipping system overlay window")
                    return@post
                }

                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager = wm

                val view = AiraWaveOverlayView(context)
                overlayView = view

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )

                wm.addView(view, params)
                isOverlayShowing = true
                view.updateStateText(stateText)
                scheduleAutoDismiss(timeoutMs)
                Log.d(TAG, "Aira transparent wave overlay shown successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying Aira wave overlay", e)
            }
        }
    }

    fun updateAmplitude(amplitude: Float) {
        mainHandler.post {
            overlayView?.updateAmplitude(amplitude)
        }
    }

    fun updateState(stateText: String) {
        mainHandler.post {
            overlayView?.updateStateText(stateText)
        }
    }

    fun hide() {
        mainHandler.post {
            autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
            autoDismissRunnable = null

            val view = overlayView
            val wm = windowManager

            if (view != null && wm != null && isOverlayShowing) {
                view.fadeOutAndRemove {
                    try {
                        wm.removeViewImmediate(view)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing overlay view", e)
                    }
                    overlayView = null
                    isOverlayShowing = false
                    Log.d(TAG, "Aira wave overlay hidden")
                }
            } else {
                overlayView = null
                isOverlayShowing = false
            }
        }
    }

    private fun scheduleAutoDismiss(timeoutMs: Long) {
        autoDismissRunnable?.let { mainHandler.removeCallbacks(it) }
        autoDismissRunnable = Runnable {
            hide()
        }
        mainHandler.postDelayed(autoDismissRunnable!!, timeoutMs)
    }
}
