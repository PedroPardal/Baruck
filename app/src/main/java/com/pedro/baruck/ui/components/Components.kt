package com.pedro.baruck.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pedro.baruck.R
import com.pedro.baruck.data.UserProfile
import com.pedro.baruck.data.Xp
import com.pedro.baruck.ui.Avatars
import com.pedro.baruck.ui.Sfx
import com.pedro.baruck.ui.shortDate
import com.pedro.baruck.ui.theme.BaruckColors
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.random.Random

/* ============================ Botões ============================ */

/** Botão "3D" estilo Duolingo: afunda ao tocar e faz um "toc". */
@Composable
fun BaruckButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = BaruckColors.Green,
    shadow: Color = BaruckColors.GreenDark,
    textColor: Color = Color.White,
    border: Color? = null,
    icon: Int? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 3.dp
    val offset by animateDpAsState(if (pressed && enabled) depth else 0.dp, tween(60), label = "press")
    val face = if (enabled) color else Color(0xFFE5E5E5)
    val base = if (enabled) shadow else Color(0xFFCECECE)
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.height(52.dp)) {
        Box(Modifier.matchParentSize().padding(top = depth).clip(shape).background(base))
        Box(
            Modifier.fillMaxWidth().height(48.dp).offset(y = offset).clip(shape).background(face)
                .then(if (border != null && enabled) Modifier.border(2.dp, border, shape) else Modifier)
                .clickable(interaction, null, enabled) { Sfx.play(Sfx.S.TAP); onClick() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Image(painterResource(icon), null, Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text.uppercase(), maxLines = 1,
                    color = if (enabled) textColor else Color(0xFFAFAFAF),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun BlueButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    BaruckButton(text, modifier, enabled, BaruckColors.Blue, BaruckColors.BlueDark, onClick = onClick)

@Composable
fun YellowButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) =
    BaruckButton(text, modifier, enabled, BaruckColors.Yellow, BaruckColors.YellowDark, BaruckColors.Brown, onClick = onClick)

@Composable
fun BaruckOutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = BaruckColors.Blue,
    onClick: () -> Unit
) = BaruckButton(text, modifier, true, Color.White, BaruckColors.Line, color, BaruckColors.Line, onClick = onClick)

/** Botão redondo pequeno (para + e −). */
@Composable
fun RoundButton(label: String, color: Color, shadow: Color, size: Dp = 46.dp, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val off by animateDpAsState(if (pressed) 3.dp else 0.dp, tween(50), label = "rb")
    Box(Modifier.size(size, size + 3.dp)) {
        Box(Modifier.size(size).offset(y = 3.dp).clip(RoundedCornerShape(8.dp)).background(shadow))
        Box(
            Modifier.size(size).offset(y = off).clip(RoundedCornerShape(8.dp)).background(color)
                .clickable(interaction, null) { Sfx.play(Sfx.S.TAP); onClick() },
            contentAlignment = Alignment.Center
        ) { Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
    }
}

/* ============================ Cartões ============================ */

/** Cartão branco com borda e "degrau" embaixo; encolhe um pouco ao tocar. */
@Composable
fun BaruckCard(
    modifier: Modifier = Modifier,
    borderColor: Color = BaruckColors.Line,
    background: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "card")
    Column(
        modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp)).background(borderColor)
            .padding(start = 1.5.dp, top = 1.5.dp, end = 1.5.dp, bottom = 3.dp)
            .clip(RoundedCornerShape(7.dp)).background(background)
            .then(
                if (onClick != null) Modifier.clickable(interaction, null) { Sfx.play(Sfx.S.TAP); onClick() }
                else Modifier
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun Pill(text: String, color: Color, bg: Color) {
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleMedium, color = BaruckColors.Ink)
}

/* ============================ Abas ============================ */

/** Abas estilo Duolingo: a escolhida fica azul clarinho com borda azul. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) BaruckColors.BlueSoft else Color.White, tween(200), label = "tabbg")
            Box(
                Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp)).background(bg)
                    .border(2.dp, if (on) BaruckColors.Blue else BaruckColors.Line, RoundedCornerShape(8.dp))
                    .clickable { if (!on) { Sfx.play(Sfx.S.TAP); onSelect(i) } },
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (on) BaruckColors.BlueDark else BaruckColors.InkSoft,
                    style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Chip selecionável (categorias, culturas, estufas). */
@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (selected) BaruckColors.BlueSoft else Color.White)
            .border(2.dp, if (selected) BaruckColors.Blue else BaruckColors.Line, RoundedCornerShape(6.dp))
            .clickable { Sfx.play(Sfx.S.TAP); onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) BaruckColors.BlueDark else BaruckColors.Ink,
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/* ============================ Campos ============================ */

@Composable
fun BaruckField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    prefix: String? = null,
    supporting: String? = null,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        isError = isError,
        prefix = prefix?.let { { Text(it, color = BaruckColors.InkSoft) } },
        supportingText = supporting?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BaruckColors.Blue,
            unfocusedBorderColor = BaruckColors.Line,
            focusedContainerColor = Color(0xFFF7F7F7),
            unfocusedContainerColor = Color(0xFFF7F7F7),
            focusedLabelColor = BaruckColors.BlueDark
        )
    )
}

/** Seletor de data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, date: Date?, onPick: (Date) -> Unit) {
    var open by remember { mutableStateOf(false) }
    BaruckCard(onClick = { open = true }) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
        Text("📅  " + (if (date != null) shortDate(date) else "Escolher data"),
            style = MaterialTheme.typography.titleMedium)
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toPickerMillis(date ?: Date()))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(fromPickerMillis(it)) }
                    open = false
                }) { Text("OK", color = BaruckColors.GreenDark) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

/** O DatePicker trabalha com meia-noite UTC; convertemos para o dia local (meio-dia). */
private fun toPickerMillis(d: Date): Long {
    val local = java.util.Calendar.getInstance().apply { time = d }
    return java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(java.util.Calendar.YEAR), local.get(java.util.Calendar.MONTH), local.get(java.util.Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun fromPickerMillis(ms: Long): Date {
    val u = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
    return java.util.Calendar.getInstance().apply {
        clear()
        set(u.get(java.util.Calendar.YEAR), u.get(java.util.Calendar.MONTH), u.get(java.util.Calendar.DAY_OF_MONTH), 12, 0)
    }.time
}

/** Contador com − e + e campo editável no meio. */
@Composable
fun Stepper(label: String, value: Int, color: Color, shadow: Color, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = color)
        RoundButton("−", color, shadow) { onChange((value - 1).coerceAtLeast(0)) }
        OutlinedTextField(
            value = if (value == 0) "" else value.toString(),
            onValueChange = { t -> onChange(t.filter { it.isDigit() }.take(5).toIntOrNull() ?: 0) },
            placeholder = { Text("0", Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.width(84.dp).padding(horizontal = 8.dp)
        )
        RoundButton("+", color, shadow) { onChange(value + 1) }
    }
}

/* ============================ Baruck e perfil ============================ */

/** Faz algo "flutuar" suavemente para cima e para baixo. */
@Composable
fun Modifier.bob(amount: Dp = 6.dp, millis: Int = 1300): Modifier {
    val t = rememberInfiniteTransition(label = "bob")
    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bobY")
    val px = with(LocalDensity.current) { amount.toPx() }
    return this.graphicsLayer { translationY = -y * px }
}

@Composable
fun Mascot(res: Int, size: Dp, modifier: Modifier = Modifier, bobbing: Boolean = true) {
    Image(painterResource(res), null, (if (bobbing) modifier.bob() else modifier).size(size))
}

@Composable
fun SpeechBubble(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color.White)
            .border(2.dp, BaruckColors.Line, RoundedCornerShape(10.dp)).padding(14.dp)
    ) { Text(text, style = MaterialTheme.typography.bodyLarge, color = BaruckColors.Ink) }
}

@Composable
fun MascotSays(res: Int, text: String, size: Dp = 110.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Mascot(res, size)
        Spacer(Modifier.width(8.dp))
        SpeechBubble(text, Modifier.weight(1f))
    }
}

@Composable
fun EmptyState(res: Int, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mascot(res, 150.dp)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft, textAlign = TextAlign.Center)
    }
}

@Composable
fun AvatarImage(key: String?, size: Dp, modifier: Modifier = Modifier, ring: Color? = null) {
    Image(
        painterResource(Avatars.res(key)), null,
        modifier.size(size).clip(RoundedCornerShape(14))
            .then(if (ring != null) Modifier.border(3.dp, ring, RoundedCornerShape(14)) else Modifier)
    )
}

/** Barra de XP amarela, animada. */
@Composable
fun XpBar(xp: Int, modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val p by animateFloatAsState(Xp.progress(xp), tween(900, easing = FastOutSlowInEasing), label = "xp")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(4.dp)).background(BaruckColors.Line)) {
        Box(
            Modifier.fillMaxWidth(p).fillMaxHeight().clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(BaruckColors.Yellow, BaruckColors.Orange)))
        ) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp).height(3.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
fun LevelBadge(level: Int, size: Dp = 44.dp) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_xp), null, Modifier.fillMaxSize())
        Text("$level", color = BaruckColors.Brown, fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.32f).sp, modifier = Modifier.padding(top = 3.dp))
    }
}

/** Cabeçalho com avatar, nome, nível e barra de XP. */
@Composable
fun ProfileHeader(p: UserProfile, onClick: (() -> Unit)? = null) {
    val level = Xp.level(p.xp)
    BaruckCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(p.avatar, 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Olá, ${p.name.substringBefore(' ')}!", style = MaterialTheme.typography.titleLarge)
                Text("Nível $level · ${Xp.title(level)}", style = MaterialTheme.typography.bodyMedium,
                    color = BaruckColors.InkSoft)
            }
            LevelBadge(level)
        }
        Spacer(Modifier.height(12.dp))
        XpBar(p.xp)
        Spacer(Modifier.height(6.dp))
        Text("${p.xp} XP · faltam ${Xp.toNext(p.xp)} XP para o nível ${level + 1}",
            style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
    }
}

/* ============================ Efeitos ============================ */

private class Particle(
    val vx: Float, val vy: Float, val rot: Float, val spin: Float,
    val w: Float, val h: Float, val color: Color
)

private val confettiColors = listOf(
    BaruckColors.Green, BaruckColors.Blue, BaruckColors.Yellow, BaruckColors.Orange,
    BaruckColors.Red, BaruckColors.Purple
)

/** Chuva de confete. Mude [trigger] (>0) para disparar de novo. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger <= 0) return
    val anim = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { anim.animateTo(1f, tween(1800, easing = LinearEasing)) }
    val parts = remember(trigger) {
        List(70) {
            Particle(
                vx = Random.nextFloat() * 1.2f - 0.6f, vy = -(Random.nextFloat() * 0.8f + 0.25f),
                rot = Random.nextFloat() * 360f, spin = Random.nextFloat() * 2f - 1f,
                w = Random.nextFloat() * 10f + 10f, h = Random.nextFloat() * 8f + 6f,
                color = confettiColors[Random.nextInt(confettiColors.size)]
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val t = anim.value
        if (t >= 1f) return@Canvas
        parts.forEach { p ->
            val x = size.width * (0.5f + p.vx * t)
            val y = size.height * (0.4f + p.vy * t + 1.3f * t * t)
            rotate(p.rot + 540f * t * p.spin, Offset(x, y)) {
                drawRect(p.color.copy(alpha = 1f - t * t), Offset(x - p.w / 2, y - p.h / 2), Size(p.w, p.h))
            }
        }
    }
}

/** "+20 XP" que desce do topo e some. */
@Composable
fun XpToast(amount: Int?, modifier: Modifier = Modifier, onDone: () -> Unit) {
    var last by remember { mutableIntStateOf(0) }
    if (amount != null) last = amount
    LaunchedEffect(amount) { if (amount != null) { delay(1700); onDone() } }
    AnimatedVisibility(
        visible = amount != null, modifier = modifier,
        enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(6.dp)).background(BaruckColors.Yellow)
                .border(2.dp, BaruckColors.YellowDark, RoundedCornerShape(6.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(painterResource(R.drawable.ic_xp), null, Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text("+$last XP", color = BaruckColors.Brown, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** Comemoração de nível novo. */
@Composable
fun LevelUpDialog(level: Int, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(24.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(RoundedCornerShape(28.dp)).background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Mascot(R.drawable.baruck_celebrate, 190.dp)
                Text("Nível $level!", style = MaterialTheme.typography.displaySmall, color = BaruckColors.Orange)
                Text(Xp.title(level), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text("Você está mandando muito bem na lavoura!", textAlign = TextAlign.Center,
                    color = BaruckColors.InkSoft, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                BaruckButton("Continuar", Modifier.fillMaxWidth(), onClick = onDismiss)
            }
            ConfettiBurst(level)
        }
    }
}

/** Folha de formulário em tela cheia (diálogo largo, rolável). */
@Composable
fun FormSheet(title: String, onDismiss: () -> Unit, footer: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color.White)
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onDismiss) { Text("Fechar", color = BaruckColors.InkSoft) }
            }
            Column(
                Modifier.weight(1f).verticalScrollSafe().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
            Box(Modifier.padding(16.dp)) { footer() }
        }
    }
}

@Composable
fun Modifier.verticalScrollSafe(): Modifier = this.verticalScroll(rememberScrollState())
