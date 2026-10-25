package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HeartRed
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSecondary

@Composable
fun AnimatedGirlfriendCharacter(
    isSpeaking: Boolean,
    isListening: Boolean,
    onCharacterTouch: () -> Unit,
    modifier: Modifier = Modifier,
    characterName: String = "Aria",
    customImageRes: Int? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "anime_character_physics")

    // 1. Natural Breathing Motion
    val breathScaleY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.026f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // 2. Gentle Natural Body Swaying & Head Tilt
    val bodyTiltAngle by infiniteTransition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "body_tilt"
    )

    val bodySwayX by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "body_sway_x"
    )

    // 3. Real-time Talking Mouth / Lip-Sync Movement
    // Rapid vertical open-close motion synced to conversational speech tempo (~135ms per cycle)
    val mouthOpeningHeight by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(135, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_height"
    )

    val mouthOpeningWidth by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_width"
    )

    // 4. Subtle Blushing / Glowing Pulse when speaking
    val blushAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blush_pulse"
    )

    var touchReactionNotice by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                touchReactionNotice = true
                onCharacterTouch()
            },
        contentAlignment = Alignment.Center
    ) {
        val screenHeight = maxHeight
        val screenWidth = maxWidth

        // Main Character Canvas with natural swaying & breathing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = bodyTiltAngle
                    translationX = bodySwayX
                    scaleY = breathScaleY
                },
            contentAlignment = Alignment.Center
        ) {
            val imageRes = customImageRes ?: if (characterName.contains("Hinata", ignoreCase = true)) {
                R.drawable.img_hinata_waifu
            } else {
                R.drawable.img_anime_gf_live
            }

            // Anime Character Image
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$characterName Anime Character",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Realistic Lip-Sync Moving Mouth ("Hot") when speaking
            if (isSpeaking) {
                // Centered right over the mouth area of img_anime_gf_live (around 44% of vertical height)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            x = 0.dp,
                            y = screenHeight * 0.442f
                        )
                        .width(mouthOpeningWidth.dp)
                        .height(mouthOpeningHeight.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE57373), // Upper lip pink
                                    Color(0xFF7A1C28), // Inner mouth depth
                                    Color(0xFFFF8A80)  // Lower lip highlight
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFC2185B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(percent = 50)
                        )
                ) {
                    // Small natural teeth hint inside mouth when wide open
                    if (mouthOpeningHeight > 8f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(mouthOpeningWidth.dp * 0.6f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(Color(0xFFFFF9F9).copy(alpha = 0.9f))
                        )
                    }
                }

                // Throat / Neck vocal resonance vibration when speaking
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 0.dp, y = screenHeight * 0.485f)
                        .width(20.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Color(0xFFFF8A80).copy(alpha = (mouthOpeningHeight / 28f).coerceIn(0.08f, 0.32f))
                        )
                )

                // Cute Rosy Blushing Cheek Glow
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = (-38).dp, y = screenHeight * 0.42f)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(RosePrimary.copy(alpha = blushAlpha), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(x = 38.dp, y = screenHeight * 0.42f)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(RosePrimary.copy(alpha = blushAlpha), Color.Transparent)
                            )
                        )
                )
            }

            // Vignette Shadow at top and bottom for UI contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
        }
    }
}
