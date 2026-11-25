package com.pedro.baruck.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Toda a conversa com o Firestore fica aqui. As telas observam os
 * [Flow]s e chamam as funções suspend para gravar.
 */
class BaruckRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val uid: String get() = auth.currentUser?.uid ?: ""

    // ---------- Perfil / usuários ----------
    suspend fun ensureProfile() {
        val u = auth.currentUser ?: return
        val profile = UserProfile(
            uid = u.uid,
            name = u.displayName ?: "Produtor",
            email = u.email ?: "",
            photoUrl = u.photoUrl?.toString() ?: "",
            handle = (u.email ?: "").lowercase()
        )
        db.collection("users").document(u.uid).set(profile).await()
    }

    suspend fun saveToken(token: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .collection("tokens").document(token)
            .set(mapOf("createdAt" to FieldValue.serverTimestamp())).await()
    }

    // ---------- Lavouras ----------
    fun farms(): Flow<List<Farm>> = callbackFlow {
        val reg = db.collection("farms")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Farm::class.java) ?: emptyList())
            }
        awaitClose { reg.remove() }
    }

    suspend fun createFarm(name: String, location: String): String {
        val me = auth.currentUser!!
        val doc = db.collection("farms").document()
        val farm = Farm(
            id = doc.id, name = name, location = location,
            ownerUid = me.uid, members = listOf(me.uid),
            memberNames = mapOf(me.uid to (me.displayName ?: "Produtor"))
        )
        doc.set(farm).await()
        return doc.id
    }

    suspend fun deleteFarm(farmId: String) {
        db.collection("farms").document(farmId).delete().await()
    }

    // ---------- Estufas ----------
    fun greenhouses(farmId: String): Flow<List<Greenhouse>> =
        collectionFlow(farmPath(farmId).collection("greenhouses").orderBy("name"))

    suspend fun addGreenhouse(farmId: String, name: String, areaM2: Double, note: String) {
        val doc = farmPath(farmId).collection("greenhouses").document()
        doc.set(Greenhouse(doc.id, name, areaM2, note)).await()
    }

    suspend fun deleteGreenhouse(farmId: String, ghId: String) {
        farmPath(farmId).collection("greenhouses").document(ghId).delete().await()
    }

    // ---------- Safras ----------
    fun crops(farmId: String): Flow<List<Crop>> =
        collectionFlow(farmPath(farmId).collection("crops"))

    fun cropsOfGreenhouse(farmId: String, ghId: String): Flow<List<Crop>> =
        collectionFlow(
            farmPath(farmId).collection("crops").whereEqualTo("greenhouseId", ghId)
        )

    suspend fun addCrop(farmId: String, crop: Crop) {
        val doc = farmPath(farmId).collection("crops").document()
        doc.set(crop.copy(id = doc.id)).await()
    }

    suspend fun updateCrop(farmId: String, crop: Crop) {
        farmPath(farmId).collection("crops").document(crop.id).set(crop).await()
    }

    suspend fun deleteCrop(farmId: String, cropId: String) {
        farmPath(farmId).collection("crops").document(cropId).delete().await()
    }

    // ---------- Colheitas ----------
    fun harvests(farmId: String): Flow<List<Harvest>> =
        collectionFlow(
            farmPath(farmId).collection("harvests").orderBy("date", Query.Direction.DESCENDING)
        )

    fun harvestsOfCrop(farmId: String, cropId: String): Flow<List<Harvest>> =
        collectionFlow(
            farmPath(farmId).collection("harvests").whereEqualTo("cropId", cropId)
        )

    suspend fun addHarvest(farmId: String, harvest: Harvest) {
        val doc = farmPath(farmId).collection("harvests").document()
        doc.set(harvest.copy(id = doc.id, createdBy = uid)).await()
    }

    suspend fun deleteHarvest(farmId: String, harvestId: String) {
        farmPath(farmId).collection("harvests").document(harvestId).delete().await()
    }

    // ---------- Lembretes / notificações ----------
    fun reminders(farmId: String): Flow<List<Reminder>> =
        collectionFlow(
            farmPath(farmId).collection("reminders").orderBy("dueAt")
        )

    suspend fun addReminder(farmId: String, reminder: Reminder) {
        val doc = farmPath(farmId).collection("reminders").document()
        doc.set(reminder.copy(id = doc.id, farmId = farmId, createdBy = uid)).await()
    }

    suspend fun setReminderDone(farmId: String, reminderId: String, done: Boolean) {
        farmPath(farmId).collection("reminders").document(reminderId)
            .update("done", done).await()
    }

    suspend fun deleteReminder(farmId: String, reminderId: String) {
        farmPath(farmId).collection("reminders").document(reminderId).delete().await()
    }

    // ---------- Amigos ----------
    fun friends(): Flow<List<UserProfile>> =
        collectionFlow(db.collection("users").document(uid).collection("friends"))

    fun friendRequests(): Flow<List<FriendRequest>> =
        collectionFlow(db.collection("users").document(uid).collection("friendRequests"))

    suspend fun findUserByEmail(email: String): UserProfile? {
        val snap = db.collection("users")
            .whereEqualTo("handle", email.trim().lowercase())
            .limit(1).get().await()
        return snap.documents.firstOrNull()?.toObject(UserProfile::class.java)
    }

    suspend fun sendFriendRequest(target: UserProfile) {
        val me = auth.currentUser!!
        db.collection("users").document(target.uid)
            .collection("friendRequests").document(me.uid)
            .set(
                FriendRequest(
                    fromUid = me.uid,
                    fromName = me.displayName ?: "Produtor",
                    fromEmail = me.email ?: ""
                )
            ).await()
    }

    /** Aceita um pedido: vira amizade mútua e apaga o pedido. */
    suspend fun acceptFriend(req: FriendRequest) {
        val me = auth.currentUser!!
        val meProfile = UserProfile(me.uid, me.displayName ?: "Produtor", me.email ?: "", "", (me.email ?: "").lowercase())
        val other = db.collection("users").document(req.fromUid).get().await()
            .toObject(UserProfile::class.java) ?: UserProfile(req.fromUid, req.fromName, req.fromEmail)
        db.collection("users").document(me.uid).collection("friends").document(other.uid).set(other).await()
        db.collection("users").document(other.uid).collection("friends").document(me.uid).set(meProfile).await()
        db.collection("users").document(me.uid).collection("friendRequests").document(req.fromUid).delete().await()
    }

    suspend fun declineFriend(req: FriendRequest) {
        db.collection("users").document(uid)
            .collection("friendRequests").document(req.fromUid).delete().await()
    }

    /** Adiciona um amigo como membro de uma lavoura. */
    suspend fun addMemberToFarm(farmId: String, friend: UserProfile) {
        farmPath(farmId).update(
            mapOf(
                "members" to FieldValue.arrayUnion(friend.uid),
                "memberNames.${friend.uid}" to friend.name
            )
        ).await()
        // avisa o amigo
        db.collection("users").document(friend.uid).collection("inbox").document()
            .set(
                mapOf(
                    "title" to "Você entrou numa lavoura",
                    "body" to "Agora você ajuda a gerenciar uma lavoura no Baruck",
                    "type" to "farm_invite",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "read" to false
                )
            ).await()
    }

    suspend fun removeMemberFromFarm(farmId: String, memberUid: String) {
        farmPath(farmId).update(
            mapOf(
                "members" to FieldValue.arrayRemove(memberUid),
                "memberNames.$memberUid" to FieldValue.delete()
            )
        ).await()
    }

    // ---------- Inbox ----------
    fun inbox(): Flow<List<InboxItem>> =
        collectionFlow(
            db.collection("users").document(uid).collection("inbox")
                .orderBy("createdAt", Query.Direction.DESCENDING)
        )

    suspend fun markInboxRead(itemId: String) {
        db.collection("users").document(uid).collection("inbox")
            .document(itemId).update("read", true).await()
    }

    // ---------- helpers ----------
    private fun farmPath(farmId: String) = db.collection("farms").document(farmId)

    private inline fun <reified T : Any> collectionFlow(query: Query): Flow<List<T>> = callbackFlow {
        val reg = query.addSnapshotListener { snap, _ ->
            trySend(snap?.toObjects(T::class.java) ?: emptyList())
        }
        awaitClose { reg.remove() }
    }
}
