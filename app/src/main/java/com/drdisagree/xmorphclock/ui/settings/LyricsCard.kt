package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import com.drdisagree.xmorphclock.ui.components.AppSwitch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.LyricsOrigin
import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.ui.components.AppButton
import com.drdisagree.xmorphclock.ui.components.GlassIconButton
import com.drdisagree.xmorphclock.ui.components.MarqueeText
import com.drdisagree.xmorphclock.ui.theme.LocalUiStyle
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import java.text.DecimalFormat
import kotlin.math.abs

@Composable
internal fun LyricsCard(
    online: Boolean,
    onOnlineChange: (Boolean) -> Unit,
    playing: NowPlayingLyrics?,
    onTuningChange: (NowPlayingLyrics, LyricsTuning) -> Unit,
    onVersions: (NowPlayingLyrics) -> Unit
) {
    var last by remember { mutableStateOf(playing) }
    if (playing != null) last = playing
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Switch) { onOnlineChange(!online) }
                .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.setting_lyrics_online), style = MaterialTheme.typography.bodyLarge, color = OnInk)
                Text(stringResource(R.string.setting_lyrics_online_summary), style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
            }
            AppSwitch(selected = online, onSelect = onOnlineChange)
        }
        AnimatedVisibility(
            visible = playing != null && (online || playing.origin == LyricsOrigin.PLAYER),
            enter = fadeIn(tween(240)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(280))
        ) {
            last?.let { song -> SyncSection(song, { onTuningChange(song, it) }, { onVersions(song) }) }
        }
    }
}

@Composable
private fun SyncSection(playing: NowPlayingLyrics, onTuningChange: (LyricsTuning) -> Unit, onVersions: () -> Unit) {
    val tuning = playing.tuning
    val nudge = { delta: Long -> onTuningChange(tuning.copy(offsetMs = (tuning.offsetMs + delta).coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS))) }
    val versionsShown = !tuning.hidden && playing.origin != LyricsOrigin.PLAYER
    Column {
        Column(Modifier.padding(start = 20.dp, end = 20.dp)) {
            MarqueeText(
                listOfNotNull(playing.title, playing.artist?.takeIf { it.isNotBlank() }).joinToString(SEPARATOR),
                style = MaterialTheme.typography.titleSmall,
                color = OnInk,
                modifier = Modifier.fillMaxWidth(),
                alignment = Alignment.CenterStart
            )
            AnimatedContent(
                targetState = statusOf(playing),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "lyricsStatus"
            ) { status ->
                Text(status, style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
            }
        }
        Row(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 14.dp)
                .fillMaxWidth()
                .clip(WellShape)
                .background(OnInk.copy(alpha = WELL_FILL))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = R.drawable.ic_minus,
                onClick = { nudge(-STEP_MS) },
                size = 40.dp,
                description = stringResource(R.string.lyrics_show_later)
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = tuning.offsetMs,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    label = "lyricsOffset"
                ) { offset ->
                    Text(offsetLabel(offset), style = MaterialTheme.typography.titleSmall, color = OnInk, textAlign = TextAlign.Center)
                }
                Text(stringResource(R.string.lyrics_timing), style = MaterialTheme.typography.bodySmall, color = OnInkMuted)
            }
            GlassIconButton(
                icon = R.drawable.ic_plus,
                onClick = { nudge(STEP_MS) },
                size = 40.dp,
                description = stringResource(R.string.lyrics_show_earlier)
            )
        }
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = versionsShown,
                modifier = Modifier.weight(1f, fill = false),
                enter = fadeIn(tween(240)) + expandHorizontally(tween(320)),
                exit = fadeOut(tween(160)) + shrinkHorizontally(tween(280))
            ) {
                ActionButton(label = stringResource(R.string.lyrics_versions), onClick = onVersions)
            }
            ActionButton(
                label = stringResource(if (tuning.hidden) R.string.lyrics_show else R.string.lyrics_hide),
                onClick = { onTuningChange(tuning.copy(hidden = !tuning.hidden)) },
                modifier = Modifier.weight(1f, fill = false)
            )
            AnimatedVisibility(
                visible = tuning.offsetMs != 0L || tuning.hidden,
                enter = fadeIn(tween(240)) + expandHorizontally(tween(320)),
                exit = fadeOut(tween(160)) + shrinkHorizontally(tween(280))
            ) {
                GlassIconButton(
                    icon = R.drawable.ic_reset,
                    onClick = { onTuningChange(tuning.copy(offsetMs = 0L, hidden = false)) },
                    size = 40.dp,
                    description = stringResource(R.string.lyrics_reset)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val glass = LocalUiStyle.current == UiStyle.GLASS
    AppButton(onClick = onClick, modifier = modifier, height = 40.dp, horizontalPadding = 16.dp) {
        val color = if (glass) OnInk else LocalContentColor.current
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
            label = "lyricsAction"
        ) { text ->
            Text(text, style = MaterialTheme.typography.labelLarge, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun statusOf(playing: NowPlayingLyrics): String = when {
    playing.tuning.hidden -> stringResource(R.string.lyrics_hidden)
    playing.origin == LyricsOrigin.PLAYER -> stringResource(R.string.lyrics_from_player)
    playing.origin == LyricsOrigin.PINNED -> stringResource(R.string.lyrics_pinned)
    playing.origin == LyricsOrigin.ONLINE_OTHER_LENGTH -> stringResource(R.string.lyrics_other_length)
    playing.origin == LyricsOrigin.ONLINE -> stringResource(R.string.lyrics_found_online)
    else -> stringResource(R.string.lyrics_not_found)
}

@Composable
private fun offsetLabel(offset: Long): String {
    if (offset == 0L) return stringResource(R.string.lyrics_in_sync)
    val seconds = SECONDS_FORMAT.format(abs(offset) / MILLIS)
    return stringResource(if (offset > 0) R.string.lyrics_early else R.string.lyrics_late, seconds)
}

private val WellShape = RoundedCornerShape(20.dp)
private val SECONDS_FORMAT = DecimalFormat("0.##")
private const val SEPARATOR = " · "
private const val WELL_FILL = 0.06f
private const val STEP_MS = 250L
private const val MAX_OFFSET_MS = 10_000L
private const val MILLIS = 1000f
