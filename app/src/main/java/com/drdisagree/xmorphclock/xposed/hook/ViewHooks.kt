package com.drdisagree.xmorphclock.xposed.hook

import android.view.View
import android.view.ViewTreeObserver

fun View.onEachPreDraw(block: () -> Unit) {
    val listener = ViewTreeObserver.OnPreDrawListener {
        if (isAttachedToWindow) block()
        true
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            v.viewTreeObserver.removeOnPreDrawListener(listener)
            v.viewTreeObserver.addOnPreDrawListener(listener)
        }

        override fun onViewDetachedFromWindow(v: View) {
            v.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    })
    if (isAttachedToWindow) viewTreeObserver.addOnPreDrawListener(listener)
}

fun View.onEachPreDrawGate(block: () -> Boolean) {
    val listener = ViewTreeObserver.OnPreDrawListener { !isAttachedToWindow || block() }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            v.viewTreeObserver.removeOnPreDrawListener(listener)
            v.viewTreeObserver.addOnPreDrawListener(listener)
        }

        override fun onViewDetachedFromWindow(v: View) {
            v.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    })
    if (isAttachedToWindow) viewTreeObserver.addOnPreDrawListener(listener)
}
