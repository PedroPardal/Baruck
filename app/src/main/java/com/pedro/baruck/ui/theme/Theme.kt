package com.pedro.baruck.ui.theme

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
    val Green = Color(0xFF3F8F29)        // verde principal
    val GreenDark = Color(0xFF2E6B1D)    // sombra do botão verde
    val GreenSoft = Color(0xFFE3F0D8)    // fundo suave
    val Blue = Color(0xFF1E88C8)
    val BlueSoft = Color(0xFFDCEBF5)
    val YellowDark = Color(0xFFC8930A)
    val OrangeSoft = Color(0xFFF8E3D2)
    val OrangeDark = Color(0xFFC85A1E)
    val RedSoft = Color(0xFFF6DEDA)
    val BlueDark = Color(0xFF166A9C)
    val Red = Color(0xFFD93F2B)
    val RedDark = Color(0xFFB32F1F)
    val Yellow = Color(0xFFF2B705)
    val Orange = Color(0xFFE8702A)
    val Purple = Color(0xFFCE82FF)
    val Brown = Color(0xFF4A3330)        // marrom do Baruck
    val BandanaGreen = Color(0xFF69A04B)

    val Ink = Color(0xFF2F2F2A)
    val InkSoft = Color(0xFF6B6B63)
    val Line = Color(0xFFE2E0D8)
    val Card = Color(0xFFFFFFFF)
    val Bg = Color(0xFFFFFFFF)

    // dark
    val DarkBg = Color(0xFF131F24)
    val DarkCard = Color(0xFF1B2B33)
    val DarkLine = Color(0xFF37464F)
    val DarkInk = Color(0xFFF1F7FB)
}

/** Texto: Inter (limpa e legível). Títulos: Big Shoulders (condensada, "de máquina"). */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)
val Display = FontFamily(Font(R.font.bigshoulders_bold, FontWeight.Bold))

private fun typography() = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = 0.3.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = 0.3.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = 0.3.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.8.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

private val light = lightColorScheme(
    primary = BaruckColors.Green,
    onPrimary = Color.White,
    secondary = BaruckColors.Blue,
    background = BaruckColors.Bg,
    onBackground = BaruckColors.Ink,
    surfaceVariant = Color(0xFFF7F7F7),
    onSurfaceVariant = BaruckColors.InkSoft,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF7F7F7),
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,
    surface = BaruckColors.Card,
    onSurface = BaruckColors.Ink,
    error = BaruckColors.Red,
    outline = BaruckColors.Line
)

@Suppress("unused")
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

/** Sempre claro, mesmo com o celular no modo escuro. */
@Composable
fun BaruckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = light,
        typography = typography(),
        shapes = shapes,
        content = content
    )
}
