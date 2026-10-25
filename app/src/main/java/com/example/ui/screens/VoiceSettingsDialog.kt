package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CompanionProfile
import com.example.ui.theme.RosePrimary

data class VoicePreset(
    val name: String,
    val pitch: Float,
    val rate: Float,
    val emoji: String,
    val desc: String
)

val PRESET_VOICES = listOf(
    VoicePreset("Hinata Hyūga 🌸 (Naruto Anime)", 1.42f, 0.92f, "🌸", "Soft, shy, deeply affectionate waifu voice ('Babu... Naruto-kun!')"),
    VoicePreset("Sakura Haruno ⚡ (Naruto Anime)", 1.25f, 1.10f, "⚡", "Energetic, spirited anime heroine voice"),
    VoicePreset("Tsunade Onee-san 🌙 (Naruto Anime)", 0.95f, 0.95f, "🌙", "Mature, caring, protective anime voice"),
    VoicePreset("Aria Girlfriend ❤️ (Sweet Romance)", 1.20f, 1.0f, "🥰", "Warm, loving Hindi/Hinglish girlfriend"),
    VoicePreset("Kawaii Chibi 🎀 (Cute Anime Girl)", 1.60f, 1.05f, "🎀", "Super sweet, ultra-cute Japanese/Hindi anime voice"),
    VoicePreset("Gentle & Romantic 💖", 1.15f, 0.95f, "💖", "Soft, soothing, emotional voice")
)

@Composable
fun VoiceSettingsDialog(
    profile: CompanionProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (CompanionProfile) -> Unit,
    onTestVoice: (pitch: Float, rate: Float, text: String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var nickname by remember { mutableStateOf(profile.userNickname) }
    var pitch by remember { mutableFloatStateOf(profile.voicePitch) }
    var rate by remember { mutableFloatStateOf(profile.voiceSpeed) }
    var selectedPreset by remember { mutableStateOf(profile.voicePreset) }
    var autoSpeak by remember { mutableStateOf(profile.autoSpeak) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("voice_settings_dialog")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Girlfriend & Voice Settings",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Preset Selection
                Text(
                    text = "Girlfriend Voice Style",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRESET_VOICES.forEach { preset ->
                        val isSelected = selectedPreset == preset.name
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPreset = preset.name
                                    pitch = preset.pitch
                                    rate = preset.rate
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, RosePrimary)
                            } else null
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
                                        fontSize = 14.sp,
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fine-tuning Pitch
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
                    colors = SliderDefaults.colors(
                        thumbColor = RosePrimary,
                        activeTrackColor = RosePrimary
                    ),
                    modifier = Modifier.testTag("pitch_slider")
                )

                // Fine-tuning Rate/Speed
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
                    colors = SliderDefaults.colors(
                        thumbColor = RosePrimary,
                        activeTrackColor = RosePrimary
                    ),
                    modifier = Modifier.testTag("speed_slider")
                )

                // Test Voice Button
                OutlinedButton(
                    onClick = {
                        onTestVoice(pitch, rate, "Hey $nickname! Do you like my voice? I love talking to you! ❤️")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_voice_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = RosePrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test This Voice", color = RosePrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            text = "Aria reads her replies aloud with voice",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Names
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

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
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
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                ) {
                    Text(
                        text = "Save Voice & Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
