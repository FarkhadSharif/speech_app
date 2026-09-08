package com.farkhad.speechapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farkhad.speechapp.R

val AppBlue = Color(0xFF315D4D)
val AppGreen = Color(0xFF547562)
val AppOrange = Color(0xFFC48B2C)
val AppPurple = Color(0xFF765B4A)
val AppBackground = Color(0xFFF3EFE3)
val AppText = Color(0xFF222822)
val AppGold = Color(0xFFBD8428)
val AppTurquoise = Color(0xFF789184)
val AppNavy = Color(0xFF20352B)
val AppSurfaceMuted = Color(0xFFE8E2D4)
val AppOutline = Color(0xFFCFC6B4)
val AppRed = Color(0xFFB64A37)

private val SpeechColors = lightColorScheme(
    primary = AppBlue,
    secondary = AppGreen,
    tertiary = AppOrange,
    background = AppBackground,
    surface = Color.White,
    primaryContainer = Color(0xFFDCE6DD),
    onPrimaryContainer = AppNavy,
    secondaryContainer = Color(0xFFE0E6DD),
    onSecondaryContainer = Color(0xFF263E31),
    tertiaryContainer = Color(0xFFF1DFBB),
    onTertiaryContainer = Color(0xFF594015),
    surfaceVariant = AppSurfaceMuted,
    onSurfaceVariant = Color(0xFF69675F),
    outline = AppOutline,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppText,
    onSurface = AppText,
    error = AppRed,
    onError = Color.White,
)

private val InterfaceTypeface = FontFamily(
    Font(R.font.noto_sans_variable, FontWeight.Normal),
    Font(R.font.noto_sans_variable, FontWeight.Medium),
    Font(R.font.noto_sans_variable, FontWeight.SemiBold),
    Font(R.font.noto_sans_variable, FontWeight.Bold),
)

private val EditorialTypeface = FontFamily(
    Font(R.font.literata_variable, FontWeight.Normal),
    Font(R.font.literata_variable, FontWeight.SemiBold),
    Font(R.font.literata_variable, FontWeight.Bold),
    Font(R.font.literata_variable, FontWeight.ExtraBold),
)

private val SpeechTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = EditorialTypeface,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.4f).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialTypeface,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3f).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialTypeface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterfaceTypeface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterfaceTypeface,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterfaceTypeface,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterfaceTypeface,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterfaceTypeface,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterfaceTypeface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.15.sp,
    ),
)

private val SpeechShapes = Shapes(
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(12.dp),
)

@Composable
fun SpeechAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpeechColors,
        typography = SpeechTypography,
        shapes = SpeechShapes,
        content = content,
    )
}

