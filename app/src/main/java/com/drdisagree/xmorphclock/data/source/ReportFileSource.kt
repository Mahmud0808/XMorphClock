package com.drdisagree.xmorphclock.data.source

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportFileSource(private val context: Context) {

    suspend fun write(target: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(target.toUri(), WRITE_TRUNCATE)?.use { stream ->
                stream.write(text.toByteArray())
                true
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    suspend fun systemLog(): String? = RootShell.read(LOGCAT_COMMAND) { it.readBytes().decodeToString() }
        ?.trim()
        ?.ifEmpty { null }
}

private const val WRITE_TRUNCATE = "wt"
private const val LOGCAT_COMMAND =
    "logcat -d -v threadtime -b main,system,crash 2>/dev/null | " +
        "grep -iE 'XMorphClock|LSPosed|Vector|libxposed|AndroidRuntime|FATAL|com\\.android\\.systemui' | tail -n 800"
