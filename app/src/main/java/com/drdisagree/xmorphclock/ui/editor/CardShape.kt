package com.drdisagree.xmorphclock.ui.editor

import com.drdisagree.xmorphclock.R

enum class CardShape(val radius: Float, val label: Int) {
    ROUND(0.5f, R.string.shape_round),
    ROUNDED(0.3f, R.string.shape_rounded),
    SQUARE(0.12f, R.string.shape_square)
}
