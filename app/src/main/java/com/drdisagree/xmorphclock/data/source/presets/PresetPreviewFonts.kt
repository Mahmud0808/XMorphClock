package com.drdisagree.xmorphclock.data.source.presets

import android.content.Context
import android.os.ParcelFileDescriptor
import java.io.File

class PresetPreviewFonts(context: Context) {

    private val dir = File(context.cacheDir, DIR)

    @Synchronized
    fun show(fonts: Map<String, ByteArray>) {
        clear()
        if (fonts.isEmpty()) return
        dir.mkdirs()
        fonts.forEach { (id, bytes) -> File(dir, "$id$EXTENSION").writeBytes(bytes) }
    }

    fun open(id: String): ParcelFileDescriptor? =
        File(dir, "$id$EXTENSION").takeIf { it.isFile }?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }

    @Synchronized
    fun clear() {
        dir.deleteRecursively()
    }

    private companion object {
        const val DIR = "preview_fonts"
        const val EXTENSION = ".ttf"
    }
}
