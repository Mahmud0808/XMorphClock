package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FaceLayer
import com.drdisagree.xmorphclock.domain.model.FaceLayerType
import com.drdisagree.xmorphclock.domain.model.LayerNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FaceFontRefsTest {

    private val face = ClockFace(
        name = "Mixed",
        layers = listOf(
            FaceLayer("a", FaceLayerType.TIME, font = "user:aaaa"),
            FaceLayer("b", FaceLayerType.DATE, font = "user:bbbb?wdth=90"),
            FaceLayer("c", FaceLayerType.TEXT, font = "res:inter"),
            FaceLayer("d", FaceLayerType.TEXT)
        ),
        large = LayerNode("a", 0.5f),
        small = LayerNode("a", 0.5f)
    )

    @Test
    fun collectsOnlyUserFonts() {
        assertEquals(setOf("aaaa", "bbbb"), FaceFontRefs.ids(face))
    }

    @Test
    fun remapKeepsAxisSuffix() {
        val remapped = FaceFontRefs.remap(face, mapOf("aaaa" to "cccc", "bbbb" to "dddd"))
        assertEquals(listOf("user:cccc", "user:dddd?wdth=90", "res:inter", ""), remapped.layers.map { it.font })
    }

    @Test
    fun identityRemapReturnsSameFace() {
        assertSame(face, FaceFontRefs.remap(face, mapOf("aaaa" to "aaaa")))
    }
}
