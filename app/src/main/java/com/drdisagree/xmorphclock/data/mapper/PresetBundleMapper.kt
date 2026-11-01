package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.data.source.presets.PresetBundle
import com.drdisagree.xmorphclock.data.source.presets.PresetContainer
import com.drdisagree.xmorphclock.data.source.presets.PresetException
import com.drdisagree.xmorphclock.data.source.presets.PresetPaths
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.PresetFileError

object PresetBundleMapper {

    fun fromFile(data: ByteArray): PresetBundle {
        val archive = PresetArchiveMapper.fromZip(PresetContainer.open(data))
        val entries = archive.filesIn(PresetPaths.PRESETS)
        val faces = entries.filterKeys { !it.endsWith(PresetPaths.LAYOUT) }.map { (name, bytes) ->
            val style = name.removeSuffix(PresetPaths.JSON).takeIf { it != name }?.toIntOrNull()
                ?.takeIf { FaceKind.of(it)?.isMine(it) == true } ?: throw damaged()
            style to (ClockFaceJsonMapper.fromJson(bytes.decodeToString()) ?: throw damaged())
        }.sortedBy { it.first }.toMap()
        val layouts = faces.keys.mapNotNull { style ->
            entries["$style${PresetPaths.LAYOUT}"]?.let { style to CompositionJsonMapper.fromJson(it.decodeToString()) }
        }.toMap()
        val images = archive.filesIn(PresetPaths.IMAGES).map { (name, bytes) ->
            (name.removeSuffix(PresetPaths.WEBP).takeIf { it != name } ?: throw damaged()) to bytes
        }.toMap()
        return PresetBundle(faces, layouts, images).also { if (it.entries.isEmpty()) throw damaged() }
    }

    private fun damaged(): PresetException = PresetException(PresetFileError.DAMAGED)
}
