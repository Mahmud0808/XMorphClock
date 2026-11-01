package com.drdisagree.xmorphclock.xposed.hook

import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedInterface.HookHandle
import java.lang.reflect.Executable
import java.lang.reflect.Method

fun Class<*>.findMethod(name: String, predicate: (Method) -> Boolean = { true }): Method? {
    var current: Class<*>? = this
    while (current != null) {
        current.declaredMethods
            .firstOrNull { it.name == name && predicate(it) }
            ?.let { method ->
                method.isAccessible = true
                return method
            }
        current = current.superclass
    }
    return null
}

fun Executable.intercept(hooker: (Chain) -> Any?): HookHandle? = try {
    HookEnv.module.hook(this)
        .setExceptionMode(ExceptionMode.PROTECTIVE)
        .intercept { chain -> hooker(chain) }
} catch (throwable: Throwable) {
    HookEnv.log("Hook failed: $this", throwable)
    null
}

fun Executable.hookAfter(block: (Chain, Any?) -> Unit): HookHandle? = intercept { chain ->
    val result = chain.proceed()
    block(chain, result)
    result
}
