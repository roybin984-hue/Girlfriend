package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainUiState
import com.example.ui.components.AudioWaveIndicator
import com.example.ui.theme.HeartRed
import com.example.ui.theme.RoseDarkBackground
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary

@Composable
fun LiveAnimeVoiceScreen(
    uiState: MainUiState,
    onToggleMic: () -> Unit,
    onToggleVoice: () -> Unit,
    onSendQuickCommand: (String) -> Unit,
    onAvatarTouch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Breathing & pulse animation for anime girl
    val infiniteTransition = rememberInfiniteTransition(label = "anime_breathing")

    val breathScaleY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_y"
    )

    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val latestGirlfriendMessage = uiState.messages.lastOrNull {
        it.sender == com.example.data.MessageSender.GIRLFRIEND
    }?.text ?: "Hey ${uiState.profile.userNickname}! Tap the big mic below to talk to me! ❤️"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RoseDarkBackground)
    ) {
        // Anime Girlfriend Character Visual
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleX = 1.0f, scaleY = breathScaleY)
                .clickable(onClick = onAvatarTouch),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_anime_gf_live),
                contentDescription = "Live Anime Girlfriend",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Romantic gradient overlay at the top & bottom for high readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        // Top Status Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, RosePrimary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isListening) HeartRed else Color(0xFF4CAF50))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isListening) "Listening..." else if (uiState.isSpeaking) "Speaking..." else "Live With Aria ❤️",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleVoice,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.profile.autoSpeak) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = "Toggle Voice",
                            tint = if (uiState.profile.autoSpeak) RosePrimary else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom Section: Dialogue Subtitles + Action Chips + Giant Glowing Voice Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Dialogue Card / Subtitles
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_dialogue_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.72f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (uiState.isSpeaking) RosePrimary.copy(alpha = glowAlpha) else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.profile.name} 💕",
                            fontSize = 13.sp,
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

            // Quick Voice Action Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "💬 Open WhatsApp",
                    "💌 Send WhatsApp Hello",
                    "📸 Post Insta Story",
                    "❤️ I love you Aria",
                    "⏰ Set 7 AM Alarm",
                    "📞 Call someone",
                    "🎵 Play romantic song"
                )
                chips.forEach { chipText ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
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

            // Giant Prominent Glowing Mic / Talk Button
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clickable(onClick = onToggleMic)
                    .testTag("live_mic_giant_btn"),
                contentAlignment = Alignment.Center
            ) {
                // Expanding glowing pulse wave
                if (uiState.isListening) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(micPulse)
                            .clip(CircleShape)
                            .background(HeartRed.copy(alpha = 0.35f))
                    )
                }

                // Inner circle
                Box(
                    modifier = Modifier
                        .size(76.dp)
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
                        .border(
                            3.dp,
                            if (uiState.isListening) Color.White else Color.White.copy(alpha = 0.6f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (uiState.isListening) "Stop Listening" else "Speak to Aria",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (uiState.isListening) "Listening... Speak your command now! 🎙️" else "Tap Mic to Talk With Girlfriend 🎙️",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.isListening) HeartRed else Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}
