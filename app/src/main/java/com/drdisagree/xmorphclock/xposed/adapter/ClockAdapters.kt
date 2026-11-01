package com.drdisagree.xmorphclock.xposed.adapter

import com.drdisagree.xmorphclock.xposed.clock.ClockHost
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.report.HookReporter

object ClockAdapters {

    private val adapters: List<ClockAdapter> = listOf(
        ComposeElementAdapter(),
        ClockSectionAdapter(),
        ClockSwitchAdapter()
    )

    fun installSupported(host: ClockHost): Boolean {
        adapters.forEach { adapter ->
            val probes = adapter.probes().entries.joinToString { (symbol, found) -> "$symbol=" + if (found) "ok" else "missing" }
            HookReporter.probe("${adapter.name}: " + (if (adapter.isSupported()) "supported" else "unsupported") + " [$probes]")
        }
        val supported = adapters.filter { it.isSupported() }
        HookReporter.adapter(supported.joinToString(SEPARATOR) { it.name }.ifEmpty { null })
        if (supported.isEmpty()) {
            HookEnv.log("No supported clock adapter")
            return false
        }
        supported.forEach { adapter ->
            adapter.install(host)
            HookEnv.log("Clock adapter: ${adapter.name}")
        }
        return true
    }
}

private const val SEPARATOR = " + "
