package com.pedro.baruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.data.UserProfile
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(vm: BaruckViewModel, nav: NavController) {
    val friends by vm.friends.collectAsState()
    val requests by vm.friendRequests.collectAsState()
    var email by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<UserProfile?>(null) }
    var searched by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Amigos", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            item {
                BaruckCard(accent = BaruckColors.Green) {
                    Text("Adicionar amigo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    BaruckField(email, { email = it; searched = false; result = null },
                        "E-mail do amigo", keyboardType = KeyboardType.Email)
                    Spacer(Modifier.height(10.dp))
                    BaruckButton("Buscar", modifier = Modifier.fillMaxWidth()) {
                        message = null
                        vm.findUser(email) { u -> result = u; searched = true }
                    }
                    if (searched) {
                        Spacer(Modifier.height(10.dp))
                        if (result == null) {
                            Text("Nenhum usuário com esse e-mail.", color = BaruckColors.InkSoft)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(result!!.name, style = MaterialTheme.typography.titleMedium)
                                    Text(result!!.email, color = BaruckColors.InkSoft,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                                Button(
                                    onClick = {
                                        vm.sendFriendRequest(result!!) { ok ->
                                            message = if (ok) "Pedido enviado!" else "Não consegui enviar."
                                        }
                                        result = null; searched = false; email = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BaruckColors.Green)
                                ) { Text("Convidar") }
                            }
                        }
                    }
                    message?.let { Spacer(Modifier.height(8.dp)); Text(it, color = BaruckColors.GreenDark) }
                }
            }

            if (requests.isNotEmpty()) {
                item { SectionTitle("Pedidos recebidos") }
                items(requests, key = { it.id }) { req ->
                    BaruckCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, null, tint = BaruckColors.Blue,
                                modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(req.fromName, style = MaterialTheme.typography.titleMedium)
                                Text(req.fromEmail, color = BaruckColors.InkSoft,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(onClick = { vm.acceptFriend(req) }) {
                                Icon(Icons.Default.CheckCircle, "Aceitar", tint = BaruckColors.Green)
                            }
                            IconButton(onClick = { vm.declineFriend(req) }) {
                                Icon(Icons.Default.Cancel, "Recusar", tint = BaruckColors.Red)
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Meus amigos") }
            if (friends.isEmpty()) {
                item {
                    Text("Você ainda não tem amigos adicionados.",
                        color = BaruckColors.InkSoft, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(friends, key = { it.uid }) { f ->
                    BaruckCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, null, tint = BaruckColors.Green,
                                modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(f.name, style = MaterialTheme.typography.titleMedium)
                                Text(f.email, color = BaruckColors.InkSoft,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
