package com.pedro.baruck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.ChatMessage
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/* ------------------------------ Lista de conversas ------------------------------ */

@Composable
fun ChatsTab(vm: BaruckViewModel, nav: NavController) {
    val me by vm.me.collectAsState()
    val farms by vm.farms.collectAsState()
    val threads by vm.threads.collectAsState()
    val friends by vm.friends.collectAsState()
    val myUid = me?.uid ?: ""

    val groups = farms.sortedByDescending { it.lastMsgAt }
    val dms = friends.map { f -> f to threads.firstOrNull { it.other(myUid) == f.uid } }
        .sortedByDescending { it.second?.lastAt ?: 0L }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_chat), null, Modifier.size(42.dp).bob(3.dp))
                Spacer(Modifier.width(8.dp))
                Text("Conversas", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item(key = "gTitle") { SectionTitle("Grupos das lavouras") }
        if (groups.isEmpty()) item(key = "gEmpty") {
            Text("Crie ou entre numa lavoura para conversar com a equipe.",
                style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
        }
        items(groups, key = { "gchat_" + it.id }) { f ->
            ChatRow(
                modifier = Modifier.animateItem(),
                icon = { GroupIcon() },
                title = f.name,
                preview = if (f.lastMsgText.isBlank()) "Nenhuma mensagem ainda"
                else (if (f.lastMsgFrom == myUid) "Você" else f.lastMsgName.substringBefore(' ')) + ": " + f.lastMsgText,
                time = chatTime(f.lastMsgAt),
                unread = f.unreadFor(myUid)
            ) { nav.go("chat/farm/${f.id}") }
        }
        item(key = "dTitle") { SectionTitle("Amigos", Modifier.padding(top = 8.dp)) }
        if (dms.isEmpty()) item(key = "dEmpty") {
            EmptyState(R.drawable.baruck_wave, "Ninguém para conversar ainda",
                "Adicione amigos pelo @usuário na aba Amigos e mande um oi!")
        }
        items(dms, key = { "dm_" + it.first.uid }) { (friend, thread) ->
            ChatRow(
                modifier = Modifier.animateItem(),
                icon = { AvatarImage(friend.avatar, 52.dp) },
                title = friend.name,
                preview = when {
                    thread == null || thread.lastText.isBlank() -> "Toque para conversar"
                    thread.lastFrom == myUid -> "Você: " + thread.lastText
                    else -> thread.lastText
                },
                time = chatTime(thread?.lastAt ?: 0L),
                unread = thread?.unreadFor(myUid) == true
            ) { nav.go("chat/dm/${friend.uid}") }
        }
    }
}

@Composable
private fun GroupIcon() {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(BaruckColors.RedSoft),
        contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_tab_farm), null, Modifier.size(34.dp))
    }
}

@Composable
private fun ChatRow(
    modifier: Modifier, icon: @Composable () -> Unit, title: String, preview: String,
    time: String, unread: Boolean, onClick: () -> Unit
) {
    BaruckCard(modifier, borderColor = if (unread) BaruckColors.Green else BaruckColors.Line, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(preview, style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                    color = if (unread) BaruckColors.Ink else BaruckColors.InkSoft,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(time, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                if (unread) {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.size(12.dp).clip(CircleShape).background(BaruckColors.Green))
                }
            }
        }
    }
}

/* ------------------------------ Conversa ------------------------------ */

private sealed interface ChatItem {
    data class Msg(val m: ChatMessage, val showHeader: Boolean) : ChatItem
    data class Day(val label: String, val key: String) : ChatItem
}

/** kind = "farm" (grupo da lavoura) ou "dm" (amigo). id = farmId ou uid do amigo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: BaruckViewModel, nav: NavController, kind: String, id: String) {
    val isFarm = kind == "farm"
    val me by vm.me.collectAsState()
    val myUid = me?.uid ?: ""
    val farm by remember(id) { if (isFarm) vm.farm(id) else flowOf(null) }.collectAsState(null)
    val other by remember(id) {
        if (isFarm) flowOf(null) else vm.profiles(listOf(id)).map { it.firstOrNull() }
    }.collectAsState(null)
    val messages by remember(id) {
        if (isFarm) vm.farmMessages(id) else vm.dmMessages(vm.dmId(id))
    }.collectAsState(emptyList())

    var lastNewest by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(messages.firstOrNull()?.id) {
        val newest = messages.firstOrNull()
        if (newest != null && lastNewest != null && newest.id != lastNewest && newest.fromUid != myUid)
            Sfx.play(Sfx.S.MESSAGE)
        lastNewest = newest?.id
        if (isFarm) vm.markFarmRead(id) else vm.markDmRead(id)
    }

    var text by remember { mutableStateOf("") }
    var showStickers by remember { mutableStateOf(false) }
    fun send(t: String, sticker: String = "") {
        Sfx.play(Sfx.S.TAP)
        if (isFarm) vm.sendFarmMessage(id, t, sticker) else vm.sendDm(id, t, sticker)
    }

    // monta a lista (mais nova embaixo) com separadores de dia
    val rows = remember(messages) {
        val out = mutableListOf<ChatItem>()
        messages.forEachIndexed { i, m ->
            val older = messages.getOrNull(i + 1)
            val newDay = older == null || !sameDay(java.util.Date(older.at), java.util.Date(m.at))
            out += ChatItem.Msg(m, showHeader = newDay || older?.fromUid != m.fromUid)
            if (newDay) out += ChatItem.Day(dayLabel(m.at), "day_" + m.id)
        }
        out
    }

    Scaffold(
        containerColor = Color(0xFFF7F7F7),
        topBar = {
            Column(Modifier.background(Color.White)) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { nav.back() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = BaruckColors.InkSoft)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isFarm) GroupIcon() else AvatarImage(other?.avatar, 42.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(if (isFarm) farm?.name ?: "Grupo" else other?.name ?: "Conversa",
                                    style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(
                                    if (isFarm) "${farm?.members?.distinct()?.size ?: 0} na equipe"
                                    else other?.username?.let { "@$it" } ?: "",
                                    style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(thickness = 2.dp, color = BaruckColors.Line)
            }
        },
        bottomBar = {
            Column(Modifier.background(Color.White).navigationBarsPadding().imePadding()) {
                HorizontalDivider(thickness = 2.dp, color = BaruckColors.Line)
                AnimatedVisibility(showStickers, enter = expandVertically(), exit = shrinkVertically()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(Avatars.stickers, key = { "st_" + it.first }) { (key, res) ->
                            Image(painterResource(res), null,
                                Modifier.size(84.dp).clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF7F7F7))
                                    .clickable { send("", key); showStickers = false }
                                    .padding(4.dp))
                        }
                    }
                }
                Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { Sfx.play(Sfx.S.TAP); showStickers = !showStickers }) {
                        Image(painterResource(R.drawable.avatar_baruck), "Figurinhas", Modifier.size(36.dp))
                    }
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.take(1000) },
                        placeholder = { Text("Mensagem") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BaruckColors.Blue, unfocusedBorderColor = BaruckColors.Line,
                            focusedContainerColor = Color(0xFFF7F7F7), unfocusedContainerColor = Color(0xFFF7F7F7)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    RoundButton("➤", if (text.isBlank()) BaruckColors.Line else BaruckColors.Green,
                        if (text.isBlank()) Color(0xFFCECECE) else BaruckColors.GreenDark, 48.dp) {
                        if (text.isNotBlank()) { send(text); text = "" }
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            reverseLayout = true,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (messages.isEmpty()) item(key = "empty") {
                EmptyState(R.drawable.baruck_wave, "Diga oi!", "Mande a primeira mensagem ou uma figurinha do Baruck.")
            }
            items(rows, key = { r -> when (r) { is ChatItem.Msg -> "msg_" + r.m.id; is ChatItem.Day -> r.key } }) { r ->
                when (r) {
                    is ChatItem.Day -> Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(r.label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft,
                            modifier = Modifier.clip(CircleShape).background(Color.White)
                                .border(1.dp, BaruckColors.Line, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp))
                    }
                    is ChatItem.Msg -> Bubble(r.m, mine = r.m.fromUid == myUid, showHeader = r.showHeader,
                        group = isFarm, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage, mine: Boolean, showHeader: Boolean, group: Boolean, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = if (showHeader) 6.dp else 0.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!mine && group) {
            if (showHeader) AvatarImage(m.fromAvatar, 34.dp) else Spacer(Modifier.width(34.dp))
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 290.dp)) {
            if (!mine && group && showHeader)
                Text(m.fromName.substringBefore(' '), style = MaterialTheme.typography.labelSmall,
                    color = BaruckColors.BlueDark, fontWeight = FontWeight.Bold)
            if (m.sticker.isNotBlank()) {
                Image(painterResource(Avatars.sticker(m.sticker)), "Figurinha", Modifier.size(130.dp))
            } else {
                val shape = RoundedCornerShape(18.dp, 18.dp, if (mine) 4.dp else 18.dp, if (mine) 18.dp else 4.dp)
                Box(
                    Modifier.clip(shape)
                        .background(if (mine) BaruckColors.Green else Color.White)
                        .then(if (mine) Modifier else Modifier.border(2.dp, BaruckColors.Line, shape))
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(m.text, color = if (mine) Color.White else BaruckColors.Ink, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(clock(m.at), style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
        }
    }
}
