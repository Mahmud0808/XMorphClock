package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.StyleStatus
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class ObserveStyleStatusUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository
) {
    operator fun invoke(): Flow<StyleStatus> = combine(
        settings.settings,
        faces.faces(FaceKind.CLOCK),
        faces.faces(FaceKind.WEATHER),
        faces.faces(FaceKind.WIDGET)
    ) { current, clocks, weathers, widgets ->
        val preset = clocks.firstOrNull { it.style == current.faceStyle } ?: return@combine StyleStatus()
        val mine = FaceKind.CLOCK.isMine(preset.style)
        val available = (weathers + widgets).mapTo(HashSet()) { it.style }
        val reference = (preset.layout.takeIf { mine } ?: Composition()).retain(available)
        val changed = current.composition.retain(available).items != reference.items || (!mine && preset.isModified)
        StyleStatus(presetStyle = preset.style.takeIf { mine }, changed = changed)
    }.distinctUntilChanged()
}
