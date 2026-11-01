package com.drdisagree.xmorphclock.ui.components

import android.graphics.RectF
import android.view.ViewTreeObserver
import androidx.compose.ui.geometry.Offset
import com.drdisagree.xmorphclock.engine.ClockFaceView
import kotlin.math.abs

internal class ExtentsReporter : ViewTreeObserver.OnPreDrawListener {

    var origin: () -> Offset = { Offset.Zero }
    var onChange: ((Map<String, RectF>) -> Unit)? = null
    private var view: ClockFaceView? = null
    private var observer: ViewTreeObserver? = null
    private var last: Map<String, RectF> = emptyMap()

    fun attach(target: ClockFaceView) {
        if (view === target) return
        detach()
        view = target
        observer = target.viewTreeObserver.also { it.addOnPreDrawListener(this) }
    }

    fun detach() {
        observer?.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
        observer = null
        view = null
        last = emptyMap()
    }

    override fun onPreDraw(): Boolean {
        val target = view ?: return true
        val listener = onChange ?: return true
        val shift = origin()
        val rects = target.layerRects().mapValues { RectF(it.value).apply { offset(shift.x, shift.y) } }
        if (!same(rects, last)) {
            last = rects
            listener(rects)
        }
        return true
    }

    private fun same(a: Map<String, RectF>, b: Map<String, RectF>): Boolean {
        if (a.size != b.size) return false
        return a.all { (id, rect) ->
            val other = b[id] ?: return@all false
            abs(rect.left - other.left) < TOLERANCE && abs(rect.top - other.top) < TOLERANCE &&
                abs(rect.right - other.right) < TOLERANCE && abs(rect.bottom - other.bottom) < TOLERANCE
        }
    }

    private companion object {
        const val TOLERANCE = 0.5f
    }
}
