package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.data.mapper.CompositionJsonMapper
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.CompositionItem
import com.drdisagree.xmorphclock.domain.model.FaceAlign
import com.drdisagree.xmorphclock.domain.model.FacePlacement
import com.drdisagree.xmorphclock.domain.model.ItemState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceComposerTest {

    @Test
    fun itemScaleRoundTrips() {
        val clock = load(CLOCK_DIR).first()
        val parts = ComposedFaces(clock, emptyMap(), Composition(listOf(CompositionItem(id = 0, clock = true, largeScale = 1.5f))))
        val composed = FaceComposer.compose(parts)
        val restored = FaceComposer.decompose(composed, parts)
        assertEquals(FaceComposer.compose(restored), composed)
    }

    private fun load(dir: String): List<ClockFace> = File(dir)
        .listFiles { file -> file.extension == "json" }
        .orEmpty()
        .sortedBy { it.name }
        .map { file -> ClockFaceJsonMapper.fromJson(file.readText()).also { assertNotNull(file.name, it) }!! }

    @Test
    fun blockPresetsParseAndRoundTrip() {
        val blocks = load(WEATHER_DIR) + load(WIDGET_DIR)
        assertTrue(blocks.size >= 20)
        blocks.forEach { face -> assertEquals(face.name, face, ClockFaceJsonMapper.fromJson(ClockFaceJsonMapper.toJson(face))) }
    }

    @Test
    fun clockOnlyIsUnchanged() {
        val clock = load(CLOCK_DIR).first()
        assertSame(clock, FaceComposer.compose(ComposedFaces(clock, emptyMap(), Composition())))
    }

    @Test
    fun composeThenDecomposeRestoresParts() {
        val clock = load(CLOCK_DIR).first()
        val weather = load(WEATHER_DIR)
        val widgets = load(WIDGET_DIR)
        val faces = mapOf(3100 to weather[0], 4100 to widgets[0], 4101 to widgets[1])
        val composition = Composition()
            .add(3100, FacePlacement(0.5f, 0.7f, FaceAlign.CENTER), FacePlacement(0.0225f, 0.3f, FaceAlign.START))
            .add(4100, FacePlacement(0.3f, 0.9f, FaceAlign.CENTER), null)
            .add(4100, FacePlacement(0.7f, 0.9f, FaceAlign.END), null)
            .add(4101, null, null)
            .place(1, ItemState.AOD_LARGE, FacePlacement(0.5f, 0.4f, FaceAlign.CENTER))
        val parts = ComposedFaces(clock, faces, composition)
        val composed = FaceComposer.compose(parts)
        val roundTrip = ClockFaceJsonMapper.fromJson(ClockFaceJsonMapper.toJson(composed))!!
        val split = FaceComposer.decompose(roundTrip, parts)
        assertEquals(clock, split.clock)
        assertEquals(faces, split.widgets)
        split.composition.items.zip(composition.items).forEach { (after, before) ->
            assertEquals(before.id, after.id)
            before.large?.let { assertEquals(it, after.large) }
            before.aodLarge?.let { assertEquals(it, after.aodLarge) }
        }
    }

    @Test
    fun hiddenStatesRoundTrip() {
        val clock = load(CLOCK_DIR).first()
        val weather = load(WEATHER_DIR).first()
        val base = Composition().add(3100, FacePlacement(0.5f, 0.7f, FaceAlign.CENTER), null)
        val hidden = base.copy(items = base.items.map { if (it.clock) it.copy(hidden = setOf(ItemState.LARGE)) else it.copy(hidden = setOf(ItemState.AOD_LARGE, ItemState.AOD_SMALL)) })
        val parts = ComposedFaces(clock, mapOf(3100 to weather), hidden)
        val composed = FaceComposer.compose(parts)
        val split = FaceComposer.decompose(composed, parts)
        assertEquals(setOf(ItemState.LARGE), split.composition.items.first { it.clock }.hidden)
        assertEquals(setOf(ItemState.AOD_LARGE, ItemState.AOD_SMALL), split.composition.items.first { !it.clock }.hidden)
        assertEquals(clock, split.clock)
        val shown = split.copy(composition = split.composition.copy(items = split.composition.items.map { it.copy(hidden = emptySet()) }))
        val again = FaceComposer.decompose(FaceComposer.compose(shown), shown)
        assertTrue(again.composition.items.all { it.hidden.isEmpty() })
    }

    @Test
    fun compositionJsonRoundTrip() {
        val composition = Composition()
            .add(3100, FacePlacement(0.5f, 0.7f, FaceAlign.CENTER), null)
            .place(0, ItemState.SMALL, FacePlacement(0.1f, 0.2f, FaceAlign.START))
        assertEquals(composition, CompositionJsonMapper.fromJson(CompositionJsonMapper.toJson(composition)))
    }

    private companion object {
        const val CLOCK_DIR = "src/main/assets/clock_faces"
        const val WEATHER_DIR = "src/main/assets/weather_faces"
        const val WIDGET_DIR = "src/main/assets/widget_faces"
    }
}
