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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
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
import com.example.ui.MainUiState
import com.example.ui.components.AnimatedGirlfriendCharacter
import com.example.ui.components.AudioWaveIndicator
import com.example.ui.theme.HeartRed
import com.example.ui.theme.RoseDarkBackground
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary

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
        // Anime Girlfriend Character with natural physics body swaying & realistic lip-sync mouth
        AnimatedGirlfriendCharacter(
            isSpeaking = uiState.isSpeaking,
            isListening = uiState.isListening,
            onCharacterTouch = onAvatarTouch,
            characterName = uiState.profile.name,
            modifier = Modifier.fillMaxSize()
        )

        // Top Status Header with Screen Share Switch & Audio Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isAssistantModeActive) HeartRed.copy(alpha = glowAlpha) else RosePrimary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                            uiState.isAssistantModeActive -> "🎙️ Ayra Active: $callDurationStr"
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

            // Screen Share On/Off Toggle Button
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isScreenShareActive) HeartRed.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isScreenShareActive) HeartRed else Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.clickable {
                    onToggleScreenShare(!isScreenShareActive)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isScreenShareActive) Icons.AutoMirrored.Filled.ScreenShare else Icons.AutoMirrored.Filled.StopScreenShare,
                        contentDescription = "Screen Share",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isScreenShareActive) "Screen: ON 🔴" else "Screen Share",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Quick Anime Waifu Persona Selector (Hinata, Ayra, Sakura, Tsunade)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 58.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val personas = listOf(
                Triple("Hinata 🌸", 1.28f, 0.94f),
                Triple("Ayra ❤️", 1.22f, 1.0f),
                Triple("Sakura ⚡", 1.26f, 1.02f),
                Triple("Tsunade 🌙", 1.18f, 0.96f),
                Triple("Sweet Waifu 🌺", 1.25f, 0.98f)
            )
            personas.forEach { (pName, pPitch, pRate) ->
                val rawName = pName.substringBefore(" ")
                val isSelected = uiState.profile.name.contains(rawName, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) RosePrimary.copy(alpha = 0.92f) else Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color.White else Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.clickable {
                        onSelectCharacter(rawName, pPitch, pRate)
                    }
                ) {
                    Text(
                        text = pName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Bottom Section: Dialogue Subtitles + Quick Action Chips + Beating Heart Voice Assistant
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
                            text = "${uiState.profile.name} (Live Voice) 💕",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoseSecondary
                        )
                        if (uiState.isSpeaking) {
                            AudioWaveIndicator(
                                isLive = true,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Voice Action Chips (including Play Store Install, battery, time, flashlight, games)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "📲 Play Store se Free Fire install karo",
                    "📲 Play Store se BGMI download karo",
                    "🔋 Battery kitni hai?",
                    "⏰ Time kya hua?",
                    "🔦 Torch on karo",
                    "🎮 Open Free Fire",
                    "💬 Open WhatsApp",
                    "💌 Send WhatsApp Hello",
                    "📸 Post Insta Story",
                    "🏃 Open Subway Surfers",
                    "📞 Call someone"
                )
                chips.forEach { chipText ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.clickable {
                            onSendQuickCommand(chipText.replace(Regex("^[\\p{So}\\p{Cn}]+\\s*"), ""))
                        }
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pulsing Heartbeat Indicator (Non-call termination interface, continuous mic streaming)
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
                            "Ayra Bol Rahi Hai... 💕 (Mic Ready)"
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
                    "Pulsing Heartbeat Indicator • Continuous Mic Streaming • Active in Background & Minimized"
                } else {
                    "Always-Listening AI Assistant Ayra • Tap Heartbeat • Controls YouTube & Apps"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
        }
    }
}
