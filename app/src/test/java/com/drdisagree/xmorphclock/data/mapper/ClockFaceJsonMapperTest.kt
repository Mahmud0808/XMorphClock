package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceImage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockFaceJsonMapperTest {

    private val presets: List<File> = File(PRESET_DIR)
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .sortedBy { it.name }

    @Test
    fun presetsExist() {
        assertTrue(presets.isNotEmpty())
    }

    @Test
    fun presetsParse() {
        presets.forEach { file ->
            val face = ClockFaceJsonMapper.fromJson(file.readText())
            assertNotNull(file.name, face)
            assertTrue(file.name, face!!.layers.isNotEmpty())
        }
    }

    @Test
    fun presetsRoundTrip() {
        presets.forEach { file ->
            val face: ClockFace = ClockFaceJsonMapper.fromJson(file.readText())!!
            assertEquals(file.name, face, ClockFaceJsonMapper.fromJson(ClockFaceJsonMapper.toJson(face)))
        }
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(ClockFaceJsonMapper.fromJson("{"))
        assertNull(ClockFaceJsonMapper.fromJson("{\"layers\":[]}"))
    }

    @Test
    fun imageKeepsAspectAndRadius() {
        val face = CustomFaces.image("Image", FaceImage("abc", 1.5f)).let { custom ->
            custom.copy(layers = custom.layers.map { it.copy(radius = 0.12f, sleepMono = true) })
        }
        assertEquals(face, ClockFaceJsonMapper.fromJson(ClockFaceJsonMapper.toJson(face)))
    }
}

private const val PRESET_DIR = "src/main/assets/clock_faces"
