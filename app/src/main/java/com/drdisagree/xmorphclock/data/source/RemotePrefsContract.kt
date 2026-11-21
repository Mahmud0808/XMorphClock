package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

object RemotePrefsContract {
    const val AUTHORITY = "com.drdisagree.xmorphclock.prefs"
    const val MIRROR = "xmorph_remote"
    const val METHOD_READ = "read"
    const val VALUES = "values"
    const val TYPES = "types"
    const val REMOVED = "removed"
    const val COMPLETE = "complete"
    const val COMPLETE_MARKER = "__mirror_complete"
    const val TYPE_BOOLEAN = "b"
    const val TYPE_INT = "i"
    const val TYPE_LONG = "l"
    const val TYPE_FLOAT = "f"
    const val TYPE_STRING = "s"
    const val TYPE_STRING_SET = "S"
    private const val CHANGED = "changed"
    const val FILES = "files"
    private const val SHARED_DIR = "shared"

    val AUTHORITY_URI: Uri = "content://$AUTHORITY".toUri()
    val CHANGED_URI: Uri = AUTHORITY_URI.buildUpon().appendPath(CHANGED).build()

    fun fileUri(name: String): Uri = AUTHORITY_URI.buildUpon().appendPath(FILES).appendPath(name).build()

    fun sharedDir(context: Context): File = File(context.filesDir, SHARED_DIR)

    fun changedUri(key: String?): Uri = if (key == null) CHANGED_URI else CHANGED_URI.buildUpon().appendPath(key).build()
}
