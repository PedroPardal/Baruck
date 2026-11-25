package com.pedro.baruck.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.pedro.baruck.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BaruckViewModel(
    val repo: BaruckRepository = BaruckRepository()
) : ViewModel() {

    val farms: StateFlow<List<Farm>> =
        repo.farms().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friends: StateFlow<List<UserProfile>> =
        repo.friends().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendRequests: StateFlow<List<FriendRequest>> =
        repo.friendRequests().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inbox: StateFlow<List<InboxItem>> =
        repo.inbox().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun greenhouses(farmId: String) = repo.greenhouses(farmId)
    fun crops(farmId: String) = repo.crops(farmId)
    fun cropsOfGreenhouse(farmId: String, ghId: String) = repo.cropsOfGreenhouse(farmId, ghId)
    fun harvests(farmId: String) = repo.harvests(farmId)
    fun harvestsOfCrop(farmId: String, cropId: String) = repo.harvestsOfCrop(farmId, cropId)
    fun reminders(farmId: String) = repo.reminders(farmId)

    fun onReady() = viewModelScope.launch {
        runCatching {
            repo.ensureProfile()
            val token = FirebaseMessaging.getInstance().token.await()
            repo.saveToken(token)
        }
    }

    // atalhos de escrita (cada um numa corrotina)
    fun createFarm(name: String, loc: String) = viewModelScope.launch { repo.createFarm(name, loc) }
    fun deleteFarm(id: String) = viewModelScope.launch { repo.deleteFarm(id) }
    fun addGreenhouse(f: String, n: String, a: Double, note: String) =
        viewModelScope.launch { repo.addGreenhouse(f, n, a, note) }
    fun deleteGreenhouse(f: String, id: String) = viewModelScope.launch { repo.deleteGreenhouse(f, id) }
    fun addCrop(f: String, c: Crop) = viewModelScope.launch { repo.addCrop(f, c) }
    fun updateCrop(f: String, c: Crop) = viewModelScope.launch { repo.updateCrop(f, c) }
    fun deleteCrop(f: String, id: String) = viewModelScope.launch { repo.deleteCrop(f, id) }
    fun addHarvest(f: String, h: Harvest) = viewModelScope.launch { repo.addHarvest(f, h) }
    fun deleteHarvest(f: String, id: String) = viewModelScope.launch { repo.deleteHarvest(f, id) }
    fun addReminder(f: String, r: Reminder) = viewModelScope.launch { repo.addReminder(f, r) }
    fun setReminderDone(f: String, id: String, done: Boolean) =
        viewModelScope.launch { repo.setReminderDone(f, id, done) }
    fun deleteReminder(f: String, id: String) = viewModelScope.launch { repo.deleteReminder(f, id) }
    fun addMemberToFarm(f: String, u: UserProfile) = viewModelScope.launch { repo.addMemberToFarm(f, u) }
    fun removeMember(f: String, uid: String) = viewModelScope.launch { repo.removeMemberFromFarm(f, uid) }
    fun acceptFriend(r: FriendRequest) = viewModelScope.launch { repo.acceptFriend(r) }
    fun declineFriend(r: FriendRequest) = viewModelScope.launch { repo.declineFriend(r) }

    // busca de amigo por email (resultado via callback simples)
    fun findUser(email: String, onResult: (UserProfile?) -> Unit) = viewModelScope.launch {
        onResult(runCatching { repo.findUserByEmail(email) }.getOrNull())
    }
    fun sendFriendRequest(u: UserProfile, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        onDone(runCatching { repo.sendFriendRequest(u) }.isSuccess)
    }
}
