package com.pedro.baruck.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pedro.baruck.data.CropCatalog
import com.pedro.baruck.ui.Sfx
import com.pedro.baruck.ui.theme.BaruckColors

/** Navega tocando o "fuuu" de transição. */
fun NavController.go(route: String) {
    Sfx.play(Sfx.S.WHOOSH)
    navigate(route) { launchSingleTop = true }
}

fun NavController.back() {
    Sfx.play(Sfx.S.WHOOSH)
    popBackStack()
}

fun NavController.backToMain() {
    Sfx.play(Sfx.S.WHOOSH)
    popBackStack("main", inclusive = false)
}

fun cropEmoji(key: String): String = CropCatalog.byKey(key)?.emoji ?: "🌱"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaruckTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Column(Modifier.background(Color.White)) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = BaruckColors.InkSoft)
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        HorizontalDivider(thickness = 2.dp, color = BaruckColors.Line)
    }
}

@Composable
fun TopIcon(res: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = { Sfx.play(Sfx.S.TAP); onClick() }) {
        Image(painterResource(res), description, Modifier.size(30.dp))
    }
}

/** Faixa branca fixa embaixo da tela com o botão principal. */
@Composable
fun BottomAction(content: @Composable () -> Unit) {
    Column(Modifier.background(Color.White).navigationBarsPadding()) {
        HorizontalDivider(thickness = 2.dp, color = BaruckColors.Line)
        Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = BaruckColors.RedDark) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
