package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.ItemGrouping
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.UngroupedItem
import com.drdisagree.xmorphclock.domain.repository.FaceRepository

class UngroupItemUseCase(
    private val getDraft: GetCompositionDraftUseCase,
    private val faces: FaceRepository,
    private val update: UpdateCompositionUseCase,
    private val prune: PruneOwnedFacesUseCase
) {
    suspend operator fun invoke(id: Int): Map<Int, Int> {
        val draft = getDraft() ?: return emptyMap()
        val item = draft.parts.composition.item(id) ?: return emptyMap()
        val group = draft.parts.widgets[item.style]?.takeIf { it.category == CustomFaces.GROUP_CATEGORY } ?: return emptyMap()
        val members = ItemGrouping.ungroup(group, item)
        if (members.isEmpty()) return emptyMap()
        val first = draft.parts.composition.nextId
        val created = members.map { member ->
            UngroupedItem(faces.createFace(FaceKind.WIDGET, member.face), member.large, member.small, member.hidden)
        }
        update { current -> current.ungroup(id, created) }
        prune()
        return members.mapIndexed { index, member -> member.source to first + index }.toMap()
    }
}
