package com.drdisagree.xmorphclock.data.source

import java.security.MessageDigest

object LyricsContract {
    const val ACTION_STATE = "com.drdisagree.xmorphclock.action.LYRICS_STATE"
    const val ACTION_REQUEST = "com.drdisagree.xmorphclock.action.REQUEST_LYRICS_STATE"
    const val EXTRA_KEY = "key"
    const val EXTRA_TITLE = "title"
    const val EXTRA_ARTIST = "artist"
    const val EXTRA_ORIGIN = "origin"
    const val SENDER_PERMISSION = "android.permission.STATUS_BAR_SERVICE"
    const val EXTRA_ALBUM = "album"
    const val EXTRA_DURATION = "duration"

    fun fileName(key: String): String {
        val digest = MessageDigest.getInstance(HASH).digest(key.toByteArray(Charsets.UTF_8))
        return PREFIX + digest.take(HASH_BYTES).joinToString("") { "%02x".format(it) } + SUFFIX
    }

    private const val HASH = "SHA-256"
    private const val HASH_BYTES = 8
    private const val PREFIX = "lyrics_"
    private const val SUFFIX = ".json"
}
