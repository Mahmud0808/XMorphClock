package com.drdisagree.xmorphclock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.ElementFactory
import com.drdisagree.xmorphclock.domain.editor.ItemGroups
import com.drdisagree.xmorphclock.domain.editor.ItemPlacer
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.CompositionItem
import com.drdisagree.xmorphclock.domain.model.ElementKind
import com.drdisagree.xmorphclock.domain.model.FaceGrid
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePlacement
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.model.ItemBounds
import com.drdisagree.xmorphclock.domain.model.ItemState
import com.drdisagree.xmorphclock.domain.model.StackNode
import com.drdisagree.xmorphclock.domain.model.StyleStatus
import com.drdisagree.xmorphclock.domain.usecase.ApplyClockPresetUseCase
import com.drdisagree.xmorphclock.domain.usecase.CreateFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.DeleteFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.ImportImageUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveAppliedFaceUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveFacesUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveLiveWallpaperUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveModuleStatusUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveSettingsUseCase
import com.drdisagree.xmorphclock.domain.usecase.ObserveStyleStatusUseCase
import com.drdisagree.xmorphclock.domain.usecase.SaveStylePresetUseCase
import com.drdisagree.xmorphclock.domain.usecase.UpdateCompositionUseCase
import com.drdisagree.xmorphclock.domain.usecase.UpdateStylePresetUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class HomeViewModel(
    observeFaces: ObserveFacesUseCase,
    private val observeSettings: ObserveSettingsUseCase,
    observeModuleStatus: ObserveModuleStatusUseCase,
    observeLiveWallpaper: ObserveLiveWallpaperUseCase,
    observeAppliedFace: ObserveAppliedFaceUseCase,
    private val updateComposition: UpdateCompositionUseCase,
    private val createFace: CreateFaceUseCase,
    private val deleteFace: DeleteFaceUseCase,
    private val importImage: ImportImageUseCase,
    private val applyClockPreset: ApplyClockPresetUseCase,
    private val saveStylePreset: SaveStylePresetUseCase,
    private val updateStylePreset: UpdateStylePresetUseCase,
    observeStyleStatus: ObserveStyleStatusUseCase
) : ViewModel() {

    private val previewMode = MutableStateFlow(PreviewMode.LARGE)
    private val editing = MutableStateFlow<Int?>(null)
    private val kind = MutableStateFlow(FaceKind.CLOCK)
    private val item = MutableStateFlow<Int?>(null)
    private val focus = MutableStateFlow<String?>(null)
    private val editRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val editEvents: SharedFlow<Unit> = editRequests.asSharedFlow()
    private var measuredState: ItemState? = null
    private var measuredBounds: Map<Int, ItemBounds> = emptyMap()
    private var screenBottom: Float = DEFAULT_BOTTOM

    val styleStatus: StateFlow<StyleStatus> = observeStyleStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), StyleStatus())

    val liveWallpaper: StateFlow<Boolean> = observeLiveWallpaper()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), false)

    private val catalog = combine(
        observeFaces(FaceKind.CLOCK),
        observeFaces(FaceKind.WEATHER),
        observeFaces(FaceKind.WIDGET)
    ) { clocks, weathers, widgets ->
        mapOf(FaceKind.CLOCK to clocks, FaceKind.WEATHER to weathers, FaceKind.WIDGET to widgets)
    }

    private val local = combine(previewMode, editing, kind, item, focus) { mode, style, selected, id, layer ->
        HomeLocalState(mode, style, selected, id, layer)
    }

    private val settingsState = observeSettings().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val effective: StateFlow<Composition?> = settingsState.map { it?.composition }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<HomeUiState> = combine(
        catalog,
        settingsState.filterNotNull(),
        observeModuleStatus(),
        observeAppliedFace(draft = effective),
        local
    ) { faces, settings, status, preview, current ->
        HomeUiState(
            kind = current.kind,
            faces = faces,
            clockStyle = settings.faceStyle,
            composition = settings.composition,
            preview = preview,
            clockEnabled = settings.clockEnabled,
            moduleStatus = status,
            previewMode = current.mode,
            editingStyle = current.editing,
            selectedItem = current.item,
            focusedLayer = current.focus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), HomeUiState())

    fun onKindChange(selected: FaceKind) {
        kind.value = selected
    }

    fun onItemSelected(id: Int?) {
        item.value = id
        focus.value = null
        val state = uiState.value
        val selected = id?.let { state.composition.item(it) } ?: return
        kind.value = if (selected.clock) FaceKind.CLOCK else FaceKind.of(selected.style) ?: kind.value
    }

    fun onLayerFocused(id: Int, layer: String?) {
        item.value = id
        focus.value = layer
    }

    fun onItemsMeasured(state: ItemState, bounds: Map<Int, ItemBounds>, maxY: Float) {
        measuredState = state
        measuredBounds = bounds
        if (maxY > 0f) screenBottom = maxY
    }

    fun onFaceSelected(style: Int) {
        val state = uiState.value
        val selected = FaceKind.of(style) ?: return
        viewModelScope.launch {
            if (selected == FaceKind.CLOCK) {
                applyClockPreset(style)
                return@launch
            }
            val face = state.faceOf(style) ?: return@launch
            val large = spotFor(state, face, ItemState.LARGE)
            val small = spotFor(state, face, ItemState.SMALL)
            val id = state.composition.nextId
            updateComposition { it.add(style, large, small) }
            item.value = id
            focus.value = null
            withTimeoutOrNull(SETTLE_TIMEOUT) { settingsState.first { it?.composition?.item(id) != null } } ?: return@launch
            editRequests.tryEmit(Unit)
        }
    }

    fun onAddElement(kind: ElementKind, name: String, text: String) {
        val face = if (kind == ElementKind.TEXT) CustomFaces.text(name, text) else CustomFaces.element(name, ElementFactory.create(kind, text))
        face ?: return
        viewModelScope.launch { addCreated(createFace(FaceKind.WIDGET, face)) }
    }

    fun onAddImage(uri: String, name: String) {
        viewModelScope.launch {
            val image = importImage(uri) ?: return@launch
            addCreated(createFace(FaceKind.WIDGET, CustomFaces.image(name, image)))
        }
    }

    private suspend fun addCreated(style: Int) {
        withTimeoutOrNull(CREATE_TIMEOUT) { uiState.first { it.faceOf(style) != null } } ?: return
        onFaceSelected(style)
    }

    private fun spotFor(state: HomeUiState, face: ClockFace, itemState: ItemState): FacePlacement {
        val large = itemState == ItemState.LARGE
        val bounds = state.composition.items.associate { target ->
            target.id to if (measuredState == itemState) measuredBounds[target.id] else estimate(state, target, itemState)
        }
        val occupied = bounds.values.filterNotNull()
        val (width, height) = sizeOf(face, large)
        val clock = state.composition.clockItem?.let { bounds[it.id] }
        return ItemPlacer.place(occupied, width, height, (clock?.bottom ?: 0f) + ITEM_GAP, screenBottom)
    }

    private fun estimate(state: HomeUiState, target: CompositionItem, itemState: ItemState): ItemBounds? {
        val large = itemState == ItemState.LARGE
        val face = (if (target.clock) state.clockPreset?.face else state.faceOf(target.style)) ?: return null
        val placement = target.placement(itemState) ?: (if (large) face.large.placement else face.small.placement)
        val (width, height) = if (target.clock) (if (large) CLOCK_LARGE else CLOCK_SMALL) else sizeOf(face, large)
        return ItemBounds.of(placement, width, height)
    }

    private fun sizeOf(face: ClockFace, large: Boolean): Pair<Float, Float> {
        val root = (if (large) face.large else face.small) as? StackNode
        val width = root?.width ?: FaceGrid.width(face.span.coerceIn(1, FaceGrid.ROW_CELLS)) * (if (large) 1f else SMALL_SCALE)
        val height = root?.height ?: INLINE_HEIGHT * (if (large) 1f else SMALL_SCALE)
        return width to height
    }

    fun editTarget(): String? {
        val state = uiState.value
        state.focusedLayer?.let { return it }
        val selected = state.selectedItem?.let { state.composition.item(it) } ?: return null
        return ItemGroups.keyOfItem(selected.id, selected.clock)
    }

    fun onDuplicate(preset: FacePreset, name: String) {
        val target = FaceKind.of(preset.style) ?: return
        viewModelScope.launch { createFace(target, preset.face.copy(name = name), preset.layout) }
    }

    fun saveAsPreset(name: String) {
        viewModelScope.launch { saveStylePreset(name) }
    }

    fun updatePreset(style: Int) {
        viewModelScope.launch { updateStylePreset(style) }
    }

    fun onDelete(preset: FacePreset) {
        viewModelScope.launch { deleteFace(preset.style, preset.face) }
    }

    fun onPreviewModeChange(mode: PreviewMode) {
        previewMode.value = mode
    }

    fun onEdit(style: Int) {
        editing.value = style
    }

    fun onEditDone() {
        editing.value = null
    }
}

private const val STOP_TIMEOUT = 5_000L
private const val ITEM_GAP = 0.03f
private const val DEFAULT_BOTTOM = 1.9f
private const val SMALL_SCALE = 0.62f
private const val INLINE_HEIGHT = 0.08f
private val CLOCK_LARGE = 0.9f to 0.45f
private val CLOCK_SMALL = 0.45f to 0.2f

private const val CREATE_TIMEOUT = 3_000L
private const val SETTLE_TIMEOUT = 1_500L
