package com.pedro.baruck.notify

import android.content.Context
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Mostra notificação de mensagens novas nos grupos das lavouras e nas conversas. */
object ChatChecker {

    suspend fun check(ctx: Context, uid: String, db: FirebaseFirestore, farms: List<DocumentSnapshot>) {
        val prefs = ctx.getSharedPreferences("baruck_sync", Context.MODE_PRIVATE)
        val edit = prefs.edit()

        for (f in farms) {
            val at = f.getLong("lastMsgAt") ?: continue
            val from = f.getString("lastMsgFrom") ?: ""
            val key = "farmchat_${f.id}"
            val seen = prefs.getLong(key, -1L)
            @Suppress("UNCHECKED_CAST")
            val read = ((f.get("chatReads") as? Map<String, Any?>)?.get(uid) as? Number)?.toLong() ?: 0L
            if (seen >= 0 && at > seen && at > read && from != uid) {
                Notifier.show(
                    ctx, ("chat" + f.id).hashCode(),
                    "💬 ${f.getString("name") ?: "Lavoura"}",
                    "${f.getString("lastMsgName") ?: ""}: ${f.getString("lastMsgText") ?: ""}",
                    f.getString("lastMsgAvatar")
                )
            }
            edit.putLong(key, at)
        }

        val chats = db.collection("chats").whereArrayContains("members", uid).get().await()
        for (c in chats.documents) {
            val at = c.getLong("lastAt") ?: continue
            val from = c.getString("lastFrom") ?: ""
            val key = "dm_${c.id}"
            val seen = prefs.getLong(key, -1L)
            @Suppress("UNCHECKED_CAST")
            val read = ((c.get("reads") as? Map<String, Any?>)?.get(uid) as? Number)?.toLong() ?: 0L
            if (seen >= 0 && at > seen && at > read && from != uid) {
                Notifier.show(
                    ctx, ("dm" + c.id).hashCode(),
                    "💬 ${c.getString("lastName") ?: "Mensagem"}",
                    c.getString("lastText") ?: "",
                    c.getString("lastAvatar")
                )
            }
            edit.putLong(key, at)
        }
        edit.apply()
    }
}
