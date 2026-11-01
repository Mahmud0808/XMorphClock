package com.drdisagree.xmorphclock.xposed.clock

import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.View
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.callMethod
import com.drdisagree.xmorphclock.xposed.hook.fieldValue
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

internal object ScrimTracker {

    private const val CONTROLLER = "com.android.systemui.statusbar.phone.ScrimController"
    private const val SCRIM_VIEW = "com.android.systemui.scrim.ScrimView"
    private const val SET_ALPHA = "setViewAlpha"
    private const val BEHIND = "mScrimBehind"
    private const val NOTIFICATIONS = "mNotificationsScrim"
    private const val BEHIND_KEYGUARD = "mScrimBehindAlphaKeyguard"
    private const val GET_TINT = "getTint"
    private const val TINT_COLOR = "mTintColor"
    private const val DRAWABLE_BOUNDS = "mDrawableBounds"
    private const val CLIPS_QS = "mClipsQsScrim"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var controller: WeakReference<Any>? = null
    private var behindView: WeakReference<Any>? = null
    private var notificationsView: WeakReference<Any>? = null
    private val location = IntArray(2)
    private var installed = false

    @Volatile
    var behindAlpha = 0f
        private set

    @Volatile
    var notificationsAlpha = 0f
        private set

    @Volatile
    var wallpaperDim = 0f

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun install() {
        if (installed) return
        installed = true
        val controllerClass = HookEnv.findClass(CONTROLLER) ?: return
        controllerClass.declaredConstructors.forEach { constructor ->
            constructor.hookAfter { chain, _ -> chain.thisObject?.let { controller = WeakReference(it) } }
        }
        HookEnv.findClass(SCRIM_VIEW)?.findMethod(SET_ALPHA) { it.parameterCount == 1 }?.hookAfter { chain, _ ->
            val owner = controller?.get() ?: return@hookAfter
            val alpha = (chain.args.getOrNull(0) as? Float) ?: return@hookAfter
            val view = chain.thisObject
            when {
                view === owner.fieldValue(BEHIND) -> {
                    if (behindView?.get() !== view) behindView = view?.let { WeakReference(it) }
                    if (behindAlpha != alpha) {
                        behindAlpha = alpha
                        dispatch()
                    }
                }
                view === owner.fieldValue(NOTIFICATIONS) -> {
                    if (notificationsView?.get() !== view) notificationsView = view?.let { WeakReference(it) }
                    if (notificationsAlpha != alpha) {
                        notificationsAlpha = alpha
                        dispatch()
                    }
                }
            }
        }
    }

    fun dimOver(area: Rect): Float {
        val clips = clipsQs()
        val alpha = (if (clips) notificationsAlpha else behindAlpha).coerceIn(0f, 1f)
        if (alpha == 0f) return 0f
        val view = (if (clips) notificationsView else behindView)?.get() as? View ?: return alpha
        val bounds = view.fieldValue(DRAWABLE_BOUNDS) as? Rect ?: return alpha
        if (bounds.isEmpty) return 0f
        view.getLocationOnScreen(location)
        val left = location[0] + bounds.left
        val top = location[1] + bounds.top
        return if (Rect.intersects(area, Rect(left, top, left + bounds.width(), top + bounds.height()))) alpha else 0f
    }

    fun dimTint(): Int {
        val view = (if (clipsQs()) notificationsView else behindView)?.get() ?: return Color.BLACK
        return (view.callMethod(GET_TINT) as? Int) ?: (view.fieldValue(TINT_COLOR) as? Int) ?: Color.BLACK
    }

    fun describe(): String = "wallpaperDim=$wallpaperDim behind=$behindAlpha notifications=$notificationsAlpha clipsQs=${clipsQs()}"

    private fun clipsQs(): Boolean = (controller?.get()?.fieldValue(CLIPS_QS) as? Boolean) ?: false

    fun fade(): Float {
        val keyguard = (controller?.get()?.fieldValue(BEHIND_KEYGUARD) as? Float) ?: 0f
        val notifications = notificationsAlpha
        if (notifications <= keyguard || keyguard >= 1f) return 1f
        return ((1f - notifications) / (1f - keyguard)).coerceIn(0f, 1f)
    }

    private fun dispatch() {
        if (Looper.myLooper() == Looper.getMainLooper()) listeners.forEach { it() } else mainHandler.post { listeners.forEach { it() } }
    }
}
