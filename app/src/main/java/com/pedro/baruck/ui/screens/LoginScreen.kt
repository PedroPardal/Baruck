package com.pedro.baruck.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.pedro.baruck.R
import com.pedro.baruck.auth.AuthManager
import com.pedro.baruck.ui.components.BaruckButton
import com.pedro.baruck.ui.theme.BaruckColors
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(webClientId: String, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { AuthManager(context, webClientId) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(40.dp))
        Image(
            painterResource(R.drawable.baruck_wave),
            contentDescription = "Baruck",
            modifier = Modifier.height(200.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Baruck", fontSize = 40.sp, style = MaterialTheme.typography.displaySmall,
            color = BaruckColors.Green)
        Spacer(Modifier.height(6.dp))
        Text(
            "Gerencie suas estufas e safras\ncom a ajuda do Baruck",
            style = MaterialTheme.typography.bodyLarge,
            color = BaruckColors.InkSoft,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        if (loading) {
            CircularProgressIndicator(color = BaruckColors.Green)
        } else {
            BaruckButton(
                text = "Entrar com Google",
                modifier = Modifier.fillMaxWidth()
            ) {
                loading = true; error = null
                scope.launch {
                    val r = auth.signInWithGoogle()
                    loading = false
                    r.fold(
                        onSuccess = { onLoggedIn() },
                        onFailure = { error = "Não consegui entrar. Tente de novo." }
                    )
                }
            }
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = BaruckColors.Red, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(60.dp))
    }
}
