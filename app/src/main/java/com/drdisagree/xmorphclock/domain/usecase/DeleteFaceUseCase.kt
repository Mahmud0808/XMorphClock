package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.FaceImageRefs
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import com.drdisagree.xmorphclock.domain.repository.ImageRepository
import com.drdisagree.xmorphclock.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first

class DeleteFaceUseCase(
    private val faces: FaceRepository,
    private val settings: SettingsRepository,
    private val update: UpdateCompositionUseCase,
    private val images: ImageRepository
) {
    suspend operator fun invoke(style: Int, face: ClockFace? = null) {
        val kind = FaceKind.of(style) ?: return
        faces.deleteFace(style)
        if (kind.isMine(style) && face != null) {
            val owned = FaceImageRefs.ids(face)
            if (owned.isNotEmpty()) {
                val used = FaceKind.entries.flatMapTo(HashSet()) { entry -> faces.faces(entry).first().flatMap { FaceImageRefs.ids(it.face) } }
                images.delete(owned.filterNot { it in used })
            }
        }
        val current = settings.settings.first()
        when {
            kind == FaceKind.CLOCK && current.faceStyle == style -> update(clockStyle = kind.styleBase) { it }
            kind != FaceKind.CLOCK && style in current.composition.widgetStyles -> update { it.removeStyle(style) }
        }
    }
}
