package com.drdisagree.xmorphclock.data.repository

import android.graphics.Bitmap
import androidx.core.graphics.scale
import com.drdisagree.xmorphclock.data.source.FrameZoom
import com.drdisagree.xmorphclock.data.source.depth.DepthStore
import com.drdisagree.xmorphclock.data.source.depth.SubjectSegmenter
import com.drdisagree.xmorphclock.domain.model.DepthModelState
import com.drdisagree.xmorphclock.domain.model.DepthStatus
import java.nio.ByteBuffer
import java.util.zip.CRC32
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DepthProcessor(
    private val store: DepthStore,
    private val segmenter: SubjectSegmenter
) {

    private val lock = Mutex()

    suspend fun process(frame: Bitmap?, scale: Float, key: String? = null, force: Boolean = false) = withContext(Dispatchers.Default) {
        lock.withLock {
            if (!store.settings.value.enabled) return@withLock
            val input = frame?.let(::limited) ?: return@withLock store.setStatus(DepthStatus.WAITING)
            val frameKey = key ?: keyOf(input)
            if (!force && frameKey == store.key) {
                store.loadPreview()
                return@withLock
            }
            store.setStatus(DepthStatus.EXTRACTING)
            val subject = segmenter.subject(input)
            when {
                subject != null -> store.save(frameKey, FrameZoom.zoomed(subject, scale), subject)
                segmenter.state.value == DepthModelState.UNAVAILABLE -> {
                    store.clear()
                    store.setStatus(DepthStatus.UNAVAILABLE)
                }
                else -> {
                    store.clear()
                    store.setStatus(DepthStatus.NO_SUBJECT)
                }
            }
        }
    }

    private fun limited(bitmap: Bitmap): Bitmap {
        val source = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val longest = max(source.width, source.height)
        if (longest <= MAX_SIDE) return source
        val ratio = MAX_SIDE.toFloat() / longest
        return source.scale((source.width * ratio).roundToInt().coerceAtLeast(1), (source.height * ratio).roundToInt().coerceAtLeast(1))
    }

    private fun keyOf(bitmap: Bitmap): String {
        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)
        return "${bitmap.width}x${bitmap.height}:" + CRC32().apply { update(buffer.array()) }.value
    }

    private companion object {
        const val MAX_SIDE = 1600
    }
}
