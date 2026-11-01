package com.drdisagree.xmorphclock.engine

import android.graphics.Bitmap
import android.os.SystemClock

object FaceImages {

    private var loader: ((String) -> Bitmap?)? = null
    private val cache = HashMap<String, Bitmap>()
    private val misses = HashMap<String, Long>()

    fun init(loader: (String) -> Bitmap?) {
        this.loader = loader
        misses.clear()
    }

    fun get(id: String): Bitmap? {
        if (id.isEmpty()) return null
        cache[id]?.let { return it }
        val now = SystemClock.uptimeMillis()
        misses[id]?.let { if (now - it < RETRY_MS) return null }
        val bitmap = try {
            loader?.invoke(id)
        } catch (_: Throwable) {
            null
        }
        if (bitmap == null) {
            misses[id] = now
        } else {
            misses.remove(id)
            cache[id] = bitmap
        }
        return bitmap
    }

    private const val RETRY_MS = 5_000L
}
