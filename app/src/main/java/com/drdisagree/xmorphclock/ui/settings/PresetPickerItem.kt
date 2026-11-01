package com.drdisagree.xmorphclock.ui.settings

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceKind

data class PresetPickerItem(
    val key: Int,
    val kind: FaceKind,
    val face: ClockFace
)
