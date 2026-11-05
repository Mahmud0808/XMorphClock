package com.drdisagree.xmorphclock.xposed.systemui

import com.drdisagree.xmorphclock.engine.FaceFont
import com.drdisagree.xmorphclock.engine.FaceImages
import com.drdisagree.xmorphclock.xposed.adapter.ClockAdapters
import com.drdisagree.xmorphclock.xposed.clock.ClockController
import com.drdisagree.xmorphclock.xposed.clock.ClockHost
import com.drdisagree.xmorphclock.xposed.clock.RemoteFonts
import com.drdisagree.xmorphclock.xposed.clock.RemoteImages
import com.drdisagree.xmorphclock.xposed.clock.ScrimTracker
import com.drdisagree.xmorphclock.xposed.clock.WallpaperCapture
import com.drdisagree.xmorphclock.xposed.data.NotificationCounter
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.prefs.ModulePrefs
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import com.drdisagree.xmorphclock.xposed.signal.SignalSources

object SystemUiModule {

    const val PACKAGE = "com.android.systemui"

    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true
        val module = HookEnv.module
        HookReporter.framework("${module.frameworkName} ${module.frameworkVersion} (API ${module.apiVersion})")
        ModulePrefs.init(module)
        FaceImages.init(RemoteImages::load)
        FaceFont.setUserFonts(RemoteFonts::open)
        WallpaperCapture.install()
        ScrimTracker.install()
        SignalSources.installSupported()
        StockSmartspace.install()
        RomClockGuard.install()
        NotificationCounter.install()
        ClockController.init()
        ClockAdapters.installSupported(ClockHost())
        HookReporter.start()
    }
}
