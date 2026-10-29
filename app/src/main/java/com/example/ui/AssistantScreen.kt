package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AppMenuDialog
import com.example.ui.components.DangerConfirmDialog
import com.example.ui.components.MessageItem
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SuggestionChipsRow
import com.example.ui.components.ThinkingIndicator
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AssistantViewModel

@Composable
fun AssistantScreen(viewModel: AssistantViewModel) {
    val context = LocalContext.current
    val config by viewModel.config.collectAsStateWithLifecycle()
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val pendingDanger by viewModel.pendingDangerAction.collectAsStateWithLifecycle()
    val serviceStatus by viewModel.serviceStatus.collectAsStateWithLifecycle()
    val pendingVoiceSevaWork by viewModel.pendingVoiceCustomerWork.collectAsStateWithLifecycle()

    val isHindi = currentLanguage.startsWith("hi")
    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAppMenuDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, isBusy) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Refresh service status when screen becomes active
    LaunchedEffect(Unit) {
        viewModel.refreshServiceStatus()
    }

    // Speech-to-Text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spoken?.firstOrNull()
            if (!text.isNullOrBlank()) {
                viewModel.sendMessage(text)
            }
        }
    }

    // Auto-listen when launched as assistant
    LaunchedEffect(Unit) {
        viewModel.requestMicTrigger.collect {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "बोलिए..." else "Speak...")
                }
                speechRecognizerLauncher.launch(intent)
            } catch (_: Exception) {}
        }
    }

    // Runtime permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshServiceStatus()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionsLauncher.launch(needed.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAssistantBar(
                isHindi = isHindi,
                serviceStatus = serviceStatus,
                isTtsEnabled = config.speakAnswers,
                currentLanguage = currentLanguage,
                onOpenMenuDialog = { showAppMenuDialog = true },
                onToggleTts = { viewModel.toggleTts() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onOpenSettingsDialog = { showSettingsDialog = true },
                onClearChat = { viewModel.clearConversation() },
                onOpenAccessibilitySettings = {
                    viewModel.toolController.openSettings("accessibility")
                },
                onOpenNotificationSettings = {
                    viewModel.toolController.openSettings("notification_access")
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, SurfaceBorder.copy(alpha = 0.6f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                SuggestionChipsRow(
                    isHindi = isHindi,
                    onChipClick = { suggestion ->
                        viewModel.sendMessage(suggestion)
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isHindi) "लिखिए या 🎤 दबाइए..." else "Ask Mamta or tap 🎤...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = SurfaceBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field")
                    )

                    // Mic voice input button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, SurfaceBorder, CircleShape)
                            .clickable {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (isHindi) "बोलिए..." else "Speak to Mamta...")
                                }
                                try {
                                    speechRecognizerLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Speech recognizer not available
                                }
                            }
                            .testTag("voice_input_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Send button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isBusy) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = inputText.isNotBlank() && !isBusy) {
                                val text = inputText
                                inputText = ""
                                viewModel.sendMessage(text)
                            }
                            .testTag("send_message_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isBusy) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Spacer(modifier = Modifier.size(8.dp))
            }

            items(messages, key = { it.id }) { message ->
                MessageItem(message = message)
            }

            if (isBusy) {
                item {
                    ThinkingIndicator(isHindi = isHindi)
                }
            }

            item {
                Spacer(modifier = Modifier.size(12.dp))
            }
        }
    }

    // Safety Confirmation Modal
    pendingDanger?.let { dangerRequest ->
        DangerConfirmDialog(
            request = dangerRequest,
            isHindi = isHindi,
            onConfirm = { viewModel.confirmDangerAction(true) },
            onDeny = { viewModel.confirmDangerAction(false) }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            initialConfig = config,
            isHindi = isHindi,
            onSave = { updatedConfig -> viewModel.updateConfig(updatedConfig) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Mamta Main Menu Dialog (Rules, Events Voice Service, Permissions, SOS)
    if (showAppMenuDialog) {
        AppMenuDialog(
            viewModel = viewModel,
            isHindi = isHindi,
            rules = rules,
            onAddRule = { viewModel.addRule(it) },
            onDeleteRule = { viewModel.deleteRule(it) },
            onRequestPermission = { perm ->
                permissionsLauncher.launch(arrayOf(perm))
            },
            onTriggerSos = { viewModel.triggerSosManual() },
            onDismiss = { showAppMenuDialog = false }
        )
    }

    // Voice Command Confirmation Dialog for Seva Kendra customer work
    val voiceWork = pendingVoiceSevaWork
    if (voiceWork != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingSevaWork() },
            title = {
                Text(
                    text = if (isHindi) "ग्राहक प्रविष्टि पुष्टि (Seva Kendra)" else "Confirm Customer Entry (Seva Kendra)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isHindi) "क्या आप इस ग्राहक को सेवा केंद्र में जोड़ना चाहते हैं?" else "Do you want to add this customer work to Seva Kendra?"
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("👤 Customer: ${voiceWork.customerName}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("📋 Service: ${voiceWork.serviceName}", color = MaterialTheme.colorScheme.onSurface)
                            Text("💰 Amount: ₹${"%.0f".format(voiceWork.amount)}", fontWeight = FontWeight.SemiBold, color = CyanAccent)
                        }
                    }
                    Text(
                        text = "🔒 No ID numbers will be stored.",
                        fontSize = 11.sp,
                        color = WarningAmber
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAddSevaWork() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.testTag("confirm_voice_add_customer_button")
                ) {
                    Text(if (isHindi) "स्वीकारें व जोड़ें" else "Confirm & Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissPendingSevaWork() },
                    modifier = Modifier.testTag("cancel_voice_add_customer_button")
                ) {
                    Text(if (isHindi) "रद्द करें" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun TopAssistantBar(
    isHindi: Boolean,
    serviceStatus: com.example.ui.viewmodel.ServiceStatus,
    isTtsEnabled: Boolean,
    currentLanguage: String,
    onOpenMenuDialog: () -> Unit,
    onToggleTts: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenSettingsDialog: () -> Unit,
    onClearChat: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title & Brand "Mamta" with Menu button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onOpenMenuDialog,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("main_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Main Menu",
                        tint = CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue)
                        .border(1.dp, CyanAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Mamta Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Mamta",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (serviceStatus.apiKeyConfigured) SuccessGreen else WarningAmber)
                        )
                        Text(
                            text = if (serviceStatus.apiKeyConfigured)
                                "Online (${serviceStatus.activeProvider.replaceFirstChar { it.uppercase() }})"
                            else
                                "Offline Mode",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Quick actions & service indicators
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Settings ⚙️ button
                IconButton(
                    onClick = onOpenSettingsDialog,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (serviceStatus.apiKeyConfigured) CyanAccent else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Accessibility Service indicator & shortcut ♿
                IconButton(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("accessibility_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Accessible,
                        contentDescription = "Accessibility Service",
                        tint = if (serviceStatus.accessibilityActive) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Notification Access indicator & shortcut 🔔
                IconButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("notification_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notification Listener",
                        tint = if (serviceStatus.notificationActive) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Language toggle (हि / EN)
                IconButton(
                    onClick = onToggleLanguage,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("language_toggle_button")
                ) {
                    Text(
                        text = if (isHindi) "हि" else "EN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CyanAccent
                    )
                }

                // TTS audio toggle 🔊
                IconButton(
                    onClick = onToggleTts,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("tts_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = "Toggle Speech",
                        tint = if (isTtsEnabled) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear chat 🗑️
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
