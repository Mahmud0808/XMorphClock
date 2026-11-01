package com.drdisagree.xmorphclock.xposed.compose

import android.content.Context
import android.view.View
import com.drdisagree.xmorphclock.xposed.hook.HookEnv
import java.lang.reflect.Method
import kotlin.math.roundToInt

object ComposeViewHost {

    private const val ANDROID_VIEW_CLASS = "androidx.compose.ui.viewinterop.AndroidView_androidKt"
    private const val ANDROID_VIEW_METHOD = "AndroidView"
    private const val UPDATE_DEFAULT_MASK = 1 shl 2
    private const val LAYOUT_MODIFIER_CLASS = "androidx.compose.ui.layout.LayoutModifierKt"
    private const val CONSTRAINTS_CLASS = "androidx.compose.ui.unit.ConstraintsKt"
    private const val MEASURABLE_CLASS = "androidx.compose.ui.layout.Measurable"
    private const val MEASURE_SCOPE_CLASS = "androidx.compose.ui.layout.MeasureScope"
    private const val PLACEMENT_SCOPE_CLASS = "androidx.compose.ui.layout.Placeable\$PlacementScope"
    private const val COORDINATES_KT_CLASS = "androidx.compose.ui.layout.LayoutCoordinatesKt"
    private const val COORDINATES_CLASS = "androidx.compose.ui.layout.LayoutCoordinates"
    private const val LAYOUT = "layout"
    private const val CONSTRAINTS = "Constraints"
    private const val MEASURE = "measure"
    private const val PLACE = "place"
    private const val GET_COORDINATES = "getCoordinates"
    private const val FIND_ROOT = "findRootCoordinates"
    private const val LOCAL_POSITION = "localPositionOf"
    private const val LOW_BITS = 0xFFFFFFFFL

    private val androidView: Method? by lazy {
        val function1 = ComposeToolkit.functionClass(1)?.name
        HookEnv.findClass(ANDROID_VIEW_CLASS)?.declaredMethods?.firstOrNull { method ->
            val types = method.parameterTypes
            method.name == ANDROID_VIEW_METHOD &&
                    types.size == 6 &&
                    types[0].name == function1 &&
                    types[1].name == ComposeToolkit.MODIFIER_CLASS &&
                    types[2].name == function1 &&
                    types[3].name == ComposeToolkit.COMPOSER_CLASS
        }?.apply { isAccessible = true }
    }

    private val layoutModifier: Method? by lazy {
        ComposeToolkit.modifierFunction(LAYOUT_MODIFIER_CLASS, LAYOUT, ComposeToolkit.functionClass(3))
    }

    private val constraints: Method? by lazy {
        ComposeToolkit.staticMethod(CONSTRAINTS_CLASS) { it.name == CONSTRAINTS && it.parameterTypes.size == 4 }
    }

    private val measure: Method? by lazy {
        HookEnv.findClass(MEASURABLE_CLASS)?.methods?.firstOrNull { it.name.startsWith(MEASURE) && it.parameterTypes.size == 1 }
    }

    private val layoutResult: Method? by lazy {
        HookEnv.findClass(MEASURE_SCOPE_CLASS)?.methods?.firstOrNull {
            it.name == LAYOUT && it.parameterTypes.size == 4 && it.parameterTypes[2] == Map::class.java
        }
    }

    private val place: Method? by lazy {
        HookEnv.findClass(PLACEMENT_SCOPE_CLASS)?.methods?.firstOrNull { it.name == PLACE && it.parameterTypes.size == 4 }
    }

    private val placementCoordinates: Method? by lazy {
        HookEnv.findClass(PLACEMENT_SCOPE_CLASS)?.methods?.firstOrNull { it.name == GET_COORDINATES && it.parameterTypes.isEmpty() }
    }

    private val findRootCoordinates: Method? by lazy {
        ComposeToolkit.staticMethod(COORDINATES_KT_CLASS) { it.name == FIND_ROOT && it.parameterTypes.size == 1 }
    }

    private val localPositionOf: Method? by lazy {
        HookEnv.findClass(COORDINATES_CLASS)?.methods?.firstOrNull {
            it.name.startsWith(LOCAL_POSITION) && it.parameterTypes.size == 2 && it.parameterTypes[1] == Long::class.javaPrimitiveType
        }
    }

    fun windowFillModifier(size: () -> Pair<Int, Int>): Any? = try {
        val constraintsMethod = constraints ?: throw IllegalStateException()
        val measureMethod = measure ?: throw IllegalStateException()
        val layoutMethod = layoutResult ?: throw IllegalStateException()
        val placeMethod = place ?: throw IllegalStateException()
        val block = ComposeToolkit.function3("XMorphWindowFill") { scope, measurable, _ ->
            val (width, height) = size()
            val w = width.coerceAtLeast(0)
            val h = height.coerceAtLeast(0)
            val placeable = measureMethod.invoke(measurable, constraintsMethod.invoke(null, w, w, h, h))
            val placement = ComposeToolkit.function1("XMorphWindowFillPlacement") { placementScope ->
                val (x, y) = runCatching { positionInRoot(placementScope) }.getOrDefault(0 to 0)
                placeMethod.invoke(placementScope, placeable, -x, -y, 0f)
            }
            layoutMethod.invoke(scope, 0, 0, emptyMap<Any, Any>(), placement)
        }
        if (block == null) null else layoutModifier?.invoke(null, ComposeToolkit.emptyModifier, block)
    } catch (_: Throwable) {
        null
    }

    private fun positionInRoot(placementScope: Any?): Pair<Int, Int> {
        val coordinates = placementCoordinates?.invoke(placementScope) ?: return 0 to 0
        val root = findRootCoordinates?.invoke(null, coordinates) ?: return 0 to 0
        val packed = localPositionOf?.invoke(root, coordinates, 0L) as? Long ?: return 0 to 0
        val x = Float.fromBits((packed ushr 32).toInt())
        val y = Float.fromBits((packed and LOW_BITS).toInt())
        return x.roundToInt() to y.roundToInt()
    }

    val isAvailable: Boolean
        get() = androidView != null && ComposeToolkit.isAvailable

    fun emitInGroup(composer: Any, key: Int, modifier: Any?, factory: (Context) -> View) {
        ComposeToolkit.inGroup(composer, key) { emit(composer, modifier, factory) }
    }

    private fun emit(composer: Any, modifier: Any?, factory: (Context) -> View) {
        val method = androidView ?: return
        val factoryFunction = ComposeToolkit.function1("XMorphViewFactory") { context ->
            factory(context as Context)
        } ?: return
        method.invoke(
            null,
            factoryFunction,
            modifier ?: ComposeToolkit.emptyModifier,
            null,
            composer,
            0,
            UPDATE_DEFAULT_MASK
        )
    }
}
