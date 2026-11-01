package com.drdisagree.xmorphclock.data.source.steps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdisagree.xmorphclock.data.source.FaceDataContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StepsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FaceDataContract.ACTION_REQUEST_STEPS) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                StepsReader(app).publish()
            } finally {
                pending.finish()
            }
        }
    }
}
