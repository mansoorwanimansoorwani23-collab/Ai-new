package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionBadge
import com.example.data.model.AssistantVoiceState
import com.example.data.model.ChatMessage
import com.example.data.model.ContactInfo
import com.example.data.model.MessageSender
import com.example.ui.theme.AuraAccent
import com.example.ui.theme.AuraBackground
import com.example.ui.theme.AuraDeepEmerald
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraPrimary
import com.example.ui.theme.AuraPrimaryVariant
import com.example.ui.theme.AuraSecondary
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraSurfaceBorder
import com.example.ui.theme.AuraSurfaceElevated
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarning
import com.example.ui.theme.QuantumBrightGreen
import com.example.ui.theme.QuantumCyanGlow
import com.example.ui.theme.QuantumDarkObsidian
import com.example.ui.theme.QuantumHudBorder
import com.example.ui.theme.QuantumNeonGreen
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-Tech Quantum Hacker Cyberpunk UI / UX:
 * - Animated Matrix Digital Binary Rain backdrop
 * - Quantum Atomic Orbital Core Visualizer (3D intersecting orbital rings & pulsing nucleus)
 * - Holographic HUD status panels ("ACCESS GRANTED", "SYSTEM BREACHED", "QUANTUM ENTANGLEMENT")
 * - Live radar target scan reticle and oscilloscope wave
 * - Terminal Monospace styling, glowing green brackets & high-contrast cyberpunk cards
 */
@Composable
fun AuraScreen(
    viewModel: AuraViewModel,
    hasRecordAudioPermission: Boolean = true,
    hasContactsPermission: Boolean = true,
    hasCallPhonePermission: Boolean = true,
    hasNotificationPermission: Boolean = true,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val amplitude by viewModel.visualizerAmplitude.collectAsState()
    val isAiraActivated by viewModel.isAiraActivated.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val currentAiraText by viewModel.currentAiraText.collectAsState()
    val activeToolBadge by viewModel.activeToolBadge.collectAsState()
    val clarificationContacts by viewModel.clarificationContacts.collectAsState()
    val isMicEnabled by viewModel.isMicEnabled.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()

    val preferences = viewModel.preferences
    val wakePhrase by preferences.wakePhrase.collectAsState()
    val isFirstSetupCompleted by preferences.isFirstSetupCompleted.collectAsState()

    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val allPermissionsGranted = hasRecordAudioPermission && hasContactsPermission && hasCallPhonePermission && hasNotificationPermission

    LaunchedEffect(messages.size, currentAiraText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QuantumDarkObsidian)
    ) {
        // 1. Quantum Ambient Background Lighting & Plasma Glow
        AmbientBackgroundGlow(voiceState = voiceState, amplitude = amplitude)

        // 2. Animated Matrix Digital Rain Stream
        MatrixDigitalRain(voiceState = voiceState, amplitude = amplitude)

        // 3. Cyber Wireframe Matrix Grid & Scanlines
        QuantumMatrixGrid()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Cyber Header: [ QUANTUM HACKER // AURA ] + Status Pill & Cyber Settings Button
            ImmersiveHeaderSection(
                voiceState = voiceState,
                onSettingsClick = { showSettingsDialog = true }
            )

            // First Setup Onboarding Banner if permissions missing or setup incomplete
            if (!allPermissionsGranted && !isFirstSetupCompleted) {
                Spacer(modifier = Modifier.height(4.dp))
                FirstSetupPromptCard(
                    wakePhrase = wakePhrase,
                    onGrantPermissions = {
                        onRequestPermissions()
                        preferences.setFirstSetupCompleted(true)
                    },
                    onDismiss = { preferences.setFirstSetupCompleted(true) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cyberpunk Holographic HUD Banner ("ACCESS GRANTED" / "SYSTEM BREACHED" / Radar / Oscilloscope)
            QuantumTelemetryHUD(
                voiceState = voiceState,
                amplitude = amplitude
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quantum Atomic Core Visualizer (3D Rotating Orbits + Audio-Responsive Nucleus)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.42f),
                contentAlignment = Alignment.Center
            ) {
                ImmersiveOrbVisualizer(
                    voiceState = voiceState,
                    amplitude = amplitude,
                    wakePhrase = wakePhrase,
                    onOrbClick = { viewModel.toggleSession() }
                )
            }

            // Real-time status / Action Bridge Banner
            StatusNotificationBanner(
                bannerText = statusBanner,
                activeTool = activeToolBadge,
                onDismiss = { viewModel.clearBanner() }
            )

            // Multiple contacts clarification card
            clarificationContacts?.let { contacts ->
                ContactClarificationCard(
                    contacts = contacts,
                    onContactSelected = { viewModel.onContactSelected(it) },
                    onDismiss = { viewModel.dismissClarification() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Cyber Command Badges: [ CODE BREAK REPEAT ] [ THINK QUANTUM HACK EVERYTHING ]
            CyberCommandMottoBanner(
                onCommandSelected = { viewModel.executeQuickCommand(it) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Voice Command Chips
            QuickActionChips(
                onCommandSelected = { viewModel.executeQuickCommand(it) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Conversation History & Realtime Subtitles
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.36f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageCard(message = msg)
                }

                // Streaming Live Aira Speech Bubble
                if (currentAiraText.isNotBlank()) {
                    item(key = "streaming_aira") {
                        StreamingSpeechBubble(
                            sender = "Aira",
                            text = currentAiraText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Cyber Terminal Input & Quantum Reactor Mic Button
            ImmersiveFooterBar(
                inputText = inputText,
                onInputTextChanged = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendText(inputText)
                        inputText = ""
                    }
                },
                showTextInput = showTextInput,
                onToggleTextInput = { showTextInput = !showTextInput },
                voiceState = voiceState,
                isMicEnabled = isMicEnabled,
                onToggleSession = { viewModel.toggleSession() },
                onToggleMic = { viewModel.toggleMic() }
            )
        }

        // Stunning Aira Activation Overlay
        if (isAiraActivated && voiceState != AssistantVoiceState.DISCONNECTED) {
            AiraActivationOverlay(
                isActivated = isAiraActivated,
                voiceState = voiceState,
                amplitude = amplitude,
                onDismiss = { /* Non-blocking */ }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            AiraSettingsDialog(
                viewModel = viewModel,
                hasRecordAudioPermission = hasRecordAudioPermission,
                hasContactsPermission = hasContactsPermission,
                hasCallPhonePermission = hasCallPhonePermission,
                hasNotificationPermission = hasNotificationPermission,
                onRequestPermissions = onRequestPermissions,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

/**
 * Animated Matrix Digital Binary Rain:
 * Simulates cascading neon green binary streams ('0', '1', 'Q', 'X', '7', '8', '010100')
 */
@Composable
fun MatrixDigitalRain(
    voiceState: AssistantVoiceState,
    amplitude: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "matrix_rain_anim")
    val rainPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val columnCount = 14
        val colWidth = size.width / columnCount
        val binaryChars = listOf("1", "0", "0", "1", "0", "1", "1", "0", "Q", "7", "0", "1", "X", "0")

        for (col in 0 until columnCount) {
            val speedFactor = 0.6f + ((col * 3) % 7) * 0.12f
            val yOffset = ((rainPhase * speedFactor * 2.5f + col * 90f) % (size.height + 200f)) - 100f
            val charCount = 8

            for (i in 0 until charCount) {
                val charY = yOffset - (i * 22.dp.toPx())
                if (charY in 0f..size.height) {
                    val alpha = if (i == 0) {
                        (0.55f + amplitude * 0.35f).coerceIn(0f, 0.9f) // Bright glowing head
                    } else {
                        ((1f - (i.toFloat() / charCount)) * 0.18f).coerceIn(0.02f, 0.25f)
                    }

                    val color = if (i == 0) QuantumBrightGreen else QuantumNeonGreen.copy(alpha = alpha)

                    // Draw digital binary dot / glyph representation
                    drawCircle(
                        color = color,
                        radius = if (i == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                        center = Offset(col * colWidth + (colWidth / 2), charY)
                    )

                    // Connect with faint vertical phosphor trace
                    if (i < charCount - 1) {
                        drawLine(
                            color = QuantumNeonGreen.copy(alpha = alpha * 0.4f),
                            start = Offset(col * colWidth + (colWidth / 2), charY),
                            end = Offset(col * colWidth + (colWidth / 2), charY + 12.dp.toPx()),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cyber Matrix Grid with Corner Crosshairs
 */
@Composable
fun QuantumMatrixGrid() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 40.dp.toPx()
        val gridColor = QuantumNeonGreen.copy(alpha = 0.025f)

        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.8f
            )
            x += step
        }

        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.8f
            )
            y += step
        }
    }
}

/**
 * Dual Holographic Cyber HUD displays:
 * Left: "ACCESS GRANTED" + Radar Target Reticle
 * Right: "QUANTUM ENTANGLEMENT" + Live Sine Wave Oscilloscope
 */
@Composable
fun QuantumTelemetryHUD(
    voiceState: AssistantVoiceState,
    amplitude: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_telemetry_anim")

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Holographic HUD: ACCESS GRANTED + Radar Scanner
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AuraSurfaceElevated.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.4f)),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Radar Reticle Canvas
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(
                        color = QuantumNeonGreen.copy(alpha = 0.4f),
                        radius = size.width / 2,
                        style = Stroke(width = 1f)
                    )
                    drawCircle(
                        color = QuantumNeonGreen.copy(alpha = 0.25f),
                        radius = size.width / 3.5f,
                        style = Stroke(width = 0.8f)
                    )
                    // Rotating radar beam
                    val rad = Math.toRadians(radarAngle.toDouble())
                    val endX = (size.width / 2) + (size.width / 2) * cos(rad).toFloat()
                    val endY = (size.height / 2) + (size.height / 2) * sin(rad).toFloat()
                    drawLine(
                        color = QuantumBrightGreen,
                        start = Offset(size.width / 2, size.height / 2),
                        end = Offset(endX, endY),
                        strokeWidth = 1.5f
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "ACCESS GRANTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = QuantumNeonGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CIPHER: AES-Q256",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AuraTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Holographic HUD: QUANTUM ENTANGLEMENT + Live Waveform
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = AuraSurfaceElevated.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.4f)),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sine Wave Oscilloscope Canvas
                Canvas(modifier = Modifier.size(width = 34.dp, height = 24.dp)) {
                    val path = Path()
                    val points = 16
                    val amp = (6.dp.toPx() * (1f + amplitude * 1.5f)).coerceAtMost(size.height / 2.2f)
                    for (p in 0..points) {
                        val px = (p.toFloat() / points) * size.width
                        val py = (size.height / 2) + (sin(wavePhase + (p * 0.5f)) * amp).toFloat()
                        if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path = path,
                        color = QuantumCyanGlow,
                        style = Stroke(width = 1.5f)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Text(
                        text = "SYSTEM BREACHED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = QuantumCyanGlow,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Q-SYNC: 99.98%",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AuraTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Top Cyberpunk Header Section
 */
@Composable
fun ImmersiveHeaderSection(
    voiceState: AssistantVoiceState,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(QuantumNeonGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "QUANTUM HACKER // AURA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = QuantumNeonGreen
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Aira",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "[NEURAL VOICE CORE]",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = QuantumCyanGlow,
                    letterSpacing = 1.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiveStatusPill(voiceState = voiceState)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AuraSurfaceElevated,
                border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.5f)),
                modifier = Modifier
                    .size(38.dp)
                    .clickable(onClick = onSettingsClick)
                    .testTag("settings_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = QuantumNeonGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Status Pill with Cyberpunk Monospace labels
 */
@Composable
fun LiveStatusPill(voiceState: AssistantVoiceState) {
    val isLive = voiceState != AssistantVoiceState.DISCONNECTED && voiceState != AssistantVoiceState.ERROR
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_pill")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (label, dotColor) = when (voiceState) {
        AssistantVoiceState.DISCONNECTED -> Pair("SYS_STANDBY", AuraTextMuted)
        AssistantVoiceState.CONNECTING -> Pair("SYS_SYNC", AuraWarning)
        AssistantVoiceState.LISTENING -> Pair("SYS_ONLINE", QuantumNeonGreen)
        AssistantVoiceState.THINKING -> Pair("SYS_REASON", QuantumCyanGlow)
        AssistantVoiceState.SPEAKING -> Pair("SYS_VOICE", QuantumBrightGreen)
        AssistantVoiceState.EXECUTING_ACTION -> Pair("SYS_EXEC", AuraAccent)
        AssistantVoiceState.ERROR -> Pair("SYS_NOTICE", AuraError)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AuraSurfaceElevated,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.5f)),
        modifier = Modifier.testTag("state_status_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = if (isLive) pulseAlpha else 0.5f))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = AuraTextPrimary
            )
        }
    }
}

/**
 * Quantum Atomic Reactor Core Visualizer:
 * Features 3 intersecting 3D-angled glowing neon green electron orbits (0°, 60°, 120°),
 * rotating particles, pulsing nucleus, and audio-reactive amplitude scaling.
 */
@Composable
fun ImmersiveOrbVisualizer(
    voiceState: AssistantVoiceState,
    amplitude: Float,
    wakePhrase: String = "Hi Aira",
    onOrbClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quantum_atom_anim")

    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_1"
    )

    val orbitAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_2"
    )

    val orbitAngle3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_3"
    )

    val baseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "base_pulse"
    )

    val dynamicAmpScale by animateFloatAsState(
        targetValue = 1f + (amplitude * 0.65f),
        animationSpec = tween(100),
        label = "amp_scale"
    )

    val effectiveScale = when (voiceState) {
        AssistantVoiceState.SPEAKING -> baseScale * dynamicAmpScale * 1.18f
        AssistantVoiceState.LISTENING -> baseScale * (1f + amplitude * 0.45f)
        AssistantVoiceState.EXECUTING_ACTION -> 1.2f
        AssistantVoiceState.CONNECTING -> baseScale
        else -> baseScale
    }

    val stateHeadline = when (voiceState) {
        AssistantVoiceState.DISCONNECTED -> "Say \"$wakePhrase\" or Tap to Initialize"
        AssistantVoiceState.CONNECTING -> "Synchronizing Quantum Core..."
        AssistantVoiceState.LISTENING -> "Quantum Link Active • Listening..."
        AssistantVoiceState.THINKING -> "Synthesizing Neural Matrix..."
        AssistantVoiceState.SPEAKING -> "Quantum Stream Transmitting..."
        AssistantVoiceState.EXECUTING_ACTION -> "Executing System Command..."
        AssistantVoiceState.ERROR -> "Quantum Core Ready"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOrbClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Ambient Plasma Radial Glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(effectiveScale * 1.4f)
                    .blur(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                QuantumNeonGreen.copy(alpha = 0.55f),
                                QuantumCyanGlow.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Layer 2: 3D Quantum Atom Orbital Ellipses Canvas (Horizontal, 60 deg, -60 deg)
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .scale(effectiveScale)
            ) {
                val cx = size.width / 2
                val cy = size.height / 2
                val rx = size.width / 2.15f
                val ry = size.height / 5.2f

                // Orbit 1: Horizontal Ellipse (0 deg)
                drawOval(
                    color = QuantumNeonGreen.copy(alpha = 0.65f),
                    topLeft = Offset(cx - rx, cy - ry),
                    size = Size(rx * 2, ry * 2),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                // Electron 1
                val rad1 = Math.toRadians(orbitAngle1.toDouble())
                val e1x = cx + rx * cos(rad1).toFloat()
                val e1y = cy + ry * sin(rad1).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(e1x, e1y)
                )
                drawCircle(
                    color = QuantumBrightGreen,
                    radius = 6.dp.toPx(),
                    center = Offset(e1x, e1y),
                    style = Stroke(width = 1.2.dp.toPx())
                )

                // Orbit 2: Tilted Ellipse (60 deg tilt)
                val rad2 = Math.toRadians(orbitAngle2.toDouble())
                val raw2x = rx * cos(rad2).toFloat()
                val raw2y = ry * sin(rad2).toFloat()
                val cos60 = 0.5f
                val sin60 = 0.866f
                val e2x = cx + (raw2x * cos60 - raw2y * sin60)
                val e2y = cy + (raw2x * sin60 + raw2y * cos60)

                // Orbit 3: Tilted Ellipse (-60 deg tilt)
                val rad3 = Math.toRadians(orbitAngle3.toDouble())
                val raw3x = rx * cos(rad3).toFloat()
                val raw3y = ry * sin(rad3).toFloat()
                val cosNeg60 = 0.5f
                val sinNeg60 = -0.866f
                val e3x = cx + (raw3x * cosNeg60 - raw3y * sinNeg60)
                val e3y = cy + (raw3x * sinNeg60 + raw3y * cosNeg60)

                drawCircle(
                    color = QuantumCyanGlow,
                    radius = 3.8.dp.toPx(),
                    center = Offset(e2x, e2y)
                )
                drawCircle(
                    color = QuantumBrightGreen,
                    radius = 3.8.dp.toPx(),
                    center = Offset(e3x, e3y)
                )
            }

            // Layer 3: Central High-Tech Atomic Nucleus Core
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .scale(effectiveScale)
                    .shadow(
                        elevation = 28.dp,
                        shape = CircleShape,
                        spotColor = QuantumNeonGreen.copy(alpha = 0.7f),
                        ambientColor = QuantumCyanGlow.copy(alpha = 0.4f)
                    )
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                QuantumNeonGreen,
                                AuraPrimaryVariant,
                                QuantumDarkObsidian
                            )
                        ),
                        shape = CircleShape
                    )
                    .padding(2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0C1F14),
                                    Color(0xFF030A05)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    QuantumNeonGreen.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Aira",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = AuraTextPrimary
                        )
                        Text(
                            text = "QUANTUM CORE",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp,
                            color = QuantumNeonGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        when (voiceState) {
                            AssistantVoiceState.CONNECTING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = QuantumNeonGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                            AssistantVoiceState.EXECUTING_ACTION -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Executing Action",
                                    tint = QuantumNeonGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            AssistantVoiceState.SPEAKING -> {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Aira Speaking",
                                    tint = QuantumBrightGreen,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            AssistantVoiceState.LISTENING -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Listening",
                                    tint = QuantumNeonGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Voice",
                                    tint = QuantumNeonGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stateHeadline,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = AuraTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "CODE // BREAK // REPEAT • HINDI • HINGLISH • ENGLISH",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = QuantumNeonGreen.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Cyber Motto Tags directly from the reference image:
 * [ CODE BREAK REPEAT ], [ THINK QUANTUM HACK EVERYTHING ], [ SYSTEM BREACHED ], [ 10100101 ]
 */
@Composable
fun CyberCommandMottoBanner(
    onCommandSelected: (String) -> Unit
) {
    val mottos = listOf(
        Pair("CODE // BREAK // REPEAT", "Explain quantum hacking in code"),
        Pair("THINK QUANTUM // HACK EVERYTHING", "What is quantum computing?"),
        Pair("QUANTUM ENTANGLEMENT", "Explain quantum entanglement simply"),
        Pair("10100101 010100", "Convert 10100101 to decimal")
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(mottos) { (title, command) ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AuraSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clickable { onCommandSelected(command) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(QuantumBrightGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = title,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = QuantumNeonGreen,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionChips(
    onCommandSelected: (String) -> Unit
) {
    val prompts = listOf(
        "Hi Aira",
        "WhatsApp kholo",
        "Call Mom",
        "Open YouTube",
        "Hindi mein bolo",
        "Open Settings",
        "Write Python script",
        "Hinglish mein bolo"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(prompts) { prompt ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AuraSurfaceElevated,
                border = BorderStroke(1.dp, AuraSurfaceBorder),
                modifier = Modifier
                    .clickable { onCommandSelected(prompt) }
                    .testTag("quick_chip_${prompt.replace(" ", "_")}")
            ) {
                Text(
                    text = prompt,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = AuraTextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageCard(message: ChatMessage) {
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isSystem) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AuraSurfaceElevated.copy(alpha = 0.88f),
                border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(AuraDeepEmerald, shape = RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (message.actionDetails?.success == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (message.actionDetails?.success == true) QuantumNeonGreen else AuraError,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.text,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AuraTextPrimary
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 10.dp,
                    topEnd = 10.dp,
                    bottomStart = if (isUser) 10.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 10.dp
                ),
                color = if (isUser) AuraDeepEmerald else AuraSurfaceElevated,
                border = BorderStroke(
                    1.dp,
                    if (isUser) QuantumNeonGreen.copy(alpha = 0.5f) else QuantumHudBorder.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.85f else 0.92f)
                    .padding(vertical = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Text(
                            text = if (isUser) "USER:~$" else "AIRA:~$",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = QuantumNeonGreen
                        )
                        if (!isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "Voice output",
                                tint = QuantumNeonGreen,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = AuraTextPrimary
                    )

                    message.actionDetails?.let { badge ->
                        Spacer(modifier = Modifier.height(5.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AuraSurface,
                            border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = QuantumNeonGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badge.summary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AuraTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingSpeechBubble(
    sender: String,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AuraSurface,
        border = BorderStroke(1.dp, QuantumNeonGreen.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$sender:~$",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = QuantumNeonGreen
                )
                Spacer(modifier = Modifier.width(6.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    color = QuantumNeonGreen,
                    strokeWidth = 1.5.dp
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                color = AuraTextPrimary
            )
        }
    }
}

@Composable
fun ImmersiveFooterBar(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    showTextInput: Boolean,
    onToggleTextInput: () -> Unit,
    voiceState: AssistantVoiceState,
    isMicEnabled: Boolean,
    onToggleSession: () -> Unit,
    onToggleMic: () -> Unit
) {
    val isSessionActive = voiceState != AssistantVoiceState.DISCONNECTED && voiceState != AssistantVoiceState.ERROR

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = showTextInput,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuraSurfaceElevated,
                border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = onInputTextChanged,
                        placeholder = {
                            Text(
                                text = "AIRA:~$ type voice command or code...",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AuraTextMuted
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("command_input_field"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = QuantumNeonGreen,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = AuraSurface,
                            unfocusedContainerColor = AuraSurface,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() })
                    )

                    IconButton(
                        onClick = onSend,
                        modifier = Modifier
                            .size(36.dp)
                            .background(QuantumNeonGreen, shape = RoundedCornerShape(6.dp))
                            .testTag("send_command_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF020704),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuraSurfaceBorder,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleTextInput)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Toggle Keyboard Input",
                        tint = QuantumNeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Central Glowing Quantum Reactor Voice Trigger Button
            Surface(
                shape = CircleShape,
                color = if (isSessionActive) AuraError else QuantumNeonGreen,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(68.dp)
                    .clickable(onClick = onToggleSession)
                    .testTag("session_toggle_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSessionActive) Icons.Default.CallEnd else Icons.Default.PlayArrow,
                        contentDescription = if (isSessionActive) "End Session" else "Start Session",
                        tint = if (isSessionActive) Color.White else Color(0xFF030704),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuraSurfaceBorder,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onToggleMic)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Mic",
                        tint = if (isMicEnabled) QuantumNeonGreen else AuraTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusNotificationBanner(
    bannerText: String?,
    activeTool: ActionBadge?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = bannerText != null || activeTool != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = AuraSurfaceElevated.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AuraDeepEmerald, shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (activeTool?.toolName) {
                            "openWhatsApp" -> Icons.AutoMirrored.Filled.OpenInNew
                            "makeCall", "callContact" -> Icons.Default.Phone
                            "openUrl" -> Icons.Default.OpenInBrowser
                            else -> Icons.Default.SmartToy
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = QuantumNeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (activeTool != null) "ACTION BRIDGE ACTIVE" else "AIRA STATUS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = QuantumNeonGreen
                        )
                        Text(
                            text = activeTool?.summary ?: bannerText ?: "",
                            fontSize = 12.sp,
                            color = AuraTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = AuraTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactClarificationCard(
    contacts: List<ContactInfo>,
    onContactSelected: (ContactInfo) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AuraSurfaceElevated,
        border = BorderStroke(1.dp, QuantumHudBorder.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AuraDeepEmerald, shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = QuantumNeonGreen,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CONTACT MAPPING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = QuantumNeonGreen
                        )
                        Text(
                            text = "Multiple Targets Found. Select to dial:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AuraTextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AuraTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            contacts.forEach { contact ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AuraSurface,
                    border = BorderStroke(1.dp, AuraSurfaceBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { onContactSelected(contact) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = contact.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AuraTextPrimary
                            )
                            Text(
                                text = contact.phoneNumber,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AuraTextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = QuantumNeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FirstSetupPromptCard(
    wakePhrase: String,
    onGrantPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AuraSurfaceElevated,
        border = BorderStroke(1.dp, QuantumNeonGreen.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(AuraDeepEmerald, shape = RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = QuantumNeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "AIRA BACKGROUND SETUP",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = QuantumNeonGreen
                        )
                        Text(
                            text = "Enable Background Assistant & Mic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraTextPrimary
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = AuraTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Say \"$wakePhrase\" from anywhere to wake Aira instantly. Allow Microphone & Notification permissions for 24/7 assistance.",
                fontSize = 11.sp,
                color = AuraTextSecondary,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onGrantPermissions,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QuantumNeonGreen,
                    contentColor = Color(0xFF020704)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Enable Background Assistant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun AmbientBackgroundGlow(
    voiceState: AssistantVoiceState,
    amplitude: Float
) {
    val glowColor by animateColorAsState(
        targetValue = when (voiceState) {
            AssistantVoiceState.SPEAKING -> QuantumBrightGreen.copy(alpha = 0.28f + amplitude * 0.18f)
            AssistantVoiceState.LISTENING -> QuantumCyanGlow.copy(alpha = 0.22f + amplitude * 0.15f)
            AssistantVoiceState.EXECUTING_ACTION -> AuraAccent.copy(alpha = 0.25f)
            else -> QuantumNeonGreen.copy(alpha = 0.12f)
        },
        animationSpec = tween(400),
        label = "ambient_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(60.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        Color.Transparent
                    )
                )
            )
    )
}
