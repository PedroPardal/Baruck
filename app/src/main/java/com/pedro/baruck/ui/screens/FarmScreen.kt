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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.*
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmScreen(vm: BaruckViewModel, nav: NavController, farmId: String) {
    val farms by vm.farms.collectAsState()
    val farm = farms.firstOrNull { it.id == farmId }
    val greenhouses by remember(farmId) { vm.greenhouses(farmId) }.collectAsState(emptyList())
    val crops by remember(farmId) { vm.crops(farmId) }.collectAsState(emptyList())
    val harvests by remember(farmId) { vm.harvests(farmId) }.collectAsState(emptyList())

    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Estufas", "Safras", "Colheitas")
    var showGh by remember { mutableStateOf(false) }
    var showCrop by remember { mutableStateOf(false) }
    var showHarvest by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(farm?.name ?: "Lavoura", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { nav.navigate("reminders/$farmId") }) {
                        Icon(Icons.Default.NotificationsActive, "Lembretes")
                    }
                    IconButton(onClick = { nav.navigate("members/$farmId") }) {
                        Icon(Icons.Default.Group, "Membros")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { when (tab) { 0 -> showGh = true; 1 -> showCrop = true; else -> showHarvest = true } },
                containerColor = BaruckColors.Green, contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp))
                Text(when (tab) { 0 -> "Estufa"; 1 -> "Safra"; else -> "Colheita" })
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = BaruckColors.Green
            ) {
                tabs.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i },
                        text = { Text(t, style = MaterialTheme.typography.labelLarge) })
                }
            }
            when (tab) {
                0 -> GreenhousesTab(greenhouses, crops) { vm.deleteGreenhouse(farmId, it) }
                1 -> CropsTab(crops, greenhouses,
                    onOpen = { nav.navigate("crop/$farmId/${it.id}") },
                    onDelete = { vm.deleteCrop(farmId, it) })
                2 -> HarvestsTab(harvests) { vm.deleteHarvest(farmId, it) }
            }
        }
    }

    if (showGh) GreenhouseDialog(onDismiss = { showGh = false }) { n, a, note ->
        vm.addGreenhouse(farmId, n, a, note); showGh = false
    }
    if (showCrop) {
        if (greenhouses.isEmpty()) {
            InfoDialog("Crie uma estufa primeiro", "As safras ficam dentro de uma estufa.") { showCrop = false }
        } else CropDialog(greenhouses, onDismiss = { showCrop = false }) {
            vm.addCrop(farmId, it); showCrop = false
        }
    }
    if (showHarvest) {
        if (crops.isEmpty()) {
            InfoDialog("Cadastre uma safra primeiro", "A colheita é lançada em cima de uma safra.") { showHarvest = false }
        } else HarvestDialog(crops, onDismiss = { showHarvest = false }) {
            vm.addHarvest(farmId, it); showHarvest = false
        }
    }
}

/* ---------- Aba Estufas ---------- */
@Composable
private fun GreenhousesTab(greenhouses: List<Greenhouse>, crops: List<Crop>, onDelete: (String) -> Unit) {
    if (greenhouses.isEmpty()) {
        EmptyState(R.drawable.baruck_stand, "Nenhuma estufa", "Toque em + Estufa para cadastrar a primeira.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(greenhouses, key = { it.id }) { gh ->
            val count = crops.count { it.greenhouseId == gh.id && it.status == "ativa" }
            BaruckCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(gh.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            buildString {
                                if (gh.areaM2 > 0) append("${gh.areaM2.toInt()} m²  ·  ")
                                append("$count safra(s) ativa(s)")
                            },
                            style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft
                        )
                        if (gh.note.isNotBlank())
                            Text(gh.note, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { onDelete(gh.id) }) {
                        Icon(Icons.Default.DeleteOutline, "Excluir", tint = BaruckColors.Red)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/* ---------- Aba Safras ---------- */
@Composable
private fun CropsTab(
    crops: List<Crop>, greenhouses: List<Greenhouse>,
    onOpen: (Crop) -> Unit, onDelete: (String) -> Unit
) {
    if (crops.isEmpty()) {
        EmptyState(R.drawable.baruck_walk, "Nenhuma safra", "Cadastre uma safra escolhendo entre dezenas de culturas.")
        return
    }
    val ghName = greenhouses.associate { it.id to it.name }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(crops, key = { it.id }) { c ->
            val cat = CropCatalog.byKey(c.cropKey)
            BaruckCard(onClick = { onOpen(c) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat?.emoji ?: "🌱", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.cropName + (if (c.variety.isNotBlank()) " · ${c.variety}" else ""),
                            style = MaterialTheme.typography.titleMedium)
                        Text("Estufa: ${ghName[c.greenhouseId] ?: "—"}",
                            style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(c.status, BaruckColors.GreenDark, BaruckColors.GreenSoft)
                            c.expectedHarvestAt?.let {
                                Pill("colher ~ ${shortDate(it)}", BaruckColors.BlueDark, Color(0xFFD7F0FF))
                            }
                        }
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = BaruckColors.InkSoft)
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

/* ---------- Aba Colheitas ---------- */
@Composable
private fun HarvestsTab(harvests: List<Harvest>, onDelete: (String) -> Unit) {
    if (harvests.isEmpty()) {
        EmptyState(R.drawable.baruck_sit, "Nenhuma colheita", "Lance a produção dividida em caixas grandes, médias e miúdas.")
        return
    }
    val g = harvests.sumOf { it.boxesGrande }
    val m = harvests.sumOf { it.boxesMedia }
    val p = harvests.sumOf { it.boxesMiuda }
    val revenue = harvests.sumOf { it.revenue }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            BaruckCard(accent = BaruckColors.Green) {
                Text("Resumo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryBox("Grande", g, BaruckColors.Green)
                    SummaryBox("Média", m, BaruckColors.Blue)
                    SummaryBox("Miúda", p, BaruckColors.Orange)
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(10.dp))
                Row {
                    Text("Faturamento total", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    Text(money(revenue), style = MaterialTheme.typography.titleMedium,
                        color = BaruckColors.GreenDark)
                }
            }
        }
        items(harvests, key = { it.id }) { h ->
            BaruckCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(h.cropName, style = MaterialTheme.typography.titleMedium)
                        Text(shortDate(h.date), style = MaterialTheme.typography.bodyMedium,
                            color = BaruckColors.InkSoft)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (h.boxesGrande > 0) Pill("G: ${h.boxesGrande}", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                            if (h.boxesMedia > 0) Pill("M: ${h.boxesMedia}", BaruckColors.BlueDark, Color(0xFFD7F0FF))
                            if (h.boxesMiuda > 0) Pill("P: ${h.boxesMiuda}", BaruckColors.Orange, Color(0xFFFFE9CC))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(h.revenue), style = MaterialTheme.typography.titleMedium,
                            color = BaruckColors.GreenDark)
                        IconButton(onClick = { onDelete(h.id) }) {
                            Icon(Icons.Default.DeleteOutline, "Excluir", tint = BaruckColors.Red)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SummaryBox(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
    }
}

/* ---------- diálogos auxiliares ---------- */
@Composable
fun GreenhouseDialog(onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = name.isNotBlank(),
                onClick = { onSave(name, area.toMoney(), note) }) {
                Text("Salvar", color = BaruckColors.Green)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        title = { Text("Nova estufa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BaruckField(name, { name = it }, "Nome da estufa")
                BaruckField(area, { area = it }, "Área em m² (opcional)",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                BaruckField(note, { note = it }, "Observação (opcional)")
            }
        }
    )
}

@Composable
fun InfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendi", color = BaruckColors.Green) } },
        title = { Text(title) }, text = { Text(body) }
    )
}
