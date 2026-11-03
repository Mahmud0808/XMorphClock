package com.drdisagree.xmorphclock.ui.main

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.xmorphclock.XMorphApp
import com.drdisagree.xmorphclock.domain.model.ClockGeometry
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.engine.FaceFont
import com.drdisagree.xmorphclock.ui.AppViewModel
import com.drdisagree.xmorphclock.ui.appViewModelFactory
import com.drdisagree.xmorphclock.ui.components.DefaultClockGeometry
import com.drdisagree.xmorphclock.ui.navigation.AppNavigation
import com.drdisagree.xmorphclock.ui.theme.XMorphClockTheme

class MainActivity : ComponentActivity() {

    private fun screenGeometry(): ClockGeometry {
        val bounds = windowManager.maximumWindowMetrics.bounds
        val aspect = bounds.width().toFloat() / bounds.height().coerceAtLeast(1)
        return DefaultClockGeometry.copy(aspect = aspect)
    }


    private val container by lazy { (application as XMorphApp).container }
    private val appViewModel: AppViewModel by viewModels { appViewModelFactory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        window.isNavigationBarContrastEnforced = false
        holdSplash()
        setContent {
            val wallpaper by appViewModel.wallpaper.collectAsStateWithLifecycle()
            val geometry by appViewModel.geometry.collectAsStateWithLifecycle()
            val faceData by appViewModel.faceData.collectAsStateWithLifecycle()
            val depth by appViewModel.depth.collectAsStateWithLifecycle()
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                window.isNavigationBarContrastEnforced = false
            }
            XMorphClockTheme(dark) {
                AppNavigation(container, wallpaper, depth, geometry ?: screenGeometry(), faceData)
            }
        }
    }

    private fun holdSplash() {
        val content = window.decorView
        val started = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = FaceFont.isWarm || SystemClock.uptimeMillis() - started > SPLASH_MAX_MS
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
    }
}

private const val SPLASH_MAX_MS = 400L
