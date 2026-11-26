package com.pedro.baruck.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * A cada ~15 min (com internet) verifica avisos novos no inbox e lembretes
 * vencidos das lavouras, e mostra notificação. Funciona sem Cloud Functions.
 */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val db = FirebaseFirestore.getInstance()
        val prefs = applicationContext.getSharedPreferences("baruck_sync", Context.MODE_PRIVATE)
        return try {
            // 1) avisos novos no inbox
            val now = System.currentTimeMillis()
            val last = prefs.getLong("inbox_$uid", 0L)
            if (last == 0L) {
                prefs.edit().putLong("inbox_$uid", now).apply()
            } else {
                val snap = db.collection("users").document(uid).collection("inbox")
                    .whereGreaterThan("createdAt", Timestamp(Date(last)))
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .limit(20).get().await()
                var newest = last
                for (d in snap.documents) {
                    val created = d.getTimestamp("createdAt")?.toDate()?.time ?: continue
                    newest = maxOf(newest, created)
                    if (d.getBoolean("read") == true) continue
                    Notifier.show(
                        applicationContext, d.id.hashCode(),
                        d.getString("title") ?: "Baruck",
                        d.getString("body") ?: "",
                        d.getString("fromAvatar")
                    )
                }
                prefs.edit().putLong("inbox_$uid", newest).apply()
            }

            // 2) lembretes vencidos das lavouras
            val shown = prefs.getStringSet("reminders_shown", emptySet())!!.toMutableSet()
            val farms = db.collection("farms").whereArrayContains("members", uid).get().await()
            for (farm in farms.documents) {
                val farmName = farm.getString("name") ?: "Lavoura"
                val rems = farm.reference.collection("reminders").whereEqualTo("done", false).get().await()
                for (r in rems.documents) {
                    val due = r.getTimestamp("dueAt")?.toDate()?.time ?: continue
                    if (due > now || r.id in shown) continue
                    Notifier.show(
                        applicationContext, r.id.hashCode(),
                        "⏰ Lembrete: ${r.getString("title") ?: ""}",
                        "Lavoura $farmName", "regador"
                    )
                    shown += r.id
                }
            }
            prefs.edit().putStringSet("reminders_shown", shown).apply()

            // 3) mensagens novas nos chats
            ChatChecker.check(applicationContext, uid, db, farms.documents)

            // 4) versão nova do app (avisa uma vez por versão)
            Updates.available(applicationContext)?.let { info ->
                if (prefs.getLong("update_notified", 0L) < info.code) {
                    Notifier.show(
                        applicationContext, 7001,
                        "\uD83C\uDF89 Baruck ${info.name} chegou!",
                        Updates.headline(info) + "\nToque para baixar a atualização.",
                        "@baruck_celebrate", Updates.downloadIntent()
                    )
                    prefs.edit().putLong("update_notified", info.code).apply()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("baruck_sync", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
