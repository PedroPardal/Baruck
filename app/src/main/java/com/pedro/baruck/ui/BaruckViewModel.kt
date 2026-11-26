package com.pedro.baruck.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.pedro.baruck.R
import com.pedro.baruck.auth.AuthManager
import com.pedro.baruck.data.*
import com.pedro.baruck.notify.SyncWorker
import com.pedro.baruck.notify.Updates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

sealed interface ProfileState {
    data object Loading : ProfileState
    data object SignedOut : ProfileState
    data object Missing : ProfileState
    data class Ready(val profile: UserProfile) : ProfileState
}

enum class NameStatus { IDLE, CHECKING, FREE, TAKEN, INVALID }

@OptIn(ExperimentalCoroutinesApi::class)
class BaruckViewModel(app: Application) : AndroidViewModel(app) {

    val repo = BaruckRepository()
    private val prefs = app.getSharedPreferences("baruck", Context.MODE_PRIVATE)

    // ---------- login e perfil ----------
    val uid: StateFlow<String?> = repo.authUid()
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.uid.ifBlank { null })

    val profileState: StateFlow<ProfileState> = uid.flatMapLatest { id ->
        if (id == null) flowOf(ProfileState.SignedOut)
        else repo.profile(id)
            .map { p -> if (p == null || p.username.isBlank()) ProfileState.Missing else ProfileState.Ready(p) }
            .onStart { emit(ProfileState.Loading) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileState.Loading)

    val me: StateFlow<UserProfile?> = profileState
        .map { (it as? ProfileState.Ready)?.profile }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val farms = uid.flatMapLatest { if (it == null) flowOf(emptyList()) else repo.farms(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val friends = uid.flatMapLatest { if (it == null) flowOf(emptyList()) else repo.friends(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val requests = uid.flatMapLatest { if (it == null) flowOf(emptyList()) else repo.friendRequests(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val inbox = uid.flatMapLatest { if (it == null) flowOf(emptyList()) else repo.inbox(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val threads = uid.flatMapLatest { if (it == null) flowOf(emptyList()) else repo.threads(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Quantas conversas têm mensagem nova. */
    val chatUnread: StateFlow<Int> = combine(farms, threads, uid) { f, t, me ->
        if (me == null) 0 else f.count { it.unreadFor(me) } + t.count { it.unreadFor(me) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun farm(id: String) = repo.farm(id)
    fun greenhouses(id: String) = repo.greenhouses(id)
    fun harvests(id: String) = repo.harvests(id)
    fun costs(id: String) = repo.costs(id)
    fun reminders(id: String) = repo.reminders(id)
    fun profiles(ids: List<String>) = repo.profiles(ids)

    // ---------- rascunho do perfil (antes do login) ----------
    var draftAvatar by mutableStateOf(Avatars.DEFAULT)
    var draftName by mutableStateOf("")
    var draftUsername by mutableStateOf("")
    var draftBio by mutableStateOf("")
    var nameStatus by mutableStateOf(NameStatus.IDLE)
    var busy by mutableStateOf(false)
    var authError by mutableStateOf<String?>(null)

    fun checkUsername(raw: String) {
        val u = repo.normalizeUsername(raw)
        if (u.isEmpty()) { nameStatus = NameStatus.IDLE; return }
        if (!repo.isValidUsername(u)) { nameStatus = NameStatus.INVALID; return }
        nameStatus = NameStatus.CHECKING
        viewModelScope.launch {
            nameStatus = runCatching { if (repo.isUsernameFree(u)) NameStatus.FREE else NameStatus.TAKEN }
                .getOrElse { NameStatus.FREE } // sem rede: confirma na hora de salvar
        }
    }

    /** Login com Google. Se [createAfter], já cria o perfil do rascunho. */
    fun signIn(context: Context, createAfter: Boolean) = viewModelScope.launch {
        busy = true; authError = null
        val r = AuthManager(context, context.getString(R.string.web_client_id)).signInWithGoogle()
        if (r.isFailure) {
            busy = false
            authError = "Não consegui entrar com o Google. Tente de novo."
            Sfx.play(Sfx.S.ERROR)
            return@launch
        }
        if (createAfter) createProfileNow()
        busy = false
    }

    fun saveDraftProfile() = viewModelScope.launch {
        busy = true; createProfileNow(); busy = false
    }

    private suspend fun createProfileNow() {
        runCatching { repo.createProfile(draftName, draftUsername, draftBio, draftAvatar) }
            .onSuccess { Sfx.play(Sfx.S.SUCCESS) }
            .onFailure {
                authError = it.message ?: "Não consegui salvar o perfil."
                Sfx.play(Sfx.S.ERROR)
            }
    }

    // ---------- atualizações do app ----------
    var update by mutableStateOf<Updates.Info?>(null)

    fun checkUpdate(manual: Boolean = false) = viewModelScope.launch {
        val info = Updates.available(getApplication())
        update = info
        if (manual && info == null) snack = "Você já está com a versão mais nova \u2705"
    }

    // ---------- chat ----------
    fun dmId(friendUid: String) = repo.dmId(repo.uid, friendUid)
    fun farmMessages(farmId: String) = repo.farmMessages(farmId)
    fun dmMessages(chatId: String) = repo.dmMessages(chatId)

    fun sendFarmMessage(farmId: String, text: String, sticker: String = "") = viewModelScope.launch {
        val p = me.value ?: return@launch
        runCatching { repo.sendFarmMessage(farmId, text, sticker, p) }
            .onFailure { snack = "Mensagem não enviada: ${it.message}"; Sfx.play(Sfx.S.ERROR) }
    }

    fun sendDm(friendUid: String, text: String, sticker: String = "") = viewModelScope.launch {
        val p = me.value ?: return@launch
        runCatching { repo.sendDm(dmId(friendUid), text, sticker, p) }
            .onFailure { snack = "Mensagem não enviada: ${it.message}"; Sfx.play(Sfx.S.ERROR) }
    }

    fun markFarmRead(farmId: String) = viewModelScope.launch { runCatching { repo.markFarmRead(farmId) } }
    fun markDmRead(friendUid: String) = viewModelScope.launch { runCatching { repo.markDmRead(dmId(friendUid)) } }

    /** Depois que o perfil está pronto: token de push e verificação periódica. */
    fun onReady() = viewModelScope.launch {
        SyncWorker.schedule(getApplication())
        checkUpdate()
        runCatching { repo.saveToken(FirebaseMessaging.getInstance().token.await()) }
    }

    fun signOut() {
        repo.signOut()
        draftName = ""; draftUsername = ""; draftBio = ""; draftAvatar = Avatars.DEFAULT
        nameStatus = NameStatus.IDLE
    }

    // ---------- XP, mensagens e níveis ----------
    var xpToast by mutableStateOf<Int?>(null)
    var levelUp by mutableStateOf<Int?>(null)
    var snack by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            me.filterNotNull().collect { p ->
                val key = "level_${p.uid}"
                val lvl = Xp.level(p.xp)
                val stored = prefs.getInt(key, -1)
                if (stored in 1 until lvl) {
                    delay(700)
                    levelUp = lvl
                    Sfx.play(Sfx.S.LEVEL_UP)
                }
                if (stored != lvl) prefs.edit().putInt(key, lvl).apply()
            }
        }
    }

    private fun award(amount: Int) = viewModelScope.launch {
        if (amount <= 0) return@launch
        runCatching { repo.addXp(amount) }.onSuccess {
            delay(250)
            xpToast = amount
            Sfx.play(Sfx.S.XP)
        }
    }

    /** Executa uma ação com som, XP e mensagem de erro amigável. */
    private fun act(xp: Int = 0, ok: String? = null, block: suspend () -> String?) = viewModelScope.launch {
        runCatching { block() }
            .onSuccess { msg ->
                Sfx.play(Sfx.S.SUCCESS)
                (msg ?: ok)?.let { snack = it }
                award(xp)
            }
            .onFailure {
                snack = "Ops: " + (it.message ?: "algo deu errado")
                Sfx.play(Sfx.S.ERROR)
            }
    }

    fun updateProfile(name: String, bio: String, avatar: String) =
        act(ok = "Perfil atualizado!") { repo.updateProfile(name, bio, avatar); null }

    fun createFarm(name: String, loc: String) = act(Xp.FARM) { repo.createFarm(name, loc); null }
    fun deleteFarm(id: String) = act(ok = "Lavoura apagada") { repo.deleteFarm(id); null }
    fun leaveFarm(id: String) = act(ok = "Você saiu da lavoura") { repo.removeMember(id, repo.uid); null }

    fun addGreenhouse(farmId: String, name: String, crop: CatalogCrop, variety: String, planted: Date?, area: Double) =
        act(Xp.GREENHOUSE) { repo.addGreenhouse(farmId, name, crop, variety, planted, area); null }

    fun changeCrop(farmId: String, gh: Greenhouse, crop: CatalogCrop, variety: String, planted: Date?) =
        act(ok = "Nova cultura: ${crop.name}") { repo.changeCrop(farmId, gh, crop, variety, planted); null }

    fun deleteGreenhouse(farmId: String, id: String) = act(ok = "Estufa apagada") { repo.deleteGreenhouse(farmId, id); null }

    fun addHarvest(farmId: String, gh: Greenhouse, date: Date, g: Int, m: Int, p: Int) =
        act(Xp.HARVEST) { repo.addHarvest(farmId, gh, date, g, m, p); null }

    fun setPrices(farmId: String, id: String, pg: Double, pm: Double, pp: Double) =
        act(Xp.PRICES, "Preços salvos") { repo.setHarvestPrices(farmId, id, pg, pm, pp); null }

    fun deleteHarvest(farmId: String, id: String) = act { repo.deleteHarvest(farmId, id); null }

    fun addCost(farmId: String, cat: String, desc: String, amount: Double, date: Date, ghId: String) =
        act(Xp.COST) { repo.addCost(farmId, cat, desc, amount, date, ghId); null }

    fun deleteCost(farmId: String, id: String) = act { repo.deleteCost(farmId, id); null }

    fun addReminder(farmId: String, title: String, due: Date) =
        act(Xp.REMINDER, "Lembrete criado") { repo.addReminder(farmId, title, due); null }

    fun setReminderDone(farmId: String, id: String, done: Boolean) = act { repo.setReminderDone(farmId, id, done); null }
    fun deleteReminder(farmId: String, id: String) = act { repo.deleteReminder(farmId, id); null }

    fun sendNotice(farm: Farm, title: String, body: String) = act(Xp.NOTICE) {
        val p = me.value ?: error("perfil não carregado")
        val n = repo.sendNotice(farm, title, body, p)
        if (n == 0) "Ainda não há outros membros nessa lavoura" else "Aviso enviado para $n pessoa(s) 📢"
    }

    fun addMember(farm: Farm, friend: UserProfile) = act(ok = "${friend.name} entrou na lavoura") {
        repo.addMember(farm, friend, me.value ?: error("perfil não carregado")); null
    }

    fun removeMember(farmId: String, memberUid: String) = act { repo.removeMember(farmId, memberUid); null }

    fun findUser(raw: String, onResult: (UserProfile?) -> Unit) = viewModelScope.launch {
        onResult(runCatching { repo.findByUsername(raw) }.getOrNull())
    }

    fun sendFriendRequest(target: UserProfile) = act(ok = "Pedido enviado para @${target.username}") {
        repo.sendFriendRequest(target, me.value ?: error("perfil não carregado")); null
    }

    fun accept(req: FriendRequest) = act(Xp.FRIEND, "Agora vocês são amigos!") { repo.acceptFriend(req); null }
    fun decline(req: FriendRequest) = act { repo.declineFriend(req); null }
    fun removeFriend(uid: String) = act(ok = "Amigo removido") { repo.removeFriend(uid); null }

    fun markRead(id: String) = viewModelScope.launch { runCatching { repo.markRead(id) } }
}
