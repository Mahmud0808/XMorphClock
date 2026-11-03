package com.drdisagree.xmorphclock

import android.app.Application
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.di.AppContainer
import com.drdisagree.xmorphclock.engine.FaceFont

class XMorphApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        XposedServiceSource.init()
        FaceFont.init(this)
        FaceFont.warmUp()
        container = AppContainer(this)
    }
}
