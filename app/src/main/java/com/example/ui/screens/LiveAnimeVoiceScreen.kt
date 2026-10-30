package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainUiState
import com.example.ui.components.AnimatedGirlfriendCharacter
import com.example.ui.components.AudioWaveIndicator
import com.example.ui.theme.HeartRed
import com.example.ui.theme.RoseDarkBackground
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary
import kotlinx.coroutines.delay

@Composable
fun LiveAnimeVoiceScreen(
    uiState: MainUiState,
    onToggleAssistantMode: () -> Unit,
    onStopAssistantMode: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleVoice: () -> Unit,
    onSendQuickCommand: (String) -> Unit,
    onAvatarTouch: () -> Unit,
    onToggleScreenShare: (Boolean) -> Unit,
    isScreenShareActive: Boolean,
    onSelectCharacter: (String, Float, Float) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showSettingsPanel by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "voice_ui_effects")

    // Realistic cardiac heartbeat animation when active ("Dil Dhadakta Hai")
    val heartbeatScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 950
                1.0f at 0
                1.28f at 160 using FastOutSlowInEasing // first beat (lub)
                1.12f at 300
                1.38f at 440 using FastOutSlowInEasing // second beat (dub)
                1.0f at 650
                1.0f at 950 // resting pause
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartbeat_scale"
    )

    // Pulsing outer ripple ring for active heart
    val heartRippleScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heart_ripple_scale"
    )

    val heartRippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heart_ripple_alpha"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    var callSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.isAssistantModeActive) {
        if (uiState.isAssistantModeActive) {
            callSeconds = 0
            while (true) {
                delay(1000)
                callSeconds++
            }
        } else {
            callSeconds = 0
        }
    }

    val callDurationStr = String.format("%02d:%02d", callSeconds / 60, callSeconds % 60)

    val latestGirlfriendMessage = uiState.messages.lastOrNull {
        it.sender == com.example.data.MessageSender.GIRLFRIEND
    }?.text ?: "Hey ${uiState.profile.userNickname}! Dil par tap karo aur mujhse baat karo! ❤️"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoseDarkBackground)
    ) {
        // Anime Character Avatar with speech-coordinated lip-sync visemes and zero rigging displacement
        AnimatedGirlfriendCharacter(
            isSpeaking = uiState.isSpeaking,
            isListening = uiState.isListening,
            onCharacterTouch = onAvatarTouch,
            characterName = uiState.profile.name,
            audioLevel = uiState.audioRms,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Header with Status Chip and Unified Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Indicator Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isAssistantModeActive) HeartRed.copy(alpha = glowAlpha) else RosePrimary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    uiState.isAssistantModeActive -> Color(0xFF00E676)
                                    uiState.isListening -> HeartRed
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            uiState.isAssistantModeActive -> "Ayra Active: $callDurationStr"
                            uiState.isSpeaking -> "Speaking 💋"
                            uiState.isListening -> "Listening... 🎙️"
                            else -> "Ayra AI 🌸"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Unified Settings Button (Strictly limits UI control overlays to this unified panel)
            IconButton(
                onClick = { showSettingsPanel = true },
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("open_unified_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayra Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Section: Dialogue Subtitles & Glowing Pulsing Heartbeat Indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Dialogue Card / Subtitles
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_dialogue_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.78f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isSpeaking) RosePrimary.copy(alpha = glowAlpha) else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.profile.name} ❤️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RosePrimary
                        )

                        if (uiState.isSpeaking) {
                            AudioWaveIndicator(
                                isLive = true,
                                barCount = 4,
                                barColor = RosePrimary,
                                maxHeight = 16.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = latestGirlfriendMessage,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Glowing Pulsing Heartbeat Indicator (Replaces standard call-termination controls)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        if (uiState.isAssistantModeActive) {
                            onStopAssistantMode()
                        } else {
                            onToggleAssistantMode()
                        }
                    }
                    .testTag("pulsing_heartbeat_indicator_btn"),
                contentAlignment = Alignment.Center
            ) {
                // Multi-layered Concentric Radiating Heartbeat Waves
                if (uiState.isAssistantModeActive) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .scale(heartRippleScale * (1f + uiState.audioRms * 0.35f))
                            .clip(CircleShape)
                            .background(HeartRed.copy(alpha = heartRippleAlpha * 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .scale(1f + (heartbeatScale - 1f) * 0.65f + uiState.audioRms * 0.25f)
                            .clip(CircleShape)
                            .background(RosePrimary.copy(alpha = 0.3f))
                    )
                }

                // The Central Pulsing Heartbeat Circle Indicator
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(if (uiState.isAssistantModeActive) heartbeatScale else 1.0f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (uiState.isAssistantModeActive) {
                                    listOf(HeartRed, Color(0xFFFF1744), Color(0xFFC2185B))
                                } else {
                                    listOf(RosePrimary, RoseSecondary, Color(0xFF880E4F))
                                }
                            )
                        )
                        .border(
                            width = if (uiState.isAssistantModeActive) 2.5.dp else 1.5.dp,
                            color = if (uiState.isAssistantModeActive) Color.White else Color.White.copy(alpha = 0.75f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isAssistantModeActive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (uiState.isAssistantModeActive) "Ayra Pulsing Heartbeat Active" else "Start Always-Listening Assistant",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pulsing Heartbeat Assistant Status Chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (uiState.isAssistantModeActive) HeartRed.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.14f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isAssistantModeActive) HeartRed.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.22f)
                )
            ) {
                Text(
                    text = if (uiState.isAssistantModeActive) {
                        if (uiState.isSpeaking) {
                            "Ayra Bol Rahi Hai... 💕 (Mic Streaming)"
                        } else {
                            "🎙️ AYRA LISTENING • CONTINUOUS MIC STREAMING ACTIVE"
                        }
                    } else {
                        "Tap Pulsing Heartbeat To Activate Ayra ❤️"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isAssistantModeActive) Color(0xFFFF80AB) else Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = if (uiState.isAssistantModeActive) {
                    "Pulsing Heartbeat Indicator • Continuous Streaming Active (Android 6 - 17)"
                } else {
                    "Always-Listening AI Assistant Ayra • Tap Heartbeat to Start"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }

    // Unified Settings Panel (Advanced feature toggles modal)
    if (showSettingsPanel) {
        Dialog(onDismissRequest = { showSettingsPanel = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("unified_settings_dialog")
            ) {
                Column(
                    modifier = Modifier
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
                                text = "Settings & Feature Toggles",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ayra Always-Listening AI Assistant",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showSettingsPanel = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Live Screen Sharing Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Screen Sharing",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Analyze games and screen content in real-time",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isScreenShareActive,
                            onCheckedChange = { onToggleScreenShare(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RosePrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Character Persona Selector
                    Text(
                        text = "Persona & Character Voice",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val personas = listOf(
                        Triple("Ayra ❤️", 1.22f, 1.0f),
                        Triple("Hinata 🌸", 1.28f, 0.94f),
                        Triple("Sakura ⚡", 1.26f, 1.02f),
                        Triple("Tsunade 🌙", 1.18f, 0.96f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        personas.forEach { (pName, pPitch, pRate) ->
                            val rawName = pName.substringBefore(" ")
                            val isSelected = uiState.profile.name.contains(rawName, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    onSelectCharacter(rawName, pPitch, pRate)
                                }
                            ) {
                                Text(
                                    text = pName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Persistent Background Microphone Info
                    Column {
                        Text(
                            text = "Persistent Background Execution",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ayra runs continuous microphone capture via foreground service, persisting when the app is minimized or backgrounded across Android 6 through 17.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Quick Voice Command Hooks (YouTube & App Launcher)
                    Text(
                        text = "Quick Voice Commands & Hooks",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val commands = listOf(
                        "Play lo-fi music on YouTube",
                        "Open WhatsApp",
                        "Check battery",
                        "Torch on karo",
                        "Open Free Fire",
                        "Call someone"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        commands.forEach { cmd ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSettingsPanel = false
                                        onSendQuickCommand(cmd)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = RosePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cmd,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showSettingsPanel = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
