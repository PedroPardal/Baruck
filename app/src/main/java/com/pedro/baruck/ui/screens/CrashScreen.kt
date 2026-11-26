package com.pedro.baruck.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedro.baruck.R
import com.pedro.baruck.ui.components.BaruckButton
import com.pedro.baruck.ui.components.BaruckOutlineButton
import com.pedro.baruck.ui.components.Mascot
import com.pedro.baruck.ui.theme.BaruckColors

/** Mostrada quando o app travou na última vez: deixa copiar o erro para enviar. */
@Composable
fun CrashScreen(report: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Color.White).systemBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Mascot(R.drawable.baruck_sad, 130.dp)
        Text("Ops, o Baruck tropeçou", style = MaterialTheme.typography.titleLarge)
        Text(
            "O app fechou da última vez. Copie o erro abaixo e mande para quem está te ajudando.",
            style = MaterialTheme.typography.bodyMedium, color = BaruckColors.InkSoft, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        SelectionContainer(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)).background(Color(0xFFF2F2F2))
                .verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Text(report, color = BaruckColors.Ink, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        BaruckButton("Copiar erro", Modifier.fillMaxWidth()) { clipboard.setText(AnnotatedString(report)) }
        Spacer(Modifier.height(8.dp))
        BaruckOutlineButton("Compartilhar", Modifier.fillMaxWidth()) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, report)
            context.startActivity(Intent.createChooser(send, "Enviar erro"))
        }
        Spacer(Modifier.height(8.dp))
        BaruckOutlineButton("Continuar", Modifier.fillMaxWidth(), color = BaruckColors.InkSoft, onClick = onContinue)
    }
}
