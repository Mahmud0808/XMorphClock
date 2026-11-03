package com.drdisagree.xmorphclock.data.source.calendar

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drdisagree.xmorphclock.data.source.FaceDataContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FaceDataContract.ACTION_REQUEST_CALENDAR) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CalendarReader(app).publish()
            } finally {
                pending.finish()
            }
        }
    }
}
