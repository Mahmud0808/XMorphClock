package com.drdisagree.xmorphclock.xposed

import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.systemui.SystemUiModule
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class ModuleMain : XposedModule() {

    private var processName: String? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != SystemUiModule.PACKAGE || !param.isFirstPackage) return
        if (processName != null && processName != SystemUiModule.PACKAGE) return
        HookEnv.init(this, param.classLoader)
        SystemUiModule.start()
    }
}
