package com.drdisagree.xmorphclock.xposed.adapter

import android.animation.Animator
import android.os.SystemClock
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.drdisagree.xmorphclock.xposed.clock.ClockHost
import com.drdisagree.xmorphclock.xposed.clock.DepthLayer
import com.drdisagree.xmorphclock.xposed.hook.fieldValue
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import com.drdisagree.xmorphclock.xposed.hook.onEachPreDrawGate
import com.drdisagree.xmorphclock.xposed.hook.setFieldValue
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import com.drdisagree.xmorphclock.xposed.systemui.ClockSwitchTracker
import java.lang.reflect.Method
import java.util.WeakHashMap

class ClockSwitchAdapter : ClockAdapter {

    override val name = "KeyguardClockSwitch"

    private val switchMethods: List<Method> by lazy {
        ClockSwitchTracker.type?.declaredMethods
            ?.filter { it.name in SWITCH_METHODS }
            ?.onEach { it.isAccessible = true }
            .orEmpty()
    }

    private val states = WeakHashMap<ViewGroup, ClockSwitchState>()
    private var active = false

    override fun isSupported(): Boolean = ClockSwitchTracker.type != null && switchMethods.isNotEmpty()

    override fun probes(): Map<String, Boolean> = linkedMapOf(
        "KeyguardClockSwitch" to (ClockSwitchTracker.type != null),
        "switch methods" to switchMethods.isNotEmpty()
    )

    override fun install(host: ClockHost) {
        ClockSwitchTracker.install()
        switchMethods.forEach { method ->
            method.hookAfter { chain, _ ->
                val view = chain.thisObject as? ViewGroup ?: return@hookAfter
                val state = states[view] ?: return@hookAfter
                if (!host.isEnabled || !onMainDisplay(view)) return@hookAfter
                cancelAnimators(view)
                enforce(host, view, state)
            }
        }
        ClockSwitchTracker.addListener { view -> attach(host, view) }
    }

    private fun attach(host: ClockHost, view: ViewGroup) {
        if (states.containsKey(view)) return
        val state = ClockSwitchState()
        states[view] = state
        view.onEachPreDrawGate {
            val changed = if (host.isEnabled && onMainDisplay(view)) enforce(host, view, state) else restore(view, state)
            val now = SystemClock.uptimeMillis()
            if (changed && now - state.lastCancel > CANCEL_GAP_MS) {
                state.lastCancel = now
                false
            } else {
                true
            }
        }
    }

    private fun enforce(host: ClockHost, view: ViewGroup, state: ClockSwitchState): Boolean {
        val small = child(view, SMALL_FRAME_ID) as? ViewGroup ?: return false
        if (!active) {
            active = true
            HookReporter.adapter(name)
        }
        var changed = placeSlot(host, small, state)
        changed = fitFrame(small, state) || changed
        changed = hideStock(small, state) || changed
        (child(view, LARGE_FRAME_ID) as? ViewGroup)?.let { large ->
            changed = hideStock(large, state) || changed
            if (large.visibility != View.GONE) {
                large.visibility = View.GONE
                changed = true
            }
        }
        if (small.visibility != View.VISIBLE) {
            small.visibility = View.VISIBLE
            changed = true
        }
        small.alpha = 1f
        small.translationY = 0f
        child(view, STATUS_AREA_ID)?.let(::settleStatusArea)
        (view.fieldValue(SMARTSPACE_FIELD) as? View)?.translationY = 0f
        changed = hideExtras(view, state) || changed
        changed = placeDepth(view, small, state) || changed
        return changed
    }

    private fun placeSlot(host: ClockHost, small: ViewGroup, state: ClockSwitchState): Boolean {
        val slot = state.slot?.takeIf { it.context === small.context }
            ?: host.createSlot(small.context, large = true, pinned = true, stockBurnIn = true, capWidth = true).also { state.slot = it }
        if (slot.parent === small) return false
        (slot.parent as? ViewGroup)?.removeView(slot)
        small.addView(slot, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL))
        HookReporter.count("switchSlot")
        return true
    }

    private fun fitFrame(small: ViewGroup, state: ClockSwitchState): Boolean {
        val params = small.layoutParams ?: return false
        if (state.frameWidth == null) {
            state.frameWidth = params.width
            state.frameHeight = params.height
            state.framePaddingStart = small.paddingStart
            state.framePaddingEnd = small.paddingEnd
        }
        var changed = false
        if (params.width != ViewGroup.LayoutParams.MATCH_PARENT || params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            small.layoutParams = params
            changed = true
        }
        if (small.paddingStart != 0 || small.paddingEnd != 0) {
            small.setPaddingRelative(0, small.paddingTop, 0, small.paddingBottom)
            changed = true
        }
        return changed
    }

    private fun hideStock(frame: ViewGroup, state: ClockSwitchState): Boolean {
        var changed = false
        for (index in 0 until frame.childCount) {
            val child = frame.getChildAt(index)
            if (child === state.slot || child.visibility == View.GONE) continue
            child.visibility = View.GONE
            state.hidden += child
            changed = true
        }
        return changed
    }

    private fun hideExtras(group: ViewGroup, state: ClockSwitchState): Boolean {
        var changed = false
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            if (child === state.slot || child === state.depth) continue
            if (child.javaClass.name == SLICE_VIEW) {
                if (child.visibility != View.GONE) {
                    child.visibility = View.GONE
                    state.hidden += child
                    changed = true
                }
            } else if (child is ViewGroup) {
                changed = hideExtras(child, state) || changed
            }
        }
        return changed
    }

    private fun settleStatusArea(area: View) {
        STATUS_TRANSLATIONS.forEach { area.setFieldValue(it, 0f) }
        val aod = (area.fieldValue(TRANSLATE_X_AOD) as? Float) ?: 0f
        val unfold = (area.fieldValue(TRANSLATE_X_UNFOLD) as? Float) ?: 0f
        if (area.fieldValue(TRANSLATE_X_AOD) != null) area.translationX = aod + unfold
        area.translationY = 0f
        area.scaleX = 1f
        area.scaleY = 1f
    }

    private fun placeDepth(view: ViewGroup, small: ViewGroup, state: ClockSwitchState): Boolean {
        if (small.id == View.NO_ID) return false
        val layer = state.depth?.takeIf { it.context === view.context }
            ?: DepthLayer(view.context).also {
                it.clipToSelf = true
                state.depth = it
            }
        val index = view.indexOfChild(small) + 1
        if (layer.parent === view && view.indexOfChild(layer) == index) return false
        (layer.parent as? ViewGroup)?.removeView(layer)
        val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0).apply {
            addRule(RelativeLayout.ALIGN_TOP, small.id)
            addRule(RelativeLayout.ALIGN_BOTTOM, small.id)
        }
        view.addView(layer, view.indexOfChild(small) + 1, params)
        return true
    }

    private fun restore(view: ViewGroup, state: ClockSwitchState): Boolean {
        val slot = state.slot
        val depth = state.depth
        if (slot?.parent == null && depth?.parent == null && state.hidden.isEmpty()) return false
        (slot?.parent as? ViewGroup)?.removeView(slot)
        (depth?.parent as? ViewGroup)?.removeView(depth)
        state.hidden.toList().forEach { if (it.visibility == View.GONE) it.visibility = View.VISIBLE }
        state.hidden.clear()
        (child(view, SMALL_FRAME_ID) as? ViewGroup)?.let { small ->
            val params = small.layoutParams
            val width = state.frameWidth
            val height = state.frameHeight
            if (params != null && width != null && height != null) {
                params.width = width
                params.height = height
                small.layoutParams = params
            }
            small.setPaddingRelative(state.framePaddingStart, small.paddingTop, state.framePaddingEnd, small.paddingBottom)
        }
        return true
    }

    private fun onMainDisplay(view: View): Boolean = view.display?.displayId == Display.DEFAULT_DISPLAY

    private fun cancelAnimators(view: ViewGroup) {
        ANIMATOR_FIELDS.forEach { (view.fieldValue(it) as? Animator)?.cancel() }
    }

    private fun child(view: ViewGroup, name: String): View? {
        val id = view.resources.getIdentifier(name, ID_TYPE, view.context.packageName)
        return if (id == 0) null else view.findViewById(id)
    }
}

private val SWITCH_METHODS = setOf("updateClockViews", "animateClockChange", "setClock", "setClockPlugin")
private val ANIMATOR_FIELDS = listOf("mClockInAnim", "mClockOutAnim", "mStatusAreaAnim", "mSmartspaceAnim")
private val STATUS_TRANSLATIONS = listOf("translateYFromClockSize", "translateYFromClockDesign", "translateXFromClockDesign")
private const val SLICE_VIEW = "com.android.keyguard.KeyguardSliceView"
private const val TRANSLATE_X_AOD = "translateXFromAod"
private const val TRANSLATE_X_UNFOLD = "translateXFromUnfold"
private const val SMARTSPACE_FIELD = "mSmartspaceView"
private const val SMALL_FRAME_ID = "lockscreen_clock_view"
private const val LARGE_FRAME_ID = "lockscreen_clock_view_large"
private const val STATUS_AREA_ID = "keyguard_status_area"
private const val ID_TYPE = "id"
private const val CANCEL_GAP_MS = 500L
