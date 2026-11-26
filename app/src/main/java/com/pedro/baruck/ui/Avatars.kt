package com.pedro.baruck.ui

import com.pedro.baruck.R

/** Ícones de perfil (coisas do campo). */
object Avatars {
    data class Avatar(val key: String, val name: String, val res: Int)

    const val DEFAULT = "trator"

    val all = listOf(
        Avatar("trator", "Trator", R.drawable.avatar_trator),
        Avatar("caminhao", "Caminhão", R.drawable.avatar_caminhao),
        Avatar("tomate", "Tomate", R.drawable.avatar_tomate),
        Avatar("milho", "Milho", R.drawable.avatar_milho),
        Avatar("alface", "Alface", R.drawable.avatar_alface),
        Avatar("pimentao", "Pimentão", R.drawable.avatar_pimentao),
        Avatar("cenoura", "Cenoura", R.drawable.avatar_cenoura),
        Avatar("morango", "Morango", R.drawable.avatar_morango),
        Avatar("abobora", "Abóbora", R.drawable.avatar_abobora),
        Avatar("estufa", "Estufa", R.drawable.avatar_estufa),
        Avatar("celeiro", "Celeiro", R.drawable.avatar_celeiro),
        Avatar("caixa", "Caixa de colheita", R.drawable.avatar_caixa),
        Avatar("regador", "Regador", R.drawable.avatar_regador),
        Avatar("chapeu", "Chapéu de palha", R.drawable.avatar_chapeu),
        Avatar("broto", "Broto", R.drawable.avatar_broto)
    )

    /** Chaves antigas (bichinhos) caem no padrão. */
    fun res(key: String?): Int = all.firstOrNull { it.key == key }?.res ?: R.drawable.avatar_trator

    /** Figurinhas do Baruck para o chat. */
    val stickers = listOf(
        "baruck_wave" to R.drawable.baruck_wave,
        "baruck_sun" to R.drawable.baruck_sun,
        "baruck_celebrate" to R.drawable.baruck_celebrate,
        "baruck_love" to R.drawable.baruck_love,
        "baruck_sit" to R.drawable.baruck_sit,
        "baruck_rain" to R.drawable.baruck_rain,
        "baruck_sleep" to R.drawable.baruck_sleep,
        "baruck_sad" to R.drawable.baruck_sad
    )

    fun sticker(key: String): Int = stickers.firstOrNull { it.first == key }?.second ?: R.drawable.baruck_wave
}
