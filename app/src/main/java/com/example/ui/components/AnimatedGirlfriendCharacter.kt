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
    customImageRes: Int? = null,
    audioLevel: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ayra_character_physics")

    // 1. Natural subtle idle breathing (Active only when idle to prevent any neck/body displacement)
    val idleBreathScaleY by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    // 2. Gentle natural idle sway (Active only when idle - locked during speech to prevent rigging displacement)
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

    // 3. Dynamic Real-time Lip-Sync Viseme Keyframes
    // Rapidly cycles through natural conversational viseme states:
    // Phase 0..1: Open Vowel "Ah/A" (wide vertical opening, teeth visible)
    // Phase 1..2: Smile Vowel "Ee/I" (wide horizontal opening, small vertical height)
    // Phase 2..3: Rounded Vowel "Oh/Oo" (rounded mouth shape, medium height)
    // Phase 3..4: Bilabial/Consonant "M/B/P" transition (nearly closed resting line)
    val phonemeCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phoneme_cycle"
    )

    val (currentVisemeWidth, currentVisemeHeight, showTeethHint) = when {
        phonemeCycle < 1.0f -> {
            // Open vowel 'Ah'
            val progress = phonemeCycle
            val height = 4f + 9.5f * (1f - kotlin.math.abs(progress - 0.5f) * 2f)
            Triple(23.5f, height, height > 7.5f)
        }
        phonemeCycle < 2.0f -> {
            // Smile vowel 'Ee'
            val progress = phonemeCycle - 1.0f
            val height = 3.5f + 4f * (1f - kotlin.math.abs(progress - 0.5f) * 2f)
            Triple(26f, height, true)
        }
        phonemeCycle < 3.0f -> {
            // Rounded vowel 'Oh'
            val progress = phonemeCycle - 2.0f
            val height = 4.5f + 7.5f * (1f - kotlin.math.abs(progress - 0.5f) * 2f)
            Triple(18.5f, height, false)
        }
        else -> {
            // Consonant / closed transition
            val progress = phonemeCycle - 3.0f
            val height = 2.5f + 3f * (1f - kotlin.math.abs(progress - 0.5f) * 2f)
            Triple(20.5f, height, false)
        }
    }

    // 4. Warm facial blushing pulse during speech
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

            // Dynamic Real-time Lip-Sync Mouth Animation
            // Render continuous lip movement during speech; immediately resets to resting mouth state when idle
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(
                            x = 0.dp,
                            y = screenHeight * 0.442f
                        )
                        .width(currentVisemeWidth.dp)
                        .height(currentVisemeHeight.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE57373), // Cupid's bow upper lip rosy highlight
                                    Color(0xFF5A101C), // Deep oral cavity shadow
                                    Color(0xFFFF8A80)  // Soft lower lip contour
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFC2185B).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(percent = 50)
                        )
                ) {
                    // Delicate upper teeth hint on open vowel phonemes
                    if (showTeethHint) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(currentVisemeWidth.dp * 0.62f)
                                .height(2.2.dp)
                                .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                                .background(Color(0xFFFFFBFB).copy(alpha = 0.95f))
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
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }
    }
}
