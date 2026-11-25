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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.Reminder
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(vm: BaruckViewModel, nav: NavController, farmId: String) {
    val farms by vm.farms.collectAsState()
    val farm = farms.firstOrNull { it.id == farmId }
    val reminders by vm.reminders(farmId).collectAsState(emptyList())
    var show by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Lembretes", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { show = true },
                containerColor = BaruckColors.Green, contentColor = Color.White
            ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Lembrete") }
        }
    ) { pad ->
        if (reminders.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(R.drawable.baruck_head, "Sem lembretes",
                    "Crie lembretes de plantio, adubação ou colheita. Todos os membros recebem a notificação.")
            }
        } else {
            LazyColumn(Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reminders, key = { it.id }) { r ->
                    BaruckCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = r.done,
                                onCheckedChange = { vm.setReminderDone(farmId, r.id, it) },
                                colors = CheckboxDefaults.colors(checkedColor = BaruckColors.Green)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(r.title, style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (r.done) TextDecoration.LineThrough else null)
                                if (r.body.isNotBlank())
                                    Text(r.body, style = MaterialTheme.typography.bodyMedium,
                                        color = BaruckColors.InkSoft)
                                r.dueAt?.let {
                                    Spacer(Modifier.height(4.dp))
                                    val overdue = !r.done && it.before(Date())
                                    Pill(
                                        shortDate(it),
                                        if (overdue) BaruckColors.Red else BaruckColors.BlueDark,
                                        if (overdue) Color(0xFFFFE0E0) else Color(0xFFD7F0FF)
                                    )
                                }
                            }
                            IconButton(onClick = { vm.deleteReminder(farmId, r.id) }) {
                                Icon(Icons.Default.DeleteOutline, "Excluir", tint = BaruckColors.Red)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (show) ReminderDialog(onDismiss = { show = false }) { title, body, due ->
        val targets = farm?.members ?: listOf(vm.repo.uid)
        vm.addReminder(farmId, Reminder(
            farmId = farmId, title = title, body = body, dueAt = due, targets = targets
        ))
        show = false
    }
}

@Composable
private fun ReminderDialog(onDismiss: () -> Unit, onSave: (String, String, Date?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<Date?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Novo lembrete", style = MaterialTheme.typography.titleLarge)
                BaruckField(title, { title = it }, "Título")
                BaruckField(body, { body = it }, "Detalhe (opcional)", singleLine = false)
                DatePickerField("Data do aviso", due) { due = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = { onSave(title, body, due) },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BaruckColors.Green),
                        modifier = Modifier.weight(1f)
                    ) { Text("Salvar") }
                }
            }
        }
    }
}
