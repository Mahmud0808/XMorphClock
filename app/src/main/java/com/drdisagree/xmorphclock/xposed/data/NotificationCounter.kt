package com.drdisagree.xmorphclock.xposed.data

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

internal object NotificationCounter {

    private const val LISTENER = "com.android.systemui.statusbar.NotificationListener"
    private val CALLBACKS = setOf("onListenerConnected", "onNotificationPosted", "onNotificationRemoved")

    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var installed = false

    @Volatile
    private var service: WeakReference<NotificationListenerService>? = null

    @Volatile
    private var enabled = false

    @Volatile
    var count: Int? = null
        private set

    fun install() {
        if (installed) return
        installed = true
        val type = HookEnv.findClass(LISTENER) ?: return HookEnv.log("Notification listener not found")
        type.declaredMethods.filter { it.name in CALLBACKS }.forEach { method ->
            method.hookAfter { chain, _ ->
                (chain.thisObject as? NotificationListenerService)?.let { service = WeakReference(it) }
                if (enabled) recount()
            }
        }
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (value) recount()
    }

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    private fun recount() {
        worker.execute {
            val listener = service?.get() ?: return@execute
            val next = runCatching { listener.activeNotifications?.count(::counts) }.getOrNull() ?: return@execute
            if (next == count) return@execute
            count = next
            mainHandler.post { listeners.forEach { it() } }
        }
    }

    private fun counts(sbn: StatusBarNotification): Boolean =
        sbn.isClearable && !sbn.isOngoing && sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0
}
