package com.drdisagree.xmorphclock.ui.components

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    val component = launch.component ?: return
    context.startActivity(Intent.makeRestartActivityTask(component))
    exitProcess(0)
}
