package com.drdisagree.xmorphclock.domain.editor

import com.drdisagree.xmorphclock.domain.model.ClockFace
import com.drdisagree.xmorphclock.domain.model.FacePlacement
import com.drdisagree.xmorphclock.domain.model.ItemState

data class GroupMember(
    val source: Int,
    val face: ClockFace,
    val large: FacePlacement,
    val small: FacePlacement,
    val aodLarge: FacePlacement? = null,
    val aodSmall: FacePlacement? = null,
    val hidden: Set<ItemState>
)
