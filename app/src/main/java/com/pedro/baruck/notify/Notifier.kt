package com.pedro.baruck.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.pedro.baruck.MainActivity
import com.pedro.baruck.R
import com.pedro.baruck.ui.Avatars

/** Monta as notificações do Baruck (ícone fofo + foto do bichinho de quem enviou). */
object Notifier {
    const val CHANNEL = "baruck_alerts"

    fun show(context: Context, id: Int, title: String, body: String, avatarKey: String? = null, intent: Intent? = null) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val open = PendingIntent.getActivity(
            context, id,
            intent ?: Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val large = runCatching {
            val res = if (avatarKey?.startsWith("@") == true) Avatars.sticker(avatarKey.drop(1)) else Avatars.res(avatarKey ?: "baruck")
            ContextCompat.getDrawable(context, res)?.toBitmap(192, 192)
        }.getOrNull()

        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF58CC02.toInt())
            .setLargeIcon(large)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}
