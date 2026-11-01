package com.drdisagree.xmorphclock.xposed.signal

import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.report.HookReporter

object SignalSources {

    private val sources: List<SignalSource> = listOf(
        ClockSwitchSizeSource(),
        ClockViewModelSizeSource(),
        KeyguardTransitionSource(),
        DozeAmountSource()
    )

    fun installSupported() {
        sources.groupBy { it.type }.forEach { (type, candidates) ->
            candidates.forEach { candidate ->
                HookReporter.probe("$type/${candidate.name}: " + if (candidate.isSupported()) "supported" else "unsupported")
            }
            val supported = candidates.filter { it.isSupported() }
            val chosen = if (type.combined) supported else supported.take(1)
            HookReporter.signal(type.name, chosen.joinToString(SEPARATOR) { it.name }.ifEmpty { null })
            if (chosen.isEmpty()) HookEnv.log("No source for $type")
            chosen.forEach { source ->
                source.install()
                HookEnv.log("$type <- ${source.name}")
            }
        }
    }
}

private const val SEPARATOR = " + "
