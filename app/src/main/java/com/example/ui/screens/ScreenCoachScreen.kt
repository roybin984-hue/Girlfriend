package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary
import com.example.ui.theme.WarmGold

data class GameProfile(val id: String, val name: String, val icon: String, val tip: String)

val SUPPORTED_GAMES = listOf(
    GameProfile("bgmi", "BGMI / PUBG", "🎯", "Watch 120° angles, keep smoke grenades ready, heal behind cover!"),
    GameProfile("freefire", "Free Fire", "🔫", "Use Gloo Wall immediately when shot, aim for head level!"),
    GameProfile("cod", "Call of Duty", "⚔️", "Pre-aim corners, slide cancel for speed, use UAV streaks!"),
    GameProfile("minecraft", "Minecraft", "⛏️", "Never dig straight down! Keep torches in offhand!"),
    GameProfile("subway", "Subway Surfers", "🏃", "Stay on train roofs, upgrade 2x multiplier first!"),
    GameProfile("screen", "Any App / WhatsApp", "📱", "I watch your screen and assist whenever you ask!")
)

@Composable
fun ScreenCoachScreen(
    userNickname: String,
    onSpeakTip: (String) -> Unit,
    isScreenShareActive: Boolean = false,
    onToggleScreenShare: (Boolean) -> Unit = {},
    onLaunchGame: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedGame by remember { mutableStateOf(SUPPORTED_GAMES[0]) }
    var isOverlayActive by remember { mutableStateOf(false) }
    var currentCoachAdvice by remember {
        mutableStateOf("Ready to coach you! Launch your game and I will watch and guide you live! 🎮✨")
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    hasOverlayPermission = Settings.canDrawOverlays(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Hero Card: Gaming Coach & Screen Watcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RosePrimary.copy(alpha = 0.2f), WarmGold.copy(alpha = 0.15f))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RosePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Aria Game Coach & Screen Watcher",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "I watch your gameplay & give real-time pro tips!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "“$currentCoachAdvice”",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Overlay Permission Banner if not granted
        if (!hasOverlayPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Display Over Other Apps Permission",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Required so Aria can stay visible on screen while you play games or use WhatsApp!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Floating Overlay Control Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isOverlayActive) Color(0xFF4CAF50).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOverlayActive) Icons.Default.CheckCircle else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isOverlayActive) Color(0xFF4CAF50) else RosePrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isOverlayActive) "Screen Companion Active 🟢" else "Floating Screen Buddy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isOverlayActive) "Aria is watching your screen and games" else "Floats on top of games & WhatsApp",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        isOverlayActive = !isOverlayActive
                        val msg = if (isOverlayActive) {
                            "Aria is now floating and watching your screen! Let's win this match, $userNickname! 🔥"
                        } else {
                            "Screen overlay paused."
                        }
                        currentCoachAdvice = msg
                        onSpeakTip(msg)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOverlayActive) Color(0xFF4CAF50) else RosePrimary
                    )
                ) {
                    Text(
                        text = if (isOverlayActive) "Active" else "Start",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Game Selection
        Text(
            text = "Select What You Are Playing",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SUPPORTED_GAMES.forEach { game ->
                val isSelected = selectedGame.id == game.id
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, RosePrimary) else null,
                    modifier = Modifier.clickable {
                        selectedGame = game
                        currentCoachAdvice = "${game.name} detected! ${game.tip}"
                        onSpeakTip(currentCoachAdvice)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = game.icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = game.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) RosePrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Instant Live Tactical Advice Buttons
        Text(
            text = "Ask Aria Tactical Game Tips",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        val tacticalQuestions = listOf(
            "What should I do right now?" to "Analyze your radar babe! Look out for flanking enemies and reload before pushing into the zone! 🎯",
            "Give me a pro tip for this game!" to selectedGame.tip,
            "Cheer me up, I died in game! 🥺" to "It's totally okay $userNickname! You played bravely! Let's start a fresh match and wipe them out together! I believe in you! ❤️",
            "Is anyone behind me?" to "Watch your rear flank! Check your headphone footsteps sound, keep your crosshair steady! 💥"
        )

        tacticalQuestions.forEach { (question, answer) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable {
                        currentCoachAdvice = answer
                        onSpeakTip(answer)
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = RosePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = question,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Speak",
                        tint = RosePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
