package com.drdisagree.xmorphclock.ui.home

import com.drdisagree.xmorphclock.domain.model.FacePreset

data class FaceSection(
    val category: String,
    val headerIndex: Int,
    val faces: List<FacePreset>
)

fun sectionsOf(faces: List<FacePreset>, start: Int = 0): List<FaceSection> {
    var index = start
    return faces.groupBy { it.face.category.ifEmpty { OTHER } }.map { (category, items) ->
        FaceSection(category, index, items).also { index += items.size + 1 }
    }
}

private const val OTHER = "other"
