package com.drdisagree.xmorphclock.data.source.presets

import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.data.mapper.CompositionJsonMapper
import com.drdisagree.xmorphclock.data.mapper.PresetArchiveMapper
import com.drdisagree.xmorphclock.data.mapper.PresetBundleMapper
import com.drdisagree.xmorphclock.domain.editor.CustomFaces
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.CompositionItem
import com.drdisagree.xmorphclock.domain.model.FaceKind
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import com.drdisagree.xmorphclock.domain.model.LayerNode
import com.drdisagree.xmorphclock.domain.model.PresetFileError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PresetBundleTest {

    private val clock = face("Morning", FaceLayer("t", FaceLayerType.TIME))
    private val weather = face("Rain", FaceLayer("w", FaceLayerType.TIME))
    private val widget = face("Steps", FaceLayer("s", FaceLayerType.TIME))
    private val element = face("Photo", FaceLayer("p", FaceLayerType.IMAGE, format = "pic1")).copy(category = CustomFaces.ELEMENT_CATEGORY)
    private val layout = Composition(
        items = listOf(CompositionItem(id = 0, clock = true), CompositionItem(id = 1, style = 8000), CompositionItem(id = 2, style = 8001), CompositionItem(id = 3, style = 4005)),
        nextId = 4
    )

    private val file = seal(
        mapOf(
            "presets/6000.json" to json(clock),
            "presets/6000.layout.json" to CompositionJsonMapper.toJson(layout).toByteArray(),
            "presets/7000.json" to json(weather),
            "presets/8000.json" to json(element),
            "presets/8001.json" to json(widget),
            "images/pic1.webp" to byteArrayOf(9, 8, 7)
        )
    )

    @Test
    fun previewListsWholeScreenPresetsOnly() {
        val bundle = PresetBundleMapper.fromFile(file)
        val entries = bundle.entries
        assertEquals(listOf(6000), entries.map { it.style })
        assertEquals(listOf(FaceKind.CLOCK), entries.map { it.kind })
        assertEquals(listOf("Morning"), entries.map { it.face.name })
        assertArrayEquals(byteArrayOf(9, 8, 7), bundle.images.getValue("pic1"))
    }

    @Test
    fun subsetPullsReferencedPresetsAndImages() {
        val bundle = PresetBundleMapper.fromFile(file)
        val chosen = bundle.closure(setOf(6000))
        assertEquals(listOf(6000, 8000, 8001), chosen)
        assertEquals(setOf("pic1"), bundle.imageIds(chosen))
        assertEquals(1, bundle.layouts.size)
    }

    @Test
    fun subsetWithoutLayoutStaysAlone() {
        val bundle = PresetBundleMapper.fromFile(file)
        assertEquals(listOf(7000), bundle.closure(setOf(7000)))
        assertEquals(emptySet<String>(), bundle.imageIds(listOf(7000)))
        assertEquals(listOf(7000, 8001), bundle.closure(listOf(8001, 7000, 9999)))
        assertEquals(emptyList<Int>(), bundle.closure(emptySet()))
    }

    @Test
    fun builtInStyleIsRejected() {
        expectDamaged(seal(mapOf("presets/2000.json" to json(clock))))
    }

    @Test
    fun brokenFaceIsRejected() {
        expectDamaged(seal(mapOf("presets/6000.json" to "{".toByteArray())))
    }

    @Test
    fun elementsOnlyIsRejected() {
        expectDamaged(seal(mapOf("presets/8000.json" to json(element))))
    }

    @Test
    fun imageWithoutExtensionIsRejected() {
        expectDamaged(seal(mapOf("presets/6000.json" to json(clock), "images/pic1.png" to byteArrayOf(1))))
    }

    private fun face(name: String, layer: FaceLayer): ClockFace {
        val node = LayerNode(layer.id, 0.5f)
        return ClockFace(name = name, layers = listOf(layer), large = node, small = node)
    }

    private fun json(face: ClockFace): ByteArray = ClockFaceJsonMapper.toJson(face).toByteArray()

    private fun seal(files: Map<String, ByteArray>): ByteArray =
        PresetContainer.seal(PresetArchiveMapper.toZip(PresetArchive(1L, "1.0", files)))

    private fun expectDamaged(data: ByteArray) {
        try {
            PresetBundleMapper.fromFile(data)
            fail("Expected ${PresetFileError.DAMAGED}")
        } catch (e: PresetException) {
            assertEquals(PresetFileError.DAMAGED, e.error)
        }
    }
}
