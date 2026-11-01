package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.domain.model.ItemBounds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemPlacerTest {

    @Test
    fun fillsFreeCellsWithoutOverlapAndStaysOnScreen() {
        val occupied = mutableListOf(ItemBounds(0.05f, 0.1f, 0.95f, 0.55f))
        repeat(12) {
            val placement = ItemPlacer.place(occupied, 0.17f, 0.17f, 0.58f, 1.6f)
            val bounds = ItemBounds(placement.x, placement.y, placement.x + 0.17f, placement.y + 0.17f)
            assertFalse(occupied.any { it.overlaps(bounds) })
            assertTrue(bounds.bottom <= 1.6f + 1e-4f)
            assertTrue(bounds.left >= 0f && bounds.right <= 1f)
            occupied += bounds
        }
    }

    @Test
    fun overlapsWhenFull() {
        val occupied = listOf(ItemBounds(0f, 0f, 1f, 1f))
        val placement = ItemPlacer.place(occupied, 0.17f, 0.17f, 0.5f, 1f)
        assertTrue(placement.y + 0.17f <= 1f + 1e-4f)
    }
}
