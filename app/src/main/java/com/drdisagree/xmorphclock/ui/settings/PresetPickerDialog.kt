package com.drdisagree.xmorphclock.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.ClockFacePreview
import com.drdisagree.xmorphclock.ui.components.DialogButton
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.home.ThumbInk
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.DarkPalette
import com.drdisagree.xmorphclock.ui.theme.LocalPalette
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted

@Composable
fun PresetPickerDialog(
    visible: Boolean,
    title: String,
    message: String,
    items: List<PresetPickerItem>,
    initialSelection: Set<Int>,
    @StringRes confirmLabel: Int,
    onConfirm: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialogFrame(visible, onDismiss) {
        var selected by rememberSaveable(stateSaver = SelectionSaver) { mutableStateOf(initialSelection) }
        val all = remember(items) { items.mapTo(HashSet()) { it.key } }
        val everything = all.isNotEmpty() && selected.containsAll(all)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = OnInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (items.isNotEmpty()) {
                SelectionAction(everything) { selected = if (everything) emptySet() else all }
            }
        }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = OnInkMuted,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
        )
        if (items.isNotEmpty()) {
            val list = rememberLazyListState()
            LazyColumn(
                state = list,
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .heightIn(max = LIST_MAX)
                    .fadingEdges(list, Orientation.Vertical)
            ) {
                items(items, key = { it.key }) { item ->
                    PresetRow(item, item.key in selected) {
                        selected = if (item.key in selected) selected - item.key else selected + item.key
                    }
                }
            }
        }
        Box(
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                .fillMaxWidth()
        ) {
            if (items.isEmpty()) {
                DialogButton(stringResource(R.string.action_close), primary = true, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            } else {
                val ready = selected.isNotEmpty()
                val alpha by animateFloatAsState(if (ready) 1f else DISABLED_ALPHA, tween(200), label = "pickerReady")
                DialogButton(
                    stringResource(confirmLabel, selected.size),
                    primary = true,
                    onClick = { if (ready) onConfirm(items.map { it.key }.filter { it in selected }) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { this.alpha = alpha }
                )
            }
        }
    }
}

@Composable
private fun SelectionAction(everything: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .height(ACTION_HEIGHT)
            .clip(CircleShape)
            .background(OnInk.copy(alpha = ROW_FILL))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = everything,
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = SWAP_SCALE)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = SWAP_SCALE)) using SizeTransform(clip = false)
            },
            contentAlignment = Alignment.Center,
            label = "selectionAction"
        ) { cleared ->
            Text(
                stringResource(if (cleared) R.string.action_clear else R.string.action_select_all),
                style = MaterialTheme.typography.labelLarge,
                color = OnInk,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PresetRow(item: PresetPickerItem, checked: Boolean, onToggle: () -> Unit) {
    val fill by animateColorAsState(if (checked) Accent else OnInk.copy(alpha = UNCHECKED_FILL), tween(220), label = "presetCheck")
    val mark by animateFloatAsState(if (checked) 1f else 0f, tween(220), label = "presetMark")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .height(ROW_HEIGHT)
            .clip(RowShape)
            .background(OnInk.copy(alpha = ROW_FILL))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(start = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(THUMB_WIDTH, THUMB_HEIGHT)
                .clip(ThumbShape)
                .background(ThumbInk)
                .padding(THUMB_PADDING),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalPalette provides DarkPalette) {
                ClockFacePreview(face = item.face, modifier = Modifier.fillMaxWidth())
            }
        }
        Text(
            item.face.name,
            style = MaterialTheme.typography.labelLarge,
            color = OnInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(CHECK_SIZE)
                .drawBehind { drawCircle(fill) },
            contentAlignment = Alignment.Center
        ) {
            AppIcon(R.drawable.ic_check, tint = Color.White, size = CHECK_ICON, modifier = Modifier.graphicsLayer {
                alpha = mark
                scaleX = mark
                scaleY = mark
            })
        }
    }
}

private val SelectionSaver = listSaver<Set<Int>, Int>(save = { it.toList() }, restore = { it.toSet() })
private val RowShape = RoundedCornerShape(16.dp)
private val ThumbShape = RoundedCornerShape(10.dp)
private val ROW_HEIGHT = 60.dp
private val THUMB_WIDTH = 72.dp
private val THUMB_HEIGHT = 44.dp
private val THUMB_PADDING = 6.dp
private val ACTION_HEIGHT = 32.dp
private val LIST_MAX = 360.dp
private val CHECK_SIZE = 22.dp
private val CHECK_ICON = 13.dp
private const val ROW_FILL = 0.06f
private const val UNCHECKED_FILL = 0.12f
private const val DISABLED_ALPHA = 0.4f
private const val SWAP_SCALE = 0.9f
