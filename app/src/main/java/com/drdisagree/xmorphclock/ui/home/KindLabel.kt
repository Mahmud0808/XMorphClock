package com.drdisagree.xmorphclock.ui.home

import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.FaceKind

fun kindLabel(kind: FaceKind): Int = when (kind) {
    FaceKind.CLOCK -> R.string.kind_clock
    FaceKind.WEATHER -> R.string.kind_weather
    FaceKind.WIDGET -> R.string.kind_widgets
}
