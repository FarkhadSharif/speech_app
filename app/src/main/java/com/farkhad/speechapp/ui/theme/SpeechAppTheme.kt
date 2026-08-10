package com.farkhad.speechapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppBlue = Color(0xFF1689E8)
val AppGreen = Color(0xFF55DF31)
val AppOrange = Color(0xFFFF930F)
val AppPurple = Color(0xFFD52EC8)
val AppBackground = Color(0xFFFFFDF8)
val AppText = Color(0xFF273444)

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

