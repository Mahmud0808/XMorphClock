package com.drdisagree.xmorphclock.ui.home

import com.drdisagree.xmorphclock.R

fun tabLabel(tab: FaceTab): Int = when (tab) {
    FaceTab.CLOCK -> R.string.kind_clock
    FaceTab.WIDGETS -> R.string.kind_widgets
    FaceTab.MINE -> R.string.category_mine
}
