package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class SnapshotWidgetsUseCase(private val faces: FaceRepository) {

    suspend operator fun invoke(composition: Composition, keep: Set<Int> = emptySet()): Composition {
        val copies = HashMap<Int, Int>()
        val items = composition.items.map { item ->
            if (item.clock || item.style in keep) return@map item
            val style = copies[item.style] ?: copy(item.style)?.also { copies[item.style] = it } ?: return@map item
            item.copy(style = style)
        }
        return composition.copy(items = items)
    }

    private suspend fun copy(style: Int): Int? {
        val kind = FaceKind.of(style) ?: return null
        val face = faces.face(style)?.face ?: return null
        val category = if (face.category == CustomFaces.ELEMENT_CATEGORY) face.category else CustomFaces.OWNED_CATEGORY
        return faces.createFace(kind, face.copy(category = category))
    }
}
