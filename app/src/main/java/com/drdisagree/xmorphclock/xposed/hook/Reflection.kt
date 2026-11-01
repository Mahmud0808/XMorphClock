package com.drdisagree.xmorphclock.xposed.hook

import java.lang.reflect.Field

fun Class<*>.findField(name: String): Field? {
    var current: Class<*>? = this
    while (current != null) {
        try {
            return current.getDeclaredField(name).apply { isAccessible = true }
        } catch (_: NoSuchFieldException) {
            current = current.superclass
        }
    }
    return null
}

fun Any.fieldValue(name: String): Any? = try {
    javaClass.findField(name)?.get(this)
} catch (_: Throwable) {
    null
}

fun Any.callMethod(name: String): Any? = try {
    javaClass.findMethod(name) { it.parameterCount == 0 }?.invoke(this)
} catch (_: Throwable) {
    null
}

fun staticFieldValue(className: String, name: String): Any? = try {
    HookEnv.findClass(className)?.findField(name)?.get(null)
} catch (_: Throwable) {
    null
}

fun Any.setFieldValue(name: String, value: Any?) {
    runCatching { javaClass.findField(name)?.set(this, value) }
}
