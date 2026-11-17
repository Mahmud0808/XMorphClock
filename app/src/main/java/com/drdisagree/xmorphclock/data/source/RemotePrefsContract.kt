package com.drdisagree.xmorphclock.data.source

import android.net.Uri
import androidx.core.net.toUri

object RemotePrefsContract {
    const val AUTHORITY = "com.drdisagree.xmorphclock.prefs"
    const val MIRROR = "xmorph_remote"
    const val METHOD_READ = "read"
    const val VALUES = "values"
    const val TYPES = "types"
    const val REMOVED = "removed"
    const val TYPE_BOOLEAN = "b"
    const val TYPE_INT = "i"
    const val TYPE_LONG = "l"
    const val TYPE_FLOAT = "f"
    const val TYPE_STRING = "s"
    const val TYPE_STRING_SET = "S"
    private const val CHANGED = "changed"

    val AUTHORITY_URI: Uri = "content://$AUTHORITY".toUri()
    val CHANGED_URI: Uri = AUTHORITY_URI.buildUpon().appendPath(CHANGED).build()

    fun changedUri(key: String?): Uri = if (key == null) CHANGED_URI else CHANGED_URI.buildUpon().appendPath(key).build()
}
