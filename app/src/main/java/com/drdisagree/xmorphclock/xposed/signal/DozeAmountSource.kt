package com.drdisagree.xmorphclock.xposed.signal

import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.fieldValue
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import java.lang.reflect.Method

class DozeAmountSource : SignalSource {

    override val name = "StatusBarStateController"
    override val type = SignalType.KEYGUARD_PROGRESS

    private val method: Method? by lazy {
        HookEnv.findClass(CONTROLLER_CLASS)?.findMethod(SET_DOZE_AMOUNT) { method ->
            method.parameterCount == 1 && method.parameterTypes[0] == Float::class.javaPrimitiveType
        }
    }

    override fun isSupported(): Boolean = method != null

    override fun install() {
        method?.hookAfter { chain, _ ->
            val controller = chain.thisObject ?: return@hookAfter
            val amount = ((controller.fieldValue(DOZE_AMOUNT) as? Float) ?: return@hookAfter).coerceIn(0f, 1f)
            val dozing = (controller.fieldValue(IS_DOZING) as? Boolean) ?: (amount > 0f)
            ClockSignals.updateProgress(aod = amount, sleep = amount, asleep = dozing)
            if (!dozing && amount == 0f) HookReporter.count(HookReport.LOCKSCREEN_SHOWN)
        }
    }
}

private const val CONTROLLER_CLASS = "com.android.systemui.statusbar.StatusBarStateControllerImpl"
private const val SET_DOZE_AMOUNT = "setDozeAmountInternal"
private const val DOZE_AMOUNT = "mDozeAmount"
private const val IS_DOZING = "mIsDozing"
