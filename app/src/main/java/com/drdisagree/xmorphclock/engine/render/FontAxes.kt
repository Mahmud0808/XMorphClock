package com.drdisagree.xmorphclock.engine.render

internal object FontAxes {

    private const val TABLE_COUNT_OFFSET = 4
    private const val TABLE_DIRECTORY = 12
    private const val TABLE_RECORD = 16
    private const val FVAR = "fvar"
    private const val AXES_OFFSET = 4
    private const val AXIS_COUNT = 8
    private const val AXIS_SIZE = 10
    private const val WEIGHT = "wght"

    fun hasWeight(bytes: ByteArray): Boolean = has(bytes, WEIGHT)

    private fun has(bytes: ByteArray, tag: String): Boolean {
        if (bytes.size < TABLE_DIRECTORY) return false
        val tables = u16(bytes, TABLE_COUNT_OFFSET)
        for (index in 0 until tables) {
            val record = TABLE_DIRECTORY + index * TABLE_RECORD
            if (record + TABLE_RECORD > bytes.size) return false
            if (tag(bytes, record) != FVAR) continue
            val fvar = u32(bytes, record + 8)
            if (fvar + AXIS_SIZE + 2 > bytes.size) return false
            val axesStart = fvar + u16(bytes, fvar + AXES_OFFSET)
            val count = u16(bytes, fvar + AXIS_COUNT)
            val size = u16(bytes, fvar + AXIS_SIZE)
            for (axis in 0 until count) {
                val at = axesStart + axis * size
                if (at + 4 > bytes.size) return false
                if (tag(bytes, at) == tag) return true
            }
            return false
        }
        return false
    }

    private fun tag(bytes: ByteArray, at: Int): String = String(CharArray(4) { (bytes[at + it].toInt() and 0xFF).toChar() })

    private fun u16(bytes: ByteArray, at: Int): Int =
        ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)

    private fun u32(bytes: ByteArray, at: Int): Int =
        ((bytes[at].toInt() and 0xFF) shl 24) or ((bytes[at + 1].toInt() and 0xFF) shl 16) or
            ((bytes[at + 2].toInt() and 0xFF) shl 8) or (bytes[at + 3].toInt() and 0xFF)
}
