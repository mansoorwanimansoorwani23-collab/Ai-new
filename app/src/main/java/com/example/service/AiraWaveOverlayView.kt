package com.example.service

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-performance hardware-accelerated translucent wave overlay view for background wake mode.
 * - 100% see-through & non-blocking (Touches pass completely through via WindowManager FLAG_NOT_TOUCHABLE).
 * - Smooth fluid wave animation on screen perimeters.
 * - Minimalist "Aira" center indicator with subtle concentric wave ripples.
 */
class AiraWaveOverlayView(context: Context) : View(context) {

    private var phase1 = 0f
    private var phase2 = 0f
    private var rippleProgress = 0f
    private var amplitude = 0f
    private var stateText = "Listening..."
    private var viewAlpha = 0f

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val progress = it.animatedValue as Float
            phase1 = (progress * 2 * PI).toFloat()
            phase2 = ((1f - progress) * 2 * PI).toFloat()
            rippleProgress = (progress * 1.5f) % 1f
            invalidate()
        }
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 72f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 0, 255, 157) // Quantum Neon Green
        textSize = 32f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }

    private val waveFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val waveStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.argb(180, 0, 255, 157)
    }

    private val waveStrokePaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = Color.argb(140, 0, 229, 255) // Cyan glow
    }

    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    init {
        // Fully transparent background
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animateFade(0f, 1f, 350) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    fun updateAmplitude(amp: Float) {
        this.amplitude = amp.coerceIn(0f, 1f)
        invalidate()
    }

    fun updateStateText(text: String) {
        this.stateText = text
        invalidate()
    }

    fun fadeOutAndRemove(onComplete: () -> Unit) {
        animateFade(viewAlpha, 0f, 350) {
            animator.cancel()
            onComplete()
        }
    }

    private fun animateFade(from: Float, to: Float, durationMs: Long, onEnd: () -> Unit) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = durationMs
            addUpdateListener {
                viewAlpha = it.animatedValue as Float
                alpha = viewAlpha
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val baseWaveAmp = (35f + amplitude * 55f)

        // 1. TOP FLUID WAVE
        val topPath = Path().apply {
            moveTo(0f, 0f)
            val points = 30
            for (i in 0..points) {
                val x = (i.toFloat() / points) * w
                val angle = (x / w) * 2 * PI.toFloat() * 2 + phase1
                val y = (sin(angle) * baseWaveAmp + baseWaveAmp * 1.1f).coerceAtLeast(0f)
                lineTo(x, y)
            }
            lineTo(w, 0f)
            close()
        }
        waveFillPaint.shader = LinearGradient(
            0f, 0f, 0f, baseWaveAmp * 2.5f,
            Color.argb((70 * viewAlpha).toInt(), 0, 255, 157),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(topPath, waveFillPaint)

        val topStroke = Path().apply {
            val points = 30
            for (i in 0..points) {
                val x = (i.toFloat() / points) * w
                val angle = (x / w) * 2 * PI.toFloat() * 2 + phase1
                val y = (sin(angle) * baseWaveAmp + baseWaveAmp * 1.1f).coerceAtLeast(0f)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        waveStrokePaint.alpha = (180 * viewAlpha).toInt()
        canvas.drawPath(topStroke, waveStrokePaint)

        // 2. BOTTOM FLUID WAVE
        val botPath = Path().apply {
            moveTo(0f, h)
            val points = 30
            for (i in 0..points) {
                val x = (i.toFloat() / points) * w
                val angle = (x / w) * 2 * PI.toFloat() * 2 + phase2
                val y = h - (sin(angle) * baseWaveAmp + baseWaveAmp * 1.1f).coerceAtLeast(0f)
                lineTo(x, y)
            }
            lineTo(w, h)
            close()
        }
        waveFillPaint.shader = LinearGradient(
            0f, h - baseWaveAmp * 2.5f, 0f, h,
            Color.TRANSPARENT,
            Color.argb((70 * viewAlpha).toInt(), 0, 229, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(botPath, waveFillPaint)

        val botStroke = Path().apply {
            val points = 30
            for (i in 0..points) {
                val x = (i.toFloat() / points) * w
                val angle = (x / w) * 2 * PI.toFloat() * 2 + phase2
                val y = h - (sin(angle) * baseWaveAmp + baseWaveAmp * 1.1f).coerceAtLeast(0f)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        waveStrokePaint2.alpha = (180 * viewAlpha).toInt()
        canvas.drawPath(botStroke, waveStrokePaint2)

        // 3. CONCENTRIC EXPANDING RIPPLE RINGS BEHIND AIRA TEXT (No solid circle)
        val cx = w / 2f
        val cy = h / 2f
        val baseRadius = 110f + (amplitude * 40f)

        for (k in 0..2) {
            val ringProgressK = (rippleProgress + (k * 0.33f)) % 1f
            val ringRadius = baseRadius + (ringProgressK * 160f)
            val ringAlpha = ((1f - ringProgressK) * 0.45f * viewAlpha).coerceIn(0f, 0.45f)
            val color = if (k % 2 == 0) Color.argb((ringAlpha * 255).toInt(), 0, 255, 157) else Color.argb((ringAlpha * 255).toInt(), 0, 229, 255)
            ripplePaint.color = color
            canvas.drawCircle(cx, cy, ringRadius, ripplePaint)
        }

        // 4. CLEAN AIRA CENTER TEXT (No solid background or panel)
        textPaint.alpha = (255 * viewAlpha).toInt()
        canvas.drawText("Aira", cx, cy + 12f, textPaint)

        subTextPaint.alpha = (210 * viewAlpha).toInt()
        canvas.drawText(stateText, cx, cy + 62f, subTextPaint)
    }
}
