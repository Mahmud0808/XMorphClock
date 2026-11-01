package com.drdisagree.xmorphclock.data.source.presets

import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object PresetKey {

    val secret: SecretKey by lazy {
        SecretKeySpec(MessageDigest.getInstance(DIGEST).digest(SEED.toByteArray(Charsets.UTF_8)), ALGORITHM)
    }

    private const val SEED = "com.drdisagree.xmorphclock/presets/v1/5f3c9a1e-7b24-4d8e-a6c1-0e92b7d4f318"
    private const val DIGEST = "SHA-256"
    private const val ALGORITHM = "AES"
}
