package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.data.mapper.ClockFaceJsonMapper
import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.ComposedFaces
import com.drdisagree.xmorphclock.domain.model.Composition
import com.drdisagree.xmorphclock.domain.model.CompositionItem
import com.drdisagree.xmorphclock.domain.model.FaceAlign
import com.drdisagree.xmorphclock.domain.model.FacePlacement
import com.drdisagree.xmorphclock.domain.model.ItemState
import com.drdisagree.xmorphclock.domain.model.StackNode
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemGroupingTest {

    private fun load(dir: String): List<ClockFace> = File(dir).listFiles { file -> file.extension == "json" }.orEmpty()
        .sortedBy { it.name }
        .map { ClockFaceJsonMapper.fromJson(it.readText())!! }

    private val clock = load("src/main/assets/clock_faces").first()
    private val widgets = load("src/main/assets/widget_faces")
    private val battery = widgets[0]
    private val steps = widgets[2]
    private val data = widgets[3]
    private val faces = mapOf(4000 to battery, 4002 to steps, 4003 to data)
    private val composition = Composition(
        items = listOf(
            CompositionItem(id = 0, clock = true),
            CompositionItem(id = 1, style = 4000, large = FacePlacement(0.2f, 0.6f, FaceAlign.START), small = FacePlacement(0.1f, 0.3f, FaceAlign.START)),
            CompositionItem(id = 2, style = 4002, large = FacePlacement(0.6f, 0.6f, FaceAlign.START), small = FacePlacement(0.5f, 0.3f, FaceAlign.START)),
            CompositionItem(id = 3, style = 4003, large = FacePlacement(0.4f, 0.9f, FaceAlign.START), small = FacePlacement(0.3f, 0.5f, FaceAlign.START))
        ),
        nextId = 4
    )

    @Test
    fun groupKeepsLayoutAndUngroupRestoresItems() {
        val composed = FaceComposer.compose(ComposedFaces(clock, faces, composition))
        val group = ItemGrouping.group(composed, listOf(1, 2), emptySet(), "Group")!!
        val grouped = composition.group(listOf(1, 2), 9000)
        val regrouped = FaceComposer.compose(ComposedFaces(clock, faces + (9000 to group), grouped))
        val before = (composed.large as StackNode).children.drop(1).take(2).map { it.placement }
        val stack = (regrouped.large as StackNode).children[1] as StackNode
        val after = stack.children.map { it.placement.copy(x = it.placement.x + stack.placement.x, y = it.placement.y + stack.placement.y) }
        assertEquals(before, after)

        val members = ItemGrouping.ungroup(group, grouped.item(4)!!)
        assertEquals(listOf(1, 2), members.map { it.source })
        assertEquals(listOf(composition.item(1)!!.large, composition.item(2)!!.large), members.map { it.large })
        assertEquals(listOf(composition.item(1)!!.small, composition.item(2)!!.small), members.map { it.small })
        assertEquals(battery.layers.map { it.id }, members[0].face.layers.map { it.id })
        assertEquals(steps.layers.map { it.id }, members[1].face.layers.map { it.id })
    }

    @Test
    fun movedGroupOffsetsMembers() {
        val composed = FaceComposer.compose(ComposedFaces(clock, faces, composition))
        val group = ItemGrouping.group(composed, listOf(1, 2), emptySet(), "Group")!!
        val moved = composition.group(listOf(1, 2), 9000).place(4, ItemState.LARGE, group.large.placement.let { FacePlacement(it.x + 0.05f, it.y + 0.1f, FaceAlign.START) })
        val members = ItemGrouping.ungroup(group, moved.item(4)!!)
        assertEquals(0.25f, members[0].large.x, 0.0001f)
        assertEquals(0.7f, members[0].large.y, 0.0001f)
    }

    @Test
    fun groupingAGroupFlattensIt() {
        val composed = FaceComposer.compose(ComposedFaces(clock, faces, composition))
        val first = ItemGrouping.group(composed, listOf(1, 2), emptySet(), "Group")!!
        val grouped = composition.group(listOf(1, 2), 9000)
        val withGroup = FaceComposer.compose(ComposedFaces(clock, faces + (9000 to first), grouped))
        val merged = ItemGrouping.group(withGroup, listOf(4, 3), setOf(4), "Group")!!
        assertEquals(3, (merged.large as StackNode).children.size)
        assertEquals(setOf(1, 2, 3), merged.layers.mapNotNull { FaceComposer.itemOf(it.id) }.toSet())
    }
}
