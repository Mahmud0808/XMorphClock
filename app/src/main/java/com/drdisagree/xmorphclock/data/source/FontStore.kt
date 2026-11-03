package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.graphics.fonts.Font
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.UserFont
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class FontStore(private val context: Context) {

    private val dir = File(context.filesDir, DIR)
    private val index = File(dir, INDEX)
    private val changes = MutableStateFlow(0)

    val fonts: Flow<List<UserFont>> = changes.map { list() }.flowOn(Dispatchers.IO)

    init {
        XposedServiceSource.addBindListener { pushAll() }
    }

    fun open(id: String): ParcelFileDescriptor? =
        file(id).takeIf { it.isFile }?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }

    fun bytes(id: String): ByteArray? = file(id).takeIf { it.isFile }?.readBytes()

    fun name(id: String): String? = names()[id]

    suspend fun import(uri: String): UserFont? = withContext(Dispatchers.IO) {
        try {
            val target = uri.toUri()
            val data = context.contentResolver.openInputStream(target)?.use { it.readBytes() } ?: return@withContext null
            if (!isFont(data)) return@withContext null
            adopt(data, displayName(uri))
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    fun accepts(name: String, data: ByteArray): Boolean =
        name.removeSuffix(EXTENSION).let { it != name && ID.matches(it) } && isFont(data)

    @Synchronized
    fun adopt(data: ByteArray, name: String): UserFont {
        val id = idOf(data)
        val target = file(id)
        if (!target.isFile) {
            dir.mkdirs()
            target.writeBytes(data)
            push(id)
        }
        val stored = names()
        val label = stored[id] ?: name.ifBlank { context.getString(R.string.font_custom_name) }.also { saveNames(stored + (id to it)) }
        changes.value++
        return UserFont(id, label)
    }

    private fun list(): List<UserFont> {
        val labels = names()
        return dir.listFiles { file -> file.name.endsWith(EXTENSION) }.orEmpty()
            .map { file -> file.name.removeSuffix(EXTENSION).let { UserFont(it, labels[it] ?: context.getString(R.string.font_custom_name)) } }
            .sortedBy { it.name.lowercase() }
    }

    private fun displayName(uri: String): String = try {
        context.contentResolver.query(uri.toUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: SecurityException) {
        null
    }?.substringBeforeLast('.').orEmpty()

    private fun names(): Map<String, String> = try {
        if (!index.isFile) emptyMap() else JSONObject(index.readText()).let { json -> json.keys().asSequence().associateWith { json.getString(it) } }
    } catch (_: JSONException) {
        emptyMap()
    } catch (_: IOException) {
        emptyMap()
    }

    private fun saveNames(names: Map<String, String>) {
        dir.mkdirs()
        index.writeText(JSONObject(names).toString())
    }

    private fun isFont(data: ByteArray): Boolean {
        if (data.size < HEADER || data.size > MAX_BYTES) return false
        val tag = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.BIG_ENDIAN).int
        if (tag != TRUE_TYPE && tag != OPEN_TYPE && tag != APPLE_TRUE_TYPE) return false
        return try {
            Font.Builder(ByteBuffer.allocateDirect(data.size).put(data).also { it.flip() }).build()
            true
        } catch (_: IOException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun idOf(data: ByteArray): String =
        MessageDigest.getInstance(DIGEST).digest(data).take(ID_BYTES).joinToString("") { HEX.format(it) }

    private fun file(id: String): File = File(dir, "$id$EXTENSION")

    private fun push(id: String) {
        val source = file(id)
        XposedServiceSource.writeFile(FontContract.fileName(id)) { stream -> source.inputStream().use { it.copyTo(stream) } }
    }

    private fun pushAll() {
        dir.listFiles { file -> file.name.endsWith(EXTENSION) }.orEmpty().forEach { push(it.name.removeSuffix(EXTENSION)) }
    }

    private companion object {
        const val DIR = "user_fonts"
        const val INDEX = "names.json"
        const val EXTENSION = ".ttf"
        const val DIGEST = "SHA-256"
        const val HEX = "%02x"
        const val ID_BYTES = 8
        const val HEADER = 12
        const val MAX_BYTES = 24 * 1024 * 1024
        const val TRUE_TYPE = 0x00010000
        const val OPEN_TYPE = 0x4F54544F
        const val APPLE_TRUE_TYPE = 0x74727565
        val ID = Regex("[0-9a-f]{16}")
    }
}
