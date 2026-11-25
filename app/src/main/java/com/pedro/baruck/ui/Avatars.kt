package com.pedro.baruck.ui

import com.pedro.baruck.R

/** Animaizinhos disponíveis como foto de perfil. */
object Avatars {
    data class Avatar(val key: String, val name: String, val res: Int)

    val all = listOf(
        Avatar("baruck", "Baruck", R.drawable.avatar_baruck),
        Avatar("vaca", "Vaca", R.drawable.avatar_vaca),
        Avatar("porco", "Porco", R.drawable.avatar_porco),
        Avatar("galinha", "Galinha", R.drawable.avatar_galinha),
        Avatar("ovelha", "Ovelha", R.drawable.avatar_ovelha),
        Avatar("coelho", "Coelho", R.drawable.avatar_coelho),
        Avatar("gato", "Gato", R.drawable.avatar_gato),
        Avatar("cachorro", "Cachorro", R.drawable.avatar_cachorro),
        Avatar("pato", "Pato", R.drawable.avatar_pato),
        Avatar("sapo", "Sapo", R.drawable.avatar_sapo),
        Avatar("raposa", "Raposa", R.drawable.avatar_raposa),
        Avatar("urso", "Urso", R.drawable.avatar_urso),
        Avatar("panda", "Panda", R.drawable.avatar_panda),
        Avatar("cabra", "Cabra", R.drawable.avatar_cabra),
        Avatar("coruja", "Coruja", R.drawable.avatar_coruja)
    )

    fun res(key: String?): Int = all.firstOrNull { it.key == key }?.res ?: R.drawable.avatar_baruck

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
