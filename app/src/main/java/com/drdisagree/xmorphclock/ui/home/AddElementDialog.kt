package com.drdisagree.xmorphclock.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.ElementKind
import com.drdisagree.xmorphclock.engine.FaceFont
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.editor.elementLabel
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AddElementDialog(visible: Boolean, onAdd: (ElementKind) -> Unit, onDismiss: () -> Unit) {
    val family by produceState<FontFamily?>(null) {
        value = withContext(Dispatchers.Default) { FontFamily(Typeface(FaceFont.typeface(SYMBOLS, GLYPH_WEIGHT, GLYPH_WIDTH, GLYPH_HEIGHT))) }
    }
    val list = rememberScrollState()
    GlassDialogFrame(visible, onDismiss) {
        Text(
            stringResource(R.string.action_add_element),
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 4.dp)
        )
        Column(
            Modifier
                .heightIn(max = LIST_MAX)
                .fadingEdges(list, Orientation.Vertical)
                .verticalScroll(list)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            ElementGroups.forEach { (label, kinds) ->
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnInkMuted,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                )
                kinds.chunked(COLUMNS).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { kind -> ElementRow(kind, family, { onAdd(kind) }, Modifier.weight(1f)) }
                        repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElementRow(kind: ElementKind, family: FontFamily?, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .height(ROW_HEIGHT)
            .clip(RowShape)
            .background(OnInk.copy(alpha = ROW_FILL))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(GLYPH_BOX), contentAlignment = Alignment.Center) {
            family?.let { Text(elementGlyph(kind), fontFamily = it, fontSize = GLYPH_SIZE, lineHeight = GLYPH_SIZE, color = OnInk) }
        }
        FitText(stringResource(elementLabel(kind)), style = MaterialTheme.typography.labelLarge, color = OnInk)
    }
}

private val ElementGroups = listOf(
    R.string.element_group_basic to listOf(ElementKind.TEXT, ElementKind.ICON, ElementKind.IMAGE, ElementKind.CARD, ElementKind.RING, ElementKind.BAR, ElementKind.SPACER),
    R.string.element_group_time to listOf(ElementKind.TIME, ElementKind.DATE, ElementKind.HOURS, ElementKind.MINUTES, ElementKind.HOUR_TENS, ElementKind.HOUR_ONES, ElementKind.MINUTE_TENS, ElementKind.MINUTE_ONES, ElementKind.HANDS),
    R.string.element_group_data to listOf(
        ElementKind.TEMPERATURE, ElementKind.WEATHER_ICON, ElementKind.CONDITION, ElementKind.HIGH_LOW, ElementKind.HUMIDITY, ElementKind.SUN_TIME, ElementKind.SUN_ICON, ElementKind.UV, ElementKind.HOURLY_TEMP, ElementKind.HOURLY_ICON, ElementKind.HOURLY_TIME,
        ElementKind.BATTERY, ElementKind.BATTERY_ICON, ElementKind.STEPS, ElementKind.DATA_USAGE, ElementKind.TILE, ElementKind.ALARM, ElementKind.ALARM_ICON, ElementKind.CHARGING, ElementKind.CHARGE_ICON, ElementKind.MEDIA_TITLE, ElementKind.MEDIA_ARTIST, ElementKind.MEDIA_ICON, ElementKind.NOTIFICATIONS, ElementKind.NOTIFICATION_ICON, ElementKind.EVENT_TITLE, ElementKind.EVENT_TIME, ElementKind.EVENT_ICON
    )
)

private val RowShape = RoundedCornerShape(16.dp)
private val ROW_HEIGHT = 48.dp
private val LIST_MAX = 480.dp
private val GLYPH_BOX = 22.dp
private val GLYPH_SIZE = 20.sp
private const val ROW_FILL = 0.06f
private const val COLUMNS = 2
private const val SYMBOLS = "res:symbols"
private const val GLYPH_WEIGHT = 400
private const val GLYPH_WIDTH = 100
private const val GLYPH_HEIGHT = 700
