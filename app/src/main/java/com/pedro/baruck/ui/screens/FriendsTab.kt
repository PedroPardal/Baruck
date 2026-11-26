package com.pedro.baruck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedro.baruck.R
import com.pedro.baruck.data.UserProfile
import com.pedro.baruck.data.Xp
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@Composable
fun FriendsTab(vm: BaruckViewModel, nav: androidx.navigation.NavController) {
    val me by vm.me.collectAsState()
    val friends by vm.friends.collectAsState()
    val requests by vm.requests.collectAsState()
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<UserProfile?>(null) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<UserProfile?>(null) }

    val ranking = (friends + listOfNotNull(me)).distinctBy { it.uid }.sortedByDescending { it.xp }
    val friendIds = friends.map { it.uid }.toSet()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "add") {
            BaruckCard {
                SectionTitle("Adicionar amigo")
                Text("Procure pelo nome de usuário", style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
                Spacer(Modifier.height(8.dp))
                BaruckField(query, {
                    query = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '.' }.take(20)
                    searched = false; result = null
                }, "Nome de usuário", prefix = "@")
                Spacer(Modifier.height(10.dp))
                BlueButton(if (searching) "Procurando..." else "Procurar", Modifier.fillMaxWidth(),
                    enabled = query.length >= 3 && !searching) {
                    searching = true
                    vm.findUser(query) { result = it; searched = true; searching = false }
                }
                if (searched) {
                    Spacer(Modifier.height(12.dp))
                    val r = result
                    when {
                        r == null -> Text("Ninguém com @$query 🤔", color = BaruckColors.InkSoft)
                        r.uid == me?.uid -> Text("Esse é você! 😄", color = BaruckColors.InkSoft)
                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarImage(r.avatar, 52.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = MaterialTheme.typography.titleMedium)
                                Text("@${r.username} · Nível ${Xp.level(r.xp)}", style = MaterialTheme.typography.bodyMedium,
                                    color = BaruckColors.InkSoft)
                            }
                            if (r.uid in friendIds) Pill("Amigo", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                            else BaruckButton("Adicionar", Modifier.width(130.dp)) {
                                vm.sendFriendRequest(r); searched = false; result = null; query = ""
                            }
                        }
                    }
                }
            }
        }

        if (requests.isNotEmpty()) {
            item(key = "reqTitle") { SectionTitle("Pedidos de amizade") }
            items(requests, key = { "req_" + it.fromUid }) { req ->
                BaruckCard(Modifier.animateItem(), borderColor = BaruckColors.Blue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarImage(req.fromAvatar, 48.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(req.fromName, style = MaterialTheme.typography.titleMedium)
                            Text("@" + req.fromUsername, color = BaruckColors.InkSoft, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BaruckButton("Aceitar", Modifier.weight(1f)) { vm.accept(req) }
                        BaruckOutlineButton("Recusar", Modifier.weight(1f), color = BaruckColors.InkSoft) { vm.decline(req) }
                    }
                }
            }
        }

        item(key = "rankTitle") { SectionTitle("🏆 Ranking dos amigos", Modifier.padding(top = 4.dp)) }
        if (friends.isEmpty()) {
            item(key = "noFriends") {
                EmptyState(R.drawable.baruck_wave, "Chame seus parceiros!",
                    "Adicione amigos pelo @usuário para dividir lavouras e disputar o ranking de XP.")
            }
        }
        itemsIndexed(ranking, key = { _, p -> "rank_" + p.uid }) { i, p ->
            val isMe = p.uid == me?.uid
            BaruckCard(
                Modifier.animateItem(),
                borderColor = if (isMe) BaruckColors.Green else BaruckColors.Line,
                onClick = if (!isMe) ({ removing = p }) else null
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val medal = when (i) { 0 -> BaruckColors.Yellow; 1 -> Color(0xFFC0C0C0); 2 -> Color(0xFFD7965B); else -> BaruckColors.Line }
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(medal), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", fontWeight = FontWeight.Bold, color = BaruckColors.Brown)
                    }
                    Spacer(Modifier.width(10.dp))
                    AvatarImage(p.avatar, 48.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (isMe) "${p.name} (você)" else p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text("@${p.username} · ${Xp.title(Xp.level(p.xp))}", style = MaterialTheme.typography.bodyMedium,
                            color = BaruckColors.InkSoft, maxLines = 1)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LevelBadge(Xp.level(p.xp), 36.dp)
                        Text("${p.xp} XP", style = MaterialTheme.typography.labelSmall, color = BaruckColors.Orange)
                    }
                }
            }
        }
    }

    var confirmRemove by remember { mutableStateOf<UserProfile?>(null) }
    removing?.let { f ->
        AlertDialog(
            onDismissRequest = { removing = null },
            containerColor = Color.White,
            icon = { AvatarImage(f.avatar, 64.dp) },
            title = { Text(f.name) },
            text = { Text("@${f.username} · ${Xp.title(Xp.level(f.xp))}" + (if (f.bio.isNotBlank()) "\n\n${f.bio}" else "")) },
            confirmButton = {
                TextButton(onClick = { removing = null; nav.go("chat/dm/${f.uid}") }) {
                    Text("\uD83D\uDCAC Conversar", color = BaruckColors.BlueDark)
                }
            },
            dismissButton = {
                TextButton(onClick = { removing = null; confirmRemove = f }) { Text("Remover amigo", color = BaruckColors.RedDark) }
            }
        )
    }
    confirmRemove?.let { f ->
        ConfirmDialog("Remover ${f.name}?", "Vocês deixam de ser amigos no Baruck.", "Remover",
            onConfirm = { vm.removeFriend(f.uid) }, onDismiss = { confirmRemove = null })
    }
}
