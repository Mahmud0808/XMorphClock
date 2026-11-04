package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk

@Composable
fun ValueField(display: String, editable: String, onCommit: (Float) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = TABULAR_FEATURE, color = OnInk, textAlign = TextAlign.End)
    AnimatedContent(
        targetState = editing,
        transitionSpec = { fadeIn(tween(FADE_IN_MS)) togetherWith fadeOut(tween(FADE_OUT_MS)) },
        contentAlignment = Alignment.CenterEnd,
        label = "valueField"
    ) { active ->
        if (active) {
            val focus = remember { FocusRequester() }
            var field by remember { mutableStateOf(TextFieldValue(editable, TextRange(0, editable.length))) }
            var focused by remember { mutableStateOf(false) }
            fun commit() {
                if (!editing) return
                editing = false
                field.text.replace(',', '.').toFloatOrNull()?.let(onCommit)
            }
            LaunchedEffect(Unit) { focus.requestFocus() }
            Box(
                Modifier
                    .background(OnInk.copy(alpha = FIELD_FILL), FieldShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                BasicTextField(
                    value = field,
                    onValueChange = { next -> if (next.text.length <= MAX_CHARS && next.text.all { it.isDigit() || it == '.' || it == ',' }) field = next },
                    singleLine = true,
                    textStyle = style,
                    cursorBrush = SolidColor(Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier
                        .width(FIELD_WIDTH)
                        .focusRequester(focus)
                        .onFocusChanged { state ->
                            if (focused && !state.isFocused) commit()
                            focused = state.isFocused
                        }
                )
            }
        } else {
            Text(
                display,
                style = style,
                maxLines = 1,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = { editing = true })
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

private val FieldShape = RoundedCornerShape(10.dp)
private val FIELD_WIDTH = 64.dp
private const val FIELD_FILL = 0.1f
private const val MAX_CHARS = 6
private const val FADE_IN_MS = 180
private const val FADE_OUT_MS = 120
private const val TABULAR_FEATURE = "tnum"
