package com.pedro.baruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pedro.baruck.data.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import java.util.Calendar
import java.util.Date

/* ---------- utilidades de formulário ---------- */

fun Double?.orZero() = this ?: 0.0
fun String.toMoney(): Double = replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
fun String.toIntSafe(): Int = trim().toIntOrNull() ?: 0

/** Seletor de data simples, sem dependências externas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, date: Date?, onPick: (Date) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Text(if (date != null) "$label: ${com.pedro.baruck.ui.shortDate(date)}" else label)
    }
    if (open) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.time ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Date(it)) }
                    open = false
                }) { Text("OK", color = BaruckColors.Green) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

/* ---------- diálogo: nova/editar safra ---------- */

@Composable
fun CropDialog(
    greenhouses: List<Greenhouse>,
    initial: Crop? = null,
    onDismiss: () -> Unit,
    onSave: (Crop) -> Unit
) {
    var ghId by remember { mutableStateOf(initial?.greenhouseId ?: greenhouses.firstOrNull()?.id ?: "") }
    var selected by remember { mutableStateOf(initial?.let { CropCatalog.byKey(it.cropKey) }) }
    var variety by remember { mutableStateOf(initial?.variety ?: "") }
    var planted by remember { mutableStateOf(initial?.plantedAt) }
    var search by remember { mutableStateOf("") }
    val results = remember(search) { CropCatalog.search(search) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp)) {
                Text(if (initial == null) "Nova safra" else "Editar safra",
                    style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))

                if (greenhouses.isNotEmpty()) {
                    Text("Estufa", style = MaterialTheme.typography.labelLarge)
                    LazyRowChips(greenhouses.map { it.id to it.name }, ghId) { ghId = it }
                    Spacer(Modifier.height(12.dp))
                }

                if (selected == null) {
                    BaruckField(search, { search = it }, "Buscar cultura")
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 260.dp)) {
                        items(results, key = { it.key }) { c ->
                            ListItem(
                                headlineContent = { Text("${c.emoji}  ${c.name}") },
                                supportingContent = { Text(c.category, color = BaruckColors.InkSoft) },
                                modifier = Modifier.clickableRow { selected = c }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    val c = selected!!
                    BaruckCard(accent = BaruckColors.Green) {
                        Row {
                            Text("${c.emoji}  ${c.name}", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = { selected = null }) { Text("Trocar") }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    BaruckField(variety, { variety = it }, "Variedade (opcional)")
                    Spacer(Modifier.height(10.dp))
                    DatePickerField("Data de plantio", planted) { planted = it }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val c = selected ?: return@Button
                            val expected = planted?.let {
                                Calendar.getInstance().apply {
                                    time = it; add(Calendar.DAY_OF_YEAR, c.cycleDays)
                                }.time
                            }
                            onSave(
                                (initial ?: Crop()).copy(
                                    greenhouseId = ghId, cropKey = c.key, cropName = c.name,
                                    variety = variety, plantedAt = planted,
                                    expectedHarvestAt = expected, status = initial?.status ?: "ativa"
                                )
                            )
                        },
                        enabled = selected != null,
                        colors = ButtonDefaults.buttonColors(containerColor = BaruckColors.Green),
                        modifier = Modifier.weight(1f)
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

/* ---------- diálogo: nova colheita (divisão por caixa) ---------- */

@Composable
fun HarvestDialog(
    crops: List<Crop>,
    fixedCrop: Crop? = null,
    onDismiss: () -> Unit,
    onSave: (Harvest) -> Unit
) {
    var cropId by remember { mutableStateOf(fixedCrop?.id ?: crops.firstOrNull()?.id ?: "") }
    val crop = fixedCrop ?: crops.firstOrNull { it.id == cropId }
    var date by remember { mutableStateOf<Date?>(Date()) }
    var g by remember { mutableStateOf("") }; var m by remember { mutableStateOf("") }; var p by remember { mutableStateOf("") }
    var pg by remember { mutableStateOf("") }; var pm by remember { mutableStateOf("") }; var pp by remember { mutableStateOf("") }

    val total = g.toIntSafe() + m.toIntSafe() + p.toIntSafe()
    val revenue = g.toIntSafe() * pg.toMoney() + m.toIntSafe() * pm.toMoney() + p.toIntSafe() * pp.toMoney()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp).heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nova colheita", style = MaterialTheme.typography.titleLarge)

                if (fixedCrop == null && crops.isNotEmpty()) {
                    Text("Safra", style = MaterialTheme.typography.labelLarge)
                    LazyRowChips(crops.map { it.id to it.cropName }, cropId) { cropId = it }
                } else if (crop != null) {
                    Pill(crop.cropName, BaruckColors.GreenDark, BaruckColors.GreenSoft)
                }

                DatePickerField("Data da colheita", date) { date = it }

                BoxRow("Caixa grande", g, { g = it }, pg, { pg = it }, BaruckColors.Green)
                BoxRow("Caixa média", m, { m = it }, pm, { pm = it }, BaruckColors.Blue)
                BoxRow("Caixa miúda", p, { p = it }, pp, { pp = it }, BaruckColors.Orange)

                BaruckCard(accent = BaruckColors.Green) {
                    Row {
                        Text("$total caixas", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        Text(com.pedro.baruck.ui.money(revenue),
                            style = MaterialTheme.typography.titleMedium, color = BaruckColors.GreenDark)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val c = crop ?: return@Button
                            onSave(
                                Harvest(
                                    cropId = c.id, cropName = c.cropName, date = date,
                                    boxesGrande = g.toIntSafe(), boxesMedia = m.toIntSafe(), boxesMiuda = p.toIntSafe(),
                                    priceGrande = pg.toMoney(), priceMedia = pm.toMoney(), priceMiuda = pp.toMoney()
                                )
                            )
                        },
                        enabled = crop != null && total > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = BaruckColors.Green),
                        modifier = Modifier.weight(1f)
                    ) { Text("Salvar") }
                }
            }
        }
    }
}

@Composable
private fun BoxRow(
    label: String,
    qty: String, onQty: (String) -> Unit,
    price: String, onPrice: (String) -> Unit,
    accent: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = accent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                qty, onQty, label = { Text("Qtd") },
                singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )
            OutlinedTextField(
                price, onPrice, label = { Text("Preço/cx") },
                singleLine = true, modifier = Modifier.weight(1.3f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )
        }
    }
}
