package com.drdisagree.xmorphclock.data.source

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdisagree.xmorphclock.XMorphApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LockscreenFrameReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WallpaperContract.ACTION_FRAME) return
        val key = intent.getStringExtra(WallpaperContract.EXTRA_KEY) ?: return
        val scale = intent.getFloatExtra(WallpaperContract.EXTRA_SCALE, 1f)
        val app = context.applicationContext as? XMorphApp ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.handleLockscreenFrame(key, scale)
            } finally {
                pending.finish()
            }
        }
    }
}
