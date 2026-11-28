package com.drdisagree.xmorphclock.domain.model

data class LyricsTuning(
    val offsetMs: Long = 0L,
    val pinned: String? = null,
    val hidden: Boolean = false
) {
    val isDefault: Boolean
        get() = offsetMs == 0L && pinned == null && !hidden
}
