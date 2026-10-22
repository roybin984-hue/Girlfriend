package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HeartRed
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary

@Composable
fun GirlfriendAvatar(
    isSpeaking: Boolean,
    isListening: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onClick: () -> Unit = {}
) {
    val transition = rememberInfiniteTransition(label = "pulse_transition")

    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val isActive = isSpeaking || isListening

    Box(
        modifier = modifier
            .size(size + 14.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Glowing animated ring when speaking or listening
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(size + 10.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isSpeaking) RosePrimary.copy(alpha = 0.4f) else HeartRed.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Avatar Image
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    width = if (isActive) 2.5.dp else 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(RosePrimary, RoseSecondary)
                    ),
                    shape = CircleShape
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_girlfriend_avatar),
                contentDescription = "AI Girlfriend Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }

        // Small status badge (heart / mic / spark)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isListening) HeartRed else RosePrimary)
                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isListening) "🎙️" else if (isSpeaking) "💖" else "✨",
                fontSize = 9.sp
            )
        }
    }
}
