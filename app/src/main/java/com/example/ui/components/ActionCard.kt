package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActionType
import com.example.data.CompanionAction
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.WhatsAppGreen

@Composable
fun ActionCard(
    action: CompanionAction,
    onExecuteAction: (CompanionAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val (cardColor, icon, buttonLabel) = when (action.type) {
        ActionType.WHATSAPP_OPEN -> Triple(
            WhatsAppGreen.copy(alpha = 0.12f),
            Icons.AutoMirrored.Filled.Chat,
            "Open WhatsApp"
        )
        ActionType.WHATSAPP_SEND -> Triple(
            WhatsAppGreen.copy(alpha = 0.15f),
            Icons.AutoMirrored.Filled.Send,
            if (action.target.isNotBlank()) "Send to ${action.target}" else "Send WhatsApp"
        )
        ActionType.INSTAGRAM_OPEN -> Triple(
            InstagramPurple.copy(alpha = 0.12f),
            Icons.Default.CameraAlt,
            "Open Instagram"
        )
        ActionType.INSTAGRAM_STORY -> Triple(
            InstagramPurple.copy(alpha = 0.15f),
            Icons.Default.Share,
            "Post Story to Instagram"
        )
        ActionType.PHONE_CALL -> Triple(
            Color(0xFF2196F3).copy(alpha = 0.12f),
            Icons.Default.Call,
            "Open Phone Dialer"
        )
        ActionType.SEND_SMS -> Triple(
            Color(0xFF009688).copy(alpha = 0.12f),
            Icons.AutoMirrored.Filled.Chat,
            "Send SMS"
        )
        ActionType.SET_ALARM -> Triple(
            Color(0xFFFF9800).copy(alpha = 0.15f),
            Icons.Default.Alarm,
            "Set Clock Alarm"
        )
        ActionType.SET_TIMER -> Triple(
            Color(0xFFFF5722).copy(alpha = 0.15f),
            Icons.Default.Alarm,
            "Start Timer"
        )
        ActionType.OPEN_YOUTUBE -> Triple(
            Color(0xFFE53935).copy(alpha = 0.12f),
            Icons.Default.PlayArrow,
            "Play on YouTube"
        )
        ActionType.WEB_SEARCH -> Triple(
            Color(0xFF4285F4).copy(alpha = 0.12f),
            Icons.Default.Search,
            "Search on Google"
        )
        ActionType.OPEN_APP -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            Icons.Default.CameraAlt,
            "Open ${action.target.ifBlank { "App" }}"
        )
        ActionType.NONE -> Triple(Color.Transparent, Icons.AutoMirrored.Filled.Chat, "Action")
    }

    if (action.type == ActionType.NONE) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("action_card_${action.type.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = when (action.type) {
                                ActionType.WHATSAPP_OPEN, ActionType.WHATSAPP_SEND -> WhatsAppGreen
                                ActionType.INSTAGRAM_OPEN, ActionType.INSTAGRAM_STORY -> InstagramPurple
                                ActionType.PHONE_CALL -> Color(0xFF1976D2)
                                ActionType.SET_ALARM, ActionType.SET_TIMER -> Color(0xFFF57C00)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = action.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = action.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (action.payload.isNotBlank() && action.type != ActionType.NONE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "“${action.payload}”",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { onExecuteAction(action) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("execute_action_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (action.type) {
                        ActionType.WHATSAPP_OPEN, ActionType.WHATSAPP_SEND -> WhatsAppGreen
                        ActionType.INSTAGRAM_OPEN, ActionType.INSTAGRAM_STORY -> InstagramPurple
                        ActionType.PHONE_CALL -> Color(0xFF1976D2)
                        ActionType.SET_ALARM, ActionType.SET_TIMER -> Color(0xFFF57C00)
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}
