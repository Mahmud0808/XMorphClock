package com.drdisagree.xmorphclock.ui.editor

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.CompositionDraft

internal data class EditorSnapshot(
    val face: ClockFace,
    val draft: CompositionDraft?
)
