package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.service.AiraWakeWordService
import com.example.ui.AuraScreen
import com.example.ui.AuraViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: AuraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWakeIntent(intent)

        setContent {
            MyApplicationTheme {
                var hasRecordPerm by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var hasContactsPerm by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var hasCallPerm by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var hasNotifPerm by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else true
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    hasRecordPerm = permissions[Manifest.permission.RECORD_AUDIO] ?: hasRecordPerm
                    hasContactsPerm = permissions[Manifest.permission.READ_CONTACTS] ?: hasContactsPerm
                    hasCallPerm = permissions[Manifest.permission.CALL_PHONE] ?: hasCallPerm
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        hasNotifPerm = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotifPerm
                    }

                    if (hasRecordPerm && viewModel.preferences.isWakeWordEnabled.value && viewModel.preferences.isBackgroundAssistantEnabled.value) {
                        AiraWakeWordService.start(this@MainActivity, viewModel.preferences.wakePhrase.value)
                    }
                }

                LaunchedEffect(Unit) {
                    val needed = mutableListOf<String>()
                    if (!hasRecordPerm) needed.add(Manifest.permission.RECORD_AUDIO)
                    if (!hasContactsPerm) needed.add(Manifest.permission.READ_CONTACTS)
                    if (!hasCallPerm) needed.add(Manifest.permission.CALL_PHONE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                        needed.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    if (needed.isNotEmpty()) {
                        permissionLauncher.launch(needed.toTypedArray())
                    } else {
                        if (viewModel.preferences.isWakeWordEnabled.value && viewModel.preferences.isBackgroundAssistantEnabled.value) {
                            AiraWakeWordService.start(this@MainActivity, viewModel.preferences.wakePhrase.value)
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuraScreen(
                        viewModel = viewModel,
                        hasRecordAudioPermission = hasRecordPerm,
                        hasContactsPermission = hasContactsPerm,
                        hasCallPhonePermission = hasCallPerm,
                        hasNotificationPermission = hasNotifPerm,
                        onRequestPermissions = {
                            val perms = mutableListOf(
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_CONTACTS,
                                Manifest.permission.CALL_PHONE
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                perms.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(perms.toTypedArray())
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_AUTO_WAKE", false) == true) {
            viewModel.triggerActivationAnimation()
            viewModel.startLiveSession()
        }
    }
}
