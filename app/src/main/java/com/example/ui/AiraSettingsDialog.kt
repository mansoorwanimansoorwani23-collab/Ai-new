package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiraSettingsDialog(
    viewModel: AuraViewModel,
    hasRecordAudioPermission: Boolean,
    hasContactsPermission: Boolean,
    hasCallPhonePermission: Boolean,
    hasNotificationPermission: Boolean = true,
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    val preferences = viewModel.preferences
    val currentWakePhrase by preferences.wakePhrase.collectAsState()
    val isWakeWordEnabled by preferences.isWakeWordEnabled.collectAsState()
    val isBackgroundAssistantEnabled by preferences.isBackgroundAssistantEnabled.collectAsState()
    val isAutoStartEnabled by preferences.isAutoStartEnabled.collectAsState()
    val currentVoice by preferences.voice.collectAsState()
    val customApiKey by preferences.customApiKey.collectAsState()

    var editingWakePhrase by remember { mutableStateOf(currentWakePhrase) }
    var editingApiKey by remember { mutableStateOf(customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    val presetWakePhrases = listOf("Hi Aira", "Hey Aira", "Aira", "Namaste Aira")
    val availableVoices = listOf("alloy", "echo", "shimmer", "ash", "ballad", "coral", "sage", "verse")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AuraSurfaceElevated,
            border = BorderStroke(1.dp, AuraSurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AIRA ENGINE SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = AuraPrimary
                        )
                        Text(
                            text = "Assistant & Background Voice",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraTextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = AuraTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 1: Background Assistant & Auto-Start
                Text(
                    text = "BACKGROUND ASSISTANT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AuraPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    border = BorderStroke(1.dp, AuraSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Background Assistant Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Assistant Service",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraTextPrimary
                                )
                                Text(
                                    text = "Runs in background via secure Android Foreground Service to detect wake phrase anytime.",
                                    fontSize = 12.sp,
                                    color = AuraTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isBackgroundAssistantEnabled,
                                onCheckedChange = { viewModel.setBackgroundAssistantEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF042111),
                                    checkedTrackColor = AuraPrimary,
                                    uncheckedThumbColor = AuraTextMuted,
                                    uncheckedTrackColor = AuraSurfaceElevated
                                ),
                                modifier = Modifier.testTag("bg_assistant_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto Start on Boot Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = AuraSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Auto Start on Device Boot",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AuraTextPrimary
                                    )
                                }
                                Text(
                                    text = "Starts background wake service automatically when device restarts.",
                                    fontSize = 11.sp,
                                    color = AuraTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isAutoStartEnabled,
                                onCheckedChange = { viewModel.setAutoStartEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF042111),
                                    checkedTrackColor = AuraPrimary,
                                    uncheckedThumbColor = AuraTextMuted,
                                    uncheckedTrackColor = AuraSurfaceElevated
                                ),
                                modifier = Modifier.testTag("auto_start_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AuraDeepEmerald.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, AuraPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AuraPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Complies strictly with standard Android Foreground Service security policies.",
                                    fontSize = 11.sp,
                                    color = AuraTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 2: Wake Voice / Wake Word Configuration
                Text(
                    text = "WAKE VOICE & PHRASE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AuraPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    border = BorderStroke(1.dp, AuraSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Wake Word Detection",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraTextPrimary
                                )
                                Text(
                                    text = "Saying your wake phrase activates Aira immediately from anywhere.",
                                    fontSize = 12.sp,
                                    color = AuraTextSecondary
                                )
                            }
                            Switch(
                                checked = isWakeWordEnabled,
                                onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF042111),
                                    checkedTrackColor = AuraPrimary,
                                    uncheckedThumbColor = AuraTextMuted,
                                    uncheckedTrackColor = AuraSurfaceElevated
                                ),
                                modifier = Modifier.testTag("wake_word_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Choose Preferred Wake Phrase:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AuraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetWakePhrases.forEach { phrase ->
                                val isSelected = currentWakePhrase.equals(phrase, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AuraDeepEmerald else AuraSurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) AuraPrimary else AuraSurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            editingWakePhrase = phrase
                                            viewModel.updateWakePhrase(phrase)
                                        }
                                        .testTag("preset_phrase_$phrase")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = AuraPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "\"$phrase\"",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) AuraPrimary else AuraTextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom wake phrase input
                        Text(
                            text = "Or Set Custom Wake Phrase:",
                            fontSize = 12.sp,
                            color = AuraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editingWakePhrase,
                                onValueChange = { editingWakePhrase = it },
                                placeholder = { Text("e.g. Hi Aira", fontSize = 13.sp, color = AuraTextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_wake_phrase_input"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuraPrimary,
                                    unfocusedBorderColor = AuraSurfaceBorder,
                                    focusedContainerColor = AuraSurfaceElevated,
                                    unfocusedContainerColor = AuraSurfaceElevated,
                                    focusedTextColor = AuraTextPrimary,
                                    unfocusedTextColor = AuraTextPrimary
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (editingWakePhrase.isNotBlank()) {
                                        viewModel.updateWakePhrase(editingWakePhrase)
                                    }
                                })
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (editingWakePhrase.isNotBlank()) {
                                        viewModel.updateWakePhrase(editingWakePhrase)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuraPrimary,
                                    contentColor = Color(0xFF042111)
                                ),
                                modifier = Modifier.testTag("save_wake_phrase_btn")
                            ) {
                                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 3: Voice Persona
                Text(
                    text = "REALTIME VOICE PERSONA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AuraPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    border = BorderStroke(1.dp, AuraSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableVoices.forEach { voiceName ->
                                val isSelected = currentVoice.equals(voiceName, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) AuraDeepEmerald else AuraSurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) AuraPrimary else AuraSurfaceBorder
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.setVoice(voiceName) }
                                        .testTag("voice_option_$voiceName")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = null,
                                            tint = if (isSelected) AuraPrimary else AuraTextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = voiceName.replaceFirstChar { it.uppercase() },
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) AuraPrimary else AuraTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 4: OpenAI API Key
                Text(
                    text = "OPENAI REALTIME API KEY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AuraPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    border = BorderStroke(1.dp, AuraSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Set your OpenAI Realtime voice API Key (sk-...):",
                            fontSize = 12.sp,
                            color = AuraTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editingApiKey,
                            onValueChange = { editingApiKey = it },
                            placeholder = { Text("sk-proj-...", fontSize = 13.sp, color = AuraTextMuted) },
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle API Key visibility",
                                        tint = AuraTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraPrimary,
                                unfocusedBorderColor = AuraSurfaceBorder,
                                focusedContainerColor = AuraSurfaceElevated,
                                unfocusedContainerColor = AuraSurfaceElevated,
                                focusedTextColor = AuraTextPrimary,
                                unfocusedTextColor = AuraTextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.setCustomApiKey(editingApiKey)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraPrimary,
                                contentColor = Color(0xFF042111)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_api_key_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save API Key", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 5: Android System Permissions
                Text(
                    text = "SYSTEM PERMISSIONS STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AuraPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSurface),
                    border = BorderStroke(1.dp, AuraSurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        PermissionRow(
                            title = "Microphone (Voice Input)",
                            isGranted = hasRecordAudioPermission,
                            icon = Icons.Default.Mic
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionRow(
                            title = "Notifications (Background Service)",
                            isGranted = hasNotificationPermission,
                            icon = Icons.Default.Notifications
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionRow(
                            title = "Contacts (Call by Name)",
                            isGranted = hasContactsPermission,
                            icon = Icons.Default.Person
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionRow(
                            title = "Phone (Direct Call Action)",
                            isGranted = hasCallPhonePermission,
                            icon = Icons.Default.Phone
                        )

                        if (!hasRecordAudioPermission || !hasNotificationPermission || !hasContactsPermission || !hasCallPhonePermission) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestPermissions,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuraPrimary,
                                    contentColor = Color(0xFF042111)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Missing Permissions", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) AuraPrimary else AuraTextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                color = AuraTextPrimary
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) AuraSuccess.copy(alpha = 0.15f) else AuraError.copy(alpha = 0.15f)
        ) {
            Text(
                text = if (isGranted) "Granted" else "Required",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isGranted) AuraSuccess else AuraError,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
