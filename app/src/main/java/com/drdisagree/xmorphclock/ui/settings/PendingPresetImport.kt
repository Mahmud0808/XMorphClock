package com.drdisagree.xmorphclock.ui.settings

import com.drdisagree.xmorphclock.domain.model.PresetFileEntry

data class PendingPresetImport(
    val uri: String,
    val entries: List<PresetFileEntry>
)
