package com.pedro.baruck.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pedro.baruck.R

/** Efeitos sonoros curtos (toque, transição, sucesso, XP, nível, erro). */
object Sfx {
    enum class S(val res: Int, val vol: Float) {
        TAP(R.raw.sfx_tap, 0.5f),
        WHOOSH(R.raw.sfx_whoosh, 0.45f),
        SUCCESS(R.raw.sfx_success, 0.8f),
        XP(R.raw.sfx_xp, 0.7f),
        LEVEL_UP(R.raw.sfx_levelup, 0.9f),
        ERROR(R.raw.sfx_error, 0.7f)
    }

    private var pool: SoundPool? = null
    private val ids = mutableMapOf<S, Int>()
    private var prefs: android.content.SharedPreferences? = null

    var enabled: Boolean
        get() = prefs?.getBoolean("sound", true) ?: true
        set(v) { prefs?.edit()?.putBoolean("sound", v)?.apply() }

    fun init(context: Context) {
        if (pool != null) return
        prefs = context.getSharedPreferences("baruck", Context.MODE_PRIVATE)
        val p = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            ).build()
        S.values().forEach { ids[it] = p.load(context, it.res, 1) }
        pool = p
    }

    fun play(s: S) {
        if (!enabled) return
        val id = ids[s] ?: return
        pool?.play(id, s.vol, s.vol, 1, 0, 1f)
    }
}
