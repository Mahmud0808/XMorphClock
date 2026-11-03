package com.drdisagree.xmorphclock.di

import android.content.Context
import com.drdisagree.xmorphclock.data.repository.DepthProcessor
import com.drdisagree.xmorphclock.data.repository.DepthRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.FaceDataRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.FaceRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.ImageRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.LicenseRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.PresetTransferRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.SettingsRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.StatusRepositoryImpl
import com.drdisagree.xmorphclock.data.repository.WallpaperRepositoryImpl
import com.drdisagree.xmorphclock.data.source.BatterySource
import com.drdisagree.xmorphclock.data.source.FaceOverrideSource
import com.drdisagree.xmorphclock.data.source.HookReportSource
import com.drdisagree.xmorphclock.data.source.ImageStore
import com.drdisagree.xmorphclock.data.source.LocalPrefsSource
import com.drdisagree.xmorphclock.data.source.MyFacesSource
import com.drdisagree.xmorphclock.data.source.PaletteStore
import com.drdisagree.xmorphclock.data.source.PermissionGranter
import com.drdisagree.xmorphclock.data.source.ReportFileSource
import com.drdisagree.xmorphclock.data.source.WallpaperSource
import com.drdisagree.xmorphclock.data.source.XposedServiceSource
import com.drdisagree.xmorphclock.data.source.depth.DepthStore
import com.drdisagree.xmorphclock.data.source.presets.PresetDocumentSource
import com.drdisagree.xmorphclock.data.source.presets.PresetPreviewImages
import com.drdisagree.xmorphclock.data.source.depth.SubjectSegmenter
import com.drdisagree.xmorphclock.data.source.steps.StepsStore
import com.drdisagree.xmorphclock.data.source.weather.WeatherStore
import com.drdisagree.xmorphclock.domain.repository.DepthRepository
import com.drdisagree.xmorphclock.domain.repository.FaceDataRepository
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.ImageRepository
import com.drdisagree.xmorphclock.domain.repository.LicenseRepository
import com.drdisagree.xmorphclock.domain.repository.PresetTransferRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import com.drdisagree.xmorphclock.domain.repository.StatusRepository
import com.drdisagree.xmorphclock.domain.repository.WallpaperRepository
import com.drdisagree.xmorphclock.domain.usecase.ApplyClockPresetUseCase
import com.drdisagree.xmorphclock.domain.usecase.ApplyCompositionUseCase
import com.drdisagree.xmorphclock.domain.usecase.CheckRootUseCase
import com.drdisagree.xmorphclock.domain.usecase.ClosePresetFileUseCase
import com.drdisagree.xmorphclock.domain.usecase.CreateFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.DeleteFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.DeletePresetUseCase
import com.drdisagree.xmorphclock.domain.usecase.DuplicatePresetUseCase
import com.drdisagree.xmorphclock.domain.usecase.ExportPresetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.GetCompositionDraftUseCase
import com.drdisagree.xmorphclock.domain.usecase.GetDefaultFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.GetFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.GetLicensesUseCase
import com.drdisagree.xmorphclock.domain.usecase.ImportImageUseCase
import com.drdisagree.xmorphclock.domain.usecase.ImportPresetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveAppliedFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthModelUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthStatusUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveDepthSubjectUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFaceDataUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFacesUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFrameworkUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveHookReportUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveLiveWallpaperUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveModuleStatusUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveOwnPresetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveStyleStatusUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveWallpaperUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveWeatherUseCase
import com.drdisagree.xmorphclock.domain.usecase.ReadPresetFileUseCase
import com.drdisagree.xmorphclock.domain.usecase.RefreshWeatherUseCase
import com.drdisagree.xmorphclock.domain.usecase.RequestHookReportUseCase
import com.drdisagree.xmorphclock.domain.usecase.RestartSystemUiUseCase
import com.drdisagree.xmorphclock.domain.usecase.RetryDepthUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveCompositionUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveReportUseCase
import com.drdisagree.xmorphclock.domain.usecase.PruneOwnedFacesUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveStylePresetUseCase
import com.drdisagree.xmorphclock.domain.usecase.SnapshotWidgetsUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveWeatherSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetBurnInProtectionUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetClockEnabledUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetDepthEnabledUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetDepthOpacityUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetOnboardingDoneUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetThemeModeUseCase
import com.drdisagree.xmorphclock.domain.usecase.SetWeatherLocationUseCase
import com.drdisagree.xmorphclock.domain.usecase.SyncAppliedFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.UpdateCompositionUseCase
import com.drdisagree.xmorphclock.domain.usecase.UpdateStylePresetUseCase
import com.drdisagree.xmorphclock.engine.FaceColors
import com.drdisagree.xmorphclock.engine.FaceImages

class AppContainer(context: Context) {

    private val paletteStore = PaletteStore(context)

    init {
        FaceImages.init { presetPreviews.load(it) ?: imageStore.load(it) }
        FaceColors.setPalette(paletteStore.load())
    }

    private val myFaces = MyFacesSource(context)
    private val faceRepository: FaceRepository = FaceRepositoryImpl(context, FaceOverrideSource(context), myFaces)
    private val settingsRepository: SettingsRepository =
        SettingsRepositoryImpl(LocalPrefsSource(context), XposedServiceSource)
    private val statusRepository: StatusRepository =
        StatusRepositoryImpl(XposedServiceSource, HookReportSource(context), ReportFileSource(context))
    private val wallpaperSource = WallpaperSource(context)
    private val depthStore = DepthStore(context)
    private val imageStore = ImageStore(context)
    private val presetPreviews = PresetPreviewImages(FaceImages::remove)
    private val imageRepository: ImageRepository = ImageRepositoryImpl(imageStore)
    private val subjectSegmenter = SubjectSegmenter(context)
    private val depthProcessor = DepthProcessor(depthStore, subjectSegmenter)
    private val wallpaperRepository: WallpaperRepository =
        WallpaperRepositoryImpl(wallpaperSource, XposedServiceSource, paletteStore)
    private val depthRepository: DepthRepository = DepthRepositoryImpl(depthStore, depthProcessor, subjectSegmenter, wallpaperSource)
    private val presetTransferRepository: PresetTransferRepository =
        PresetTransferRepositoryImpl(PresetDocumentSource(context), myFaces, imageStore, presetPreviews)
    private val licenseRepository: LicenseRepository = LicenseRepositoryImpl(context)
    private val faceDataRepository: FaceDataRepository = FaceDataRepositoryImpl(
        context, WeatherStore(context), StepsStore(context), BatterySource(context), PermissionGranter(context)
    )

    val isOnboardingDone: Boolean
        get() = settingsRepository.isOnboardingDone

    val observeFaces = ObserveFacesUseCase(faceRepository)
    val observeOwnPresets = ObserveOwnPresetsUseCase(faceRepository)
    val observeSettings = ObserveSettingsUseCase(settingsRepository)
    val observeModuleStatus = ObserveModuleStatusUseCase(statusRepository)
    val observeFramework = ObserveFrameworkUseCase(statusRepository)
    val observeHookReport = ObserveHookReportUseCase(statusRepository)
    val observeWallpaper = ObserveWallpaperUseCase(wallpaperRepository)
    val observeLiveWallpaper = ObserveLiveWallpaperUseCase(wallpaperRepository)
    val requestHookReport = RequestHookReportUseCase(statusRepository)
    val restartSystemUi = RestartSystemUiUseCase(statusRepository)
    val saveReport = SaveReportUseCase(statusRepository)
    private val applyComposition = ApplyCompositionUseCase(settingsRepository, faceRepository, faceDataRepository)
    val updateComposition = UpdateCompositionUseCase(settingsRepository, applyComposition)
    val syncAppliedFace = SyncAppliedFaceUseCase(applyComposition)
    val createFace = CreateFaceUseCase(faceRepository)
    val deleteFace = DeleteFaceUseCase(faceRepository, settingsRepository, updateComposition, imageRepository)
    val getCompositionDraft = GetCompositionDraftUseCase(settingsRepository, faceRepository)
    val saveComposition = SaveCompositionUseCase(faceRepository, applyComposition, deleteFace)
    val observeAppliedFace = ObserveAppliedFaceUseCase(settingsRepository, faceRepository)
    val observeFaceData = ObserveFaceDataUseCase(faceDataRepository)
    val observeWeather = ObserveWeatherUseCase(faceDataRepository)
    val saveWeatherSettings = SaveWeatherSettingsUseCase(faceDataRepository)
    val refreshWeather = RefreshWeatherUseCase(faceDataRepository)
    val setWeatherLocation = SetWeatherLocationUseCase(faceDataRepository)
    val importImage = ImportImageUseCase(imageRepository)
    val observeDepthSettings = ObserveDepthSettingsUseCase(depthRepository)
    val observeDepthModel = ObserveDepthModelUseCase(depthRepository)
    val setDepthOpacity = SetDepthOpacityUseCase(depthRepository)
    val observeDepthStatus = ObserveDepthStatusUseCase(depthRepository)
    val retryDepth = RetryDepthUseCase(depthRepository)
    val observeDepthSubject = ObserveDepthSubjectUseCase(depthRepository)
    val setDepthEnabled = SetDepthEnabledUseCase(depthRepository)
    val getDefaultFace = GetDefaultFaceUseCase(faceRepository)
    val getFace = GetFaceUseCase(faceRepository)
    val setClockEnabled = SetClockEnabledUseCase(settingsRepository)
    val setBurnInProtection = SetBurnInProtectionUseCase(settingsRepository)
    val setThemeMode = SetThemeModeUseCase(settingsRepository)
    val setOnboardingDone = SetOnboardingDoneUseCase(settingsRepository)
    val exportPresets = ExportPresetsUseCase(presetTransferRepository)
    val readPresetFile = ReadPresetFileUseCase(presetTransferRepository)
    val importPresets = ImportPresetsUseCase(presetTransferRepository)
    val closePresetFile = ClosePresetFileUseCase(presetTransferRepository)
    val applyClockPreset = ApplyClockPresetUseCase(faceRepository, updateComposition)
    private val snapshotWidgets = SnapshotWidgetsUseCase(faceRepository)
    private val pruneOwnedFaces = PruneOwnedFacesUseCase(settingsRepository, faceRepository, deleteFace)
    val saveStylePreset = SaveStylePresetUseCase(settingsRepository, faceRepository, updateComposition, snapshotWidgets, pruneOwnedFaces)
    val updateStylePreset = UpdateStylePresetUseCase(settingsRepository, faceRepository, updateComposition, snapshotWidgets, pruneOwnedFaces)
    val duplicatePreset = DuplicatePresetUseCase(faceRepository, snapshotWidgets)
    val deletePreset = DeletePresetUseCase(deleteFace, pruneOwnedFaces)
    val observeStyleStatus = ObserveStyleStatusUseCase(settingsRepository, faceRepository)
    val checkRoot = CheckRootUseCase(statusRepository)
    val getLicenses = GetLicensesUseCase(licenseRepository)

    suspend fun handleLockscreenFrame(key: String, scale: Float) {
        val frame = wallpaperRepository.onLockscreenFrame(scale) ?: return
        depthProcessor.process(frame, scale, key)
    }
}
