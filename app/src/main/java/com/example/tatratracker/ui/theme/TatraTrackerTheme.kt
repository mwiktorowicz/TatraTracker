package com.example.tatratracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TatraGreen = Color(0xFF72A832)
val TatraGreenDark = Color(0xFF527D22)
val TatraGreenSoft = Color(0xFFE4EED5)
val TatraGreenVerySoft = Color(0xFFF1F6EA)

val TatraDark = Color(0xFF101A1D)
val TatraDarkSurface = Color(0xFF172326)
val TatraDarkCard = Color(0xFF1D2A2D)
val TatraDarkMuted = Color(0xFF263438)

val TatraBackground = Color(0xFFF3F4EE)
val TatraSurface = Color(0xFFFFFFFF)
val TatraSurfaceSoft = Color(0xFFF8F9F5)
val TatraSurfaceStrong = Color(0xFFEBEEE8)

val TatraText = Color(0xFF152023)
val TatraTextMuted = Color(0xFF687276)
val TatraTextSoft = Color(0xFF899194)
val TatraOutline = Color(0xFFD8DDD5)
val TatraWhite = Color(0xFFFFFFFF)

val TatraDanger = Color(0xFFC8473E)
val TatraDangerSoft = Color(0xFFF7E6E3)
val TatraWarning = Color(0xFFE2A23B)
val TatraWarningSoft = Color(0xFFF8EED8)
val TatraInfo = Color(0xFF4B7FA8)
val TatraInfoSoft = Color(0xFFE7F0F7)

val TatraTrailNotCompleted = Color(0xFFD76B58)
val TatraTrailCompleted = Color(0xFF539E41)
val TatraTrailPartial = Color(0xFFE2A23B)

private val TatraColorScheme =
    lightColorScheme(
        primary = TatraGreen,
        onPrimary = TatraWhite,
        primaryContainer = TatraGreenSoft,
        onPrimaryContainer = TatraDark,
        secondary = TatraGreenDark,
        onSecondary = TatraWhite,
        background = TatraBackground,
        onBackground = TatraText,
        surface = TatraSurface,
        onSurface = TatraText,
        surfaceVariant = TatraSurfaceSoft,
        onSurfaceVariant = TatraTextMuted,
        outline = TatraOutline,
        error = TatraDanger,
        onError = TatraWhite
    )

private val TatraTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 39.sp,
                letterSpacing = (-0.5).sp
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                letterSpacing = (-0.35).sp
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.2).sp
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 25.sp
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                lineHeight = 24.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 21.sp
            ),
        titleSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 19.sp
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.sp
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 15.sp
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
    )

private val TatraShapes =
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp)
    )

object TatraSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}

@Composable
fun TatraTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TatraColorScheme,
        typography = TatraTypography,
        shapes = TatraShapes,
        content = content
    )
}