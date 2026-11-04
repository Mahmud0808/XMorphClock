package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.editor.FaceComposer
import com.drdisagree.xmorphclock.domain.editor.FaceFontRefs
import com.drdisagree.xmorphclock.domain.editor.FaceImageRefs
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.PresetFileEntry

class PresetBundle(
    val faces: Map<Int, ClockFace>,
    val layouts: Map<Int, Composition>,
    val images: Map<String, ByteArray>,
    val fonts: Map<String, ByteArray> = emptyMap(),
    val fontNames: Map<String, String> = emptyMap()
) {
    val entries: List<PresetFileEntry>
        get() = faces.mapNotNull { (style, face) ->
            val kind = FaceKind.of(style)
            if (kind != FaceKind.CLOCK || CustomFaces.isPrivate(face)) null else PresetFileEntry(style, kind, face, previewOf(style, face))
        }

    private fun previewOf(style: Int, face: ClockFace): ClockFace {
        val layout = layouts[style] ?: return face
        val used = layout.widgetStyles.mapNotNull { widget -> faces[widget]?.let { widget to it } }.toMap()
        return FaceComposer.compose(ComposedFaces(face, used, layout.retain(used.keys)))
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

    fun fontIds(styles: Collection<Int>): Set<String> = styles
        .flatMapTo(LinkedHashSet()) { style -> faces[style]?.let(FaceFontRefs::ids).orEmpty() }
        .filterTo(LinkedHashSet()) { it in fonts }

    fun imageIds(styles: Collection<Int>): Set<String> = styles
        .flatMapTo(LinkedHashSet()) { style -> faces[style]?.let(FaceImageRefs::ids).orEmpty() }
        .filterTo(LinkedHashSet()) { it in images }

}
