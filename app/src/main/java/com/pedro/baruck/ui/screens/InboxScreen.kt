package com.pedro.baruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.ui.BaruckViewModel
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.shortDate
import com.pedro.baruck.ui.theme.BaruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(vm: BaruckViewModel, nav: NavController) {
    val inbox by vm.inbox.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Notificações", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { pad ->
        if (inbox.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(R.drawable.baruck_sleep, "Tudo tranquilo",
                    "Suas notificações de lavoura e amizade aparecem aqui.")
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(inbox, key = { it.id }) { item ->
                    BaruckCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!item.read)
                                Icon(Icons.Default.Circle, null, tint = BaruckColors.Green,
                                    modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(if (item.read) 0.dp else 10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                if (item.body.isNotBlank())
                                    Text(item.body, style = MaterialTheme.typography.bodyMedium,
                                        color = BaruckColors.InkSoft)
                                Text(shortDate(item.createdAt), style = MaterialTheme.typography.labelSmall,
                                    color = BaruckColors.InkSoft)
                            }
                        }
                    }
                    LaunchedEffect(item.id) { if (!item.read) vm.repo.markInboxRead(item.id) }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}
