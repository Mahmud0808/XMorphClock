package com.drdisagree.xmorphclock.domain.usecase

class RemoveFromGroupUseCase(
    private val ungroup: UngroupItemUseCase,
    private val group: GroupItemsUseCase
) {
    suspend operator fun invoke(groupId: Int, member: Int, name: String): Int? {
        val created = ungroup(groupId)
        val removed = created[member] ?: return null
        val rest = created.values.filter { it != removed }.toSet()
        if (rest.size >= 2) group(rest, name)
        return removed
    }
}
