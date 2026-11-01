package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.domain.model.PresetFileError

class PresetArchive(
    val createdAt: Long,
    val appVersion: String,
    val files: Map<String, ByteArray>
) {
    fun filesIn(dir: String): Map<String, ByteArray> {
        val prefix = "$dir$SEPARATOR"
        return files.filterKeys { it.startsWith(prefix) }.mapKeys { (path, _) ->
            path.removePrefix(prefix).also { if (it.isEmpty() || SEPARATOR in it) throw PresetException(PresetFileError.DAMAGED) }
        }
    }

    companion object {
        const val SEPARATOR = '/'
    }
}
