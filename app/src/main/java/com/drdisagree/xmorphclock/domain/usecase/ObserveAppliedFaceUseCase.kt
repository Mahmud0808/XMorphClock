package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class ObserveAppliedFaceUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository
) {
    operator fun invoke(draft: Flow<Composition?> = flowOf(null)): Flow<ClockFace?> = combine(
        settings.settings,
        faces.faces(FaceKind.CLOCK),
        faces.faces(FaceKind.WEATHER),
        faces.faces(FaceKind.WIDGET),
        draft
    ) { current, clocks, weathers, widgets, pending ->
        val clock = (clocks.firstOrNull { it.style == current.faceStyle } ?: clocks.firstOrNull())?.face ?: return@combine null
        val catalog = (weathers + widgets).associate { it.style to it.face }
        val composition = pending ?: current.composition
        val used = composition.widgetStyles.mapNotNull { style -> catalog[style]?.let { style to it } }.toMap()
        FaceComposer.compose(ComposedFaces(clock, used, composition.retain(used.keys)))
    }
}
