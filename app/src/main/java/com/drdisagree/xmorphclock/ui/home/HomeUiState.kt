package com.drdisagree.xmorphclock.ui.home

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.model.ModuleStatus

data class HomeUiState(
    val tab: FaceTab = FaceTab.CLOCK,
    val faces: Map<FaceKind, List<FacePreset>> = emptyMap(),
    val presetPreviews: Map<Int, ClockFace> = emptyMap(),
    val clockStyle: Int = -1,
    val composition: Composition = Composition(),
    val preview: ClockFace? = null,
    val clockEnabled: Boolean = true,
    val moduleStatus: ModuleStatus = ModuleStatus.INACTIVE,
    val previewMode: PreviewMode = PreviewMode.LARGE,
    val editingStyle: Int? = null,
    val selectedItem: Int? = null,
    val focusedLayer: String? = null
) {
    val clockPreset: FacePreset?
        get() = faces[FaceKind.CLOCK].orEmpty().let { list -> list.firstOrNull { it.style == clockStyle } ?: list.firstOrNull() }

    fun isChecked(preset: FacePreset): Boolean = when (FaceKind.of(preset.style)) {
        FaceKind.CLOCK -> composition.clockItem != null && preset.style == clockPreset?.style
        null -> false
        else -> preset.style in composition.widgetStyles
    }

    fun facesOf(tab: FaceTab): List<FacePreset> = when (tab) {
        FaceTab.CLOCK -> library(FaceKind.CLOCK)
        FaceTab.WIDGETS -> library(FaceKind.WIDGET) + library(FaceKind.WEATHER)
        FaceTab.MINE -> faces[FaceKind.CLOCK].orEmpty().filter { FaceKind.CLOCK.isMine(it.style) }
    }

    private fun library(kind: FaceKind): List<FacePreset> = faces[kind].orEmpty().filterNot { kind.isMine(it.style) }

    fun faceOf(style: Int): ClockFace? = faces.values.firstNotNullOfOrNull { list -> list.firstOrNull { it.style == style } }?.face
}
