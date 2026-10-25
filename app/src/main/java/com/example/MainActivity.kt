package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.ChatBubble
import com.example.ui.components.GirlfriendAvatar
import com.example.ui.screens.ActionHubDialog
import com.example.ui.screens.LiveAnimeVoiceScreen
import com.example.ui.screens.OnboardingDialog
import com.example.ui.screens.ScreenCoachScreen
import com.example.ui.screens.VoiceProfileScreen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import com.example.ui.screens.AdminControlDialog
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.theme.HeartRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary

enum class AppNavTab(val title: String) {
    VOICE("Live Voice"),
    CHAT("Chat"),
    COACH("Game Coach"),
    PROFILE("Voice & Love")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GirlfriendApp()
            }
        }
    }
}

@Composable
fun GirlfriendApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(AppNavTab.VOICE) }

    // If Server Maintenance Mode is active in Firebase, block screen and show Maintenance UI
    if (uiState.serverConfig.maintenanceMode || !uiState.serverConfig.serverActive) {
        MaintenanceScreen(
            serverConfig = uiState.serverConfig,
            isSyncing = uiState.isFirebaseSyncing,
            onRefreshStatus = viewModel::refreshServerStatus,
            onOpenAdminLogin = { viewModel.setAdminLoginPromptVisible(true) }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                if (currentTab != AppNavTab.VOICE) {
                    GirlfriendTopBar(
                        uiState = uiState,
                        onAvatarClick = {
                            viewModel.speakAloud("I love you ${uiState.profile.userNickname}! You mean the whole world to me! ❤️")
                        },
                        onToggleVoice = { viewModel.toggleAutoSpeak() },
                        onOpenActionHub = { viewModel.showActionHubDialog(true) }
                    )
                }
            },
            bottomBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    // For Chat tab: show chat input box right above the navigation bar
                    if (currentTab == AppNavTab.CHAT) {
                        ChatInputRow(
                            uiState = uiState,
                            onTextChanged = viewModel::onInputTextChanged,
                            onSendMessage = { viewModel.sendMessage(uiState.inputText) },
                            onToggleMic = viewModel::toggleVoiceListening
                        )
                    }

                    // Bottom Navigation Bar with 4 tabs
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == AppNavTab.VOICE,
                            onClick = { currentTab = AppNavTab.VOICE },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Live Voice"
                                )
                            },
                            label = { Text("Voice Call", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RosePrimary,
                                selectedTextColor = RosePrimary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_voice")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppNavTab.CHAT,
                            onClick = { currentTab = AppNavTab.CHAT },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Chat"
                                )
                            },
                            label = { Text("Chat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RosePrimary,
                                selectedTextColor = RosePrimary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_chat")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppNavTab.COACH,
                            onClick = { currentTab = AppNavTab.COACH },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = "Game Coach"
                                )
                            },
                            label = { Text("Game Coach", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RosePrimary,
                                selectedTextColor = RosePrimary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_coach")
                        )

                        NavigationBarItem(
                            selected = currentTab == AppNavTab.PROFILE,
                            onClick = { currentTab = AppNavTab.PROFILE },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Voice & Settings"
                                )
                            },
                            label = { Text("Voice Style", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RosePrimary,
                                selectedTextColor = RosePrimary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_tab_profile")
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppNavTab.VOICE -> {
                        LiveAnimeVoiceScreen(
                            uiState = uiState,
                            onToggleAssistantMode = viewModel::toggleAssistantMode,
                            onStopAssistantMode = viewModel::stopAssistantMode,
                            onToggleMic = viewModel::toggleVoiceListening,
                            onToggleVoice = viewModel::toggleAutoSpeak,
                            onSendQuickCommand = viewModel::sendMessage,
                            onAvatarTouch = {
                                viewModel.speakAloud("Hehe, that tickles, ${uiState.profile.userNickname}! You are so cute! 🥰❤️")
                            },
                            onToggleScreenShare = viewModel::toggleScreenShare,
                            isScreenShareActive = uiState.isScreenShareActive,
                            onSelectCharacter = viewModel::selectCharacterPersona
                        )
                    }

                    AppNavTab.CHAT -> {
                        GirlfriendChatContent(
                            uiState = uiState,
                            innerPadding = PaddingValues(0.dp),
                            onSendMessage = viewModel::sendMessage,
                            onSpeakMessage = viewModel::speakAloud,
                            onExecuteAction = viewModel::executeAction
                        )
                    }

                    AppNavTab.COACH -> {
                        ScreenCoachScreen(
                            userNickname = uiState.profile.userNickname,
                            onSpeakTip = viewModel::speakAloud,
                            isScreenShareActive = uiState.isScreenShareActive,
                            onToggleScreenShare = viewModel::toggleScreenShare,
                            onLaunchGame = { gameName -> viewModel.sendMessage("open $gameName") }
                        )
                    }

                    AppNavTab.PROFILE -> {
                        VoiceProfileScreen(
                            profile = uiState.profile,
                            geminiApiKey = uiState.geminiApiKey,
                            onSaveProfile = viewModel::saveProfile,
                            onTestVoice = viewModel::testVoice,
                            onSaveApiKey = viewModel::saveGeminiApiKey,
                            onOpenAdminPanel = { viewModel.setAdminLoginPromptVisible(true) }
                        )
                    }
                }
            }
        }
    }

    // Admin PIN Login Dialog
    if (uiState.showAdminLoginPrompt) {
        var enteredPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.setAdminLoginPromptVisible(false) },
            title = { Text("Admin PIN Required 🛡️", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter PIN to access Firebase server control (Default: 1234):", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { enteredPin = it },
                        singleLine = true,
                        placeholder = { Text("Enter PIN...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.verifyAdminPin(enteredPin) },
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setAdminLoginPromptVisible(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Firebase Admin Control Dashboard Dialog
    if (uiState.showAdminDialog) {
        AdminControlDialog(
            config = uiState.serverConfig,
            isSyncing = uiState.isFirebaseSyncing,
            onDismiss = { viewModel.setAdminDialogVisible(false) },
            onSaveConfig = viewModel::updateServerConfig
        )
    }

    // Action Hub Shortcuts Dialog
    if (uiState.showActionHub) {
        ActionHubDialog(
            onDismiss = { viewModel.showActionHubDialog(false) },
            onRunAction = viewModel::executeAction,
            onSendCommand = viewModel::sendMessage
        )
    }

    // Onboarding Dialog
    if (uiState.showOnboarding) {
        OnboardingDialog(
            onComplete = viewModel::completeOnboarding
        )
    }
}

@Composable
fun GirlfriendTopBar(
    uiState: MainUiState,
    onAvatarClick: () -> Unit,
    onToggleVoice: () -> Unit,
    onOpenActionHub: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onAvatarClick)
            ) {
                GirlfriendAvatar(
                    isSpeaking = uiState.isSpeaking,
                    isListening = uiState.isListening,
                    size = 44.dp,
                    onClick = onAvatarClick
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.profile.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Lvl ${uiState.profile.relationshipLevel} ❤️",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = RosePrimary
                            )
                        }
                    }

                    val statusText = when {
                        uiState.isListening -> "Listening to you... 🎙️"
                        uiState.isSpeaking -> "Speaking... 💖"
                        uiState.isThinking -> "Thinking of you... 💕"
                        else -> "Online with ${uiState.profile.userNickname} 🥰"
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = if (uiState.isListening) HeartRed else MaterialTheme.colorScheme.primary,
                        fontWeight = if (uiState.isListening || uiState.isSpeaking) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier.testTag("toggle_voice_btn")
                ) {
                    Icon(
                        imageVector = if (uiState.profile.autoSpeak) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Toggle voice",
                        tint = if (uiState.profile.autoSpeak) RosePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onOpenActionHub,
                    modifier = Modifier.testTag("open_action_hub_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Aria Action Hub",
                        tint = RosePrimary
                    )
                }
            }
        }
    }
}

@Composable
fun GirlfriendChatContent(
    uiState: MainUiState,
    innerPadding: PaddingValues,
    onSendMessage: (String) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onExecuteAction: (com.example.data.CompanionAction) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isThinking) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        QuickSuggestionsRow(onSelectSuggestion = onSendMessage)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    onSpeakMessage = onSpeakMessage,
                    onExecuteAction = onExecuteAction
                )
            }

            if (uiState.isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GirlfriendAvatar(isSpeaking = false, isListening = false, size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag("thinking_indicator")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = RosePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Aria is typing... 💕",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
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
fun QuickSuggestionsRow(onSelectSuggestion: (String) -> Unit) {
    val suggestions = listOf(
        "💬 Open WhatsApp",
        "💌 Send WhatsApp Hello",
        "📸 Post Insta Story",
        "❤️ I love you Aria",
        "⏰ Set alarm for 7 AM",
        "🥰 Kaise ho babu?",
        "🎵 Play romantic song",
        "📷 Open Camera"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { chipText ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .clickable { onSelectSuggestion(chipText.replace(Regex("^[\\p{So}\\p{Cn}]+\\s*"), "")) }
                    .testTag("suggestion_chip")
            ) {
                Text(
                    text = chipText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInputRow(
    uiState: MainUiState,
    onTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onToggleMic: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = if (uiState.isListening) "Listening..." else "Message Aria or ask to open apps...",
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RosePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = if (uiState.isListening) {
                                listOf(HeartRed, Color(0xFFFF5252))
                            } else {
                                listOf(RosePrimary, RoseSecondary)
                            }
                        )
                    )
                    .clickable(onClick = onToggleMic)
                    .testTag("mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) "Stop Listening" else "Speak",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (uiState.inputText.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RosePrimary)
                        .clickable(onClick = onSendMessage)
                        .testTag("send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
