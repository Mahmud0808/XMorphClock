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

    private val settleCheck = Runnable { reportFrame() }

    private val frameListener = ViewTreeObserver.OnPreDrawListener {
        removeCallbacks(settleCheck)
        postDelayed(settleCheck, SETTLE_MS)
        true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(frameListener)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(settleCheck)
        viewTreeObserver.removeOnPreDrawListener(frameListener)
        super.onDetachedFromWindow()
    }

    private fun reportFrame() {
        if (ClockSignals.isAsleep || childCount == 0 || width <= 0 || !isShown) return
        val bounds = context.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds ?: return
        if (bounds.width() <= 0 || bounds.height() <= 0) return
        getLocationOnScreen(location)
        HookReporter.frame(
            large,
            ClockFrame(
                left = location[0].toFloat() / bounds.width(),
                top = (location[1] + paddingTop).toFloat() / bounds.height(),
                width = width.toFloat() / bounds.width()
            ),
            bounds.width().toFloat() / bounds.height()
        )
    }

    private companion object {
        const val SETTLE_MS = 250L
    }
}
