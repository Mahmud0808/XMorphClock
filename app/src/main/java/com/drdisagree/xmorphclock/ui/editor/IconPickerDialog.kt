package com.drdisagree.xmorphclock.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.drdisagree.xmorphclock.engine.FaceFont
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.components.InputField
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IconPickerDialog(visible: Boolean, font: String, selected: String?, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(visible) { if (visible) query = "" }
    val family by produceState<FontFamily?>(null, font) {
        value = withContext(Dispatchers.Default) { FontFamily(Typeface(FaceFont.typeface(font.ifEmpty { SYMBOLS }, GLYPH_WEIGHT, GLYPH_WIDTH, GLYPH_HEIGHT))) }
    }
    val needle = query.trim()
    val groups = IconCatalog.groups
        .map { (label, icons) -> label to icons.filter { needle.isEmpty() || it.name.contains(needle, ignoreCase = true) } }
        .filter { it.second.isNotEmpty() }
    val list = rememberScrollState()
    GlassDialogFrame(visible, onDismiss) {
        Text(
            stringResource(R.string.choose_icon),
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 12.dp)
        )
        InputField(
            value = "",
            hint = stringResource(R.string.search_icons),
            onValueChange = { query = it },
            modifier = Modifier.padding(horizontal = 20.dp),
            key = visible
        )
        Column(
            Modifier
                .heightIn(max = LIST_MAX)
                .fadingEdges(list, Orientation.Vertical)
                .verticalScroll(list)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            if (groups.isEmpty()) {
                Text(
                    stringResource(R.string.no_icons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnInkMuted,
                    modifier = Modifier.padding(start = 8.dp, top = 20.dp)
                )
            }
            groups.forEach { (label, icons) ->
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnInkMuted,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                )
                icons.chunked(COLUMNS).forEach { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { icon -> IconTile(icon, family, icon.glyph == selected, { onPick(icon.glyph) }, Modifier.weight(1f)) }
                        repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: IconEntry, family: FontFamily?, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(TileShape)
            .background(if (selected) Accent.copy(alpha = SELECTED_FILL) else OnInk.copy(alpha = TILE_FILL))
            .clickable(role = Role.Button, onClickLabel = icon.name, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        family?.let { Text(icon.glyph, fontFamily = it, fontSize = GLYPH_SIZE, lineHeight = GLYPH_SIZE, color = OnInk) }
    }
}

private val TileShape = RoundedCornerShape(14.dp)
private val LIST_MAX = 440.dp
private val GLYPH_SIZE = 24.sp
private const val COLUMNS = 5
private const val TILE_FILL = 0.06f
private const val SELECTED_FILL = 0.22f
private const val SYMBOLS = "res:symbols"
private const val GLYPH_WEIGHT = 400
private const val GLYPH_WIDTH = 100
private const val GLYPH_HEIGHT = 700
