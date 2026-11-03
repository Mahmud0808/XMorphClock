package com.drdisagree.xmorphclock.xposed.systemui

import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.findField
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

object ClockViewModelTracker {

    private const val CLASS = "com.android.systemui.keyguard.ui.viewmodel.KeyguardClockViewModel"
    private const val GET_VALUE = "getValue"

    private val getters = ConcurrentHashMap<Class<*>, Method>()
    private var installed = false

    @Volatile
    private var instance: WeakReference<Any>? = null

    val type: Class<*>? by lazy { HookEnv.findClass(CLASS) }

    @Synchronized
    fun install() {
        if (installed) return
        installed = true
        type?.declaredConstructors?.forEach { constructor ->
            constructor.hookAfter { chain, _ -> instance = WeakReference(chain.thisObject) }
        }
    }

    fun field(name: String): Field? = type?.findField(name)

    fun flowValue(field: Field?): Any? {
        val target = instance?.get() ?: return null
        return try {
            val flow = field?.get(target) ?: return null
            val getter = getters[flow.javaClass]
                ?: flow.javaClass.findMethod(GET_VALUE) { it.parameterCount == 0 }?.also { getters[flow.javaClass] = it }
                ?: return null
            getter.invoke(flow)
        } catch (_: Throwable) {
            null
        }
    }
}
