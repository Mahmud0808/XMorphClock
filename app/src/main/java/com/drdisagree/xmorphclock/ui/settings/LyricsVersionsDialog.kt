package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.DialogButton
import com.drdisagree.xmorphclock.ui.components.GlassDialogFrame
import com.drdisagree.xmorphclock.ui.components.rememberSpin
import com.drdisagree.xmorphclock.ui.components.fadingEdges
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LyricsVersionsDialog(
    state: LyricsVersionsUiState?,
    onPick: (LyricsVersion?) -> Unit,
    onDismiss: () -> Unit
) {
    var shown by remember { mutableStateOf(state) }
    LaunchedEffect(state) { if (state != null) shown = state }
    GlassDialogFrame(state != null, onDismiss) {
        val current = state ?: shown ?: return@GlassDialogFrame
        Text(
            stringResource(R.string.lyrics_versions_title),
            style = MaterialTheme.typography.titleLarge,
            color = OnInk,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp)
        )
        Text(
            current.playing.title,
            style = MaterialTheme.typography.bodyMedium,
            color = OnInkMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 2.dp, bottom = 12.dp)
        )
        AnimatedContent(
            targetState = current.loading,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
            label = "lyricsVersions"
        ) { loading ->
            if (loading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(LOADING_HEIGHT),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val angle by rememberSpin(active = true, durationMs = SPIN_MS)
                    AppIcon(
                        R.drawable.ic_refresh,
                        tint = OnInkMuted,
                        size = 20.dp,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .graphicsLayer { rotationZ = angle }
                    )
                    Text(stringResource(R.string.lyrics_versions_searching), style = MaterialTheme.typography.bodyMedium, color = OnInkMuted)
                }
            } else {
                val list = rememberLazyListState()
                LazyColumn(
                    state = list,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .heightIn(max = LIST_MAX)
                        .fadingEdges(list, Orientation.Vertical)
                ) {
                    item(key = AUTO_KEY) {
                        VersionRow(
                            title = stringResource(R.string.lyrics_versions_auto),
                            detail = stringResource(R.string.lyrics_versions_auto_summary),
                            selected = current.playing.tuning.pinned == null,
                            onClick = { onPick(null) }
                        )
                    }
                    items(current.versions, key = { it.id }) { version ->
                        VersionRow(
                            title = version.preview.ifEmpty { version.source },
                            detail = detailOf(version, current.playing.durationMs),
                            selected = current.playing.tuning.pinned == version.id,
                            onClick = { onPick(version) }
                        )
                    }
                    if (current.versions.isEmpty()) {
                        item(key = EMPTY_KEY) {
                            Text(
                                stringResource(if (current.failed) R.string.lyrics_versions_failed else R.string.lyrics_versions_none),
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnInkMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
        Box(
            Modifier
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                .fillMaxWidth()
        ) {
            DialogButton(stringResource(R.string.action_close), primary = false, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun VersionRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val ring by animateColorAsState(if (selected) Accent else Color.Transparent, tween(220), label = "versionRing")
    val fill by animateColorAsState(if (selected) Accent.copy(alpha = SELECTED_FILL) else OnInk.copy(alpha = ROW_FILL), tween(220), label = "versionFill")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(fill)
            .border(RING, ring, RowShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = OnInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = OnInkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "versionCheck"
        ) { checked ->
            if (checked) AppIcon(R.drawable.ic_check, tint = Accent, size = 20.dp, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun detailOf(version: LyricsVersion, durationMs: Long): String {
    val parts = ArrayList<String>()
    parts += version.source
    version.artist?.let { parts += it }
    version.durationSec?.let { seconds ->
        val total = seconds.roundToInt()
        parts += stringResource(R.string.lyrics_version_length, total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE)
        if (durationMs > 0) {
            val gap = (seconds - durationMs / MILLIS).roundToInt()
            parts += when {
                abs(gap) <= SAME_LENGTH_SECONDS -> stringResource(R.string.lyrics_same_length)
                gap > 0 -> stringResource(R.string.lyrics_longer, gap)
                else -> stringResource(R.string.lyrics_shorter, -gap)
            }
        }
    }
    return parts.joinToString(SEPARATOR)
}

private val RowShape = RoundedCornerShape(18.dp)
private val RING = 1.5.dp
private val LIST_MAX = 380.dp
private val LOADING_HEIGHT = 120.dp
private const val ROW_FILL = 0.06f
private const val SPIN_MS = 900
private const val SELECTED_FILL = 0.14f
private const val SAME_LENGTH_SECONDS = 3
private const val SECONDS_PER_MINUTE = 60
private const val MILLIS = 1000.0
private const val SEPARATOR = " · "
private const val AUTO_KEY = "auto"
private const val EMPTY_KEY = "empty"
