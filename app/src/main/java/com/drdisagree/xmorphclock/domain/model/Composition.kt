package com.drdisagree.xmorphclock.domain.model

data class Composition(
    val items: List<CompositionItem> = listOf(CompositionItem(id = 0, clock = true)),
    val nextId: Int = 1
) {
    val clockItem: CompositionItem?
        get() = items.firstOrNull { it.clock }

    val widgetStyles: Set<Int>
        get() = items.filterNot { it.clock }.mapTo(LinkedHashSet()) { it.style }

    fun item(id: Int): CompositionItem? = items.firstOrNull { it.id == id }

    fun withClock(): Composition = if (clockItem != null) this else copy(items = items + CompositionItem(id = nextId, clock = true), nextId = nextId + 1)

    fun add(style: Int, large: FacePlacement?, small: FacePlacement?): Composition =
        copy(items = items + CompositionItem(id = nextId, style = style, large = large, small = small), nextId = nextId + 1)

    fun remove(id: Int): Composition = copy(items = items.filterNot { it.id == id })

    fun removeStyle(style: Int): Composition = copy(items = items.filterNot { !it.clock && it.style == style })

    fun scale(id: Int, state: ItemState, value: Float): Composition =
        copy(items = items.map { if (it.id == id) it.withScale(state, value) else it })

    fun place(id: Int, state: ItemState, placement: FacePlacement): Composition =
        copy(items = items.map { if (it.id == id) it.with(state, placement) else it })

    fun move(id: Int, steps: Int): Composition {
        val from = items.indexOfFirst { it.id == id }
        if (from < 0) return this
        val to = (from + steps).coerceIn(0, items.lastIndex)
        if (to == from) return this
        return copy(items = items.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun retain(available: Set<Int>): Composition = copy(items = items.filter { it.clock || it.style in available })
}
