package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantVoiceState
import com.example.ui.theme.QuantumBrightGreen
import com.example.ui.theme.QuantumCyanGlow
import com.example.ui.theme.QuantumNeonGreen
import kotlin.math.PI
import kotlin.math.sin

/**
 * Fluid, fully transparent AI energy wave wake animation:
 * - Completely transparent / translucent (NO black screen, NO solid circles/squares, NO opaque panels).
 * - Smooth, fluid wave flowing along the display edges.
 * - Minimalist, elegant "Aira" center indicator with subtle concentric wave ripples.
 * - Non-blocking: All phone screen contents remain clearly visible and interactive.
 */
@Composable
fun AiraActivationOverlay(
    isActivated: Boolean,
    voiceState: AssistantVoiceState,
    amplitude: Float,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim_transition")

    // Smooth wave phases for multi-harmonic oscillation
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_1"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = (2 * PI).toFloat(),
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_2"
    )

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_progress"
    )

    val dynamicScale by animateFloatAsState(
        targetValue = 1f + (amplitude * 0.45f),
        animationSpec = tween(120),
        label = "dynamic_scale"
    )

    AnimatedVisibility(
        visible = isActivated,
        enter = fadeIn(tween(350)) + scaleIn(tween(400, easing = FastOutSlowInEasing), initialScale = 0.94f),
        exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 0.96f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 1. Fluid AI Energy Edge Waves Canvas (Top, Bottom, Left, Right)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val baseWaveAmp = (14.dp.toPx() + amplitude * 22.dp.toPx())

                // --- TOP EDGE WAVES ---
                val topPath1 = Path().apply {
                    moveTo(0f, 0f)
                    val points = 30
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val angle = (x / width) * 2 * PI.toFloat() * 2 + phase1
                        val y = (sin(angle) * baseWaveAmp + baseWaveAmp * 1.2f).coerceAtLeast(0f)
                        lineTo(x, y)
                    }
                    lineTo(width, 0f)
                    close()
                }
                drawPath(
                    path = topPath1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            QuantumNeonGreen.copy(alpha = 0.28f),
                            QuantumCyanGlow.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )

                val topStrokePath = Path().apply {
                    val points = 30
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val angle = (x / width) * 2 * PI.toFloat() * 2 + phase1
                        val y = (sin(angle) * baseWaveAmp + baseWaveAmp * 1.2f).coerceAtLeast(0f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = topStrokePath,
                    color = QuantumNeonGreen.copy(alpha = 0.65f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Secondary top wave harmonic
                val topStrokePath2 = Path().apply {
                    val points = 30
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val angle = (x / width) * 2 * PI.toFloat() * 3 + phase2
                        val y = (sin(angle) * (baseWaveAmp * 0.7f) + baseWaveAmp * 0.9f).coerceAtLeast(0f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = topStrokePath2,
                    color = QuantumCyanGlow.copy(alpha = 0.45f),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // --- BOTTOM EDGE WAVES ---
                val botPath1 = Path().apply {
                    moveTo(0f, height)
                    val points = 30
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val angle = (x / width) * 2 * PI.toFloat() * 2 + phase2
                        val y = height - (sin(angle) * baseWaveAmp + baseWaveAmp * 1.2f).coerceAtLeast(0f)
                        lineTo(x, y)
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = botPath1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            QuantumCyanGlow.copy(alpha = 0.12f),
                            QuantumNeonGreen.copy(alpha = 0.28f)
                        )
                    )
                )

                val botStrokePath = Path().apply {
                    val points = 30
                    for (i in 0..points) {
                        val x = (i.toFloat() / points) * width
                        val angle = (x / width) * 2 * PI.toFloat() * 2 + phase2
                        val y = height - (sin(angle) * baseWaveAmp + baseWaveAmp * 1.2f).coerceAtLeast(0f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = botStrokePath,
                    color = QuantumBrightGreen.copy(alpha = 0.65f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // --- LEFT & RIGHT SIDE SUBTLE WAVE ACCENTS ---
                val leftPath = Path().apply {
                    val points = 24
                    for (i in 0..points) {
                        val y = (i.toFloat() / points) * height
                        val angle = (y / height) * 2 * PI.toFloat() * 3 + phase1
                        val x = (sin(angle) * (baseWaveAmp * 0.5f) + baseWaveAmp * 0.6f).coerceAtLeast(0f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = leftPath,
                    color = QuantumNeonGreen.copy(alpha = 0.35f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                val rightPath = Path().apply {
                    val points = 24
                    for (i in 0..points) {
                        val y = (i.toFloat() / points) * height
                        val angle = (y / height) * 2 * PI.toFloat() * 3 + phase2
                        val x = width - (sin(angle) * (baseWaveAmp * 0.5f) + baseWaveAmp * 0.6f).coerceAtLeast(0f)
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(
                    path = rightPath,
                    color = QuantumCyanGlow.copy(alpha = 0.35f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 2. Subtle Concentric Fluid Waves Behind Center Text (No solid circle or square)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                val baseRadius = 50.dp.toPx() * dynamicScale

                // 3 expanding translucent harmonic ripple rings
                for (k in 0..2) {
                    val ringProgress = (rippleProgress + (k * 0.33f)) % 1f
                    val ringRadius = baseRadius + (ringProgress * 65.dp.toPx())
                    val ringAlpha = ((1f - ringProgress) * 0.45f).coerceIn(0f, 0.45f)
                    val ringColor = if (k % 2 == 0) QuantumNeonGreen else QuantumCyanGlow

                    drawCircle(
                        color = ringColor.copy(alpha = ringAlpha),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = (1.6f * (1f - ringProgress * 0.5f)).dp.toPx())
                    )
                }
            }

            // 3. Transparent Minimalist "Aira" Center Indicator (Clean typography, no solid box)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "Aira",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 2.5.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitle = when (voiceState) {
                    AssistantVoiceState.SPEAKING -> "Speaking..."
                    AssistantVoiceState.LISTENING -> "Listening..."
                    AssistantVoiceState.THINKING -> "Thinking..."
                    AssistantVoiceState.EXECUTING_ACTION -> "Executing..."
                    AssistantVoiceState.CONNECTING -> "Connecting..."
                    else -> "Online"
                }

                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = QuantumNeonGreen.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
