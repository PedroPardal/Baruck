package com.pedro.baruck.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val ptBR = Locale("pt", "BR")
private val brl = NumberFormat.getCurrencyInstance(ptBR)
private val df = SimpleDateFormat("dd/MM/yyyy", ptBR)
private val dfShort = SimpleDateFormat("dd MMM", ptBR)
private val monthFmt = SimpleDateFormat("MMMM 'de' yyyy", ptBR)

fun money(v: Double): String = brl.format(v)
fun shortDate(d: Date?): String = d?.let { df.format(it) } ?: "—"
fun dayMonth(d: Date?): String = d?.let { dfShort.format(it) } ?: "—"
fun monthLabel(d: Date?): String = d?.let { monthFmt.format(it).replaceFirstChar { c -> c.uppercase() } } ?: ""

/** Aceita "12,50", "12.50", "1.234,50" e "R$ 12". */
fun parseMoney(raw: String): Double {
    var s = raw.replace("R$", "").replace(" ", "").trim()
    if (s.isEmpty()) return 0.0
    s = if (s.contains(',')) s.replace(".", "").replace(',', '.')
    else if (s.count { it == '.' } == 1 && s.substringAfter('.').length <= 2) s
    else s.replace(".", "")
    return s.toDoubleOrNull() ?: 0.0
}

/** Valor para pré-preencher campo de preço ("12,50"); vazio se zero. */
fun priceText(v: Double): String =
    if (v <= 0.0) "" else String.format(ptBR, "%.2f", v)

fun daysSince(d: Date?): Long? =
    d?.let { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.time) }

fun timeAgo(d: Date?): String {
    val ms = System.currentTimeMillis() - (d?.time ?: return "")
    val min = ms / 60000
    return when {
        min < 1 -> "agora"
        min < 60 -> "há $min min"
        min < 60 * 24 -> "há ${min / 60} h"
        min < 60 * 24 * 7 -> "há ${min / (60 * 24)} d"
        else -> shortDate(d)
    }
}

fun sameMonth(a: Date?, b: Date): Boolean {
    if (a == null) return false
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.MONTH) == cb.get(Calendar.MONTH)
}

fun sameYear(a: Date?, b: Date): Boolean {
    if (a == null) return false
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
}

private val hm = SimpleDateFormat("HH:mm", ptBR)
private val dm = SimpleDateFormat("dd/MM", ptBR)

/** Hora se for hoje; senão dia/mês. */
fun chatTime(ms: Long): String {
    if (ms <= 0) return ""
    val d = Date(ms)
    return if (sameDay(d, Date())) hm.format(d) else dm.format(d)
}

fun clock(ms: Long): String = hm.format(Date(ms))

fun sameDay(a: Date, b: Date): Boolean {
    val ca = Calendar.getInstance().apply { time = a }
    val cb = Calendar.getInstance().apply { time = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

fun dayLabel(ms: Long): String {
    val d = Date(ms)
    val y = Date(System.currentTimeMillis() - 24 * 3600 * 1000L)
    return when {
        sameDay(d, Date()) -> "Hoje"
        sameDay(d, y) -> "Ontem"
        else -> shortDate(d)
    }
}
