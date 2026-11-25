package com.pedro.baruck.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pedro.baruck.ui.theme.BaruckColors

/**
 * Botão "3D" estilo Duolingo: topo colorido sobre uma faixa mais escura,
 * que afunda quando pressionado.
 */
@Composable
fun BaruckButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = BaruckColors.Green,
    shadow: Color = BaruckColors.GreenDark,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 4.dp
    val offset by animateDpAsState(if (pressed) depth else 0.dp, label = "press")
    val faceColor = if (enabled) color else BaruckColors.Line
    val baseColor = if (enabled) shadow else Color(0xFFCCCCCC)

    Box(modifier.height(56.dp)) {
        // faixa de baixo (sombra sólida)
        Box(
            Modifier.fillMaxSize().padding(top = depth)
                .clip(RoundedCornerShape(16.dp)).background(baseColor)
        )
        // face de cima
        Box(
            Modifier.fillMaxWidth().height(52.dp).offset(y = offset)
                .clip(RoundedCornerShape(16.dp)).background(faceColor)
                .clickable(interaction, null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text.uppercase(),
                color = if (enabled) textColor else BaruckColors.InkSoft,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/** Botão secundário: contorno, sem preenchimento. */
@Composable
fun BaruckOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BaruckColors.Blue,
    onClick: () -> Unit
) {
    Box(
        modifier.height(52.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, BaruckColors.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), color = color, style = MaterialTheme.typography.labelLarge)
    }
}

/** Cartão branco com borda inferior sólida (o visual "chunky" do Duolingo). */
@Composable
fun BaruckCard(
    modifier: Modifier = Modifier,
    accent: Color = BaruckColors.Line,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
    Column(
        (if (onClick != null) base.clickable { onClick() } else base)
            .padding(16.dp)
            .then(modifier),
        content = content
    )
}

@Composable
fun Pill(text: String, color: Color, bg: Color) {
    Box(
        Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 5.dp)
    ) { Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
}

/** Campo de texto arredondado, cheio de personalidade. */
@Composable
fun BaruckField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: androidx.compose.ui.text.input.KeyboardType =
        androidx.compose.ui.text.input.KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BaruckColors.Green,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

/** Estado vazio amigável com o Baruck. */
@Composable
fun EmptyState(baruckRes: Int, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(baruckRes),
            contentDescription = null,
            modifier = Modifier.height(140.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle, style = MaterialTheme.typography.bodyMedium,
            color = BaruckColors.InkSoft, textAlign = TextAlign.Center
        )
    }
}
