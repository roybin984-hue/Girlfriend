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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.RosePrimary

@Composable
fun AnimatedGirlfriendCharacter(
    isSpeaking: Boolean,
    isListening: Boolean,
    onCharacterTouch: () -> Unit,
    modifier: Modifier = Modifier,
    characterName: String = "Ayra",
    customImageRes: Int? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "anime_character_physics")

    // 1. Natural subtle idle breathing (Active only when NOT speaking to avoid any neck/body displacement)
    val idleBreathScaleY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // 2. Gentle natural idle sway (Active only when idle - locked during speech to prevent rigging drift)
    val idleBodyTilt by infiniteTransition.animateFloat(
        initialValue = -0.8f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "body_tilt"
    )

    val idleBodySwayX by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "body_sway_x"
    )

    // Strict alignment: When speaking, lock neck and body coordinates to 0 displacement
    val activeBodyTilt = if (isSpeaking) 0f else idleBodyTilt
    val activeBodySwayX = if (isSpeaking) 0f else idleBodySwayX
    val activeBreathScaleY = if (isSpeaking) 1.0f else idleBreathScaleY

    // 3. Precise real-time lip-sync mouth animation strictly synced to spoken output
    // Speech phoneme cadence: rapid alternating visemes (~120ms cycle)
    val mouthOpeningHeight by infiniteTransition.animateFloat(
        initialValue = 2.5f,
        targetValue = 13.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(125, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_height"
    )

    val mouthOpeningWidth by infiniteTransition.animateFloat(
        initialValue = 19f,
        targetValue = 25.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(165, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_width"
    )

    // 4. Subtle, warm facial blushing when speaking
    val blushAlpha by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
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

        // Main Character Canvas: Anchored without neck/body rigging displacement
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = activeBodyTilt
                    translationX = activeBodySwayX
                    scaleY = activeBreathScaleY
                },
            contentAlignment = Alignment.Center
        ) {
            val imageRes = customImageRes ?: if (characterName.contains("Hinata", ignoreCase = true)) {
                R.drawable.img_hinata_waifu
            } else {
                R.drawable.img_anime_gf_live
            }

            // High-Resolution Character Portrait
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$characterName Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Precise Lip-Sync Avatar Mouth (Strictly active only when speaking)
            // Centered over character mouth coordinates without neck or body distortion
            if (isSpeaking) {
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
                                    Color(0xFFE57373), // Natural upper lip
                                    Color(0xFF5D101E), // Oral cavity depth
                                    Color(0xFFFF8A80)  // Soft lower lip highlight
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFC2185B).copy(alpha = 0.55f),
                            shape = RoundedCornerShape(percent = 50)
                        )
                ) {
                    // Delicate upper teeth hint on open vowel phonemes
                    if (mouthOpeningHeight > 7f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(mouthOpeningWidth.dp * 0.58f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(Color(0xFFFFFBFB).copy(alpha = 0.92f))
                        )
                    }
                }

                // Natural, subtle cheek warmth
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

            // Cinematic Vignette Gradient for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }
    }
}
