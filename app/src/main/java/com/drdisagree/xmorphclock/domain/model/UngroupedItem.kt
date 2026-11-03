package com.drdisagree.xmorphclock.domain.model

data class UngroupedItem(
    val style: Int,
    val large: FacePlacement,
    val small: FacePlacement,
    val hidden: Set<ItemState>
)
