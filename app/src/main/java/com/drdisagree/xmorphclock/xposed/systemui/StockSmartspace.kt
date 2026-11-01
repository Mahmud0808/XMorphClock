package com.drdisagree.xmorphclock.xposed.systemui

import android.view.View
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import com.drdisagree.xmorphclock.xposed.hook.intercept
import com.drdisagree.xmorphclock.xposed.prefs.ModulePrefs
import java.util.Collections
import java.util.WeakHashMap

object StockSmartspace {

    private const val CONTROLLER = "com.android.systemui.statusbar.lockscreen.LockscreenSmartspaceController"
    private const val SET_VISIBILITY = "setVisibility"
    private val BUILDERS = setOf("buildAndConnectView", "buildAndConnectDateView", "buildAndConnectWeatherView", "buildView")

    private val views: MutableSet<View> = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap()))

    fun install() {
        val controller = HookEnv.findClass(CONTROLLER) ?: return
        val builders = controller.declaredMethods.filter { method ->
            method.name in BUILDERS && View::class.java.isAssignableFrom(method.returnType)
        }
        if (builders.isEmpty()) return
        builders.forEach { method ->
            method.isAccessible = true
            method.hookAfter { _, result -> (result as? View)?.let(::register) }
        }
        View::class.java.getDeclaredMethod(SET_VISIBILITY, Int::class.javaPrimitiveType).intercept { chain ->
            val view = chain.thisObject
            if (views.isEmpty() || view !is View || view !in views || !ModulePrefs.isClockEnabled) {
                chain.proceed()
            } else {
                chain.proceed(arrayOf<Any?>(View.GONE))
            }
        }
    }

    private fun register(view: View) {
        views += view
        if (ModulePrefs.isClockEnabled) view.visibility = View.GONE
    }
}
