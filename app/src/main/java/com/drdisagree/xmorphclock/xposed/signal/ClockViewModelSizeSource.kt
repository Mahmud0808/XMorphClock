package com.drdisagree.xmorphclock.xposed.signal

import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.findField
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method

class ClockViewModelSizeSource : SignalSource {

    override val name = "KeyguardClockViewModel"
    override val type = SignalType.CLOCK_SIZE

    private val viewModelClass: Class<*>? by lazy { HookEnv.findClass(VIEW_MODEL_CLASS) }
    private val visibleField: Field? by lazy { viewModelClass?.findField(LARGE_CLOCK_VISIBLE) }

    @Volatile
    private var viewModel: WeakReference<Any>? = null

    @Volatile
    private var getValue: Method? = null

    override fun isSupported(): Boolean = visibleField != null

    override fun install() {
        viewModelClass?.declaredConstructors?.forEach { constructor ->
            constructor.hookAfter { chain, _ -> viewModel = WeakReference(chain.thisObject) }
        }
        ClockSignals.addLargeClockProvider(::isLargeClockVisible)
    }

    private fun isLargeClockVisible(): Boolean? {
        val target = viewModel?.get() ?: return null
        return try {
            val flow = visibleField?.get(target) ?: return null
            val method = getValue ?: flow.javaClass.findMethod(GET_VALUE) { it.parameterCount == 0 }
                ?.also { getValue = it }
            method?.invoke(flow) as? Boolean
        } catch (_: Throwable) {
            null
        }
    }
}

private const val VIEW_MODEL_CLASS = "com.android.systemui.keyguard.ui.viewmodel.KeyguardClockViewModel"
private const val LARGE_CLOCK_VISIBLE = "isLargeClockVisible"
private const val GET_VALUE = "getValue"
