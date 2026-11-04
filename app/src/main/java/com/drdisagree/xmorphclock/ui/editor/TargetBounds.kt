package com.drdisagree.xmorphclock.ui.editor

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ItemBounds

data class TargetBounds(val id: String, val face: ClockFace, val bounds: ItemBounds, val maxBottom: Float)
