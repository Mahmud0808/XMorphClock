package com.drdisagree.xmorphclock.xposed.systemui

import android.view.Display
import android.view.ViewGroup
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CopyOnWriteArrayList

object ClockSwitchTracker {

    private const val CLASS = "com.android.keyguard.KeyguardClockSwitch"
    private const val ON_FINISH_INFLATE = "onFinishInflate"

    private val views: MutableSet<ViewGroup> = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))
    private val listeners = CopyOnWriteArrayList<(ViewGroup) -> Unit>()
    private var installed = false

    val type: Class<*>? by lazy { HookEnv.findClass(CLASS) }

    @Synchronized
    fun install() {
        if (installed) return
        installed = true
        type?.findMethod(ON_FINISH_INFLATE) { it.parameterCount == 0 }?.hookAfter { chain, _ ->
            val view = chain.thisObject as? ViewGroup ?: return@hookAfter
            views += view
            listeners.forEach { it(view) }
        }
    }

    fun addListener(listener: (ViewGroup) -> Unit) {
        listeners += listener
        snapshot().forEach(listener)
    }

    fun current(): ViewGroup? {
        val all = snapshot()
        return all.lastOrNull { it.isAttachedToWindow && it.display?.displayId == Display.DEFAULT_DISPLAY }
            ?: all.lastOrNull { it.isAttachedToWindow }
    }

    private fun snapshot(): List<ViewGroup> = synchronized(views) { views.toList() }
}
