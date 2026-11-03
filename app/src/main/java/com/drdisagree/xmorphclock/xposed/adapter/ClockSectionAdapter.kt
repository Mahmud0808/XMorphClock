package com.drdisagree.xmorphclock.xposed.adapter

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.drdisagree.xmorphclock.xposed.clock.ClockHost
import com.drdisagree.xmorphclock.xposed.clock.DepthLayer
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import com.drdisagree.xmorphclock.xposed.hook.callMethod
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import com.drdisagree.xmorphclock.xposed.hook.hookAfter
import com.drdisagree.xmorphclock.xposed.hook.onEachPreDrawGate
import com.drdisagree.xmorphclock.xposed.report.HookReporter
import com.drdisagree.xmorphclock.xposed.systemui.ClockViewModelTracker
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.WeakHashMap
import kotlin.math.abs

class ClockSectionAdapter : ClockAdapter {

    override val name = "ClockSection"

    private val binderClass: Class<*>? by lazy { HookEnv.findClass(BINDER_CLASS) }

    private val addClockViews: Method? by lazy {
        binderClass?.findMethod(ADD_CLOCK_VIEWS) { method ->
            method.parameterCount >= 2 && ViewGroup::class.java.isAssignableFrom(method.parameterTypes[1])
        }
    }

    private val cleanupClockViews: Method? by lazy {
        binderClass?.findMethod(CLEANUP_CLOCK_VIEWS) { it.parameterCount >= 2 }
    }

    private val currentClock: Field? by lazy { ClockViewModelTracker.field(CURRENT_CLOCK) }

    private val applyConstraints: Method? by lazy {
        HookEnv.findClass(SECTION_CLASS)?.findMethod(APPLY_CONSTRAINTS) { it.parameterCount == 1 }
    }

    private val constraints: ConstraintSetAccess? by lazy {
        applyConstraints?.parameterTypes?.firstOrNull()?.let { constraintSetAccess(it) }
    }

    private val slots = arrayOfNulls<View>(2)
    private var depthLayer: WeakReference<View>? = null
    private val stock = Array(2) { emptyList<WeakReference<View>>() }
    private val clockIds = IntArray(2) { View.NO_ID }
    private var active = false
    private var controller: WeakReference<Any>? = null
    private var extras = emptyList<WeakReference<View>>()
    private val hidden = WeakHashMap<View, Boolean>()
    private var lastScan = 0L
    private var scanUntil = 0L
    private var lastCancel = 0L
    private var smartspace: WeakReference<View>? = null
    private val cardLocation = IntArray(2)
    private val slotLocation = IntArray(2)
    private var watchedRoot: WeakReference<ViewGroup>? = null

    override fun isSupported(): Boolean =
        (addClockViews != null || currentClock != null) && applyConstraints != null && constraints != null

    override fun probes(): Map<String, Boolean> = linkedMapOf(
        "KeyguardClockViewBinder" to (binderClass != null),
        "addClockViews" to (addClockViews != null),
        "currentClock" to (currentClock != null),
        "cleanupClockViews" to (cleanupClockViews != null),
        "ClockSection.applyConstraints" to (applyConstraints != null),
        "ConstraintSet" to (constraints != null)
    )

    override fun install(host: ClockHost) {
        if (addClockViews == null) ClockViewModelTracker.install()
        addClockViews?.hookAfter { chain, _ ->
            HookReporter.count("addClockViews")
            val clock = chain.args.getOrNull(0) ?: return@hookAfter
            controller = WeakReference(clock)
            if (host.isEnabled) swap(host, clock)
        }
        cleanupClockViews?.hookAfter { _, _ -> detachSlots() }
        applyConstraints?.hookAfter { chain, _ ->
            HookReporter.count("applyConstraints")
            val set = chain.args.getOrNull(0) ?: return@hookAfter
            if (!host.isEnabled) {
                restore()
                return@hookAfter
            }
            if (addClockViews == null) adoptCurrentClock(host)
            if (slots.none { it?.parent != null }) controller?.get()?.let { swap(host, it) }
            constrain(set)
            slots.firstNotNullOfOrNull { it?.parent as? ViewGroup }?.let(::ensureDepthLayer)
        }
    }

    private fun adoptCurrentClock(host: ClockHost) {
        val latest = ClockViewModelTracker.flowValue(currentClock) ?: return
        if (latest === controller?.get()) return
        controller = WeakReference(latest)
        swap(host, latest)
    }

    private fun swap(host: ClockHost, controller: Any) {
        listOf(false, true).forEach { large ->
            val face = controller.callMethod(if (large) GET_LARGE_CLOCK else GET_SMALL_CLOCK) ?: return@forEach
            val main = face.callMethod(GET_VIEW) as? View ?: return@forEach
            val parent = main.parent as? ViewGroup ?: return@forEach
            val views = (face.callMethod(GET_LAYOUT)?.callMethod(GET_VIEWS) as? List<*>)
                ?.filterIsInstance<View>()
                ?.ifEmpty { null }
                ?: listOf(main)
            val index = parent.indexOfChild(main)
            val params = main.layoutParams
            val slot = slotFor(host, parent, large)
            if (main.id != View.NO_ID) clockIds[slotIndex(large)] = main.id
            (slot.parent as? ViewGroup)?.removeView(slot)
            slot.id = main.id
            slot.visibility = main.visibility
            views.forEach { (it.parent as? ViewGroup)?.removeView(it) }
            if (params != null) {
                parent.addView(slot, index.coerceIn(0, parent.childCount), params)
            } else {
                parent.addView(slot, index.coerceIn(0, parent.childCount))
            }
            stock[slotIndex(large)] = (listOf(main) + (views - main)).map { WeakReference(it) }
            HookReporter.count(if (large) "swapLarge" else "swapSmall")
            scanUntil = SystemClock.uptimeMillis() + SCAN_WINDOW_MS
            watchExtras(host, parent)
            ensureDepthLayer(parent)
        }
        if (!active) {
            active = true
            HookReporter.adapter(name)
        }
    }

    private fun watchExtras(host: ClockHost, root: ViewGroup) {
        if (watchedRoot?.get() === root) return
        watchedRoot = WeakReference(root)
        extras = emptyList()
        root.onEachPreDrawGate {
            val detached = extras.any { it.get()?.isAttachedToWindow != true }
            val now = SystemClock.uptimeMillis()
            val searching = extras.size < EXPECTED_EXTRAS && now < scanUntil
            if ((searching || detached) && now - lastScan > RESCAN_MS) {
                lastScan = now
                val found = collectExtras(root.rootView as? ViewGroup ?: root, hiddenIds(root))
                if (found.size != extras.size) HookEnv.log("Smartspace extras: ${found.size}")
                extras = found.map { WeakReference(it) }
            }
            var changed = host.isEnabled && enforce(host, root)
            extras.forEach { ref ->
                val view = ref.get() ?: return@forEach
                if (host.isEnabled) {
                    if (view.visibility != View.GONE) {
                        view.visibility = View.GONE
                        hidden[view] = true
                        changed = true
                    }
                } else if (hidden.remove(view) != null && view.isGone) {
                    view.visibility = View.VISIBLE
                    changed = true
                }
            }
            avoidSmartspace(root)
            if (changed && now - lastCancel > CANCEL_GAP_MS) {
                lastCancel = now
                false
            } else {
                true
            }
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun enforce(host: ClockHost, root: ViewGroup): Boolean {
        var changed = false
        listOf(false, true).forEach { large ->
            val index = slotIndex(large)
            val id = clockIds[index].takeIf { it != View.NO_ID }
                ?: root.resources.getIdentifier(CLOCK_IDS[index], ID_TYPE, root.context.packageName).takeIf { it != 0 }
                ?: return@forEach
            for (position in root.childCount - 1 downTo 0) {
                val child = root.getChildAt(position)
                if (child.id != id || child === slots[index]) continue
                val slot = slotFor(host, root, large)
                val params = child.layoutParams
                root.removeView(child)
                if (slot.parent == null) {
                    slot.id = id
                    if (params != null) root.addView(slot, position, params) else root.addView(slot, position)
                }
                stock[index] = (listOf(WeakReference(child)) + stock[index]).distinctBy { it.get() }
                changed = true
            }
        }
        if (changed) {
            HookReporter.count("stockReplaced")
            HookEnv.log("Stock clock re-added, replaced")
        }
        return changed
    }

    @SuppressLint("DiscouragedApi")
    private fun hiddenIds(root: ViewGroup): Set<Int> = HIDDEN_IDS
        .map { root.resources.getIdentifier(it, ID_TYPE, root.context.packageName) }
        .filter { it != 0 }
        .toSet()

    private fun avoidSmartspace(root: ViewGroup) {
        val slot = slots[slotIndex(true)] ?: return
        if (slot.parent == null) return
        val card = smartspace?.get()?.takeIf { it.isAttachedToWindow } ?: findSmartspace(root)?.also { smartspace = WeakReference(it) }
        val needed = if (card != null && card.isShown && card.height > 0 && slot.isShown) {
            card.getLocationOnScreen(cardLocation)
            slot.getLocationOnScreen(slotLocation)
            val gap = (SMARTSPACE_GAP_DP * slot.resources.displayMetrics.density).toInt()
            (cardLocation[1] + card.height + gap - slotLocation[1]).coerceAtLeast(0)
        } else {
            0
        }
        if (abs(needed - slot.paddingTop) > PADDING_SLACK) {
            slot.setPadding(slot.paddingLeft, needed, slot.paddingRight, slot.paddingBottom)
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun findSmartspace(root: ViewGroup): View? {
        val id = root.resources.getIdentifier(SMARTSPACE_ID, ID_TYPE, root.context.packageName)
        return if (id == 0) null else root.findViewById(id)
    }

    private fun collectExtras(group: ViewGroup, ids: Set<Int>): List<View> {
        val found = mutableListOf<View>()
        for (index in 0 until group.childCount) {
            val child = group.getChildAt(index)
            val name = child.javaClass.name
            if (name in HIDDEN_CLASSES || child.id in ids) found += child
            if (child is ViewGroup) found += collectExtras(child, ids)
        }
        return found
    }

    private fun slotFor(host: ClockHost, parent: ViewGroup, large: Boolean): View {
        val index = slotIndex(large)
        val existing = slots[index]
        if (existing != null && existing.context === parent.context) return existing
        return host.createSlot(parent.context, large, pinned = true).also { slots[index] = it }
    }

    private fun constrain(set: Any) {
        val access = constraints ?: return
        listOf(false, true).forEach { large ->
            val slot = slots[slotIndex(large)] ?: return@forEach
            if (slot.parent == null || slot.id == View.NO_ID) return@forEach
            try {
                access.wrapHeight(set, slot.id)
                if (!large) access.fillWidth(set, slot.id)
                val small = slots[slotIndex(false)]
                if (large && small != null && small.parent != null && small.id != View.NO_ID) access.alignTop(set, slot.id, small.id)
            } catch (throwable: Throwable) {
                HookEnv.log("Clock constraints failed", throwable)
            }
        }
    }

    private fun restore() {
        removeDepthLayer()
        listOf(false, true).forEach { large ->
            val index = slotIndex(large)
            val slot = slots[index] ?: return@forEach
            val parent = slot.parent as? ViewGroup ?: return@forEach
            val views = stock[index].mapNotNull { it.get() }.filter { it.parent == null }
            val position = parent.indexOfChild(slot)
            val params = slot.layoutParams
            parent.removeView(slot)
            views.forEachIndexed { offset, view ->
                val at = (position + offset).coerceIn(0, parent.childCount)
                if (offset == 0 && params != null) parent.addView(view, at, params) else parent.addView(view, at)
            }
            stock[index] = emptyList()
        }
        hidden.keys.toList().forEach { view -> if (view.isGone) view.isVisible = true }
        hidden.clear()
    }

    private fun ensureDepthLayer(parent: ViewGroup) {
        val layer = depthLayer?.get() ?: DepthLayer(parent.context).also {
            it.id = View.generateViewId()
            depthLayer = WeakReference(it)
        }
        val ours = slots.filterNotNull().filter { it.parent === parent }.sortedBy { parent.indexOfChild(it) }
        if (ours.isEmpty()) return
        if (layer.parent !== parent) {
            (layer.parent as? ViewGroup)?.removeView(layer)
            parent.addView(layer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val front = ours + layer
        if (front.withIndex().all { (index, view) -> parent.getChildAt(index) === view }) return
        val others = (0 until parent.childCount).map(parent::getChildAt).filter { it !in front }
        front.forEach(parent::bringChildToFront)
        others.forEach(parent::bringChildToFront)
    }

    private fun removeDepthLayer() {
        depthLayer?.get()?.let { layer -> (layer.parent as? ViewGroup)?.removeView(layer) }
    }

    private fun detachSlots() {
        removeDepthLayer()
        slots.forEach { slot -> (slot?.parent as? ViewGroup)?.removeView(slot) }
        stock.indices.forEach { stock[it] = emptyList() }
    }

    private fun slotIndex(large: Boolean): Int = if (large) 1 else 0
}

private const val BINDER_CLASS = "com.android.systemui.keyguard.ui.binder.KeyguardClockViewBinder"
private const val SECTION_CLASS = "com.android.systemui.keyguard.ui.view.layout.sections.ClockSection"
private const val ADD_CLOCK_VIEWS = "addClockViews"
private const val CLEANUP_CLOCK_VIEWS = "cleanupClockViews"
private const val CURRENT_CLOCK = "currentClock"
private const val APPLY_CONSTRAINTS = "applyConstraints"
private const val GET_SMALL_CLOCK = "getSmallClock"
private const val GET_LARGE_CLOCK = "getLargeClock"
private const val GET_VIEW = "getView"
private const val GET_LAYOUT = "getLayout"
private const val GET_VIEWS = "getViews"
private const val DATE_VIEW = "com.google.android.systemui.smartspace.DateSmartspaceView"
private const val WEATHER_VIEW = "com.google.android.systemui.smartspace.WeatherSmartspaceView"
private val HIDDEN_CLASSES = setOf(DATE_VIEW, WEATHER_VIEW)
private const val RESCAN_MS = 100L
private const val SCAN_WINDOW_MS = 5_000L
private const val CANCEL_GAP_MS = 500L
private const val EXPECTED_EXTRAS = 5
private val HIDDEN_IDS = listOf("date_smartspace_view", "date_smartspace_view_large", "bc_smartspace_view")
private const val SMARTSPACE_ID = "bc_smartspace_view"
private val CLOCK_IDS = listOf("lockscreen_clock_view", "lockscreen_clock_view_large")
private const val ID_TYPE = "id"
private const val SMARTSPACE_GAP_DP = 8f
private const val PADDING_SLACK = 2
