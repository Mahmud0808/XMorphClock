package com.drdisagree.xmorphclock.data.source

import android.content.Context
import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.data.mapper.CompositionJsonMapper
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.FaceKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class MyFacesSource(context: Context) {

    private val dir = File(context.filesDir, DIR).apply { mkdirs() }
    private val version = MutableStateFlow(0)

    val changes: StateFlow<Int> = version.asStateFlow()

    fun load(kind: FaceKind): Map<Int, ClockFace> = dir.listFiles { file -> file.extension == EXTENSION }
        .orEmpty()
        .mapNotNull { file ->
            val style = file.nameWithoutExtension.toIntOrNull()?.takeIf(kind::isMine) ?: return@mapNotNull null
            val face = ClockFaceJsonMapper.fromJson(file.readText()) ?: return@mapNotNull null
            style to face
        }
        .sortedBy { it.first }
        .toMap()

    fun face(style: Int): ClockFace? =
        File(dir, "$style.$EXTENSION").takeIf { it.isFile }?.let { ClockFaceJsonMapper.fromJson(it.readText()) }

    fun layout(style: Int): Composition? =
        layoutFile(style).takeIf { it.isFile }?.let { CompositionJsonMapper.fromJson(it.readText()) }

    fun create(kind: FaceKind, face: ClockFace): Int {
        val used = load(kind).keys
        val style = (used.maxOrNull() ?: (kind.mineBase - 1)) + 1
        save(style, face)
        return style
    }

    fun save(style: Int, face: ClockFace) {
        File(dir, "$style.$EXTENSION").writeText(ClockFaceJsonMapper.toJson(face))
        version.value++
    }

    fun saveLayout(style: Int, layout: Composition?) {
        if (layout == null) layoutFile(style).delete() else layoutFile(style).writeText(CompositionJsonMapper.toJson(layout))
        version.value++
    }

    fun delete(style: Int) {
        File(dir, "$style.$EXTENSION").delete()
        layoutFile(style).delete()
        version.value++
    }

    private fun layoutFile(style: Int): File = File(dir, "$style$LAYOUT_SUFFIX")
}

private const val DIR = "my_faces"
private const val EXTENSION = "json"
private const val LAYOUT_SUFFIX = ".layout.json"
