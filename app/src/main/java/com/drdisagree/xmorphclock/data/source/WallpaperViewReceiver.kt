package com.drdisagree.xmorphclock.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WallpaperViewReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WallpaperContract.ACTION_REQUEST_VIEW) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val view = WallpaperSource(app).measureView() ?: return@launch
                app.sendBroadcast(
                    Intent(WallpaperContract.ACTION_VIEW)
                        .setPackage(ReportContract.SYSTEMUI_PACKAGE)
                        .putExtra(WallpaperContract.EXTRA_VIEW, view)
                )
            } finally {
                pending.finish()
            }
        }
    }
}
