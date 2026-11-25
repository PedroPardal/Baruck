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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.data.UserProfile
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(vm: BaruckViewModel, nav: NavController, farmId: String) {
    val farms by vm.farms.collectAsState()
    val farm = farms.firstOrNull { it.id == farmId }
    val friends by vm.friends.collectAsState()
    val meUid = vm.repo.uid
    val isOwner = farm?.ownerUid == meUid
    val memberUids = farm?.members ?: emptyList()
    val available = friends.filter { it.uid !in memberUids }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Membros da lavoura", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            item { SectionTitle("Quem gerencia") }
            items(memberUids.distinct(), key = { "member_$it" }) { uid ->
                val name = farm?.memberNames?.get(uid) ?: "Produtor"
                BaruckCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, null, tint = BaruckColors.Green,
                            modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            if (uid == farm?.ownerUid)
                                Pill("dono", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                        }
                        if (isOwner && uid != meUid) {
                            IconButton(onClick = { vm.removeMember(farmId, uid) }) {
                                Icon(Icons.Default.PersonRemove, "Remover", tint = BaruckColors.Red)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionTitle("Adicionar um amigo")
            }
            if (available.isEmpty()) {
                item {
                    Text(
                        if (friends.isEmpty())
                            "Você ainda não tem amigos. Adicione amigos pela tela de amigos para poder convidá-los."
                        else "Todos os seus amigos já estão nesta lavoura.",
                        style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft
                    )
                }
            } else {
                items(available.distinctBy { it.uid }, key = { "avail_" + it.uid }) { f ->
                    FriendAddRow(f) { vm.addMemberToFarm(farmId, it) }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun FriendAddRow(friend: UserProfile, onAdd: (UserProfile) -> Unit) {
    BaruckCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, null, tint = BaruckColors.Blue, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(friend.name, style = MaterialTheme.typography.titleMedium)
                Text(friend.email, style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
            }
            FilledTonalButton(
                onClick = { onAdd(friend) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = BaruckColors.GreenSoft, contentColor = BaruckColors.GreenDark
                )
            ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Adicionar") }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = BaruckColors.InkSoft)
}
