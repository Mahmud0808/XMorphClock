package com.drdisagree.xmorphclock.xposed.compose

import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.findField
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import java.lang.reflect.Method
import java.lang.reflect.Proxy

object ComposeToolkit {

    const val COMPOSER_CLASS = "androidx.compose.runtime.Composer"
    const val MODIFIER_CLASS = "androidx.compose.ui.Modifier"
    private const val FUNCTION_CLASS_PREFIX = "kotlin.jvm.functions.Function"
    private const val UNIT_CLASS = "kotlin.Unit"

    private val composerClass: Class<*>? by lazy { HookEnv.findClass(COMPOSER_CLASS) }

    private val startGroup: Method? by lazy {
        composerClass?.findMethod("startReplaceGroup") { it.parameterCount == 1 }
            ?: composerClass?.findMethod("startReplaceableGroup") { it.parameterCount == 1 }
    }

    private val endGroup: Method? by lazy {
        composerClass?.findMethod("endReplaceGroup") { it.parameterCount == 0 }
            ?: composerClass?.findMethod("endReplaceableGroup") { it.parameterCount == 0 }
    }

    private val hostUnit: Any? by lazy {
        try {
            HookEnv.findClass(UNIT_CLASS)?.findField("INSTANCE")?.get(null)
        } catch (_: Throwable) {
            null
        }
    }

    val emptyModifier: Any? by lazy {
        try {
            HookEnv.findClass(MODIFIER_CLASS)?.findField("Companion")?.get(null)
        } catch (_: Throwable) {
            null
        }
    }

    val isAvailable: Boolean
        get() = startGroup != null && endGroup != null && emptyModifier != null && functionClass(1) != null

    fun functionClass(arity: Int): Class<*>? = HookEnv.findClass("$FUNCTION_CLASS_PREFIX$arity")

    fun function1(name: String, block: (Any?) -> Any?): Any? = function(1, name) { args -> block(args.getOrNull(0)) }

    fun function3(name: String, block: (Any?, Any?, Any?) -> Any?): Any? =
        function(3, name) { args -> block(args.getOrNull(0), args.getOrNull(1), args.getOrNull(2)) }

    private fun function(arity: Int, name: String, block: (Array<out Any?>) -> Any?): Any? {
        val type = functionClass(arity) ?: return null
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "invoke" -> block(args ?: emptyArray()).let { if (it === Unit) hostUnit else it }
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> name
                else -> null
            }
        }
    }

    fun staticMethod(className: String, predicate: (Method) -> Boolean): Method? =
        HookEnv.findClass(className)?.declaredMethods?.firstOrNull(predicate)?.apply { isAccessible = true }

    fun modifierFunction(className: String, name: String, vararg extraTypes: Class<*>?): Method? = staticMethod(className) { method ->
        val types = method.parameterTypes
        (method.name == name || method.name.startsWith("$name-")) &&
            types.size == extraTypes.size + 1 &&
            types[0].name == MODIFIER_CLASS &&
            extraTypes.withIndex().all { (index, type) -> type == null || types[index + 1] == type }
    }

    fun inGroup(composer: Any, key: Int, block: () -> Unit) {
        val start = startGroup ?: return
        val end = endGroup ?: return
        start.invoke(composer, key)
        try {
            block()
        } finally {
            end.invoke(composer)
        }
    }

    fun parameterIndex(method: Method, typeName: String): Int =
        method.parameterTypes.indexOfFirst { it.name == typeName }
}
