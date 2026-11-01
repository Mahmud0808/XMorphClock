package com.drdisagree.xmorphclock.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.WeatherProvider
import com.drdisagree.xmorphclock.domain.model.WeatherSettings
import com.drdisagree.xmorphclock.domain.model.WeatherState
import com.drdisagree.xmorphclock.domain.model.WeatherStatus
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.GlassIconButton
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.components.rememberSpin
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import java.text.DateFormat
import java.util.Date

@Composable
fun WeatherCard(
    state: WeatherState,
    notFound: Boolean,
    onChange: (WeatherSettings) -> Unit,
    onLocation: (String?) -> Unit,
    onRefresh: () -> Unit
) {
    var custom by remember(state.settings.hasCustomLocation) { mutableStateOf(state.settings.hasCustomLocation) }
    val settings = state.settings
    GlassCard {
        Row(
            Modifier.padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.section_weather), style = MaterialTheme.typography.titleSmall, color = OnInk)
                Text(
                    statusText(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val spin by rememberSpin(state.status == WeatherStatus.UPDATING, REFRESH_SPIN_MS)
            GlassIconButton(
                R.drawable.ic_refresh,
                onClick = onRefresh,
                size = 40.dp,
                description = stringResource(R.string.action_refresh),
                iconModifier = Modifier.graphicsLayer { rotationZ = spin }
            )
        }
        Choice(
            label = stringResource(R.string.weather_provider),
            options = listOf(stringResource(R.string.weather_open_meteo), stringResource(R.string.weather_open_weather_map)),
            selected = settings.provider.ordinal,
            onSelect = { onChange(settings.copy(provider = WeatherProvider.entries[it])) }
        )
        AnimatedVisibility(
            visible = settings.provider == WeatherProvider.OPEN_WEATHER_MAP,
            enter = fadeIn(tween(240)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(280))
        ) {
            TextInput(
                value = settings.apiKey,
                hint = stringResource(R.string.weather_api_key),
                password = true,
                commitOnDone = true,
                onCommit = { onChange(settings.copy(apiKey = it.trim())) }
            )
        }
        Choice(
            label = stringResource(R.string.weather_location),
            options = listOf(stringResource(R.string.weather_location_auto), stringResource(R.string.weather_location_custom)),
            selected = if (custom) 1 else 0,
            onSelect = { index ->
                custom = index == 1
                if (index == 0 && settings.hasCustomLocation) onLocation(null)
            }
        )
        AnimatedVisibility(
            visible = custom,
            enter = fadeIn(tween(240)) + expandVertically(tween(320)),
            exit = fadeOut(tween(160)) + shrinkVertically(tween(280))
        ) {
            Column {
                TextInput(
                    value = settings.locationName,
                    hint = stringResource(R.string.weather_city),
                    password = false,
                    commitOnDone = true,
                    onCommit = { if (it.isNotBlank()) onLocation(it) }
                )
                AnimatedVisibility(visible = notFound) {
                    Text(
                        stringResource(R.string.weather_city_not_found),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnInkMuted,
                        modifier = Modifier.padding(start = 28.dp, top = 6.dp)
                    )
                }
            }
        }
        Choice(
            label = stringResource(R.string.weather_units),
            options = listOf(stringResource(R.string.weather_celsius), stringResource(R.string.weather_fahrenheit)),
            selected = if (settings.metric) 0 else 1,
            onSelect = { onChange(settings.copy(metric = it == 0)) }
        )
        Choice(
            label = stringResource(R.string.weather_interval),
            options = INTERVALS.map { stringResource(R.string.weather_hours, it) },
            selected = INTERVALS.indexOf(settings.intervalHours).coerceAtLeast(0),
            onSelect = { onChange(settings.copy(intervalHours = INTERVALS[it])) },
            last = true
        )
    }
}

@Composable
private fun statusText(state: WeatherState): String = when (state.status) {
    WeatherStatus.UPDATING -> stringResource(R.string.weather_updating)
    WeatherStatus.NO_LOCATION -> stringResource(R.string.weather_no_location)
    WeatherStatus.NO_NETWORK -> stringResource(R.string.weather_no_network)
    WeatherStatus.SERVICE_BUSY -> stringResource(R.string.weather_busy)
    else -> state.snapshot?.let { snapshot ->
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(snapshot.timestamp))
        if (snapshot.location.isEmpty()) stringResource(R.string.weather_updated, time)
        else stringResource(R.string.weather_updated_at, time, snapshot.location)
    } ?: stringResource(R.string.weather_waiting)
}

@Composable
private fun Choice(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit, last: Boolean = false) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = OnInkMuted,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
    )
    LiquidTabs(
        selectedIndex = selected,
        onSelected = onSelect,
        backdrop = LocalBackdrop.current,
        tabsCount = options.size,
        height = 44.dp,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = if (last) 16.dp else 4.dp)
            .fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            LiquidTab(onClick = { onSelect(index) }) {
                FitText(option, style = MaterialTheme.typography.labelLarge, color = OnInk, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun TextInput(value: String, hint: String, password: Boolean, commitOnDone: Boolean, onCommit: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    val focus = LocalFocusManager.current
    val pending = commitOnDone && text.trim() != value && (password || text.isNotBlank())
    fun commit() {
        onCommit(text)
        focus.clearFocus()
    }
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(OnInk.copy(alpha = FIELD_FILL))
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (text.isEmpty()) {
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = OnInkMuted)
            }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    if (!commitOnDone) onCommit(it)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnInk),
                cursorBrush = SolidColor(Accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                    imeAction = if (commitOnDone && !password) ImeAction.Search else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onSearch = { commit() }, onDone = { commit() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        AnimatedVisibility(
            visible = pending,
            enter = fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.6f),
            exit = fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.6f)
        ) {
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Accent)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_save)) { commit() },
                contentAlignment = Alignment.Center
            ) {
                AppIcon(R.drawable.ic_check, tint = Color.White, size = 18.dp)
            }
        }
    }
}

private val INTERVALS = listOf(1, 3, 6)
private const val FIELD_FILL = 0.08f
private const val REFRESH_SPIN_MS = 900
