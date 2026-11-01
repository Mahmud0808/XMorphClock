package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.FaceImageRefs
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.PresetFileEntry

class PresetBundle(
    val faces: Map<Int, ClockFace>,
    val layouts: Map<Int, Composition>,
    val images: Map<String, ByteArray>
) {
    val entries: List<PresetFileEntry>
        get() = faces.mapNotNull { (style, face) ->
            val kind = FaceKind.of(style)
            if (kind == null || isElement(face)) null else PresetFileEntry(style, kind, face)
        }

    fun closure(styles: Collection<Int>): List<Int> {
        val pending = ArrayDeque(styles.filter { it in faces })
        val chosen = sortedSetOf<Int>()
        while (pending.isNotEmpty()) {
            val style = pending.removeFirst()
            if (!chosen.add(style)) continue
            layouts[style]?.widgetStyles?.filterTo(pending) { it in faces && it !in chosen }
        }
        return chosen.toList()
    }

    fun imageIds(styles: Collection<Int>): Set<String> = styles
        .flatMapTo(LinkedHashSet()) { style -> faces[style]?.let(FaceImageRefs::ids).orEmpty() }
        .filterTo(LinkedHashSet()) { it in images }

    fun isElement(face: ClockFace): Boolean = face.category == CustomFaces.ELEMENT_CATEGORY
}
