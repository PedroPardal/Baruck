package com.pedro.baruck.ui

import com.pedro.baruck.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val brl = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
private val df = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))

fun money(v: Double): String = brl.format(v)
fun shortDate(d: Date?): String = d?.let { df.format(it) } ?: "—"

/** Poses do Baruck em rotação, para dar vida às telas. */
val baruckPoses = listOf(
    R.drawable.baruck_wave, R.drawable.baruck_stand, R.drawable.baruck_walk,
    R.drawable.baruck_run, R.drawable.baruck_sit, R.drawable.baruck_jump,
    R.drawable.baruck_head, R.drawable.baruck_head_side
)

fun baruckFor(seed: String): Int =
    baruckPoses[(seed.hashCode() and 0x7fffffff) % baruckPoses.size]
