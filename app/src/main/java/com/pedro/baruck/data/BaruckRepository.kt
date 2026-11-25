package com.pedro.baruck.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Toda a conversa com o Firebase fica aqui. */
@OptIn(ExperimentalCoroutinesApi::class)
class BaruckRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val uid: String get() = auth.currentUser?.uid ?: ""

    /** Emite o uid sempre que o login muda (null = deslogado). */
    fun authUid(): Flow<String?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(l)
        awaitClose { auth.removeAuthStateListener(l) }
    }

    fun signOut() = auth.signOut()

    // ---------------- Perfil ----------------

    /** Perfil do usuário; emite null se ainda não existe. Erros não emitem. */
    fun profile(userId: String): Flow<UserProfile?> = callbackFlow {
        val reg = db.collection("users").document(userId).addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            trySend(if (snap.exists()) runCatching { snap.toObject(UserProfile::class.java) }.getOrNull() else null)
        }
        awaitClose { reg.remove() }
    }

    fun normalizeUsername(raw: String) = raw.trim().removePrefix("@").lowercase()

    fun isValidUsername(u: String) = Regex("^[a-z0-9_.]{3,20}$").matches(u)

    /** Verifica se o @usuário está livre (leitura pública). */
    suspend fun isUsernameFree(raw: String): Boolean {
        val u = normalizeUsername(raw)
        if (!isValidUsername(u)) return false
        val doc = db.collection("usernames").document(u).get().await()
        return !doc.exists() || doc.getString("uid") == uid
    }

    /** Cria o perfil reservando o @usuário. Não sobrescreve perfil existente. */
    suspend fun createProfile(name: String, username: String, bio: String, avatar: String) {
        val me = uid.ifBlank { error("Sem login") }
        val u = normalizeUsername(username)
        require(isValidUsername(u)) { "Nome de usuário inválido" }
        db.runTransaction { tx ->
            val userRef = db.collection("users").document(me)
            val nameRef = db.collection("usernames").document(u)
            val existing = tx.get(userRef)
            val taken = tx.get(nameRef)
            if (!existing.exists()) {
                if (taken.exists() && taken.getString("uid") != me) error("@$u já está em uso")
                tx.set(nameRef, mapOf("uid" to me))
                tx.set(userRef, mapOf("name" to name.trim(), "username" to u, "bio" to bio.trim(),
                    "avatar" to avatar, "xp" to 0))
            }
            true
        }.await()
    }

    suspend fun updateProfile(name: String, bio: String, avatar: String) {
        db.collection("users").document(uid)
            .update(mapOf("name" to name.trim(), "bio" to bio.trim(), "avatar" to avatar)).await()
    }

    suspend fun addXp(amount: Int) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).update("xp", FieldValue.increment(amount.toLong())).await()
    }

    suspend fun saveToken(token: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).collection("tokens").document(token)
            .set(mapOf("createdAt" to FieldValue.serverTimestamp())).await()
    }

    /** Perfis ao vivo de uma lista de usuários. */
    fun profiles(ids: List<String>): Flow<List<UserProfile>> {
        val unique = ids.filter { it.isNotBlank() }.distinct()
        if (unique.isEmpty()) return flowOf(emptyList())
        val chunks = unique.chunked(10).map { chunk ->
            list<UserProfile> { db.collection("users").whereIn(FieldPath.documentId(), chunk) }
        }
        return combine(chunks) { arr -> arr.flatMap { it }.distinctBy { it.uid } }
    }

    // ---------------- Lavouras ----------------

    fun farms(userId: String): Flow<List<Farm>> =
        list<Farm> { db.collection("farms").whereArrayContains("members", userId) }
            .map { l -> l.distinctBy { it.id }.sortedBy { it.name.lowercase() } }

    fun farm(farmId: String): Flow<Farm?> = callbackFlow {
        val reg = runCatching {
            farmRef(farmId).addSnapshotListener { snap, _ ->
                trySend(snap?.takeIf { it.exists() }?.let { runCatching { it.toObject(Farm::class.java) }.getOrNull() })
            }
        }.getOrNull()
        awaitClose { reg?.remove() }
    }

    suspend fun createFarm(name: String, location: String) {
        db.collection("farms").document().set(
            mapOf("name" to name.trim(), "location" to location.trim(), "ownerUid" to uid,
                "members" to listOf(uid), "createdAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun deleteFarm(farmId: String) {
        farmRef(farmId).delete().await()
    }

    suspend fun addMember(farm: Farm, friend: UserProfile, me: UserProfile) {
        farmRef(farm.id).update("members", FieldValue.arrayUnion(friend.uid)).await()
        sendInbox(friend.uid, "Você entrou na lavoura ${farm.name} 🌱",
            "${me.name} adicionou você para gerenciar junto.", "farm_invite", farm.id, me)
    }

    suspend fun removeMember(farmId: String, memberUid: String) {
        farmRef(farmId).update("members", FieldValue.arrayRemove(memberUid)).await()
    }

    // ---------------- Estufas (com cultura) ----------------

    fun greenhouses(farmId: String): Flow<List<Greenhouse>> =
        list<Greenhouse> { farmRef(farmId).collection("greenhouses") }
            .map { l -> l.distinctBy { it.id }.sortedBy { it.name.lowercase() } }

    /** Cria estufa e a safra da cultura numa única gravação. */
    suspend fun addGreenhouse(farmId: String, name: String, crop: CatalogCrop, variety: String, plantedAt: Date?, area: Double) {
        val ghRef = farmRef(farmId).collection("greenhouses").document()
        val cropRef = farmRef(farmId).collection("crops").document()
        db.runBatch { b ->
            b.set(cropRef, mapOf("greenhouseId" to ghRef.id, "cropKey" to crop.key, "cropName" to crop.name,
                "variety" to variety.trim(), "plantedAt" to plantedAt, "status" to "ativa"))
            b.set(ghRef, mapOf("name" to name.trim(), "areaM2" to area, "cropId" to cropRef.id,
                "cropKey" to crop.key, "cropName" to crop.name, "variety" to variety.trim(),
                "plantedAt" to plantedAt, "createdAt" to FieldValue.serverTimestamp()))
        }.await()
    }

    /** Encerra a cultura atual e começa outra na mesma estufa. */
    suspend fun changeCrop(farmId: String, gh: Greenhouse, crop: CatalogCrop, variety: String, plantedAt: Date?) {
        val cropRef = farmRef(farmId).collection("crops").document()
        db.runBatch { b ->
            if (gh.cropId.isNotBlank())
                b.update(farmRef(farmId).collection("crops").document(gh.cropId), "status", "encerrada")
            b.set(cropRef, mapOf("greenhouseId" to gh.id, "cropKey" to crop.key, "cropName" to crop.name,
                "variety" to variety.trim(), "plantedAt" to plantedAt, "status" to "ativa"))
            b.update(farmRef(farmId).collection("greenhouses").document(gh.id), mapOf(
                "cropId" to cropRef.id, "cropKey" to crop.key, "cropName" to crop.name,
                "variety" to variety.trim(), "plantedAt" to plantedAt))
        }.await()
    }

    suspend fun deleteGreenhouse(farmId: String, ghId: String) {
        farmRef(farmId).collection("greenhouses").document(ghId).delete().await()
    }

    // ---------------- Colheitas ----------------

    fun harvests(farmId: String): Flow<List<Harvest>> =
        list<Harvest> { farmRef(farmId).collection("harvests") }
            .map { l -> l.distinctBy { it.id }.sortedByDescending { it.date?.time ?: 0L } }

    suspend fun addHarvest(farmId: String, gh: Greenhouse, date: Date, g: Int, m: Int, p: Int) {
        farmRef(farmId).collection("harvests").document().set(
            mapOf("greenhouseId" to gh.id, "cropId" to gh.cropId, "cropName" to gh.cropName, "date" to date,
                "boxesGrande" to g, "boxesMedia" to m, "boxesMiuda" to p,
                "priceGrande" to 0.0, "priceMedia" to 0.0, "priceMiuda" to 0.0,
                "priced" to false, "createdBy" to uid)
        ).await()
    }

    suspend fun setHarvestPrices(farmId: String, harvestId: String, pg: Double, pm: Double, pp: Double) {
        farmRef(farmId).collection("harvests").document(harvestId).update(
            mapOf("priceGrande" to pg, "priceMedia" to pm, "priceMiuda" to pp, "priced" to true)
        ).await()
    }

    suspend fun deleteHarvest(farmId: String, id: String) {
        farmRef(farmId).collection("harvests").document(id).delete().await()
    }

    // ---------------- Custos ----------------

    fun costs(farmId: String): Flow<List<Cost>> =
        list<Cost> { farmRef(farmId).collection("costs") }
            .map { l -> l.distinctBy { it.id }.sortedByDescending { it.date?.time ?: 0L } }

    suspend fun addCost(farmId: String, category: String, description: String, amount: Double, date: Date, ghId: String) {
        farmRef(farmId).collection("costs").document().set(
            mapOf("category" to category, "description" to description.trim(), "amount" to amount,
                "date" to date, "greenhouseId" to ghId, "createdBy" to uid)
        ).await()
    }

    suspend fun deleteCost(farmId: String, id: String) {
        farmRef(farmId).collection("costs").document(id).delete().await()
    }

    // ---------------- Lembretes e avisos ----------------

    fun reminders(farmId: String): Flow<List<Reminder>> =
        list<Reminder> { farmRef(farmId).collection("reminders") }
            .map { l -> l.distinctBy { it.id }.sortedBy { it.dueAt?.time ?: Long.MAX_VALUE } }

    suspend fun addReminder(farmId: String, title: String, dueAt: Date) {
        farmRef(farmId).collection("reminders").document().set(
            mapOf("farmId" to farmId, "title" to title.trim(), "dueAt" to dueAt, "done" to false, "createdBy" to uid)
        ).await()
    }

    suspend fun setReminderDone(farmId: String, id: String, done: Boolean) {
        farmRef(farmId).collection("reminders").document(id).update("done", done).await()
    }

    suspend fun deleteReminder(farmId: String, id: String) {
        farmRef(farmId).collection("reminders").document(id).delete().await()
    }

    /** Administrador envia um aviso para todos os outros membros. Retorna quantos receberam. */
    suspend fun sendNotice(farm: Farm, title: String, body: String, me: UserProfile): Int {
        val targets = farm.members.distinct().filter { it != uid }
        if (targets.isEmpty()) return 0
        db.runBatch { b ->
            targets.forEach { t ->
                b.set(db.collection("users").document(t).collection("inbox").document(),
                    inboxMap("📢 ${farm.name}: $title", body, "notice", farm.id, me))
            }
        }.await()
        return targets.size
    }

    // ---------------- Amigos ----------------

    fun friendIds(userId: String): Flow<List<String>> = callbackFlow {
        val reg = db.collection("users").document(userId).collection("friends")
            .addSnapshotListener { snap, _ -> trySend(snap?.documents?.map { it.id }?.distinct() ?: emptyList()) }
        awaitClose { reg.remove() }
    }

    fun friends(userId: String): Flow<List<UserProfile>> =
        friendIds(userId).flatMapLatest { ids -> profiles(ids.filter { it != userId }) }

    fun friendRequests(userId: String): Flow<List<FriendRequest>> =
        list<FriendRequest> { db.collection("users").document(userId).collection("friendRequests") }
            .map { l -> l.filter { it.fromUid != userId }.distinctBy { it.fromUid } }

    suspend fun findByUsername(raw: String): UserProfile? {
        val u = normalizeUsername(raw)
        if (!isValidUsername(u)) return null
        val owner = db.collection("usernames").document(u).get().await().getString("uid") ?: return null
        return db.collection("users").document(owner).get().await().toObject(UserProfile::class.java)
    }

    suspend fun sendFriendRequest(target: UserProfile, me: UserProfile) {
        require(target.uid != uid) { "Esse é você 😄" }
        db.collection("users").document(target.uid).collection("friendRequests").document(uid).set(
            mapOf("fromUid" to uid, "fromName" to me.name, "fromUsername" to me.username,
                "fromAvatar" to me.avatar, "createdAt" to FieldValue.serverTimestamp())
        ).await()
        sendInbox(target.uid, "Pedido de amizade 👋", "${me.name} (@${me.username}) quer ser seu amigo no Baruck.",
            "friend_request", "", me)
    }

    suspend fun acceptFriend(req: FriendRequest) {
        val me = uid
        db.runBatch { b ->
            b.set(db.collection("users").document(me).collection("friends").document(req.fromUid),
                mapOf("since" to FieldValue.serverTimestamp()))
            b.set(db.collection("users").document(req.fromUid).collection("friends").document(me),
                mapOf("since" to FieldValue.serverTimestamp()))
        }.await()
        db.collection("users").document(me).collection("friendRequests").document(req.fromUid).delete().await()
    }

    suspend fun declineFriend(req: FriendRequest) {
        db.collection("users").document(uid).collection("friendRequests").document(req.fromUid).delete().await()
    }

    suspend fun removeFriend(friendUid: String) {
        db.runBatch { b ->
            b.delete(db.collection("users").document(uid).collection("friends").document(friendUid))
            b.delete(db.collection("users").document(friendUid).collection("friends").document(uid))
        }.await()
    }

    // ---------------- Inbox ----------------

    fun inbox(userId: String): Flow<List<InboxItem>> =
        list<InboxItem> {
            db.collection("users").document(userId).collection("inbox")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
        }.map { l -> l.distinctBy { it.id } }

    suspend fun markRead(itemId: String) {
        db.collection("users").document(uid).collection("inbox").document(itemId).update("read", true).await()
    }

    private suspend fun sendInbox(to: String, title: String, body: String, type: String, farmId: String, me: UserProfile) {
        db.collection("users").document(to).collection("inbox").document()
            .set(inboxMap(title, body, type, farmId, me)).await()
    }

    private fun inboxMap(title: String, body: String, type: String, farmId: String, me: UserProfile) = mapOf(
        "title" to title, "body" to body, "type" to type, "farmId" to farmId,
        "fromName" to me.name, "fromAvatar" to me.avatar,
        "createdAt" to FieldValue.serverTimestamp(), "read" to false
    )

    // ---------------- helpers ----------------

    private fun farmRef(farmId: String) =
        db.collection("farms").document(farmId.ifBlank { error("lavoura vazia") })

    /** Consulta montada só na hora de observar; erro vira lista vazia. */
    private inline fun <reified T : Any> list(crossinline build: () -> Query): Flow<List<T>> = callbackFlow {
        val query = runCatching { build() }.getOrNull()
        if (query == null) {
            trySend(emptyList())
            awaitClose { }
        } else {
            val reg = query.addSnapshotListener { snap, _ ->
                trySend(runCatching { snap?.toObjects(T::class.java) }.getOrNull() ?: emptyList())
            }
            awaitClose { reg.remove() }
        }
    }
}
