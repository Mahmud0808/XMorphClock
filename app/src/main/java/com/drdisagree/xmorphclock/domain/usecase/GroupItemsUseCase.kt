package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.ItemGrouping
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class GroupItemsUseCase(
    private val getDraft: GetCompositionDraftUseCase,
    private val faces: FaceRepository,
    private val update: UpdateCompositionUseCase,
    private val prune: PruneOwnedFacesUseCase
) {
    suspend operator fun invoke(members: Set<Int>, name: String): Int? {
        val draft = getDraft() ?: return null
        val composition = draft.parts.composition
        val chosen = composition.items.filter { it.id in members && !it.clock }
        val groups = chosen.filter { draft.parts.widgets[it.style]?.category == CustomFaces.GROUP_CATEGORY }.mapTo(HashSet()) { it.id }
        val ids = chosen.map { it.id }
        val face = ItemGrouping.group(draft.face, ids, groups, name) ?: return null
        val style = faces.createFace(FaceKind.WIDGET, face)
        val created = composition.nextId
        update { current -> current.group(ids, style) }
        prune()
        return created
    }
}
