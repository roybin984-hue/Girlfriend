package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CompanionProfile
import com.example.ui.theme.HeartRed
import com.example.ui.theme.RosePrimary

@Composable
fun VoiceProfileScreen(
    profile: CompanionProfile,
    geminiApiKey: String,
    onSaveProfile: (CompanionProfile) -> Unit,
    onTestVoice: (pitch: Float, rate: Float, text: String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onOpenAdminPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    var nickname by remember(profile.userNickname) { mutableStateOf(profile.userNickname) }
    var pitch by remember(profile.voicePitch) { mutableFloatStateOf(profile.voicePitch) }
    var rate by remember(profile.voiceSpeed) { mutableFloatStateOf(profile.voiceSpeed) }
    var selectedPreset by remember(profile.voicePreset) { mutableStateOf(profile.voicePreset) }
    var autoSpeak by remember(profile.autoSpeak) { mutableStateOf(profile.autoSpeak) }
    var apiKeyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var adminTapCount by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(3.dp, RosePrimary, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_anime_gf_live),
                        contentDescription = "Aria Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Dating $nickname 💕",
                    fontSize = 13.sp,
                    color = RosePrimary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Relationship Level Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = HeartRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Level ${profile.relationshipLevel} Bond",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${profile.affectionXp} / ${profile.relationshipLevel * 100} XP",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val progress = (profile.affectionXp % 100) / 100f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = RosePrimary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Girlfriend Voice Persona",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                PRESET_VOICES.forEach { preset ->
                    val isSelected = selectedPreset == preset.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedPreset = preset.name
                                pitch = preset.pitch
                                rate = preset.rate
                                if (preset.name.contains("Hinata")) name = "Hinata"
                                else if (preset.name.contains("Sakura")) name = "Sakura"
                                else if (preset.name.contains("Tsunade")) name = "Tsunade"
                                else if (preset.name.contains("Aria")) name = "Aria"
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(preset.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = preset.desc,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pitch Slider
                Text(
                    text = "Voice Pitch: ${String.format("%.2f", pitch)}x",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = pitch,
                    onValueChange = {
                        pitch = it
                        selectedPreset = "Custom"
                    },
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = RosePrimary, activeTrackColor = RosePrimary)
                )

                // Speed Slider
                Text(
                    text = "Speaking Speed: ${String.format("%.2f", rate)}x",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = rate,
                    onValueChange = {
                        rate = it
                        selectedPreset = "Custom"
                    },
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = RosePrimary, activeTrackColor = RosePrimary)
                )

                // Test Voice Button
                OutlinedButton(
                    onClick = {
                        onTestVoice(pitch, rate, "Hey $nickname! Do you like my new voice? I love talking with you! ❤️")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RosePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test This Voice", color = RosePrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto speak toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto Speak Responses",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Aria reads her replies with voice",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { autoSpeak = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RosePrimary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Edit Names
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Name & Nickname",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Girlfriend Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Your Nickname (What she calls you)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Spacer(modifier = Modifier.height(16.dp))

        // Google AI Studio API Key Card: Completely hidden if already set and active
        if (geminiApiKey.isBlank() || geminiApiKey == "MY_GEMINI_API_KEY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google AI Studio API Key 🔑",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Gray.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Setup Required ⚪",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Apni Google AI Studio Gemini API key paste karke save karein. Save karne ke baad ye section automatic hide ho jayega!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("Paste AI Studio API Key...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onSaveApiKey(apiKeyInput) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                    ) {
                        Text("Save & Activate API Key 💾", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save All Settings Button
        Button(
            onClick = {
                onSaveProfile(
                    profile.copy(
                        name = name.ifBlank { "Aria" },
                        userNickname = nickname.ifBlank { "Babe" },
                        voicePitch = pitch,
                        voiceSpeed = rate,
                        voicePreset = selectedPreset,
                        autoSpeak = autoSpeak
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_profile_tab_btn"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
        ) {
            Text("Save Settings ❤️", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hidden Admin Panel Access: Secret 5-tap on version label (completely hidden from normal users)
        Text(
            text = "Aria AI Girlfriend • Version 1.0.0",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable {
                    adminTapCount++
                    if (adminTapCount >= 5) {
                        adminTapCount = 0
                        onOpenAdminPanel()
                    }
                }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
