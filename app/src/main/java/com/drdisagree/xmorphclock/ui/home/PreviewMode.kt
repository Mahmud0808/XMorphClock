package com.drdisagree.xmorphclock.ui.home

import androidx.annotation.StringRes
import com.drdisagree.xmorphclock.R

enum class PreviewMode(@param:StringRes val label: Int) {
    LARGE(R.string.preview_large),
    SMALL(R.string.preview_small),
    AOD(R.string.preview_aod)
}
