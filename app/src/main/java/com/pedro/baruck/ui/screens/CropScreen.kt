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
import com.pedro.baruck.data.CropCatalog
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(vm: BaruckViewModel, nav: NavController, farmId: String, cropId: String) {
    val crops by vm.crops(farmId).collectAsState(emptyList())
    val crop = crops.firstOrNull { it.id == cropId }
    val harvests by vm.harvestsOfCrop(farmId, cropId).collectAsState(emptyList())
    var showHarvest by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(crop?.cropName ?: "Safra", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showHarvest = true },
                containerColor = BaruckColors.Green, contentColor = Color.White
            ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Colheita") }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val cat = crop?.let { CropCatalog.byKey(it.cropKey) }
                BaruckCard(accent = BaruckColors.Green) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat?.emoji ?: "🌱", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(crop?.cropName ?: "", style = MaterialTheme.typography.titleLarge)
                            if (crop?.variety?.isNotBlank() == true)
                                Text(crop.variety, color = BaruckColors.InkSoft)
                            Text("Plantio: ${shortDate(crop?.plantedAt)}  ·  Colheita prevista: ${shortDate(crop?.expectedHarvestAt)}",
                                style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
                        }
                    }
                }
            }
            item {
                val g = harvests.sumOf { it.boxesGrande }
                val m = harvests.sumOf { it.boxesMedia }
                val p = harvests.sumOf { it.boxesMiuda }
                val rev = harvests.sumOf { it.revenue }
                BaruckCard {
                    Text("Total colhido", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatCol("Grande", g, BaruckColors.Green)
                        StatCol("Média", m, BaruckColors.Blue)
                        StatCol("Miúda", p, BaruckColors.Orange)
                        StatCol("Caixas", g + m + p, BaruckColors.Ink)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Faturamento: ${money(rev)}", style = MaterialTheme.typography.titleMedium,
                        color = BaruckColors.GreenDark)
                }
            }
            if (harvests.isEmpty()) {
                item {
                    EmptyState(R.drawable.baruck_jump, "Sem colheitas",
                        "Lance a primeira colheita desta safra.")
                }
            } else {
                items(harvests, key = { it.id }) { h ->
                    BaruckCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(shortDate(h.date), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (h.boxesGrande > 0) Pill("G ${h.boxesGrande} × ${money(h.priceGrande)}", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                                    if (h.boxesMedia > 0) Pill("M ${h.boxesMedia}", BaruckColors.BlueDark, Color(0xFFD7F0FF))
                                    if (h.boxesMiuda > 0) Pill("P ${h.boxesMiuda}", BaruckColors.Orange, Color(0xFFFFE9CC))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(money(h.revenue), color = BaruckColors.GreenDark,
                                    style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { vm.deleteHarvest(farmId, h.id) }) {
                                    Icon(Icons.Default.DeleteOutline, "Excluir", tint = BaruckColors.Red)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showHarvest && crop != null) HarvestDialog(
        crops = crops, fixedCrop = crop,
        onDismiss = { showHarvest = false }
    ) { vm.addHarvest(farmId, it); showHarvest = false }
}

@Composable
private fun StatCol(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
    }
}
