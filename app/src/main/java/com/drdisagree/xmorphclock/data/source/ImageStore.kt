package com.drdisagree.xmorphclock.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import com.drdisagree.xmorphclock.domain.model.FaceImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class ImageStore(private val context: Context) {

    private val dir = File(context.filesDir, DIR)

    init {
        XposedServiceSource.addBindListener { pushAll() }
    }

    fun load(id: String): Bitmap? = file(id).takeIf { it.isFile }?.let { BitmapFactory.decodeFile(it.path) }

    suspend fun import(uri: String): FaceImage? = withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, uri.toUri())
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = max(info.size.width, info.size.height)
                if (longest > MAX_SIDE) {
                    val ratio = MAX_SIDE.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * ratio).roundToInt().coerceAtLeast(1),
                        (info.size.height * ratio).roundToInt().coerceAtLeast(1)
                    )
                }
            }
            val id = newId()
            dir.mkdirs()
            file(id).outputStream().use { bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, QUALITY, it) }
            push(id)
            FaceImage(id, bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1))
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    suspend fun delete(ids: List<String>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            file(id).delete()
            XposedServiceSource.deleteFile(ImageContract.fileName(id))
        }
    }

    fun bytes(id: String): ByteArray? = file(id).takeIf { it.isFile }?.readBytes()

    fun accepts(name: String, data: ByteArray): Boolean {
        val id = name.removeSuffix(EXTENSION).takeIf { it != name } ?: return false
        if (!ID_PATTERN.matches(id)) return false
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        return bounds.outWidth > 0 && bounds.outHeight > 0
    }

    suspend fun adopt(id: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        val existing = file(id)
        if (existing.isFile && existing.readBytes().contentEquals(data)) return@withContext id
        val target = if (existing.exists()) newId() else id
        dir.mkdirs()
        file(target).writeBytes(data)
        push(target)
        target
    }

    private fun newId(): String = UUID.randomUUID().toString().replace(DASH, "").take(ID_LENGTH)

    private fun file(id: String): File = File(dir, "$id$EXTENSION")

    private fun push(id: String) {
        val source = file(id)
        if (!source.isFile) return
        XposedServiceSource.writeFile(ImageContract.fileName(id)) { output -> source.inputStream().use { it.copyTo(output) } }
    }

    private fun pushAll() {
        dir.listFiles()?.forEach { push(it.nameWithoutExtension) }
    }

    private companion object {
        const val DIR = "face_images"
        const val EXTENSION = ".webp"
        const val DASH = "-"
        const val ID_LENGTH = 12
        const val MAX_SIDE = 1024
        const val QUALITY = 92
        val ID_PATTERN = Regex("[A-Za-z0-9]{1,64}")
    }
}
