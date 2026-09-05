package com.farkhad.speechapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppBlue = Color(0xFF00B2FF) // Sky Blue
val AppGreen = Color(0xFF4CAF50)
val AppOrange = Color(0xFFFFC107) // Gold
val AppPurple = Color(0xFF9C27B0)
val AppBackground = Color(0xFFF8FBFD)
val AppText = Color(0xFF1A237E)
val AppGold = Color(0xFFFFD700)
val AppTurquoise = Color(0xFF00F2FF)

private val SpeechColors = lightColorScheme(
    primary = AppBlue,
    secondary = AppGreen,
    tertiary = AppOrange,
    background = AppBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppText,
    onSurface = AppText,
)

@Composable
fun SpeechAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpeechColors,
        typography = androidx.compose.material3.Typography(
            headlineLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
            ),
        ),
        content = content,
    )
}

