package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

internal fun DrawScope.checkerboard() {
    val cell = CHECKER_CELL.toPx()
    drawRect(CheckerLight)
    var row = 0
    var y = 0f
    while (y < size.height) {
        var x = if (row % 2 == 0) 0f else cell
        while (x < size.width) {
            drawRect(CheckerDark, Offset(x, y), Size(cell, cell))
            x += cell * 2f
        }
        y += cell
        row++
    }
}

private val CHECKER_CELL = 5.dp
private val CheckerLight = Color(0xFF8A8A8A)
private val CheckerDark = Color(0xFF5E5E5E)
