package com.drdisagree.xmorphclock.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

class PermissionGranter(private val context: Context) {

    fun granted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    suspend fun grant(permissions: List<String>) {
        val missing = permissions.filterNot(::granted)
        if (missing.isEmpty()) return
        RootShell.run(missing.joinToString(SEPARATOR) { "pm grant ${context.packageName} $it" })
    }

    companion object {
        private const val SEPARATOR = "; "
        val LOCATION = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val CALENDAR = listOf(Manifest.permission.READ_CALENDAR)
        val ACTIVITY = listOf(Manifest.permission.ACTIVITY_RECOGNITION, READ_STEPS, READ_IN_BACKGROUND)
        private const val READ_STEPS = "android.permission.health.READ_STEPS"
        private const val READ_IN_BACKGROUND = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
    }
}
