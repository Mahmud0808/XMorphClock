package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.domain.model.FaceAlign
import com.drdisagree.xmorphclock.domain.model.ItemBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemSnapperTest {

    @Test
    fun snapsLeftEdgeToNeighbour() {
        val result = ItemSnapper.snap(ItemBounds(0.21f, 0.6f, 0.38f, 0.77f), listOf(ItemBounds(0.2f, 0.3f, 0.37f, 0.47f)))
        assertEquals(-0.01f, result.dx, 1e-4f)
        assertEquals(0.2f, result.guideX!!, 1e-4f)
        assertEquals(FaceAlign.START, result.anchorX)
    }

    @Test
    fun ignoresFarTargets() {
        val result = ItemSnapper.snap(ItemBounds(0.1f, 0.9f, 0.2f, 1f), listOf(ItemBounds(0.6f, 0.2f, 0.7f, 0.3f)))
        assertNull(result.guideY)
    }
}
