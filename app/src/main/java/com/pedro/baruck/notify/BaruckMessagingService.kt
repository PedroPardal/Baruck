package com.pedro.baruck.notify

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Push via FCM (quando as Cloud Functions estiverem publicadas). */
class BaruckMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("tokens").document(token)
            .set(mapOf("createdAt" to FieldValue.serverTimestamp()))
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Baruck"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val id = message.data["inboxId"]?.hashCode() ?: System.currentTimeMillis().toInt()
        Notifier.show(this, id, title, body, message.data["fromAvatar"])
    }
}
