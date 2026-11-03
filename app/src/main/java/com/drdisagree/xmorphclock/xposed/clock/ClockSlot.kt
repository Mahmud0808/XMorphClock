package com.drdisagree.xmorphclock.xposed.clock

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import com.drdisagree.xmorphclock.domain.model.ClockFrame
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import com.drdisagree.xmorphclock.xposed.signal.ClockSignals

class ClockSlot(
    context: Context,
    private val heights: SlotHeights,
    val large: Boolean,
    private val pinned: Boolean,
    val stockBurnIn: Boolean,
    private val onVisibilityChange: () -> Unit
) : FrameLayout(context) {

    private val location = IntArray(2)

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        clipChildren = false
        clipToPadding = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this) onVisibilityChange()
    }

    override fun setTranslationX(translationX: Float) {
        if (!pinned) super.setTranslationX(translationX)
    }

    override fun setTranslationY(translationY: Float) {
        if (!pinned) super.setTranslationY(translationY)
    }

    override fun setScaleX(scaleX: Float) {
        if (!pinned) super.setScaleX(scaleX)
    }

    override fun setScaleY(scaleY: Float) {
        if (!pinned) super.setScaleY(scaleY)
    }

    override fun setRotation(rotation: Float) {
        if (!pinned) super.setRotation(rotation)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (childCount > 0) {
            if (measuredHeight > 0) heights.last = measuredHeight
        } else if (heights.last > 0) {
            setMeasuredDimension(measuredWidth, resolveSize(heights.last, heightMeasureSpec))
        }
    }

    private var pending: ClockFrame? = null
    private var pendingAspect = 0f

    private val settleCheck = Runnable {
        val frame = pending ?: return@Runnable
        if (frameNow() == frame) HookReporter.frame(large, frame, pendingAspect)
    }

    private val frameListener = ViewTreeObserver.OnPreDrawListener {
        val frame = frameNow()
        if (frame != pending) {
            pending = frame
            removeCallbacks(settleCheck)
            if (frame != null) postDelayed(settleCheck, SETTLE_MS)
        }
        true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(frameListener)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(settleCheck)
        pending = null
        viewTreeObserver.removeOnPreDrawListener(frameListener)
        super.onDetachedFromWindow()
    }

    private fun frameNow(): ClockFrame? {
        if (ClockSignals.isAsleep || childCount == 0 || width <= 0 || !isShown || !atRest()) return null
        val bounds = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds ?: return null
        if (bounds.width() <= 0 || bounds.height() <= 0) return null
        getLocationOnScreen(location)
        pendingAspect = bounds.width().toFloat() / bounds.height()
        return ClockFrame(
            left = location[0].toFloat() / bounds.width(),
            top = (location[1] + paddingTop).toFloat() / bounds.height(),
            width = width.toFloat() / bounds.width()
        )
    }

    private fun atRest(): Boolean {
        var view: View? = this
        while (view != null) {
            if (view.scaleX != 1f || view.scaleY != 1f || view.rotation != 0f || view.alpha < OPAQUE) return false
            view = view.parent as? View
        }
        return true
    }

    private companion object {
        const val OPAQUE = 0.999f
        const val SETTLE_MS = 250L
    }
}
