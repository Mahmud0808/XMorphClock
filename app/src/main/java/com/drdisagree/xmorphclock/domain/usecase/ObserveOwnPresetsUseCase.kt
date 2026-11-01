package com.drdisagree.xmorphclock.domain.usecase

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FacePreset
import com.drdisagree.xmorphclock.domain.repository.FaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveOwnPresetsUseCase(private val repository: FaceRepository) {
    operator fun invoke(): Flow<List<FacePreset>> = combine(FaceKind.entries.map { kind -> repository.faces(kind) }) { lists ->
        FaceKind.entries.flatMapIndexed { index, kind ->
            lists[index].filter { kind.isMine(it.style) && it.face.category != CustomFaces.ELEMENT_CATEGORY }
        }
    }
}
