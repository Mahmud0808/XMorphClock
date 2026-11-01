package com.drdisagree.xmorphclock.xposed.adapter

import com.drdisagree.xmorphclock.xposed.hook.findField
import com.drdisagree.xmorphclock.xposed.hook.findMethod
import java.lang.reflect.Method

internal class ConstraintSetAccess(
    private val constrainWidth: Method,
    private val constrainHeight: Method,
    private val constrainMaxHeight: Method?,
    private val connect: Method,
    private val getConstraint: Method?,
    private val setVerticalBias: Method?
) {

    fun wrapHeight(set: Any, id: Int) {
        constrainHeight.invoke(set, id, WRAP_CONTENT)
        constrainMaxHeight?.invoke(set, id, 0)
        setVerticalBias?.invoke(set, id, TOP_BIAS)
    }

    fun alignTop(set: Any, id: Int, anchor: Int) {
        connect.invoke(set, id, TOP, anchor, TOP, 0)
    }

    fun fillWidth(set: Any, id: Int) {
        val margin = startMargin(set, id)
        constrainWidth.invoke(set, id, MATCH_CONSTRAINT)
        connect.invoke(set, id, END, PARENT_ID, END, margin)
    }

    private fun startMargin(set: Any, id: Int): Int = try {
        val constraint = getConstraint?.invoke(set, id)
        val layout = constraint?.let { it.javaClass.findField(LAYOUT)?.get(it) }
        layout?.let { it.javaClass.findField(START_MARGIN)?.getInt(it) } ?: 0
    } catch (_: Throwable) {
        0
    }
}

internal fun constraintSetAccess(type: Class<*>): ConstraintSetAccess? {
    val int = Int::class.javaPrimitiveType
    fun ints(count: Int): (Method) -> Boolean = { method ->
        method.parameterCount == count && method.parameterTypes.all { it == int }
    }
    return ConstraintSetAccess(
        constrainWidth = type.findMethod(CONSTRAIN_WIDTH, ints(2)) ?: return null,
        constrainHeight = type.findMethod(CONSTRAIN_HEIGHT, ints(2)) ?: return null,
        constrainMaxHeight = type.findMethod(CONSTRAIN_MAX_HEIGHT, ints(2)),
        connect = type.findMethod(CONNECT, ints(5)) ?: return null,
        getConstraint = type.findMethod(GET_CONSTRAINT, ints(1)),
        setVerticalBias = type.findMethod(SET_VERTICAL_BIAS) { method ->
            method.parameterCount == 2 && method.parameterTypes[0] == int &&
                    method.parameterTypes[1] == Float::class.javaPrimitiveType
        }
    )
}

private const val PARENT_ID = 0
private const val END = 7
private const val TOP = 3
private const val WRAP_CONTENT = -2
private const val MATCH_CONSTRAINT = 0
private const val LAYOUT = "layout"
private const val START_MARGIN = "startMargin"
private const val CONSTRAIN_WIDTH = "constrainWidth"
private const val CONSTRAIN_HEIGHT = "constrainHeight"
private const val CONSTRAIN_MAX_HEIGHT = "constrainMaxHeight"
private const val CONNECT = "connect"
private const val GET_CONSTRAINT = "getConstraint"
private const val SET_VERTICAL_BIAS = "setVerticalBias"
private const val TOP_BIAS = 0f
