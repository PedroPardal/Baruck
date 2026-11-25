package com.pedro.baruck.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedro.baruck.R
import com.pedro.baruck.data.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.dayMonth
import com.pedro.baruck.ui.money
import com.pedro.baruck.ui.parseMoney
import com.pedro.baruck.ui.priceText
import com.pedro.baruck.ui.theme.BaruckColors
import java.util.Date

@Composable
fun NewFarmSheet(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var loc by remember { mutableStateOf("") }
    FormSheet("Nova lavoura", onDismiss, footer = {
        BaruckButton("Criar lavoura", Modifier.fillMaxWidth(), enabled = name.isNotBlank()) { onSave(name, loc) }
    }) {
        MascotSays(R.drawable.baruck_wave, "Como se chama essa lavoura?", 90.dp)
        BaruckField(name, { name = it.take(40) }, "Nome (ex.: Sítio São José)")
        BaruckField(loc, { loc = it.take(40) }, "Local (opcional)")
    }
}

/** Busca + categorias + chips das culturas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CropPicker(selected: CatalogCrop?, onSelect: (CatalogCrop) -> Unit) {
    var search by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("Todas") }
    val cats = listOf("Todas") + CropCatalog.categories
    val list = CropCatalog.search(search).filter { cat == "Todas" || it.category == cat }

    Text("Cultura", style = MaterialTheme.typography.titleMedium)
    if (selected != null) {
        BaruckCard(borderColor = BaruckColors.Green, background = BaruckColors.GreenSoft) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selected.emoji, fontSize = 32.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(selected.name, style = MaterialTheme.typography.titleMedium)
                    Text("${selected.category} · colheita em ~${selected.cycleDays} dias",
                        style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                }
                Text("✓", color = BaruckColors.GreenDark, fontSize = 24.sp)
            }
        }
    }
    BaruckField(search, { search = it }, "Buscar cultura (ex.: tomate)")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cats, key = { "cat_$it" }) { c -> ChoiceChip(c, c == cat) { cat = c } }
    }
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { c -> ChoiceChip("${c.emoji} ${c.name}", c.key == selected?.key) { onSelect(c) } }
    }
    if (list.isEmpty()) Text("Nada encontrado 🤔", color = BaruckColors.InkSoft)
}

/** Estufa + cultura numa tela só. */
@Composable
fun NewGreenhouseSheet(
    suggestedName: String,
    onDismiss: () -> Unit,
    onSave: (name: String, crop: CatalogCrop, variety: String, planted: Date, area: Double) -> Unit
) {
    var name by remember { mutableStateOf(suggestedName) }
    var crop by remember { mutableStateOf<CatalogCrop?>(null) }
    var variety by remember { mutableStateOf("") }
    var planted by remember { mutableStateOf(Date()) }
    var area by remember { mutableStateOf("") }
    FormSheet("Nova estufa", onDismiss, footer = {
        BaruckButton("Criar estufa", Modifier.fillMaxWidth(), enabled = name.isNotBlank() && crop != null) {
            onSave(name, crop!!, variety, planted, parseMoney(area))
        }
    }) {
        BaruckField(name, { name = it.take(30) }, "Nome da estufa")
        CropPicker(crop) { crop = it }
        BaruckField(variety, { variety = it.take(30) }, "Variedade (opcional)")
        DatePickerField("Data do plantio", planted) { planted = it }
        BaruckField(area, { area = it.take(8) }, "Área em m² (opcional)", keyboardType = KeyboardType.Decimal)
    }
}

@Composable
fun ChangeCropSheet(current: String, onDismiss: () -> Unit, onSave: (CatalogCrop, String, Date) -> Unit) {
    var crop by remember { mutableStateOf<CatalogCrop?>(null) }
    var variety by remember { mutableStateOf("") }
    var planted by remember { mutableStateOf(Date()) }
    FormSheet("Trocar cultura", onDismiss, footer = {
        BaruckButton("Começar nova safra", Modifier.fillMaxWidth(), enabled = crop != null) {
            onSave(crop!!, variety, planted)
        }
    }) {
        Text("A safra de $current será encerrada. As colheitas dela continuam no histórico.",
            style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
        CropPicker(crop) { crop = it }
        BaruckField(variety, { variety = it.take(30) }, "Variedade (opcional)")
        DatePickerField("Data do plantio", planted) { planted = it }
    }
}

/** Colheita rápida: data + caixas. Preço fica para depois. */
@Composable
fun HarvestSheet(cropName: String, onDismiss: () -> Unit, onSave: (Date, Int, Int, Int) -> Unit) {
    var date by remember { mutableStateOf(Date()) }
    var g by remember { mutableIntStateOf(0) }
    var m by remember { mutableIntStateOf(0) }
    var p by remember { mutableIntStateOf(0) }
    val total = g + m + p
    FormSheet("Nova colheita", onDismiss, footer = {
        BaruckButton("Salvar colheita", Modifier.fillMaxWidth(), enabled = total > 0) { onSave(date, g, m, p) }
    }) {
        MascotSays(R.drawable.baruck_wave, "Quantas caixas de $cropName saíram? O preço você coloca depois.", 90.dp)
        DatePickerField("Data da colheita", date) { date = it }
        Stepper("Grande", g, BaruckColors.Green, BaruckColors.GreenDark) { g = it }
        Stepper("Média", m, BaruckColors.Blue, BaruckColors.BlueDark) { m = it }
        Stepper("Miúda", p, BaruckColors.Orange, BaruckColors.OrangeDark) { p = it }
        Text("🧺  $total caixas no total", style = MaterialTheme.typography.titleLarge)
    }
}

/** Preços por caixa de uma colheita (pode ser feito depois). */
@Composable
fun PricesSheet(h: Harvest, suggestion: Harvest?, onDismiss: () -> Unit, onSave: (Double, Double, Double) -> Unit) {
    fun start(cur: Double, sug: Double?) = priceText(if (cur > 0) cur else sug ?: 0.0)
    var pg by remember { mutableStateOf(start(h.priceGrande, suggestion?.priceGrande)) }
    var pm by remember { mutableStateOf(start(h.priceMedia, suggestion?.priceMedia)) }
    var pp by remember { mutableStateOf(start(h.priceMiuda, suggestion?.priceMiuda)) }
    val total = h.boxesGrande * parseMoney(pg) + h.boxesMedia * parseMoney(pm) + h.boxesMiuda * parseMoney(pp)
    FormSheet("Preços da colheita", onDismiss, footer = {
        BaruckButton("Salvar preços", Modifier.fillMaxWidth(), enabled = total > 0) {
            onSave(parseMoney(pg), parseMoney(pm), parseMoney(pp))
        }
    }) {
        Text("${h.cropName} · ${dayMonth(h.date)} · ${h.totalBoxes} caixas",
            style = MaterialTheme.typography.bodyLarge, color = BaruckColors.InkSoft)
        if (suggestion != null && !h.priced)
            Text("Preenchi com os últimos preços que você usou. É só conferir 😉",
                style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
        if (h.boxesGrande > 0) BaruckField(pg, { pg = it.take(10) }, "Preço da caixa grande (${h.boxesGrande} cx)", prefix = "R$ ", keyboardType = KeyboardType.Decimal)
        if (h.boxesMedia > 0) BaruckField(pm, { pm = it.take(10) }, "Preço da caixa média (${h.boxesMedia} cx)", prefix = "R$ ", keyboardType = KeyboardType.Decimal)
        if (h.boxesMiuda > 0) BaruckField(pp, { pp = it.take(10) }, "Preço da caixa miúda (${h.boxesMiuda} cx)", prefix = "R$ ", keyboardType = KeyboardType.Decimal)
        BaruckCard(borderColor = BaruckColors.Green, background = BaruckColors.GreenSoft) {
            Text("Total da colheita", style = MaterialTheme.typography.labelSmall, color = BaruckColors.GreenDark)
            Text(money(total), style = MaterialTheme.typography.headlineMedium, color = BaruckColors.GreenDark)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CostSheet(greenhouses: List<Greenhouse>, onDismiss: () -> Unit, onSave: (String, String, Double, Date, String) -> Unit) {
    var cat by remember { mutableStateOf(CostCategories.all.first().key) }
    var amount by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Date()) }
    var gh by remember { mutableStateOf("") }
    val value = parseMoney(amount)
    FormSheet("Novo custo", onDismiss, footer = {
        BaruckButton("Salvar custo", Modifier.fillMaxWidth(), enabled = value > 0) { onSave(cat, desc, value, date, gh) }
    }) {
        Text("Categoria", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CostCategories.all.forEach { c -> ChoiceChip("${c.emoji} ${c.label}", c.key == cat) { cat = c.key } }
        }
        BaruckField(amount, { amount = it.take(12) }, "Valor", prefix = "R$ ", keyboardType = KeyboardType.Decimal)
        BaruckField(desc, { desc = it.take(60) }, "Descrição (opcional)")
        DatePickerField("Data", date) { date = it }
        if (greenhouses.isNotEmpty()) {
            Text("Estufa (opcional)", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("Geral", gh.isEmpty()) { gh = "" }
                greenhouses.forEach { g -> ChoiceChip(g.name, g.id == gh) { gh = g.id } }
            }
        }
    }
}

