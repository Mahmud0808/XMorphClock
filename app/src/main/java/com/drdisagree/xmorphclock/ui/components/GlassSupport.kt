package com.drdisagree.xmorphclock.ui.components

import android.os.Build

object GlassSupport {
    val full: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
