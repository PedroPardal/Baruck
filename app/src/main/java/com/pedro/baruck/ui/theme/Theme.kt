package com.pedro.baruck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedro.baruck.R

// ---- Paleta estilo Duolingo ----
object BaruckColors {
    val Green = Color(0xFF58CC02)        // verde principal
    val GreenDark = Color(0xFF58A700)    // sombra do botão verde
    val GreenSoft = Color(0xFFD7FFB8)    // fundo suave
    val Blue = Color(0xFF1CB0F6)
    val BlueDark = Color(0xFF1899D6)
    val Red = Color(0xFFFF4B4B)
    val RedDark = Color(0xFFEA2B2B)
    val Yellow = Color(0xFFFFC800)
    val Orange = Color(0xFFFF9600)
    val Purple = Color(0xFFCE82FF)
    val Brown = Color(0xFF4A3330)        // marrom do Baruck
    val BandanaGreen = Color(0xFF69A04B)

    val Ink = Color(0xFF3C3C3C)
    val InkSoft = Color(0xFF777777)
    val Line = Color(0xFFE5E5E5)
    val Card = Color(0xFFFFFFFF)
    val Bg = Color(0xFFFFFFFF)

    // dark
    val DarkBg = Color(0xFF131F24)
    val DarkCard = Color(0xFF1B2B33)
    val DarkLine = Color(0xFF37464F)
    val DarkInk = Color(0xFFF1F7FB)
}

val Poppins = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

private fun typography() = Typography(
    displaySmall = TextStyle(Poppins, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(Poppins, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(Poppins, fontWeight = FontWeight.Bold, fontSize = 19.sp),
    titleMedium = TextStyle(Poppins, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(Poppins, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(Poppins, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

private val shapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

private val light = lightColorScheme(
    primary = BaruckColors.Green,
    onPrimary = Color.White,
    secondary = BaruckColors.Blue,
    background = BaruckColors.Bg,
    onBackground = BaruckColors.Ink,
    surface = BaruckColors.Card,
    onSurface = BaruckColors.Ink,
    error = BaruckColors.Red,
    outline = BaruckColors.Line
)

private val dark = darkColorScheme(
    primary = BaruckColors.Green,
    onPrimary = Color.White,
    secondary = BaruckColors.Blue,
    background = BaruckColors.DarkBg,
    onBackground = BaruckColors.DarkInk,
    surface = BaruckColors.DarkCard,
    onSurface = BaruckColors.DarkInk,
    error = BaruckColors.Red,
    outline = BaruckColors.DarkLine
)

@Composable
fun BaruckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) dark else light,
        typography = typography(),
        shapes = shapes,
        content = content
    )
}
