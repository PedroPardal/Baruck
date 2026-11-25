package com.pedro.baruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.Farm
import com.pedro.baruck.data.UserProfile
import com.pedro.baruck.data.Xp
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import java.util.Date

/* ------------------------------ Equipe ------------------------------ */

@Composable
fun MembersScreen(vm: BaruckViewModel, nav: NavController, farmId: String) {
    val farm by remember(farmId) { vm.farm(farmId) }.collectAsState(null)
    val me by vm.me.collectAsState()
    val friends by vm.friends.collectAsState()
    val memberIds = farm?.members?.distinct() ?: emptyList()
    val members by remember(memberIds) { vm.profiles(memberIds) }.collectAsState(emptyList())
    val isAdmin = farm?.ownerUid == me?.uid
    val available = friends.filter { it.uid !in memberIds }
    var removing by remember { mutableStateOf<UserProfile?>(null) }
    var leaving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.White, topBar = { BaruckTopBar("Equipe", onBack = { nav.back() }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "intro") {
                MascotSays(R.drawable.baruck_sit,
                    if (isAdmin) "Chame seus amigos para cuidar da lavoura com você!"
                    else "Essa é a equipe da ${farm?.name ?: "lavoura"}.", 90.dp)
            }
            item(key = "mTitle") { SectionTitle("Quem participa") }
            items(members.sortedByDescending { it.uid == farm?.ownerUid }, key = { "member_" + it.uid }) { p ->
                PersonRow(p, Modifier.animateItem(), badge = if (p.uid == farm?.ownerUid) "Administrador" else null) {
                    if (isAdmin && p.uid != me?.uid)
                        IconButton(onClick = { removing = p }) {
                            Icon(Icons.Outlined.PersonRemove, "Remover", tint = BaruckColors.InkSoft)
                        }
                }
            }
            if (isAdmin) {
                item(key = "aTitle") { SectionTitle("Adicionar amigos", Modifier.padding(top = 8.dp)) }
                if (available.isEmpty()) item(key = "noAvail") {
                    Text(
                        if (friends.isEmpty()) "Adicione amigos pelo @usuário na aba Amigos e depois volte aqui."
                        else "Todos os seus amigos já estão nesta lavoura 🎉",
                        style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft
                    )
                }
                items(available, key = { "avail_" + it.uid }) { f ->
                    PersonRow(f, Modifier.animateItem()) {
                        BaruckButton("Adicionar", Modifier.width(128.dp)) { farm?.let { vm.addMember(it, f) } }
                    }
                }
                item(key = "del") {
                    Spacer(Modifier.height(16.dp))
                    BaruckOutlineButton("Apagar lavoura", Modifier.fillMaxWidth(), color = BaruckColors.RedDark) { deleting = true }
                }
            } else {
                item(key = "leave") {
                    Spacer(Modifier.height(16.dp))
                    BaruckOutlineButton("Sair da lavoura", Modifier.fillMaxWidth(), color = BaruckColors.RedDark) { leaving = true }
                }
            }
        }
    }

    removing?.let { p ->
        ConfirmDialog("Remover ${p.name}?", "Ele deixa de ver esta lavoura.", "Remover",
            onConfirm = { vm.removeMember(farmId, p.uid) }, onDismiss = { removing = null })
    }
    if (leaving) ConfirmDialog("Sair da lavoura?", "Você deixa de ver as estufas e avisos dela.", "Sair",
        onConfirm = { vm.leaveFarm(farmId); nav.backToMain() }, onDismiss = { leaving = false })
    if (deleting) ConfirmDialog("Apagar a lavoura?", "Ela some para todos os membros. Não dá para desfazer.", "Apagar",
        onConfirm = { vm.deleteFarm(farmId); nav.backToMain() }, onDismiss = { deleting = false })
}

@Composable
fun PersonRow(p: UserProfile, modifier: Modifier = Modifier, badge: String? = null, trailing: @Composable () -> Unit = {}) {
    BaruckCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(p.avatar, 50.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("@${p.username} · Nível ${Xp.level(p.xp)}", style = MaterialTheme.typography.bodyMedium,
                    color = BaruckColors.InkSoft, maxLines = 1)
                if (badge != null) {
                    Spacer(Modifier.height(4.dp))
                    Pill(badge, BaruckColors.GreenDark, BaruckColors.GreenSoft)
                }
            }
            trailing()
        }
    }
}

/* ------------------------------ Avisos da lavoura ------------------------------ */

private val templates = listOf(
    "🧺 Colheita amanhã cedo",
    "💧 Irrigar as estufas hoje",
    "🌧️ Chuva forte: fechar as estufas",
    "⚠️ Pulverização hoje, não entrar",
    "🗣️ Reunião na estufa",
    "🚚 Entrega no Ceasa"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticesScreen(vm: BaruckViewModel, nav: NavController, farmId: String) {
    val farm by remember(farmId) { vm.farm(farmId) }.collectAsState(null)
    val reminders by remember(farmId) { vm.reminders(farmId) }.collectAsState(emptyList())
    val me by vm.me.collectAsState()
    val isAdmin = farm?.ownerUid == me?.uid
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var remTitle by remember { mutableStateOf("") }
    var remDate by remember { mutableStateOf(Date()) }

    Scaffold(containerColor = Color.White, topBar = { BaruckTopBar("Avisos da lavoura", onBack = { nav.back() }) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScrollSafe().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAdmin) {
                BaruckCard(borderColor = BaruckColors.Blue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            androidx.compose.ui.res.painterResource(R.drawable.ic_bell), null, Modifier.size(40.dp).bob(3.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar aviso para a equipe", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        templates.forEach { t -> ChoiceChip(t, title == t) { title = t } }
                    }
                    Spacer(Modifier.height(10.dp))
                    BaruckField(title, { title = it.take(60) }, "Título do aviso")
                    Spacer(Modifier.height(8.dp))
                    BaruckField(body, { body = it.take(300) }, "Mensagem (opcional)", singleLine = false)
                    Spacer(Modifier.height(10.dp))
                    BlueButton("Enviar aviso", Modifier.fillMaxWidth(), enabled = title.isNotBlank() && farm != null) {
                        farm?.let { f -> vm.sendNotice(f, title, body) }
                        title = ""; body = ""
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Todos os membros recebem no celular e na aba Avisos.",
                        style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                }
            } else {
                MascotSays(R.drawable.baruck_sit,
                    "Só o administrador manda avisos. Você recebe todos aqui e no celular!", 90.dp)
            }

            SectionTitle("⏰ Lembretes", Modifier.padding(top = 8.dp))
            BaruckCard {
                BaruckField(remTitle, { remTitle = it.take(60) }, "Ex.: Adubar estufa 2")
                Spacer(Modifier.height(8.dp))
                DatePickerField("Avisar no dia", remDate) { remDate = it }
                Spacer(Modifier.height(8.dp))
                BaruckButton("Criar lembrete", Modifier.fillMaxWidth(), enabled = remTitle.isNotBlank()) {
                    vm.addReminder(farmId, remTitle, remDate); remTitle = ""
                }
            }
            if (reminders.isEmpty())
                Text("Nenhum lembrete ainda.", color = BaruckColors.InkSoft, style = MaterialTheme.typography.bodyMedium)
            reminders.forEach { r ->
                val overdue = !r.done && (r.dueAt?.before(Date()) == true)
                BaruckCard(borderColor = if (overdue) BaruckColors.Red else BaruckColors.Line) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(r.done, { vm.setReminderDone(farmId, r.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = BaruckColors.Green))
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.titleMedium,
                                textDecoration = if (r.done) TextDecoration.LineThrough else null)
                            Text(shortDate(r.dueAt), style = MaterialTheme.typography.labelSmall,
                                color = if (overdue) BaruckColors.RedDark else BaruckColors.InkSoft)
                        }
                        IconButton(onClick = { vm.deleteReminder(farmId, r.id) }) {
                            Icon(Icons.Outlined.Delete, "Apagar", tint = BaruckColors.InkSoft)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/* ------------------------------ Editar perfil ------------------------------ */

@Composable
fun EditProfileScreen(vm: BaruckViewModel, nav: NavController) {
    val me by vm.me.collectAsState()
    val p = me ?: return
    var avatar by remember { mutableStateOf(p.avatar) }
    var name by remember { mutableStateOf(p.name) }
    var bio by remember { mutableStateOf(p.bio) }

    Scaffold(
        containerColor = Color.White,
        topBar = { BaruckTopBar("Editar perfil", onBack = { nav.back() }) },
        bottomBar = {
            BottomAction {
                BaruckButton("Salvar", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
                    vm.updateProfile(name, bio, avatar); nav.back()
                }
            }
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScrollSafe().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarImage(avatar, 110.dp, Modifier.bob(4.dp), ring = BaruckColors.Green)
            Text("@" + p.username, color = BaruckColors.InkSoft)
            BaruckField(name, { name = it.take(40) }, "Nome")
            BaruckField(bio, { bio = it.take(120) }, "Bio", singleLine = false, supporting = "${bio.length}/120")
            SectionTitle("Bichinho do perfil", Modifier.fillMaxWidth())
            AvatarPicker(avatar) { avatar = it }
        }
    }
}
