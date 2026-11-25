package com.pedro.baruck.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pedro.baruck.R
import com.pedro.baruck.data.*
import com.pedro.baruck.ui.*
import com.pedro.baruck.ui.components.*
import com.pedro.baruck.ui.theme.BaruckColors
import java.util.Date

@Composable
fun FarmScreen(vm: BaruckViewModel, nav: NavController, farmId: String) {
    val farm by remember(farmId) { vm.farm(farmId) }.collectAsState(null)
    val ghs by remember(farmId) { vm.greenhouses(farmId) }.collectAsState(emptyList())
    val harvests by remember(farmId) { vm.harvests(farmId) }.collectAsState(emptyList())
    val costs by remember(farmId) { vm.costs(farmId) }.collectAsState(emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            BaruckTopBar(farm?.name ?: "Lavoura", onBack = { nav.back() }) {
                TopIcon(R.drawable.ic_bell, "Avisos") { nav.go("notices/$farmId") }
                TopIcon(R.drawable.ic_tab_friends, "Equipe") { nav.go("members/$farmId") }
            }
        },
        bottomBar = {
            if (tab < 2) BottomAction {
                if (tab == 0) BaruckButton("Nova estufa", Modifier.fillMaxWidth()) { sheet = "gh" }
                else BaruckButton("Novo custo", Modifier.fillMaxWidth(), color = BaruckColors.Red,
                    shadow = BaruckColors.RedDark) { sheet = "cost" }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.padding(16.dp)) {
                SegmentedTabs(listOf("🏠 Estufas", "💸 Custos", "📊 Resumo"), tab) { tab = it }
            }
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(260)) { it * dir / 3 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it * dir / 3 } + fadeOut(tween(160)))
                },
                label = "farmTabs"
            ) { t ->
                when (t) {
                    0 -> GreenhouseList(ghs, harvests) { nav.go("gh/$farmId/${it.id}") }
                    1 -> CostList(costs, ghs) { vm.deleteCost(farmId, it) }
                    else -> Summary(ghs, harvests, costs)
                }
            }
        }
    }

    when (sheet) {
        "gh" -> NewGreenhouseSheet("Estufa ${ghs.size + 1}", onDismiss = { sheet = null }) { n, c, v, d, a ->
            vm.addGreenhouse(farmId, n, c, v, d, a); sheet = null
        }
        "cost" -> CostSheet(ghs, onDismiss = { sheet = null }) { cat, desc, value, date, gh ->
            vm.addCost(farmId, cat, desc, value, date, gh); sheet = null
        }
    }
}

/* ------------------------------ Estufas ------------------------------ */

@Composable
private fun GreenhouseList(ghs: List<Greenhouse>, harvests: List<Harvest>, onOpen: (Greenhouse) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ghs.isEmpty()) item(key = "empty") {
            EmptyState(R.drawable.baruck_wave, "Nenhuma estufa ainda",
                "Toque em \"Nova estufa\". Você já escolhe a cultura na mesma tela.")
        }
        items(ghs, key = { "gh_" + it.id }) { gh ->
            val mine = harvests.filter { it.greenhouseId == gh.id && it.cropId == gh.cropId }
            val unpriced = mine.count { !it.priced }
            BaruckCard(Modifier.animateItem(), onClick = { onOpen(gh) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(BaruckColors.GreenSoft),
                        contentAlignment = Alignment.Center) { Text(cropEmoji(gh.cropKey), fontSize = 32.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(gh.name, style = MaterialTheme.typography.titleMedium)
                        Text(gh.cropName + (if (gh.variety.isNotBlank()) " · ${gh.variety}" else ""),
                            style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill("🧺 ${mine.sumOf { it.totalBoxes }} cx", BaruckColors.GreenDark, BaruckColors.GreenSoft)
                            daysSince(gh.plantedAt)?.let { Pill("🌱 $it dias", BaruckColors.BlueDark, BaruckColors.BlueSoft) }
                            if (unpriced > 0) Pill("$unpriced sem preço", BaruckColors.OrangeDark, BaruckColors.OrangeSoft)
                        }
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = BaruckColors.Line)
                }
            }
        }
    }
}

/* ------------------------------ Custos ------------------------------ */

@Composable
private fun CostList(costs: List<Cost>, ghs: List<Greenhouse>, onDelete: (String) -> Unit) {
    val now = Date()
    val monthTotal = costs.filter { sameMonth(it.date, now) }.sumOf { it.amount }
    var deleting by remember { mutableStateOf<Cost?>(null) }
    val ghName = ghs.associate { it.id to it.name }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "total") {
            BaruckCard(borderColor = BaruckColors.Red, background = BaruckColors.RedSoft) {
                Text("Gasto neste mês", style = MaterialTheme.typography.labelSmall, color = BaruckColors.RedDark)
                Text(money(monthTotal), style = MaterialTheme.typography.headlineMedium, color = BaruckColors.RedDark)
                Text("Total registrado: ${money(costs.sumOf { it.amount })}",
                    style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
            }
        }
        if (costs.isEmpty()) item(key = "empty") {
            EmptyState(R.drawable.baruck_sit, "Nenhum custo anotado",
                "Anote adubo, mudas, mão de obra, frete... e veja o lucro no Resumo.")
        }
        items(costs, key = { "cost_" + it.id }) { c ->
            val cat = CostCategories.of(c.category)
            BaruckCard(Modifier.animateItem()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFF7F7F7)), contentAlignment = Alignment.Center) {
                        Text(cat.emoji, fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.description.ifBlank { cat.label }, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(listOfNotNull(dayMonth(c.date), cat.label, ghName[c.greenhouseId]).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                    }
                    Text(money(c.amount), style = MaterialTheme.typography.titleMedium, color = BaruckColors.RedDark)
                    IconButton(onClick = { deleting = c }) {
                        Icon(Icons.Outlined.Delete, "Apagar", tint = BaruckColors.InkSoft)
                    }
                }
            }
        }
    }
    deleting?.let { c ->
        ConfirmDialog("Apagar custo?", "${c.description.ifBlank { CostCategories.of(c.category).label }} · ${money(c.amount)}",
            "Apagar", onConfirm = { onDelete(c.id) }, onDismiss = { deleting = null })
    }
}

/* ------------------------------ Resumo ------------------------------ */

@Composable
private fun Summary(ghs: List<Greenhouse>, harvests: List<Harvest>, costs: List<Cost>) {
    var period by rememberSaveable { mutableIntStateOf(0) }
    val now = Date()
    val inPeriod: (Date?) -> Boolean = { d -> when (period) { 0 -> sameMonth(d, now); 1 -> sameYear(d, now); else -> true } }
    val hs = harvests.filter { inPeriod(it.date) }
    val cs = costs.filter { inPeriod(it.date) }
    val revenue = hs.sumOf { it.revenue }
    val spent = cs.sumOf { it.amount }
    val profit = revenue - spent
    val unpriced = hs.count { !it.priced }

    Column(
        Modifier.fillMaxSize().verticalScrollSafe().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SegmentedTabs(listOf("Este mês", "Este ano", "Tudo"), period) { period = it }

        BaruckCard(
            borderColor = if (profit >= 0) BaruckColors.Green else BaruckColors.Red,
            background = if (profit >= 0) BaruckColors.GreenSoft else BaruckColors.RedSoft
        ) {
            Text(if (profit >= 0) "Lucro" else "Prejuízo", style = MaterialTheme.typography.labelSmall,
                color = if (profit >= 0) BaruckColors.GreenDark else BaruckColors.RedDark)
            Text(money(profit), style = MaterialTheme.typography.displaySmall,
                color = if (profit >= 0) BaruckColors.GreenDark else BaruckColors.RedDark)
            Spacer(Modifier.height(6.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Receita", style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                    Text(money(revenue), style = MaterialTheme.typography.titleMedium, color = BaruckColors.GreenDark)
                }
                Column(Modifier.weight(1f)) {
                    Text("Custos", style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
                    Text(money(spent), style = MaterialTheme.typography.titleMedium, color = BaruckColors.RedDark)
                }
            }
        }

        if (unpriced > 0) {
            BaruckCard(borderColor = BaruckColors.Yellow, background = Color(0xFFFFF7D6)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Mascot(R.drawable.baruck_sad, 64.dp, bobbing = false)
                    Spacer(Modifier.width(8.dp))
                    Text("$unpriced colheita(s) ainda sem preço. Abra a estufa e toque em \"Adicionar preços\" para a receita ficar completa.",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        BaruckCard {
            Text("Caixas colhidas", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BigNumber("${hs.sumOf { it.boxesGrande }}", "Grande", BaruckColors.Green)
                BigNumber("${hs.sumOf { it.boxesMedia }}", "Média", BaruckColors.Blue)
                BigNumber("${hs.sumOf { it.boxesMiuda }}", "Miúda", BaruckColors.Orange)
                BigNumber("${hs.sumOf { it.totalBoxes }}", "Total", BaruckColors.Ink)
            }
        }

        if (ghs.isNotEmpty()) {
            BaruckCard {
                Text("Por estufa", style = MaterialTheme.typography.titleMedium)
                val rows = ghs.map { g -> Triple(g, hs.filter { it.greenhouseId == g.id }.sumOf { it.totalBoxes },
                    hs.filter { it.greenhouseId == g.id }.sumOf { it.revenue }) }
                val max = (rows.maxOfOrNull { it.third } ?: 0.0).coerceAtLeast(1.0)
                rows.forEach { (g, boxes, rev) ->
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cropEmoji(g.cropKey) + " " + g.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("$boxes cx · ${money(rev)}", style = MaterialTheme.typography.labelLarge, color = BaruckColors.GreenDark)
                    }
                    Bar((rev / max).toFloat(), BaruckColors.Green)
                }
            }
        }

        if (cs.isNotEmpty()) {
            BaruckCard {
                Text("Custos por categoria", style = MaterialTheme.typography.titleMedium)
                val byCat = cs.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }
                    .entries.sortedByDescending { it.value }
                val max = (byCat.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(1.0)
                byCat.forEach { (k, v) ->
                    val c = CostCategories.of(k)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${c.emoji} ${c.label}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(money(v), style = MaterialTheme.typography.labelLarge, color = BaruckColors.RedDark)
                    }
                    Bar((v / max).toFloat(), BaruckColors.Red)
                }
            }
        }
    }
}

@Composable
private fun BigNumber(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = BaruckColors.InkSoft)
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "bar")
    Spacer(Modifier.height(4.dp))
    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(BaruckColors.Line)) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(CircleShape).background(color))
    }
}
