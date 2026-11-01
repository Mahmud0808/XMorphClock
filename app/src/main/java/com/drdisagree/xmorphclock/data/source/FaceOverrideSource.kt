package com.drdisagree.xmorphclock.data.source

import android.content.Context
import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.domain.model.ClockFace
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class FaceOverrideSource(context: Context) {

    private val dir = File(context.filesDir, DIR).apply { mkdirs() }
    private val version = MutableStateFlow(0)

    val changes: StateFlow<Int> = version.asStateFlow()

    fun load(): Map<Int, ClockFace> = dir.listFiles { file -> file.extension == EXTENSION }
        .orEmpty()
        .mapNotNull { file ->
            val style = file.nameWithoutExtension.toIntOrNull() ?: return@mapNotNull null
            val face = ClockFaceJsonMapper.fromJson(file.readText()) ?: return@mapNotNull null
            style to face
        }
        .toMap()

    fun save(style: Int, face: ClockFace) {
        File(dir, "$style.$EXTENSION").writeText(ClockFaceJsonMapper.toJson(face))
        version.value++
    }

    fun delete(style: Int) {
        File(dir, "$style.$EXTENSION").delete()
        version.value++
    }
}

private const val DIR = "face_overrides"
private const val EXTENSION = "json"
