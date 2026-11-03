package com.drdisagree.xmorphclock.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drdisagree.xmorphclock.R
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.ui.components.AppIcon
import com.drdisagree.xmorphclock.ui.components.FitText
import com.drdisagree.xmorphclock.ui.components.LocalBackdrop
import com.drdisagree.xmorphclock.ui.components.PageScaffold
import com.drdisagree.xmorphclock.ui.components.glass
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTab
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidTabs
import com.drdisagree.xmorphclock.ui.components.liquid.LiquidToggle
import com.drdisagree.xmorphclock.ui.theme.Accent
import com.drdisagree.xmorphclock.ui.theme.OnInk
import com.drdisagree.xmorphclock.ui.theme.OnInkMuted
import com.drdisagree.xmorphclock.ui.theme.Surface
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TileShape = RoundedCornerShape(28.dp)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val transfer by viewModel.presetTransfer.collectAsStateWithLifecycle()
    val pendingImport by viewModel.presetImport.collectAsStateWithLifecycle()
    var exporting by rememberSaveable { mutableStateOf(false) }
    var exportStyles by remember { mutableStateOf(emptyList<Int>()) }
    var lastImport by remember { mutableStateOf(pendingImport) }
    LaunchedEffect(pendingImport) { if (pendingImport != null) lastImport = pendingImport }
    val shownImport = pendingImport ?: lastImport
    val exportItems = remember(state.ownPresets) {
        state.ownPresets.map { preset -> PresetPickerItem(preset.style, preset.face) }
    }
    val importItems = remember(shownImport) {
        shownImport?.entries.orEmpty().map { PresetPickerItem(it.style, it.face) }
    }
    val importSelection = remember(importItems) { importItems.mapTo(HashSet()) { it.key } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(PRESET_MIME)) { uri ->
        uri?.let { viewModel.exportPresets(exportStyles, it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.openPresetFile(it.toString()) }
    }
    PageScaffold(
        title = stringResource(R.string.tab_settings),
        onBack = onBack,
        overlay = {
            PresetPickerDialog(
                visible = exporting,
                title = stringResource(R.string.export_dialog_title),
                message = stringResource(if (exportItems.isEmpty()) R.string.export_empty else R.string.export_dialog_message),
                items = exportItems,
                initialSelection = emptySet(),
                confirmLabel = R.string.action_export_count,
                onConfirm = { styles ->
                    exportStyles = styles
                    exporting = false
                    exportLauncher.launch(presetFileName())
                },
                onDismiss = { exporting = false }
            )
            PresetPickerDialog(
                visible = pendingImport != null,
                title = stringResource(R.string.import_dialog_title),
                message = stringResource(R.string.import_dialog_message),
                items = importItems,
                initialSelection = importSelection,
                confirmLabel = R.string.action_import_count,
                onConfirm = viewModel::importPresets,
                onDismiss = viewModel::onImportDismiss
            )
            PresetTransferDialog(transfer, viewModel::onTransferDismiss)
        }
    ) {
        if (!state.loaded) return@PageScaffold
        item(key = "toggles") {
            Row(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ToggleTile(
                    icon = R.drawable.ic_faces,
                    title = stringResource(R.string.setting_clock),
                    summary = stringResource(R.string.setting_clock_summary),
                    checked = state.clockEnabled,
                    onCheckedChange = viewModel::onClockEnabledChange,
                    modifier = Modifier.weight(1f)
                )
                ToggleTile(
                    icon = R.drawable.ic_shield,
                    title = stringResource(R.string.setting_burn_in),
                    summary = stringResource(R.string.setting_burn_in_summary),
                    checked = state.burnInProtection,
                    onCheckedChange = viewModel::onBurnInChange,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item(key = "depth") { DepthCard(state.depth, state.depthStatus, viewModel::onDepthEnabledChange, viewModel::onDepthOpacityChange, viewModel::onDepthRetry) }
        item(key = "weather") { WeatherCard(state.weather, state.locationNotFound, viewModel::onWeatherSettingsChange, viewModel::onWeatherLocation, viewModel::onRefreshWeather) }
        item(key = "presets") {
            PresetsCard(
                ownCount = state.ownPresets.size,
                onExport = { exporting = true },
                onImport = { importLauncher.launch(arrayOf(ANY_MIME)) }
            )
        }
        item(key = "appearance") { AppearanceCard(state.themeMode, viewModel::onThemeModeChange) }
    }
}

@Composable
private fun ToggleTile(
    icon: Int,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val surface = Surface
    val accent = Accent
    val tint by animateColorAsState(
        if (checked) accent.copy(alpha = 0.2f).compositeOver(surface) else surface,
        tween(360),
        label = "tileTint"
    )
    val badge by animateColorAsState(if (checked) accent else OnInk.copy(alpha = 0.08f), tween(360), label = "badge")
    val glyph by animateColorAsState(if (checked) Color.White else OnInk, tween(360), label = "glyph")
    Column(
        modifier
            .glass(LocalBackdrop.current, TileShape, tint = { tint })
            .clickable(interactionSource = null, indication = null, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .drawBehind { drawCircle(badge) },
                contentAlignment = Alignment.Center
            ) {
                AppIcon(icon, tint = glyph, size = 22.dp)
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        FitText(title, style = MaterialTheme.typography.titleSmall, color = OnInk)
        Spacer(Modifier.height(2.dp))
        Text(summary, style = MaterialTheme.typography.bodySmall, color = OnInkMuted, minLines = 2, maxLines = 2)
        Spacer(Modifier.height(14.dp))
        LiquidToggle(checked, onCheckedChange, emptyBackdrop())
    }
}

@Composable
private fun AppearanceCard(mode: ThemeMode, onModeChange: (ThemeMode) -> Unit) {
    GlassCard {
        Text(
            stringResource(R.string.section_appearance),
            style = MaterialTheme.typography.titleSmall,
            color = OnInk,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp)
        )
        LiquidTabs(
            selectedIndex = mode.ordinal,
            onSelected = { onModeChange(ThemeMode.entries[it]) },
            backdrop = LocalBackdrop.current,
            tabsCount = ThemeMode.entries.size,
            height = 44.dp,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
        ) {
            ThemeMode.entries.forEach { entry ->
                LiquidTab(onClick = { onModeChange(entry) }) {
                    FitText(
                        stringResource(themeLabel(entry)),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnInk,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    val surface = rememberLayerBackdrop()
    Column(
        Modifier
            .fillMaxWidth()
            .glass(LocalBackdrop.current, TileShape, exported = surface)
            .clip(TileShape)
    ) {
        CompositionLocalProvider(LocalBackdrop provides surface) { content() }
    }
}

private fun presetFileName(): String =
    PRESET_PREFIX + SimpleDateFormat(PRESET_STAMP, Locale.US).format(Date()) + PRESET_EXTENSION

private const val PRESET_MIME = "application/octet-stream"
private const val ANY_MIME = "*/*"
private const val PRESET_PREFIX = "xmorphclock-presets-"
private const val PRESET_STAMP = "yyyyMMdd"
private const val PRESET_EXTENSION = ".xmcpreset"

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

