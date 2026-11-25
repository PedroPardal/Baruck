package com.pedro.baruck.notify

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Confere no GitHub se saiu uma versão nova do app. */
object Updates {
    const val APK_URL = "https://github.com/PedroPardal/Baruck/releases/download/latest/Baruck.apk"
    private const val INFO_URL = "https://github.com/PedroPardal/Baruck/releases/download/latest/version.json"

    data class Info(val code: Long, val name: String, val notes: String)

    suspend fun fetch(): Info? = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(INFO_URL).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = true
            c.connectTimeout = 8000
            c.readTimeout = 8000
            c.setRequestProperty("Cache-Control", "no-cache")
            val text = c.inputStream.bufferedReader().use { it.readText() }
            val j = JSONObject(text)
            Info(j.getLong("versionCode"), j.optString("versionName"), j.optString("notes").trim())
        }.getOrNull()
    }

    fun installedCode(ctx: Context): Long = runCatching {
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0))
    }.getOrDefault(0L)

    fun installedName(ctx: Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    /** Versão nova disponível, ou null. */
    suspend fun available(ctx: Context): Info? {
        val info = fetch() ?: return null
        return if (info.code > installedCode(ctx)) info else null
    }

    fun downloadIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(APK_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun openDownload(ctx: Context) {
        runCatching { ctx.startActivity(downloadIntent()) }
    }

    /** Título curto das novidades (primeira linha do texto). */
    fun headline(info: Info): String = info.notes.lineSequence().firstOrNull { it.isNotBlank() } ?: "Melhorias no app"
}
