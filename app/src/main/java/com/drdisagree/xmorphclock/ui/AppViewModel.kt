package com.drdisagree.xmorphclock.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.xmorphclock.domain.model.ClockGeometry
import com.drdisagree.xmorphclock.domain.model.FaceData
import com.drdisagree.xmorphclock.domain.model.ThemeMode
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthSubjectUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFaceDataUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveHookReportUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveSampleFaceDataUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveWallpaperUseCase
import com.drdisagree.xmorphclock.domain.usecase.SyncAppliedFaceUseCase
import com.drdisagree.xmorphclock.engine.render.BackdropCache
import com.drdisagree.xmorphclock.ui.components.DepthLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    observeWallpaper: ObserveWallpaperUseCase,
    observeHookReport: ObserveHookReportUseCase,
    observeSettings: ObserveSettingsUseCase,
    syncAppliedFace: SyncAppliedFaceUseCase,
    observeFaceData: ObserveFaceDataUseCase,
    observeSampleFaceData: ObserveSampleFaceDataUseCase,
    observeDepthSettings: ObserveDepthSettingsUseCase,
    observeDepthSubject: ObserveDepthSubjectUseCase
) : ViewModel() {

    init {
        viewModelScope.launch { syncAppliedFace() }
    }

    val themeMode: StateFlow<ThemeMode> = observeSettings()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)


    val wallpaper: StateFlow<ImageBitmap?> = observeWallpaper()
        .map { bitmap -> bitmap?.also(BackdropCache::prepare)?.asImageBitmap() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val depth: StateFlow<DepthLayer?> = combine(observeDepthSettings(), observeDepthSubject()) { settings, subject ->
        if (settings.enabled && subject != null) DepthLayer(subject, settings.opacity) else null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val faceData: StateFlow<FaceData> = observeFaceData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), FaceData())

    val sampleFaceData: StateFlow<FaceData> = observeSampleFaceData()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), FaceData())

    val geometry: StateFlow<ClockGeometry?> = observeHookReport()
        .map { report ->
            val large = report?.largeFrame ?: return@map null
            ClockGeometry(large, report.smallFrame ?: large, report.screenAspect ?: return@map null)
        }
        .filterNotNull()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

private const val STOP_TIMEOUT = 5_000L
