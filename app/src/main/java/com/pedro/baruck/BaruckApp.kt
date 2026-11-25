package com.pedro.baruck

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import java.io.File

class BaruckApp : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashRecorder()
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

    /** Guarda o erro de um travamento para mostrar na próxima abertura do app. */
    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(filesDir, CRASH_FILE).writeText(
                    "${versionTag(this)}\n" +
                    "Aparelho: ${Build.MANUFACTURER} ${Build.MODEL} " +
                        "(Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})\n\n" +
                        error.stackTraceToString()
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"

        @Suppress("DEPRECATION")
        fun versionTag(ctx: android.content.Context): String {
            val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            return "Versão: ${info.versionName} (build ${info.versionCode})"
        }
    }
}
