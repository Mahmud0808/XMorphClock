package com.drdisagree.xmorphclock.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    minSize: TextUnit = MIN_SIZE
) {
    val resolved = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val max = if (style.fontSize.isSpecified) style.fontSize else DEFAULT_SIZE
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolved),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = max)
    )
}

private val MIN_SIZE = 9.sp
private val DEFAULT_SIZE = 14.sp
