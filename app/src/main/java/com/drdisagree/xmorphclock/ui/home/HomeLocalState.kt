package com.drdisagree.xmorphclock.ui.home

import com.drdisagree.xmorphclock.domain.model.FaceKind

internal data class HomeLocalState(
    val mode: PreviewMode,
    val editing: Int?,
    val kind: FaceKind,
    val item: Int?,
    val focus: String?
)
