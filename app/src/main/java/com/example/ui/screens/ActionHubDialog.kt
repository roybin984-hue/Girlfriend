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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ActionType
import com.example.data.CompanionAction
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.WhatsAppGreen

data class ActionShortcut(
    val title: String,
    val subtitle: String,
    val commandText: String,
    val icon: ImageVector,
    val iconColor: Color,
    val action: CompanionAction
)

@Composable
fun ActionHubDialog(
    onDismiss: () -> Unit,
    onRunAction: (CompanionAction) -> Unit,
    onSendCommand: (String) -> Unit
) {
    val shortcuts = listOf(
        ActionShortcut(
            title = "Open WhatsApp",
            subtitle = "Say: 'Open WhatsApp' or 'WhatsApp kholo'",
            commandText = "Open WhatsApp",
            icon = Icons.AutoMirrored.Filled.Chat,
            iconColor = WhatsAppGreen,
            action = CompanionAction(ActionType.WHATSAPP_OPEN, "Open WhatsApp", "Opening WhatsApp app")
        ),
        ActionShortcut(
            title = "Send WhatsApp Hello",
            subtitle = "Say: 'Send WhatsApp message Hello to Rahul'",
            commandText = "Send WhatsApp message Hello to my friend",
            icon = Icons.AutoMirrored.Filled.Chat,
            iconColor = WhatsAppGreen,
            action = CompanionAction(
                ActionType.WHATSAPP_SEND,
                "Send WhatsApp Message",
                "Send Hello on WhatsApp",
                payload = "Hello! ❤️ Hope you are having a wonderful day!"
            )
        ),
        ActionShortcut(
            title = "Post Instagram Story",
            subtitle = "Say: 'Instagram story lagao' or 'Post story on Insta'",
            commandText = "Share a story on Instagram",
            icon = Icons.Default.Share,
            iconColor = InstagramPurple,
            action = CompanionAction(
                ActionType.INSTAGRAM_STORY,
                "Instagram Story",
                "Ready to post story",
                payload = "Chilling with my favorite people! ✨🌸"
            )
        ),
        ActionShortcut(
            title = "Open Instagram",
            subtitle = "Say: 'Instagram open karo'",
            commandText = "Open Instagram",
            icon = Icons.Default.CameraAlt,
            iconColor = InstagramPurple,
            action = CompanionAction(ActionType.INSTAGRAM_OPEN, "Open Instagram", "Opening Instagram feed")
        ),
        ActionShortcut(
            title = "Make a Phone Call",
            subtitle = "Say: 'Call Mom' or 'Call 9876543210'",
            commandText = "Call someone for me",
            icon = Icons.Default.Call,
            iconColor = Color(0xFF1976D2),
            action = CompanionAction(ActionType.PHONE_CALL, "Phone Call", "Opening dialer", target = "")
        ),
        ActionShortcut(
            title = "Set Morning Alarm",
            subtitle = "Say: 'Set alarm for 7 AM' or 'Alarm lagao'",
            commandText = "Set alarm for 7:00 AM",
            icon = Icons.Default.Alarm,
            iconColor = Color(0xFFF57C00),
            action = CompanionAction(ActionType.SET_ALARM, "Set Alarm", "7:00 AM Wake up", target = "7:00 AM", payload = "Wake up handsome ❤️")
        ),
        ActionShortcut(
            title = "Play YouTube Song",
            subtitle = "Say: 'Play romantic songs on YouTube'",
            commandText = "Play romantic songs on YouTube",
            icon = Icons.Default.PlayArrow,
            iconColor = Color(0xFFD32F2F),
            action = CompanionAction(ActionType.OPEN_YOUTUBE, "YouTube Music", "Play romantic songs", payload = "romantic acoustic love songs")
        ),
        ActionShortcut(
            title = "Open Camera",
            subtitle = "Say: 'Camera open karo' or 'Take a photo'",
            commandText = "Open camera",
            icon = Icons.Default.CameraAlt,
            iconColor = RosePrimary,
            action = CompanionAction(ActionType.OPEN_APP, "Open Camera", "Ready for photos", target = "camera")
        ),
        ActionShortcut(
            title = "Google Search",
            subtitle = "Say: 'Search best restaurants nearby'",
            commandText = "Search romantic date spots near me",
            icon = Icons.Default.Search,
            iconColor = Color(0xFF4285F4),
            action = CompanionAction(ActionType.WEB_SEARCH, "Google Search", "Searching places", payload = "romantic restaurants nearby")
        )
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("action_hub_dialog")
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
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Aria Action Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Aria does what you ask! Tap any card to test directly or speak to her via voice mic! 🌸",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    shortcuts.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRunAction(item.action)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(item.iconColor, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "Run ⚡",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
