package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.domain.model.EditScope
import com.drdisagree.xmorphclock.domain.model.StyleKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FaceEditorTest {

    @Test
    fun dateSizeChangesInEveryScope() {
        File("src/main/assets/clock_faces").listFiles().orEmpty().sorted().forEach { file ->
            val face = ClockFaceJsonMapper.fromJson(file.readText()) ?: error("parse ${file.name}")
            EditScope.entries.forEach { scope ->
                val before = FaceEditor.read(face, "date", scope, StyleKey.SIZE)
                if (before == 0f) return@forEach
                val edited = FaceEditor.write(face, listOf("date"), scope, StyleKey.SIZE, before + 0.02f)
                val after = FaceEditor.read(edited, "date", scope, StyleKey.SIZE)
                assertEquals("${file.name} $scope", before + 0.02f, after, 0.0001f)
            }
        }
    }
}
