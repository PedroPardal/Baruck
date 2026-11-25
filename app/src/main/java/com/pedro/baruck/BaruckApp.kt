package com.pedro.baruck

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BaruckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "baruck_alerts",
                "Alertas do Baruck",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Lembretes de safra, colheita e lavoura" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
