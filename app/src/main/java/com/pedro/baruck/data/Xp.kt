package com.pedro.baruck.data

/** Níveis e pontos de experiência do produtor. */
object Xp {
    const val FARM = 25
    const val GREENHOUSE = 30
    const val HARVEST = 20
    const val PRICES = 10
    const val COST = 5
    const val NOTICE = 5
    const val REMINDER = 5
    const val FRIEND = 15

    private val starts = listOf(0, 100, 250, 450, 700, 1000, 1400, 1900, 2500, 3200)
    private val titles = listOf(
        "Semente", "Broto", "Muda", "Plantador", "Cultivador",
        "Hortelão", "Agricultor", "Produtor", "Mestre da Estufa", "Lenda do Campo"
    )

    fun levelStart(level: Int): Int =
        if (level <= starts.size) starts[(level - 1).coerceAtLeast(0)]
        else starts.last() + (level - starts.size) * 800

    fun level(xp: Int): Int {
        var l = 1
        while (xp >= levelStart(l + 1)) l++
        return l
    }

    fun title(level: Int): String = titles[(level - 1).coerceIn(0, titles.lastIndex)]

    fun progress(xp: Int): Float {
        val l = level(xp)
        val a = levelStart(l)
        val b = levelStart(l + 1)
        return ((xp - a).toFloat() / (b - a)).coerceIn(0f, 1f)
    }

    fun toNext(xp: Int): Int = levelStart(level(xp) + 1) - xp
}
