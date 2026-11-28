package com.drdisagree.xmorphclock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import com.drdisagree.xmorphclock.domain.model.LyricsVersion
import com.drdisagree.xmorphclock.domain.model.NowPlayingLyrics
import com.drdisagree.xmorphclock.domain.model.PresetFileError
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.domain.model.UiStyle
import com.drdisagree.xmorphclock.domain.model.WeatherSettings
import com.drdisagree.xmorphclock.domain.usecase.ClosePresetFileUseCase
import com.drdisagree.xmorphclock.domain.usecase.ExportPresetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ImportPresetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthStatusUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveOwnPresetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveWeatherUseCase
import com.drdisagree.xmorphclock.domain.usecase.ReadPresetFileUseCase
import com.drdisagree.xmorphclock.domain.usecase.RefreshWeatherUseCase
import com.drdisagree.xmorphclock.domain.usecase.RetryDepthUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveWeatherSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetBurnInProtectionUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetDynamicClockUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetLyricsOnlineUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetClockEnabledUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetDepthEnabledUseCase
import com.drdisagree.xmorphclock.domain.usecase.ComposePresetPreviewsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFacesUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveNowPlayingLyricsUseCase
import com.drdisagree.xmorphclock.domain.usecase.PinLyricsVersionUseCase
import com.drdisagree.xmorphclock.domain.usecase.SearchLyricsVersionsUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetLyricsTuningUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetDepthOpacityUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetDepthWidgetsFrontUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetThemeModeUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetUiStyleUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetWeatherLocationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    private val setClockEnabled: SetClockEnabledUseCase,
    private val setBurnInProtection: SetBurnInProtectionUseCase,
    private val setDynamicClock: SetDynamicClockUseCase,
    private val setLyricsOnline: SetLyricsOnlineUseCase,
    observeNowPlayingLyrics: ObserveNowPlayingLyricsUseCase,
    private val setLyricsTuning: SetLyricsTuningUseCase,
    private val searchLyricsVersions: SearchLyricsVersionsUseCase,
    private val pinLyricsVersion: PinLyricsVersionUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setUiStyle: SetUiStyleUseCase,
    observeWeather: ObserveWeatherUseCase,
    private val saveWeatherSettings: SaveWeatherSettingsUseCase,
    private val refreshWeather: RefreshWeatherUseCase,
    private val setWeatherLocation: SetWeatherLocationUseCase,
    observeDepthSettings: ObserveDepthSettingsUseCase,
    observeDepthStatus: ObserveDepthStatusUseCase,
    private val retryDepth: RetryDepthUseCase,
    private val setDepthEnabled: SetDepthEnabledUseCase,
    private val setDepthOpacity: SetDepthOpacityUseCase,
    private val setDepthWidgetsFront: SetDepthWidgetsFrontUseCase,
    observeOwnPresets: ObserveOwnPresetsUseCase,
    observeFaces: ObserveFacesUseCase,
    composePresetPreviews: ComposePresetPreviewsUseCase,
    private val exportPresetFile: ExportPresetsUseCase,
    private val readPresetFile: ReadPresetFileUseCase,
    private val importPresetFile: ImportPresetsUseCase,
    private val closePresetFile: ClosePresetFileUseCase
) : ViewModel() {

    private val notFound = MutableStateFlow(false)
    private val transfer = MutableStateFlow<PresetTransfer?>(null)
    private val pendingImport = MutableStateFlow<PendingPresetImport?>(null)
    private val versionsState = MutableStateFlow<LyricsVersionsUiState?>(null)
    private var versionsJob: Job? = null

    val lyricsVersions: StateFlow<LyricsVersionsUiState?> = versionsState.asStateFlow()

    val presetTransfer: StateFlow<PresetTransfer?> = transfer.asStateFlow()
    val presetImport: StateFlow<PendingPresetImport?> = pendingImport.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeSettings(),
        observeWeather(),
        notFound,
        combine(observeDepthSettings(), observeDepthStatus(), observeNowPlayingLyrics()) { depth, status, playing -> Triple(depth, status, playing) },
        combine(observeOwnPresets(), observeFaces(FaceKind.WEATHER), observeFaces(FaceKind.WIDGET)) { own, weathers, widgets ->
            own to composePresetPreviews(mapOf(FaceKind.CLOCK to own, FaceKind.WEATHER to weathers, FaceKind.WIDGET to widgets))
        }
    ) { settings, weather, missing, (depth, depthStatus, playing), (own, previews) ->
        SettingsUiState(
            clockEnabled = settings.clockEnabled,
            burnInProtection = settings.burnInProtection,
            dynamicClock = settings.dynamicClock,
            lyricsOnline = settings.lyricsOnline,
            nowPlaying = playing,
            themeMode = settings.themeMode,
            uiStyle = settings.uiStyle,
            weather = weather,
            locationNotFound = missing,
            depth = depth,
            depthStatus = depthStatus,
            ownPresets = own,
            ownPreviews = previews,
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun onDepthEnabledChange(enabled: Boolean) {
        viewModelScope.launch { setDepthEnabled(enabled) }
    }

    fun onDepthRetry() {
        viewModelScope.launch { retryDepth() }
    }

    fun onDepthOpacityChange(opacity: Float) {
        viewModelScope.launch { setDepthOpacity(opacity) }
    }

    fun onDepthWidgetsFrontChange(front: Boolean) {
        viewModelScope.launch { setDepthWidgetsFront(front) }
    }

    fun onClockEnabledChange(enabled: Boolean) {
        viewModelScope.launch { setClockEnabled(enabled) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onUiStyleChange(style: UiStyle) {
        viewModelScope.launch { setUiStyle(style) }
    }

    fun onWeatherSettingsChange(settings: WeatherSettings) {
        viewModelScope.launch { saveWeatherSettings(settings) }
    }

    fun onWeatherLocation(query: String?) {
        viewModelScope.launch { notFound.value = !setWeatherLocation(query) }
    }

    fun onRefreshWeather() {
        viewModelScope.launch { refreshWeather() }
    }

    fun onBurnInChange(enabled: Boolean) {
        viewModelScope.launch { setBurnInProtection(enabled) }
    }

    fun onDynamicClockChange(enabled: Boolean) {
        viewModelScope.launch { setDynamicClock(enabled) }
    }

    fun onLyricsOnlineChange(enabled: Boolean) {
        viewModelScope.launch { setLyricsOnline(enabled) }
    }

    fun onLyricsTuningChange(playing: NowPlayingLyrics, tuning: LyricsTuning) {
        viewModelScope.launch { setLyricsTuning(playing.key, tuning) }
    }

    fun onOpenLyricsVersions(playing: NowPlayingLyrics) {
        versionsJob?.cancel()
        versionsState.value = LyricsVersionsUiState(playing)
        versionsJob = viewModelScope.launch {
            val found = searchLyricsVersions(playing)
            versionsState.value = LyricsVersionsUiState(playing, loading = false, versions = found.orEmpty(), failed = found == null)
        }
    }

    fun onPickLyricsVersion(version: LyricsVersion?) {
        val playing = versionsState.value?.playing ?: return
        onLyricsVersionsDismiss()
        viewModelScope.launch { pinLyricsVersion(playing, version) }
    }

    fun onLyricsVersionsDismiss() {
        versionsJob?.cancel()
        versionsState.value = null
    }

    fun exportPresets(styles: List<Int>, uri: String) {
        viewModelScope.launch {
            val saved = exportPresetFile(styles, uri)
            transfer.value = PresetTransfer(if (saved) PresetTransferStatus.EXPORTED else PresetTransferStatus.SAVE_FAILED)
        }
    }

    fun openPresetFile(uri: String) {
        viewModelScope.launch {
            val preview = readPresetFile(uri)
            val error = preview.error
            if (error == null) {
                pendingImport.value = PendingPresetImport(uri, preview.entries)
            } else {
                closePresetFile()
                transfer.value = PresetTransfer(statusOf(error))
            }
        }
    }

    fun importPresets(styles: List<Int>) {
        val pending = pendingImport.value ?: return
        pendingImport.value = null
        viewModelScope.launch {
            val result = importPresetFile(pending.uri, styles.toSet())
            closePresetFile()
            transfer.value = result.error?.let { PresetTransfer(statusOf(it)) }
                ?: PresetTransfer(PresetTransferStatus.IMPORTED, result.imported)
        }
    }

    fun onImportDismiss() {
        pendingImport.value = null
        closePresetFile()
    }

    fun onTransferDismiss() {
        transfer.value = null
    }

    override fun onCleared() {
        closePresetFile()
    }

    private fun statusOf(error: PresetFileError): PresetTransferStatus = when (error) {
        PresetFileError.NOT_PRESET_FILE -> PresetTransferStatus.NOT_PRESET_FILE
        PresetFileError.UNSUPPORTED_VERSION -> PresetTransferStatus.UNSUPPORTED_VERSION
        PresetFileError.DAMAGED -> PresetTransferStatus.DAMAGED
        PresetFileError.READ_FAILED -> PresetTransferStatus.READ_FAILED
    }
}

