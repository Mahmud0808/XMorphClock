package com.drdisagree.xmorphclock.ui.editor

import com.drdisagree.xmorphclock.domain.editor.ItemGroups
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.CompositionDraft
import com.drdisagree.xmorphclock.domain.model.EditScope

data class EditorUiState(
    val face: ClockFace? = null,
    val style: Int? = null,
    val default: ClockFace? = null,
    val selectedId: String? = null,
    val scope: EditScope = EditScope.LARGE,
    val aodLarge: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val saving: Boolean = false,
    val draft: CompositionDraft? = null,
    val editingOn: Boolean = false
) {
    val targets: List<String>
        get() = listOfNotNull(primary)

    val primary: String?
        get() = selectedId?.takeUnless(ItemGroups::isGroup)

    val group: String?
        get() = selectedId?.takeIf(ItemGroups::isGroup)
}
