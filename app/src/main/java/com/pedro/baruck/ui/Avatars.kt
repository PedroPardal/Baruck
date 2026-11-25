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
}
