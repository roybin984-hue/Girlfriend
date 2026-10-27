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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Phone
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
                            uiState.isAssistantModeActive -> "📞 In Call: $callDurationStr"
                            uiState.isSpeaking -> "Speaking 💋"
                            uiState.isListening -> "Listening... 🎙️"
                            else -> "Aria AI 🌸"
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

            // Mute / Voice Toggle Icon
            IconButton(
                onClick = onToggleVoice,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = if (uiState.profile.autoSpeak) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle Voice",
                    tint = if (uiState.profile.autoSpeak) RosePrimary else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Anime Waifu Persona Selector (Hinata, Aria, Sakura, Tsunade)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 58.dp, start = 14.dp, end = 14.dp)
                .align(Alignment.TopCenter)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val personas = listOf(
                Triple("Hinata 🌸", 1.08f, 0.94f),
                Triple("Aria ❤️", 1.02f, 1.0f),
                Triple("Sakura ⚡", 1.06f, 1.04f),
                Triple("Tsunade 🌙", 0.98f, 0.95f),
                Triple("Sweet Waifu 🌺", 1.03f, 0.98f)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Call Controls: Beating Heart + Red End Call Button (When Active)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Beating Heart (Dil Dhadakta Hai) Assistant Button
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clickable {
                            if (uiState.isAssistantModeActive) {
                                onStopAssistantMode()
                            } else {
                                onToggleAssistantMode()
                            }
                        }
                        .testTag("beating_heart_assistant_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Concentric Radiating Pulse Rings when heart is beating
                    if (uiState.isAssistantModeActive) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .scale(heartRippleScale)
                                .clip(CircleShape)
                                .background(HeartRed.copy(alpha = heartRippleAlpha * 0.4f))
                        )
                    }

                    // The Beating Heart Circle Button
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .scale(if (uiState.isAssistantModeActive) heartbeatScale else 1.0f)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = if (uiState.isAssistantModeActive) {
                                        listOf(HeartRed, Color(0xFFFF1744), Color(0xFFD50000))
                                    } else {
                                        listOf(RosePrimary, RoseSecondary, Color(0xFFC2185B))
                                    }
                                )
                            )
                            .border(
                                width = 2.5.dp,
                                color = if (uiState.isAssistantModeActive) Color.White else Color.White.copy(alpha = 0.65f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isAssistantModeActive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (uiState.isAssistantModeActive) "Stop Assistant (Dil Dhadak Raha Hai)" else "Start Assistant with Heart",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Dedicated Red End Call Button when call is active
                if (uiState.isAssistantModeActive) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onStopAssistantMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("end_call_red_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "End Call 🛑",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Descriptive Subtitle
            Text(
                text = if (uiState.isAssistantModeActive) {
                    if (uiState.isSpeaking) {
                        "📞 CALL ACTIVE • $callDurationStr • Girlfriend Bol Rahi Hai 💕 (Mic Ready)"
                    } else {
                        "📞 CALL ACTIVE • $callDurationStr • MIC ALWAYS ON 🎙️ (Bolo, sun rahi hoon!)"
                    }
                } else {
                    "Dil par tap karein phone call start karne ke liye ❤️ (Real Hands-Free Call)"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (uiState.isAssistantModeActive) HeartRed else Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}
