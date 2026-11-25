package com.pedro.baruck.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Tamanhos de caixa usados na divisão da colheita. */
enum class BoxSize(val label: String) {
    GRANDE("Caixa grande"),
    MEDIA("Caixa média"),
    MIUDA("Caixa miúda")
}

/** Perfil público do usuário (sistema de amigos). */
data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val handle: String = "" // email em minúsculas, para busca
)

/** Lavoura: a unidade compartilhável. Reúne estufas, safras e colheitas. */
data class Farm(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val ownerUid: String = "",
    val members: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    @ServerTimestamp val createdAt: Date? = null
)

/** Estufa dentro de uma lavoura. */
data class Greenhouse(
    @DocumentId val id: String = "",
    val name: String = "",
    val areaM2: Double = 0.0,
    val note: String = ""
)

/** Safra: um ciclo de uma cultura, normalmente dentro de uma estufa. */
data class Crop(
    @DocumentId val id: String = "",
    val greenhouseId: String = "",
    val cropKey: String = "",      // chave do catálogo (ex.: "tomate")
    val cropName: String = "",     // nome exibido
    val variety: String = "",
    val plantedAt: Date? = null,
    val expectedHarvestAt: Date? = null,
    val status: String = "ativa",  // ativa | colhida | encerrada
    val note: String = ""
)

/** Colheita: produção de uma safra, dividida por tamanho de caixa. */
data class Harvest(
    @DocumentId val id: String = "",
    val cropId: String = "",
    val cropName: String = "",
    val date: Date? = null,
    val boxesGrande: Int = 0,
    val boxesMedia: Int = 0,
    val boxesMiuda: Int = 0,
    val priceGrande: Double = 0.0,
    val priceMedia: Double = 0.0,
    val priceMiuda: Double = 0.0,
    val note: String = "",
    val createdBy: String = ""
) {
    @get:Exclude val totalBoxes: Int get() = boxesGrande + boxesMedia + boxesMiuda
    @get:Exclude val revenue: Double
        get() = boxesGrande * priceGrande + boxesMedia * priceMedia + boxesMiuda * priceMiuda
}

/** Lembrete / notificação personalizada ligada a uma lavoura. */
data class Reminder(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val title: String = "",
    val body: String = "",
    val dueAt: Date? = null,
    val done: Boolean = false,
    val notified: Boolean = false,
    val targets: List<String> = emptyList(), // uids que recebem o push
    val createdBy: String = ""
)

/** Pedido de amizade recebido. */
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromEmail: String = "",
    @ServerTimestamp val createdAt: Date? = null
)

/** Item do inbox de notificações do usuário. */
data class InboxItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    val read: Boolean = false
)
