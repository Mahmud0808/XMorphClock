package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.domain.model.PresetFileError
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

object PresetContainer {

    const val VERSION = 1

    private val MAGIC = byteArrayOf(0x58, 0x4D, 0x43, 0x50)
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private const val TAG_SIZE = TAG_BITS / 8
    private const val UNSIGNED = 0xFF
    private val VERSION_AT = MAGIC.size
    private val IV_AT = VERSION_AT + 1
    private val HEADER_SIZE = IV_AT + IV_SIZE
    private val random = SecureRandom()

    fun seal(payload: ByteArray): ByteArray {
        val iv = ByteArray(IV_SIZE).also(random::nextBytes)
        val header = MAGIC + byteArrayOf(VERSION.toByte()) + iv
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, PresetKey.secret, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(header)
        return header + cipher.doFinal(payload)
    }

    fun open(data: ByteArray): ByteArray {
        if (data.size < MAGIC.size || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw PresetException(PresetFileError.NOT_PRESET_FILE)
        }
        if (data.size <= VERSION_AT) throw PresetException(PresetFileError.DAMAGED)
        if (data[VERSION_AT].toInt() and UNSIGNED != VERSION) throw PresetException(PresetFileError.UNSUPPORTED_VERSION)
        if (data.size < HEADER_SIZE + TAG_SIZE) throw PresetException(PresetFileError.DAMAGED)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, PresetKey.secret, GCMParameterSpec(TAG_BITS, data, IV_AT, IV_SIZE))
            cipher.updateAAD(data, 0, HEADER_SIZE)
            cipher.doFinal(data, HEADER_SIZE, data.size - HEADER_SIZE)
        } catch (_: GeneralSecurityException) {
            throw PresetException(PresetFileError.DAMAGED)
        }
    }
}
