package com.pedro.baruck.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.Harvest
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@Composable
fun GreenhouseScreen(vm: BaruckViewModel, nav: NavController, farmId: String, ghId: String) {
    val farm by remember(farmId) { vm.farm(farmId) }.collectAsState(null)
    val ghs by remember(farmId) { vm.greenhouses(farmId) }.collectAsState(emptyList())
    val all by remember(farmId) { vm.harvests(farmId) }.collectAsState(emptyList())
    val me by vm.me.collectAsState()
    val gh = ghs.firstOrNull { it.id == ghId }
    val harvests = all.filter { it.greenhouseId == ghId }
    val current = harvests.filter { it.cropId == gh?.cropId }
    val isAdmin = farm?.ownerUid == me?.uid

    var showHarvest by remember { mutableStateOf(false) }
    var showChange by remember { mutableStateOf(false) }
    var pricing by remember { mutableStateOf<Harvest?>(null) }
    var deleting by remember { mutableStateOf<Harvest?>(null) }
    var deleteGh by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                BaruckTopBar(gh?.name ?: "Estufa", onBack = { nav.back() }) {
                    if (isAdmin) IconButton(onClick = { deleteGh = true }) {
                        Icon(Icons.Outlined.Delete, "Apagar estufa", tint = BaruckColors.InkSoft)
                    }
                }
            },
            bottomBar = {
                if (gh != null) BottomAction {
                    BaruckButton("Registrar colheita", Modifier.fillMaxWidth()) { showHarvest = true }
                }
            }
        ) { pad ->
            if (gh == null) {
                Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BaruckColors.Green)
                }
                return@Scaffold
            }
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "head") {
                    BaruckCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(76.dp).clip(RoundedCornerShape(20.dp)).background(BaruckColors.GreenSoft),
                                contentAlignment = Alignment.Center) {
                                Text(cropEmoji(gh.cropKey), fontSize = 42.sp, modifier = Modifier.bob(4.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(gh.cropName, style = MaterialTheme.typography.headlineMedium)
                                if (gh.variety.isNotBlank()) Text(gh.variety, color = BaruckColors.InkSoft)
                                Text("Plantado em ${shortDate(gh.plantedAt)}" +
                                    (daysSince(gh.plantedAt)?.let { " · há $it dias" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                                if (gh.areaM2 > 0) Text("${gh.areaM2.toInt()} m²", style = MaterialTheme.typography.labelSmall,
                                    color = BaruckColors.InkSoft)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        BaruckOutlineButton("Trocar cultura", Modifier.fillMaxWidth()) { showChange = true }
                    }
                }
                item(key = "stats") {
                    BaruckCard {
                        Text("Safra atual", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Num("${current.sumOf { it.boxesGrande }}", "Grande", BaruckColors.Green)
                            Num("${current.sumOf { it.boxesMedia }}", "Média", BaruckColors.Blue)
                            Num("${current.sumOf { it.boxesMiuda }}", "Miúda", BaruckColors.Orange)
                            Num("${current.sumOf { it.totalBoxes }}", "Caixas", BaruckColors.Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Receita: ${money(current.sumOf { it.revenue })}", style = MaterialTheme.typography.titleMedium,
                            color = BaruckColors.GreenDark)
                    }
                }
                item(key = "hTitle") { SectionTitle("Colheitas") }
                if (harvests.isEmpty()) item(key = "empty") {
                    EmptyState(R.drawable.baruck_sit, "Nenhuma colheita ainda",
                        "Toque em \"Registrar colheita\" e anote as caixas. O preço pode ficar para depois.")
                }
                items(harvests, key = { "h_" + it.id }) { h ->
                    HarvestCard(h, Modifier.animateItem(),
                        onPrices = { pricing = h },
                        onDelete = if (isAdmin || h.createdBy == me?.uid) ({ deleting = h }) else null)
                }
            }
        }
        ConfettiBurst(confetti)
    }

    if (showHarvest && gh != null) HarvestSheet(gh.cropName, onDismiss = { showHarvest = false }) { d, g, m, p ->
        vm.addHarvest(farmId, gh, d, g, m, p)
        showHarvest = false
        confetti++
    }
    if (showChange && gh != null) ChangeCropSheet(gh.cropName, onDismiss = { showChange = false }) { c, v, d ->
        vm.changeCrop(farmId, gh, c, v, d); showChange = false
    }
    pricing?.let { h ->
        val suggestion = harvests.firstOrNull { it.priced && it.id != h.id }
        PricesSheet(h, suggestion, onDismiss = { pricing = null }) { pg, pm, pp ->
            vm.setPrices(farmId, h.id, pg, pm, pp); pricing = null
        }
    }
    deleting?.let { h ->
        ConfirmDialog("Apagar colheita?", "${dayMonth(h.date)} · ${h.totalBoxes} caixas", "Apagar",
            onConfirm = { vm.deleteHarvest(farmId, h.id) }, onDismiss = { deleting = null })
    }
    if (deleteGh) ConfirmDialog("Apagar estufa?", "A estufa sai da lista. As colheitas continuam no resumo.", "Apagar",
        onConfirm = { vm.deleteGreenhouse(farmId, ghId); nav.back() }, onDismiss = { deleteGh = false })
}

@Composable
private fun HarvestCard(h: Harvest, modifier: Modifier, onPrices: () -> Unit, onDelete: (() -> Unit)?) {
    BaruckCard(modifier, borderColor = if (h.priced) BaruckColors.Line else BaruckColors.Yellow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${dayMonth(h.date)} · ${h.cropName}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (h.boxesGrande > 0) Pill("G ${h.boxesGrande}", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                    if (h.boxesMedia > 0) Pill("M ${h.boxesMedia}", BaruckColors.BlueDark, BaruckColors.BlueSoft)
                    if (h.boxesMiuda > 0) Pill("Mi ${h.boxesMiuda}", BaruckColors.OrangeDark, BaruckColors.OrangeSoft)
                }
            }
            if (h.priced) Text(money(h.revenue), style = MaterialTheme.typography.titleMedium, color = BaruckColors.GreenDark)
            if (onDelete != null) IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Apagar", tint = BaruckColors.InkSoft)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (h.priced) BaruckOutlineButton("Editar preços", Modifier.fillMaxWidth(), color = BaruckColors.InkSoft, onClick = onPrices)
        else YellowButton("Adicionar preços", Modifier.fillMaxWidth(), onClick = onPrices)
    }
}

@Composable
private fun Num(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
    }
}
