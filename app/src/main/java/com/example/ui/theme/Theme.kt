package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RoseDarkPrimary,
    onPrimary = Color.White,
    primaryContainer = RoseDarkSurfaceVariant,
    onPrimaryContainer = RoseTertiary,
    secondary = RoseDarkSecondary,
    onSecondary = Color.Black,
    tertiary = WarmGold,
    background = RoseDarkBackground,
    onBackground = Color(0xFFFCEEFA),
    surface = RoseDarkSurface,
    onSurface = Color(0xFFFCEEFA),
    surfaceVariant = RoseDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFE0C4DC)
)

private val LightColorScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E4),
    onPrimaryContainer = Color(0xFF5A0022),
    secondary = RoseSecondary,
    onSecondary = Color.White,
    tertiary = WarmGold,
    background = RoseBackgroundLight,
    onBackground = Color(0xFF2C1623),
    surface = RoseSurfaceLight,
    onSurface = Color(0xFF2C1623),
    surfaceVariant = Color(0xFFF9E7EF),
    onSurfaceVariant = Color(0xFF53434B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent girlfriend aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
