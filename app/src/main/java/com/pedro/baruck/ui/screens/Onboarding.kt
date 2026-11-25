package com.pedro.baruck.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedro.baruck.R
import com.pedro.baruck.data.Xp
import com.pedro.baruck.ui.Avatars
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.NameStatus
import com.pedro.baruck.ui.Sfx
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import kotlinx.coroutines.delay

/**
 * Criação de perfil estilo Duolingo: bichinho → nome/@usuário/bio → salvar.
 * Se [signedIn], o login já foi feito e o último passo só salva.
 */
@Composable
fun OnboardingScreen(vm: BaruckViewModel, signedIn: Boolean) {
    val first = if (signedIn) 1 else 0
    var step by rememberSaveable { mutableIntStateOf(if (signedIn && vm.authError != null) 2 else first) }
    val context = LocalContext.current

    fun goTo(s: Int) { Sfx.play(Sfx.S.WHOOSH); step = s }
    BackHandler(enabled = step > first) { goTo(step - 1) }

    Column(Modifier.fillMaxSize().background(Color.White).systemBarsPadding().imePadding()) {
        if (step > 0) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (step > first) goTo(step - 1) }, enabled = step > first) {
                    Icon(Icons.Default.Close, "Voltar", tint = if (step > first) BaruckColors.InkSoft else Color.Transparent)
                }
                val p by animateFloatAsState(step / 3f, tween(500), label = "onb")
                Box(Modifier.weight(1f).height(16.dp).clip(CircleShape).background(BaruckColors.Line)) {
                    Box(Modifier.fillMaxWidth(p).fillMaxHeight().clip(CircleShape).background(BaruckColors.Green))
                }
                Spacer(Modifier.width(16.dp))
            }
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState)
                    (slideInHorizontally(tween(320)) { it } + fadeIn()) togetherWith (slideOutHorizontally(tween(320)) { -it / 2 } + fadeOut())
                else
                    (slideInHorizontally(tween(320)) { -it } + fadeIn()) togetherWith (slideOutHorizontally(tween(320)) { it / 2 } + fadeOut())
            },
            modifier = Modifier.weight(1f), label = "steps"
        ) { s ->
            when (s) {
                0 -> WelcomeStep()
                1 -> AvatarStep(vm)
                2 -> InfoStep(vm)
                else -> SaveStep(vm)
            }
        }

        vm.authError?.let {
            Text(it, color = BaruckColors.RedDark, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vm.busy) {
                Box(Modifier.fillMaxWidth().height(54.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BaruckColors.Green)
                }
            } else when (step) {
                0 -> {
                    BaruckButton("Começar", Modifier.fillMaxWidth()) { vm.authError = null; goTo(1) }
                    BaruckOutlineButton("Já tenho uma conta", Modifier.fillMaxWidth()) { vm.signIn(context, createAfter = false) }
                }
                1 -> BaruckButton("Continuar", Modifier.fillMaxWidth()) { goTo(2) }
                2 -> BaruckButton(
                    "Continuar", Modifier.fillMaxWidth(),
                    enabled = vm.draftName.isNotBlank() && vm.nameStatus == NameStatus.FREE
                ) { goTo(3) }
                else -> if (signedIn)
                    BaruckButton("Salvar perfil", Modifier.fillMaxWidth()) { vm.saveDraftProfile() }
                else
                    BlueButton("Salvar e entrar com Google", Modifier.fillMaxWidth()) { vm.signIn(context, createAfter = true) }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        Modifier.fillMaxSize().verticalScrollSafe().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(24.dp))
        Mascot(R.drawable.baruck_wave, 230.dp)
        Text("Baruck", fontSize = 44.sp, style = MaterialTheme.typography.displaySmall, color = BaruckColors.Green)
        Text("Suas estufas, colheitas e custos\nde um jeito divertido.",
            style = MaterialTheme.typography.bodyLarge, color = BaruckColors.InkSoft, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        listOf(
            "🏠" to "Estufa e cultura num só cadastro",
            "🧺" to "Colheita em caixas grandes, médias e miúdas",
            "💰" to "Custos, receita e lucro",
            "👫" to "Lavoura em equipe, com avisos e níveis"
        ).forEach { (e, t) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(e, fontSize = 22.sp); Spacer(Modifier.width(10.dp))
                Text(t, style = MaterialTheme.typography.bodyMedium, color = BaruckColors.Ink)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarPicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        Avatars.all.forEach { a ->
            val on = a.key == selected
            val scale by animateFloatAsState(if (on) 1.08f else 1f, spring(dampingRatio = 0.4f), label = "av")
            Column(
                Modifier.width(100.dp).clip(MaterialTheme.shapes.medium)
                    .clickable { Sfx.play(Sfx.S.TAP); onSelect(a.key) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarImage(a.key, 80.dp, Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                    ring = if (on) BaruckColors.Green else null)
                Text(a.name, style = MaterialTheme.typography.labelSmall,
                    color = if (on) BaruckColors.GreenDark else BaruckColors.InkSoft)
            }
        }
    }
}

@Composable
private fun AvatarStep(vm: BaruckViewModel) {
    Column(Modifier.fillMaxSize().verticalScrollSafe().padding(horizontal = 16.dp)) {
        MascotSays(R.drawable.baruck_sit, "Oi! Eu sou o Baruck. Escolha o seu bichinho de perfil!", 100.dp)
        Spacer(Modifier.height(16.dp))
        AvatarPicker(vm.draftAvatar) { vm.draftAvatar = it }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoStep(vm: BaruckViewModel) {
    LaunchedEffect(vm.draftUsername) {
        delay(450)
        vm.checkUsername(vm.draftUsername)
    }
    Column(
        Modifier.fillMaxSize().verticalScrollSafe().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(vm.draftAvatar, 72.dp, Modifier.bob(4.dp))
            Spacer(Modifier.width(12.dp))
            Text("Agora conta um pouco sobre você", style = MaterialTheme.typography.titleLarge)
        }
        BaruckField(vm.draftName, { vm.draftName = it.take(40) }, "Seu nome")
        val (msg, err) = when (vm.nameStatus) {
            NameStatus.CHECKING -> "Verificando..." to false
            NameStatus.FREE -> "✓ Disponível" to false
            NameStatus.TAKEN -> "Esse já tem dono, tente outro" to true
            NameStatus.INVALID -> "Use 3 a 20 letras minúsculas, números, _ ou ." to true
            NameStatus.IDLE -> "É assim que seus amigos vão te achar" to false
        }
        BaruckField(
            vm.draftUsername,
            { vm.draftUsername = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20) },
            "Nome de usuário", prefix = "@", supporting = msg, isError = err
        )
        BaruckField(vm.draftBio, { vm.draftBio = it.take(120) }, "Bio (opcional)", singleLine = false,
            supporting = "${vm.draftBio.length}/120 · ex.: Produtor de tomate em Vassouras 🍅")
    }
}

@Composable
private fun SaveStep(vm: BaruckViewModel) {
    Column(
        Modifier.fillMaxSize().verticalScrollSafe().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MascotSays(R.drawable.baruck_celebrate, "Ficou lindo! Agora é só salvar seu perfil.", 110.dp)
        Spacer(Modifier.height(16.dp))
        BaruckCard {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarImage(vm.draftAvatar, 104.dp, ring = BaruckColors.Green)
                Spacer(Modifier.height(8.dp))
                Text(vm.draftName, style = MaterialTheme.typography.titleLarge)
                Text("@" + vm.draftUsername, color = BaruckColors.InkSoft)
                if (vm.draftBio.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(vm.draftBio, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadge(1, 34.dp); Spacer(Modifier.width(6.dp))
                    Text("Nível 1 · ${Xp.title(1)}", style = MaterialTheme.typography.labelLarge, color = BaruckColors.Orange)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Usamos sua conta Google só para guardar seu perfil e suas lavouras com segurança.",
            style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft, textAlign = TextAlign.Center)
    }
}

/** Tela de "carregando". */
@Composable
fun LoadingScreen(text: String = "Carregando...") {
    Column(
        Modifier.fillMaxSize().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Mascot(R.drawable.baruck_wave, 170.dp)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = BaruckColors.InkSoft)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(color = BaruckColors.Green)
    }
}
