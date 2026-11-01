package com.drdisagree.xmorphclock.xposed.signal

import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.findField
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.lang.reflect.Field
import java.lang.reflect.Method

class KeyguardTransitionSource : SignalSource {

    override val name = "KeyguardTransitionRepository"
    override val type = SignalType.KEYGUARD_PROGRESS

    private val method: Method? by lazy {
        HookEnv.findClass(REPOSITORY_CLASS)?.findMethod(EMIT_TRANSITION) { method ->
            method.parameterTypes.firstOrNull()?.name == STEP_CLASS
        }
    }

    private val stepClass: Class<*>? by lazy { HookEnv.findClass(STEP_CLASS) }
    private val fromField: Field? by lazy { stepClass?.findField("from") }
    private val toField: Field? by lazy { stepClass?.findField("to") }
    private val valueField: Field? by lazy { stepClass?.findField("value") }

    override fun isSupported(): Boolean =
        method != null && fromField != null && toField != null && valueField != null

    override fun install() {
        method?.hookAfter { chain, _ ->
            val step = chain.args.firstOrNull() ?: return@hookAfter
            val from = (fromField?.get(step) as? Enum<*>)?.name ?: return@hookAfter
            val to = (toField?.get(step) as? Enum<*>)?.name ?: return@hookAfter
            val value = (valueField?.get(step) as? Float ?: return@hookAfter).coerceIn(0f, 1f)
            ClockSignals.updateProgress(
                aod = progressOf(from, to, value) { it == AOD },
                sleep = progressOf(from, to, value) { it in DOZE_STATES },
                asleep = to in DOZE_STATES
            )
        }
    }

    private inline fun progressOf(from: String, to: String, value: Float, matches: (String) -> Boolean): Float {
        val fromMatch = matches(from)
        val toMatch = matches(to)
        return when {
            toMatch && !fromMatch -> value
            fromMatch && !toMatch -> 1f - value
            toMatch -> 1f
            else -> 0f
        }
    }
}

private const val REPOSITORY_CLASS =
    "com.android.systemui.keyguard.data.repository.KeyguardTransitionRepositoryImpl"
private const val STEP_CLASS = "com.android.systemui.keyguard.shared.model.TransitionStep"
private const val EMIT_TRANSITION = "emitTransition"
private const val AOD = "AOD"
private val DOZE_STATES = setOf(AOD, "DOZING")
