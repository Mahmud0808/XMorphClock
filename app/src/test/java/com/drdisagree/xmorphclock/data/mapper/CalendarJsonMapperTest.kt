package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.CalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarJsonMapperTest {

    @Test
    fun eventsRoundTrip() {
        val events = listOf(
            CalendarEvent("Stand-up", 1_000L, 2_000L, false, "Room 4"),
            CalendarEvent("Holiday", 3_000L, 90_000L, true, "")
        )
        assertEquals(events, CalendarJsonMapper.fromJson(CalendarJsonMapper.toJson(events)))
    }

    @Test
    fun brokenJsonGivesNoEvents() {
        assertEquals(emptyList<CalendarEvent>(), CalendarJsonMapper.fromJson("{"))
        assertEquals(emptyList<CalendarEvent>(), CalendarJsonMapper.fromJson(null))
    }
}
