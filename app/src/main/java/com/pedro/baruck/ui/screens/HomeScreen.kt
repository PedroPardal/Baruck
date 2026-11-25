package com.pedro.baruck.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.pedro.baruck.data.Farm
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: BaruckViewModel, nav: NavController, onSignOut: () -> Unit) {
    val farms by vm.farms.collectAsState()
    val inbox by vm.inbox.collectAsState()
    val requests by vm.friendRequests.collectAsState()
    var showNew by remember { mutableStateOf(false) }
    val unread = inbox.count { !it.read } + requests.size

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Minhas lavouras", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    BadgedBox(badge = {
                        if (unread > 0) Badge(containerColor = BaruckColors.Red) { Text("$unread") }
                    }) {
                        IconButton(onClick = { nav.navigate("inbox") }) {
                            Icon(Icons.Default.Notifications, "Notificações")
                        }
                    }
                    IconButton(onClick = { nav.navigate("friends") }) {
                        Icon(Icons.Default.People, "Amigos")
                    }
                    IconButton(onClick = { FirebaseAuth.getInstance().signOut(); onSignOut() }) {
                        Icon(Icons.Default.Logout, "Sair")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNew = true },
                containerColor = BaruckColors.Green,
                contentColor = androidx.compose.ui.graphics.Color.White
            ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Nova lavoura") }
        }
    ) { pad ->
        if (farms.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    com.pedro.baruck.R.drawable.baruck_sit,
                    "Nenhuma lavoura ainda",
                    "Crie sua primeira lavoura para começar a cadastrar estufas e safras."
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(farms, key = { it.id }) { farm ->
                    FarmRow(farm) { nav.navigate("farm/${farm.id}") }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showNew) NewFarmDialog(
        onDismiss = { showNew = false },
        onCreate = { name, loc -> vm.createFarm(name, loc); showNew = false }
    )
}

@Composable
private fun FarmRow(farm: Farm, onClick: () -> Unit) {
    BaruckCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(baruckFor(farm.id)),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(farm.name, style = MaterialTheme.typography.titleMedium)
                if (farm.location.isNotBlank())
                    Text(farm.location, style = MaterialTheme.typography.bodyMedium,
                        color = BaruckColors.InkSoft)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill("${farm.members.size} membro(s)", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = BaruckColors.InkSoft)
        }
    }
}

@Composable
private fun NewFarmDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, loc) }) {
                Text("Criar", color = BaruckColors.Green)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nova lavoura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BaruckField(name, { name = it }, "Nome da lavoura")
                BaruckField(loc, { loc = it }, "Local (opcional)")
            }
        }
    )
}
