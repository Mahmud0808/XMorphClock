package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ColumnNode
import com.drdisagree.xmorphclock.domain.model.ElementKind
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import com.drdisagree.xmorphclock.domain.model.LayerNode
import com.drdisagree.xmorphclock.domain.model.StackNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceBuilderTest {

    private val clock = ClockFace(
        name = "Test",
        layers = listOf(FaceLayer("hours", FaceLayerType.HOURS), FaceLayer("minutes", FaceLayerType.MINUTES), FaceLayer("date", FaceLayerType.DATE)),
        large = ColumnNode(children = listOf(LayerNode("hours", 0.3f), LayerNode("minutes", 0.3f), LayerNode("date", 0.05f))),
        small = ColumnNode(children = listOf(LayerNode("hours", 0.15f), LayerNode("minutes", 0.15f), LayerNode("date", 0.03f)))
    )

    @Test
    fun removeDropsLayerAndNode() {
        val face = FaceBuilder.remove(clock, "date")
        assertTrue(face.layers.none { it.id == "date" })
        assertEquals(2, (face.large as ColumnNode).children.size)
    }

    @Test
    fun cannotRemoveLastElement() {
        val single = clock.copy(layers = clock.layers.take(1), large = LayerNode("hours", 0.3f), small = LayerNode("hours", 0.15f))
        assertEquals(single, FaceBuilder.remove(single, "hours"))
    }

    @Test
    fun moveStepsWithinGroup() {
        assertEquals(2, FaceBuilder.position(clock, "date")!!.index)
        val moved = FaceBuilder.move(clock, "date", -1)
        assertEquals(1, FaceBuilder.position(moved, "date")!!.index)
        val back = FaceBuilder.move(moved, "date", -5)
        assertEquals(0, FaceBuilder.position(back, "date")!!.index)
        assertNotNull(FaceBuilder.position(back, "hours"))
    }

    @Test
    fun singleElementHasNoPosition() {
        val single = clock.copy(layers = clock.layers.take(1), large = LayerNode("hours", 0.3f), small = LayerNode("hours", 0.15f))
        assertNull(FaceBuilder.position(single, "hours"))
    }

    @Test
    fun handsElementStacksBothLayers() {
        val face = CustomFaces.element("Hands", ElementFactory.create(ElementKind.HANDS, ""))!!
        assertEquals(2, face.layers.size)
        assertEquals(2, (face.large as StackNode).children.size)
        assertEquals(CustomFaces.ELEMENT_CATEGORY, face.category)
    }
}
