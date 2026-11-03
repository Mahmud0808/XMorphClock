package com.drdisagree.xmorphclock.xposed.clock

import android.content.Context
import android.view.WindowManager

internal object SlotWidth {

    private const val SPLIT_SHADE = "config_use_split_notification_shade"
    private const val STATUS_MARGIN = "status_view_margin_horizontal"
    private const val UNRESOLVED = -1

    private var splitId = UNRESOLVED
    private var marginId = UNRESOLVED

    fun cap(context: Context): Int {
        val bounds = context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds ?: return 0
        val width = bounds.width()
        val height = bounds.height()
        if (width <= 0 || height <= 0) return 0
        val shortest = minOf(width, height)
        if (width <= height || !splitShade(context)) return shortest
        return minOf(shortest, width / 2 - 2 * margin(context))
    }

    private fun splitShade(context: Context): Boolean {
        if (splitId == UNRESOLVED) splitId = context.resources.getIdentifier(SPLIT_SHADE, "bool", context.packageName)
        return splitId != 0 && runCatching { context.resources.getBoolean(splitId) }.getOrDefault(false)
    }

    private fun margin(context: Context): Int {
        if (marginId == UNRESOLVED) marginId = context.resources.getIdentifier(STATUS_MARGIN, "dimen", context.packageName)
        return if (marginId == 0) 0 else runCatching { context.resources.getDimensionPixelSize(marginId) }.getOrDefault(0)
    }
}
