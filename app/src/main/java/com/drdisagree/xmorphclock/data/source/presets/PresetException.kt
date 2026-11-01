package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.domain.model.PresetFileError

class PresetException(val error: PresetFileError) : Exception(error.name)
