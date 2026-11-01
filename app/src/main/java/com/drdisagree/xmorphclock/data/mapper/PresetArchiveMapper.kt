package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.data.source.presets.PresetArchive
import com.drdisagree.xmorphclock.data.source.presets.PresetException
import com.drdisagree.xmorphclock.domain.model.PresetFileError
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONException
import org.json.JSONObject

object PresetArchiveMapper {

    const val FORMAT = 1

    private const val MANIFEST = "manifest.json"
    private const val FILES = "files/"
    private const val KEY_FORMAT = "format"
    private const val KEY_CREATED = "created"
    private const val KEY_APP = "app"
    private const val MAX_ENTRIES = 2048
    private const val MAX_TOTAL = 128L * 1024 * 1024
    private const val BUFFER_SIZE = 16 * 1024
    private const val PARENT = ".."
    private const val CURRENT = "."
    private const val BACKSLASH = '\\'

    fun toZip(archive: PresetArchive): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest(archive).toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            archive.files.toSortedMap().forEach { (path, bytes) ->
                require(isSafe(path))
                zip.putNextEntry(ZipEntry(FILES + path))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    fun fromZip(data: ByteArray): PresetArchive = try {
        var manifest: JSONObject? = null
        val files = LinkedHashMap<String, ByteArray>()
        var total = 0L
        var count = 0
        ZipInputStream(ByteArrayInputStream(data)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > MAX_ENTRIES) throw PresetException(PresetFileError.DAMAGED)
                if (entry.isDirectory) continue
                val bytes = readEntry(zip, MAX_TOTAL - total)
                total += bytes.size
                when {
                    entry.name == MANIFEST -> manifest = JSONObject(bytes.toString(Charsets.UTF_8))
                    entry.name.startsWith(FILES) -> {
                        val path = entry.name.removePrefix(FILES)
                        if (!isSafe(path) || files.put(path, bytes) != null) throw PresetException(PresetFileError.DAMAGED)
                    }
                }
            }
        }
        parse(manifest ?: throw PresetException(PresetFileError.DAMAGED), files)
    } catch (_: JSONException) {
        throw PresetException(PresetFileError.DAMAGED)
    } catch (_: IOException) {
        throw PresetException(PresetFileError.DAMAGED)
    } catch (_: IllegalArgumentException) {
        throw PresetException(PresetFileError.DAMAGED)
    }

    private fun manifest(archive: PresetArchive): JSONObject = JSONObject()
        .put(KEY_FORMAT, FORMAT)
        .put(KEY_CREATED, archive.createdAt)
        .put(KEY_APP, archive.appVersion)

    private fun parse(json: JSONObject, files: Map<String, ByteArray>): PresetArchive {
        if (json.getInt(KEY_FORMAT) != FORMAT) throw PresetException(PresetFileError.UNSUPPORTED_VERSION)
        return PresetArchive(json.optLong(KEY_CREATED), json.optString(KEY_APP), files)
    }

    private fun readEntry(input: InputStream, budget: Long): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            if (output.size() > budget) throw PresetException(PresetFileError.DAMAGED)
        }
        return output.toByteArray()
    }

    private fun isSafe(path: String): Boolean =
        path.isNotEmpty() && !path.startsWith(PresetArchive.SEPARATOR) && BACKSLASH !in path &&
            path.split(PresetArchive.SEPARATOR).none { it.isEmpty() || it == PARENT || it == CURRENT }
}
