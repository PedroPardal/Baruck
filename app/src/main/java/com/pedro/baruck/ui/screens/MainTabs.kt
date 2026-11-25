package com.pedro.baruck.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.Farm
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import kotlinx.coroutines.delay

/** Tela principal com as abas de baixo: Lavouras, Amigos, Avisos e Perfil. */
@Composable
fun MainTabs(vm: BaruckViewModel, nav: NavController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val me by vm.me.collectAsState()
    val inbox by vm.inbox.collectAsState()
    val requests by vm.requests.collectAsState()
    val unread = inbox.count { !it.read }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Column(Modifier.background(Color.White)) {
                HorizontalDivider(thickness = 2.dp, color = BaruckColors.Line)
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    val items = listOf(
                        Triple("Lavouras", R.drawable.ic_tab_farm, 0),
                        Triple("Amigos", R.drawable.ic_tab_friends, requests.size),
                        Triple("Avisos", R.drawable.ic_bell, unread),
                        Triple("Perfil", 0, 0)
                    )
                    items.forEachIndexed { i, (label, icon, badge) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { if (tab != i) { Sfx.play(Sfx.S.TAP); tab = i } },
                            icon = {
                                BadgedBox(badge = {
                                    if (badge > 0) Badge(containerColor = BaruckColors.Red) { Text("$badge") }
                                }) {
                                    if (icon != 0) Image(painterResource(icon), label, Modifier.size(30.dp))
                                    else AvatarImage(me?.avatar, 30.dp)
                                }
                            },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = BaruckColors.BlueSoft,
                                selectedTextColor = BaruckColors.BlueDark,
                                unselectedTextColor = BaruckColors.InkSoft
                            )
                        )
                    }
                }
            }
        }
    ) { pad ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 14 }) togetherWith fadeOut(tween(120)) },
            modifier = Modifier.padding(pad), label = "tabs"
        ) { t ->
            when (t) {
                0 -> FarmsTab(vm, nav)
                1 -> FriendsTab(vm)
                2 -> InboxTab(vm, nav)
                else -> ProfileTab(vm, nav)
            }
        }
    }
}

/* ---------------------------- Lavouras ---------------------------- */

@Composable
private fun FarmsTab(vm: BaruckViewModel, nav: NavController) {
    val me by vm.me.collectAsState()
    val farms by vm.farms.collectAsState()
    var showNew by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") { me?.let { ProfileHeader(it) } }
        item(key = "title") { SectionTitle("Suas lavouras", Modifier.padding(top = 8.dp)) }
        if (farms.isEmpty()) {
            item(key = "empty") {
                EmptyState(R.drawable.baruck_sit, "Nenhuma lavoura ainda",
                    "Crie sua primeira lavoura. Depois é só cadastrar as estufas com a cultura de cada uma.")
            }
        }
        items(farms, key = { "farm_" + it.id }) { f ->
            FarmCard(f, isOwner = f.ownerUid == me?.uid, Modifier.animateItem()) { nav.go("farm/${f.id}") }
        }
        item(key = "new") {
            BaruckButton("Nova lavoura", Modifier.fillMaxWidth(), icon = R.drawable.ic_tab_farm) { showNew = true }
        }
    }
    if (showNew) NewFarmSheet(onDismiss = { showNew = false }) { n, l -> vm.createFarm(n, l); showNew = false }
}

@Composable
private fun FarmCard(f: Farm, isOwner: Boolean, modifier: Modifier, onClick: () -> Unit) {
    BaruckCard(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)).background(BaruckColors.RedSoft),
                contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.ic_tab_farm), null, Modifier.size(40.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.titleMedium)
                if (f.location.isNotBlank())
                    Text("📍 " + f.location, style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isOwner) Pill("Administrador", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                    else Pill("Membro", BaruckColors.BlueDark, BaruckColors.BlueSoft)
                    Pill("👥 ${f.members.distinct().size}", BaruckColors.InkSoft, Color(0xFFF1F1F1))
                }
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = BaruckColors.Line)
        }
    }
}

/* ---------------------------- Avisos ---------------------------- */

@Composable
private fun InboxTab(vm: BaruckViewModel, nav: NavController) {
    val inbox by vm.inbox.collectAsState()
    LaunchedEffect(inbox.map { it.id to it.read }) {
        delay(1500)
        inbox.filter { !it.read }.forEach { vm.markRead(it.id) }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_bell), null, Modifier.size(40.dp))
                Spacer(Modifier.width(8.dp))
                Text("Avisos", style = MaterialTheme.typography.headlineMedium)
            }
        }
        if (inbox.isEmpty()) {
            item(key = "empty") {
                EmptyState(R.drawable.baruck_sleep, "Nenhum aviso por aqui",
                    "Quando o administrador mandar um aviso, ou alguém te chamar para uma lavoura, aparece aqui.")
            }
        }
        items(inbox, key = { "inbox_" + it.id }) { item ->
            BaruckCard(
                Modifier.animateItem(),
                borderColor = if (item.read) BaruckColors.Line else BaruckColors.Blue,
                onClick = if (item.farmId.isNotBlank()) ({ nav.go("farm/${item.farmId}") }) else null
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    if (item.fromAvatar.isNotBlank()) AvatarImage(item.fromAvatar, 46.dp)
                    else Image(painterResource(R.drawable.ic_bell), null, Modifier.size(46.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        if (item.body.isNotBlank())
                            Text(item.body, style = MaterialTheme.typography.bodyMedium, color = BaruckColors.Ink)
                        Text(
                            listOf(item.fromName, timeAgo(item.createdAt)).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft
                        )
                    }
                    if (!item.read) Box(Modifier.size(10.dp).clip(CircleShape).background(BaruckColors.Blue))
                }
            }
        }
    }
}

/* ---------------------------- Perfil ---------------------------- */

@Composable
private fun ProfileTab(vm: BaruckViewModel, nav: NavController) {
    val me by vm.me.collectAsState()
    val farms by vm.farms.collectAsState()
    val friends by vm.friends.collectAsState()
    val context = LocalContext.current
    var sound by remember { mutableStateOf(Sfx.enabled) }
    var notifOk by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    var confirmOut by remember { mutableStateOf(false) }
    val p = me ?: return

    Column(
        Modifier.fillMaxSize().verticalScrollSafe().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AvatarImage(p.avatar, 120.dp, Modifier.bob(5.dp), ring = BaruckColors.Green)
        Text(p.name, style = MaterialTheme.typography.headlineMedium)
        Text("@" + p.username, color = BaruckColors.InkSoft)
        if (p.bio.isNotBlank()) Text(p.bio, style = MaterialTheme.typography.bodyMedium)
        ProfileHeader(p)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox("🏡", "${farms.size}", "Lavouras", Modifier.weight(1f))
            StatBox("👫", "${friends.size}", "Amigos", Modifier.weight(1f))
        }
        BaruckCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊  Sons do app", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = sound,
                    onCheckedChange = { sound = it; Sfx.enabled = it; Sfx.play(Sfx.S.TAP) },
                    colors = SwitchDefaults.colors(checkedTrackColor = BaruckColors.Green)
                )
            }
            if (!notifOk && Build.VERSION.SDK_INT >= 33) {
                Spacer(Modifier.height(8.dp))
                BlueButton("Ativar notificações", Modifier.fillMaxWidth()) {
                    askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        BaruckOutlineButton("Editar perfil", Modifier.fillMaxWidth()) { nav.go("editProfile") }
        BaruckOutlineButton("Sair da conta", Modifier.fillMaxWidth(), color = BaruckColors.RedDark) { confirmOut = true }
        Spacer(Modifier.height(16.dp))
    }
    if (confirmOut) ConfirmDialog("Sair da conta?", "Seus dados continuam salvos. É só entrar de novo com o Google.",
        "Sair", onConfirm = { vm.signOut() }, onDismiss = { confirmOut = false })
}

@Composable
fun StatBox(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    BaruckCard(modifier) {
        Text(emoji + "  " + value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
    }
}
