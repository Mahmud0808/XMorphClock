package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class UpdateStylePresetUseCase(
    private val settings: SettingsRepository,
    private val faces: FaceRepository,
    private val update: UpdateCompositionUseCase,
    private val snapshot: SnapshotWidgetsUseCase,
    private val prune: PruneOwnedFacesUseCase
) {
    suspend operator fun invoke(style: Int): Boolean {
        if (!FaceKind.CLOCK.isMine(style)) return false
        val target = faces.face(style) ?: return false
        val current = settings.settings.first()
        val switching = current.faceStyle != style
        if (switching) {
            val clock = faces.face(current.faceStyle)?.face ?: return false
            faces.saveFace(style, clock.copy(name = target.face.name, category = target.face.category))
        }
        val owned = HashSet<Int>()
        target.layout?.widgetStyles.orEmpty().forEach { widget ->
            if (FaceKind.of(widget)?.isMine(widget) == true && faces.face(widget)?.face?.let(CustomFaces::isPrivate) == true) owned += widget
        }
        val layout = snapshot(current.composition, owned)
        faces.saveLayout(style, layout)
        update(clockStyle = style) { layout }
        prune()
        return true
    }
}
