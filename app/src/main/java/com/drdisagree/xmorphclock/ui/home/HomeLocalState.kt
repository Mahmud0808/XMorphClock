package com.drdisagree.xmorphclock.ui.home


internal data class HomeLocalState(
    val mode: PreviewMode,
    val editing: Int?,
    val tab: FaceTab,
    val item: Int?,
    val focus: String?
)
