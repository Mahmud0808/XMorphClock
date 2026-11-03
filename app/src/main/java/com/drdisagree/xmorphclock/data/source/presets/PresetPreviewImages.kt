package com.drdisagree.xmorphclock.data.source.presets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class PresetPreviewImages(private val onEvict: (Collection<String>) -> Unit) {

    private val shown = ConcurrentHashMap<String, ByteArray>()

    fun show(images: Map<String, ByteArray>): Map<String, String> {
        clear()
        return images.mapValues { (_, bytes) -> previewId(bytes).also { shown[it] = bytes } }
    }

    fun load(id: String): Bitmap? = shown[id]?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    fun clear() {
        val ids = shown.keys.toList()
        shown.clear()
        if (ids.isNotEmpty()) onEvict(ids)
    }

    private fun previewId(bytes: ByteArray): String =
        PREFIX + MessageDigest.getInstance(DIGEST).digest(bytes).joinToString("") { HEX.format(it) }

    private companion object {
        const val PREFIX = "preview-"
        const val DIGEST = "SHA-256"
        const val HEX = "%02x"
    }
}
