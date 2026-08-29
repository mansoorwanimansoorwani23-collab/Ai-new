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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantVoiceState
import com.example.ui.theme.AuraAccent
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraPrimaryVariant
import com.example.ui.theme.AuraSecondary
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.QuantumBrightGreen
import com.example.ui.theme.QuantumCyanGlow
import com.example.ui.theme.QuantumHudBorder
import com.example.ui.theme.QuantumNeonGreen
import kotlin.math.cos
import kotlin.math.sin

/**
 * Quantum Hacker Cyber Activation Animation:
 * - Glowing Matrix Cyber edge brackets and vignette.
 * - Central 3D Quantum Atom Orbital holographic reactor.
 * - Monospace cyber status HUD.
 */
@Composable
fun AiraActivationOverlay(
    isActivated: Boolean,
    voiceState: AssistantVoiceState,
    amplitude: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aira_activation_anim")

    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_1"
    )

    val orbitAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val dynamicScale by animateFloatAsState(
        targetValue = 1f + (amplitude * 0.5f),
        animationSpec = tween(100),
        label = "dynamic_amp_scale"
    )

    val edgeGlowAlpha by animateFloatAsState(
        targetValue = if (isActivated) 0.88f else 0f,
        animationSpec = tween(500),
        label = "edge_glow_alpha"
    )

    AnimatedVisibility(
        visible = isActivated,
        enter = fadeIn(tween(350)) + scaleIn(tween(450, easing = FastOutSlowInEasing), initialScale = 0.88f),
        exit = fadeOut(tween(350)) + scaleOut(tween(350), targetScale = 0.92f),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            // 1. Cyber Matrix Green Edge Glow around the entire viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                QuantumNeonGreen.copy(alpha = edgeGlowAlpha * 0.8f),
                                QuantumCyanGlow.copy(alpha = edgeGlowAlpha * 0.9f),
                                QuantumBrightGreen.copy(alpha = edgeGlowAlpha * 0.7f),
                                AuraPrimaryVariant.copy(alpha = edgeGlowAlpha * 0.8f),
                                QuantumNeonGreen.copy(alpha = edgeGlowAlpha * 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .blur(16.dp)
            )

            // 2. Corner Matrix Vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                QuantumNeonGreen.copy(alpha = edgeGlowAlpha * 0.08f),
                                Color(0xFF020704).copy(alpha = edgeGlowAlpha * 0.6f)
                            )
                        )
                    )
            )

            // 3. Central Quantum Atom Holographic Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 30.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Outer diffuse green plasma halo
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .scale(pulseScale * dynamicScale * 1.35f)
                            .blur(40.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        QuantumNeonGreen.copy(alpha = 0.65f),
                                        QuantumCyanGlow.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    // 3D Atomic Orbit Rings
                    Canvas(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseScale * dynamicScale)
                    ) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val rx = size.width / 2.2f
                        val ry = size.height / 5.5f

                        // Orbit 1: Horizontal Ellipse (0 deg)
                        drawOval(
                            color = QuantumNeonGreen.copy(alpha = 0.75f),
                            topLeft = Offset(cx - rx, cy - ry),
                            size = Size(rx * 2, ry * 2),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        // Electron 1
                        val rad1 = Math.toRadians(orbitAngle1.toDouble())
                        val e1x = cx + rx * cos(rad1).toFloat()
                        val e1y = cy + ry * sin(rad1).toFloat()
                        drawCircle(
                            color = Color.White,
                            radius = 4.5.dp.toPx(),
                            center = Offset(e1x, e1y)
                        )

                        // Orbit 2: Tilted Ellipse (60 deg tilt)
                        val rad2 = Math.toRadians(orbitAngle2.toDouble())
                        val raw2x = rx * cos(rad2).toFloat()
                        val raw2y = ry * sin(rad2).toFloat()
                        val cos60 = 0.5f
                        val sin60 = 0.866f
                        val e2x = cx + (raw2x * cos60 - raw2y * sin60)
                        val e2y = cy + (raw2x * sin60 + raw2y * cos60)

                        drawCircle(
                            color = QuantumCyanGlow,
                            radius = 4.dp.toPx(),
                            center = Offset(e2x, e2y)
                        )
                    }

                    // Inner Quantum Core
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale * dynamicScale)
                            .shadow(
                                elevation = 24.dp,
                                shape = CircleShape,
                                spotColor = QuantumNeonGreen.copy(alpha = 0.7f),
                                ambientColor = QuantumCyanGlow.copy(alpha = 0.4f)
                            )
                            .background(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        QuantumCyanGlow,
                                        QuantumNeonGreen,
                                        AuraAccent,
                                        AuraPrimaryVariant,
                                        QuantumCyanGlow
                                    )
                                ),
                                shape = CircleShape
                            )
                            .padding(2.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0C1F14),
                                            Color(0xFF030A05)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.5f),
                                            QuantumNeonGreen.copy(alpha = 0.6f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aira",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = AuraTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = when (voiceState) {
                                        AssistantVoiceState.SPEAKING -> "TRANSMITTING"
                                        AssistantVoiceState.LISTENING -> "LISTENING"
                                        AssistantVoiceState.THINKING -> "COMPUTING"
                                        AssistantVoiceState.EXECUTING_ACTION -> "EXECUTING"
                                        AssistantVoiceState.CONNECTING -> "SYNCING"
                                        else -> "ONLINE"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
                                    color = QuantumNeonGreen,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "[ QUANTUM HACKER // ACCESS GRANTED ]",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = QuantumNeonGreen,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Aira is listening • Speak naturally in Hindi, Hinglish or English",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = AuraTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
