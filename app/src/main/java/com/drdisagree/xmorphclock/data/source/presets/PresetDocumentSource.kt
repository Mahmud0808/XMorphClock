package com.drdisagree.xmorphclock.data.source.presets

import android.content.Context
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream

class PresetDocumentSource(private val context: Context) {

    fun read(source: String): ByteArray? = try {
        context.contentResolver.openInputStream(source.toUri())?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                if (output.size() > MAX_SIZE) return null
            }
            output.toByteArray()
        }
    } catch (_: Exception) {
        null
    }

    fun write(target: String, data: ByteArray): Boolean = try {
        context.contentResolver.openOutputStream(target.toUri(), WRITE_TRUNCATE)?.use { stream ->
            stream.write(data)
            true
        } ?: false
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_SIZE = 64 * 1024 * 1024
        const val WRITE_TRUNCATE = "wt"
    }
}
