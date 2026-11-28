package com.drdisagree.xmorphclock.domain.model

data class Lyrics(
    val key: String,
    val lines: List<LyricLine>
) {
    fun indexAt(position: Long): Int {
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (lines[middle].start <= position) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return result
    }

    fun nextStart(position: Long): Long? = lines.getOrNull(indexAt(position) + 1)?.start
}
