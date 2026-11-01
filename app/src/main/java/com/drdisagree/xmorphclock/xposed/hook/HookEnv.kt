package com.drdisagree.xmorphclock.xposed.hook

import android.util.Log
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import io.github.libxposed.api.XposedInterface
import java.util.concurrent.ConcurrentHashMap

object HookEnv {

    private const val TAG = "XMorphClock"

    lateinit var module: XposedInterface
        private set

    lateinit var classLoader: ClassLoader
        private set

    private val classes = ConcurrentHashMap<String, Any>()
    private val missing = Any()

    fun init(module: XposedInterface, classLoader: ClassLoader) {
        this.module = module
        this.classLoader = classLoader
    }

    fun findClass(name: String): Class<*>? {
        val cached = classes.getOrPut(name) {
            try {
                Class.forName(name, false, classLoader)
            } catch (_: Throwable) {
                missing
            }
        }
        return cached as? Class<*>
    }

    fun log(message: String, throwable: Throwable? = null) {
        val priority = if (throwable == null) Log.INFO else Log.ERROR
        if (throwable == null) {
            module.log(priority, TAG, message)
        } else {
            module.log(priority, TAG, message, throwable)
        }
        HookReporter.log(message, throwable)
    }
}
