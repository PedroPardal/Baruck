package com.pedro.baruck.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Perfil público do produtor. */
data class UserProfile(
    @DocumentId val uid: String = "",
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val avatar: String = "trator",
    val xp: Int = 0
)

/** Lavoura: reúne estufas, colheitas e custos. Pode ser compartilhada. */
data class Farm(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val ownerUid: String = "",
    val members: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Date? = null,
    // grupo de conversa da lavoura
    val lastMsgText: String = "",
    val lastMsgFrom: String = "",
    val lastMsgName: String = "",
    val lastMsgAvatar: String = "",
    val lastMsgAt: Long = 0L,
    val chatReads: Map<String, Long> = emptyMap()
) {
    fun unreadFor(uid: String) = lastMsgAt > 0 && lastMsgFrom != uid && lastMsgAt > (chatReads[uid] ?: 0L)
}

/** Estufa, já com a cultura atual (safra em andamento). */
data class Greenhouse(
    @DocumentId val id: String = "",
    val name: String = "",
    val areaM2: Double = 0.0,
    val cropId: String = "",
    val cropKey: String = "",
    val cropName: String = "",
    val variety: String = "",
    val plantedAt: Date? = null,
    @ServerTimestamp val createdAt: Date? = null
)

/** Histórico de safras (uma por cultura plantada em cada estufa). */
data class Crop(
    @DocumentId val id: String = "",
    val greenhouseId: String = "",
    val cropKey: String = "",
    val cropName: String = "",
    val variety: String = "",
    val plantedAt: Date? = null,
    val status: String = "ativa"
)

/** Colheita, dividida por tamanho de caixa. Preços podem vir depois. */
data class Harvest(
    @DocumentId val id: String = "",
    val greenhouseId: String = "",
    val cropId: String = "",
    val cropName: String = "",
    val date: Date? = null,
    val boxesGrande: Int = 0,
    val boxesMedia: Int = 0,
    val boxesMiuda: Int = 0,
    val priceGrande: Double = 0.0,
    val priceMedia: Double = 0.0,
    val priceMiuda: Double = 0.0,
    val priced: Boolean = false,
    val createdBy: String = ""
) {
    @get:Exclude val totalBoxes: Int get() = boxesGrande + boxesMedia + boxesMiuda
    @get:Exclude val revenue: Double
        get() = boxesGrande * priceGrande + boxesMedia * priceMedia + boxesMiuda * priceMiuda
}

/** Custo / despesa da lavoura. */
data class Cost(
    @DocumentId val id: String = "",
    val description: String = "",
    val category: String = "outros",
    val amount: Double = 0.0,
    val date: Date? = null,
    val greenhouseId: String = "",
    val createdBy: String = ""
)

object CostCategories {
    data class Cat(val key: String, val label: String, val emoji: String)

    val all = listOf(
        Cat("adubo", "Adubo", "🧪"),
        Cat("defensivo", "Defensivos", "🛡️"),
        Cat("mudas", "Sementes e mudas", "🌱"),
        Cat("mao_obra", "Mão de obra", "👷"),
        Cat("combustivel", "Combustível e frete", "⛽"),
        Cat("embalagem", "Caixas e embalagens", "📦"),
        Cat("agua_luz", "Água e luz", "💡"),
        Cat("manutencao", "Manutenção", "🔧"),
        Cat("outros", "Outros", "🧾")
    )

    fun of(key: String): Cat = all.firstOrNull { it.key == key } ?: all.last()
}

/** Lembrete com data, para todos os membros da lavoura. */
data class Reminder(
    @DocumentId val id: String = "",
    val farmId: String = "",
    val title: String = "",
    val dueAt: Date? = null,
    val done: Boolean = false,
    val createdBy: String = ""
)

data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromUsername: String = "",
    val fromAvatar: String = "trator",
    @ServerTimestamp val createdAt: Date? = null
)

/** Item de notificação do usuário (aviso do admin, convite, amizade...). */
data class InboxItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "",
    val farmId: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    @ServerTimestamp val createdAt: Date? = null,
    val read: Boolean = false
)

/** Conversa privada entre dois amigos (id = uids ordenados, unidos por "_"). */
data class ChatThread(
    @DocumentId val id: String = "",
    val members: List<String> = emptyList(),
    val lastText: String = "",
    val lastFrom: String = "",
    val lastName: String = "",
    val lastAvatar: String = "",
    val lastAt: Long = 0L,
    val reads: Map<String, Long> = emptyMap()
) {
    fun other(uid: String): String? = members.firstOrNull { it != uid }
    fun unreadFor(uid: String) = lastAt > 0 && lastFrom != uid && lastAt > (reads[uid] ?: 0L)
}

/** Mensagem de texto ou figurinha. */
data class ChatMessage(
    @DocumentId val id: String = "",
    val text: String = "",
    val sticker: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val fromAvatar: String = "",
    val at: Long = 0L
)
