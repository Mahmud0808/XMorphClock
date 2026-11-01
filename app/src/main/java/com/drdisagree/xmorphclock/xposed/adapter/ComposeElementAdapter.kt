package com.drdisagree.xmorphclock.xposed.adapter

import com.drdisagree.xmorphclock.xposed.clock.ClockHost
import com.drdisagree.xmorphclock.xposed.clock.DepthLayer
import com.drdisagree.xmorphclock.xposed.clock.DepthOverlay
import com.drdisagree.xmorphclock.xposed.compose.ComposeToolkit
import com.drdisagree.xmorphclock.xposed.compose.ComposeViewHost
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.callMethod
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.intercept
import com.drdisagree.xmorphclock.xposed.hook.staticFieldValue
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import java.lang.reflect.Method

class ComposeElementAdapter : ClockAdapter {

    override val name = "LockscreenElementFactory"

    private val method: Method? by lazy {
        HookEnv.findClass(FACTORY_CLASS)?.findMethod(LOCKSCREEN_ELEMENT) { method ->
            ComposeToolkit.parameterIndex(method, KEY_CLASS) >= 0 &&
                    ComposeToolkit.parameterIndex(method, ComposeToolkit.COMPOSER_CLASS) >= 0
        }
    }

    private val regionKeys: List<Any> by lazy {
        val region = staticFieldValue(REGION_CLOCK_CLASS, INSTANCE) ?: return@lazy emptyList()
        listOfNotNull(region.callMethod(GET_SMALL), region.callMethod(GET_LARGE))
    }

    @Volatile
    private var active = false

    override fun isSupported(): Boolean =
        method != null && regionKeys.isNotEmpty() && ComposeViewHost.isAvailable

    override fun probes(): Map<String, Boolean> = linkedMapOf(
        "LockscreenElementFactoryImpl.LockscreenElement" to (method != null),
        "Region.Clock keys" to regionKeys.isNotEmpty(),
        "ComposeViewHost" to ComposeViewHost.isAvailable
    )

    override fun install(host: ClockHost) {
        val target = method ?: return
        val keyIndex = ComposeToolkit.parameterIndex(target, KEY_CLASS)
        val modifierIndex = ComposeToolkit.parameterIndex(target, ComposeToolkit.MODIFIER_CLASS)
        val composerIndex = ComposeToolkit.parameterIndex(target, ComposeToolkit.COMPOSER_CLASS)

        target.intercept { chain ->
            val key = chain.args.getOrNull(keyIndex)
            if (key == null || regionKeys.none { it == key } || !host.isEnabled) {
                return@intercept chain.proceed()
            }
            val composer = chain.args.getOrNull(composerIndex) ?: return@intercept chain.proceed()
            val large = key == regionKeys.getOrNull(1)
            if (!active) {
                active = true
                HookReporter.adapter(name)
            }
            HookReporter.count(if (large) "composeLarge" else "composeSmall")
            ComposeViewHost.emitInGroup(composer, GROUP_KEY, chain.args.getOrNull(modifierIndex)) { context ->
                host.createSlot(context, large)
            }
            ComposeViewHost.windowFillModifier { DepthOverlay.windowSize }?.let { fill ->
                ComposeViewHost.emitInGroup(composer, DEPTH_GROUP_KEY, fill) { context -> DepthLayer(context) }
            }
            null
        }
    }
}

private const val FACTORY_CLASS =
    "com.android.systemui.keyguard.ui.composable.elements.LockscreenElementFactoryImpl"
private const val REGION_CLOCK_CLASS =
    "com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys\$Region\$Clock"
private const val KEY_CLASS = "com.android.compose.animation.scene.Key"
private const val LOCKSCREEN_ELEMENT = "LockscreenElement"
private const val INSTANCE = "INSTANCE"
private const val GET_SMALL = "getSmall"
private const val GET_LARGE = "getLarge"
private const val GROUP_KEY = 0x584D4331
private const val DEPTH_GROUP_KEY = 0x584D4444
