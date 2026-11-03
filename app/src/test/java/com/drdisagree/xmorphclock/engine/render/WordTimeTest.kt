package com.drdisagree.xmorphclock.engine.render

import org.junit.Assert.assertEquals
import org.junit.Test

class WordTimeTest {

    @Test
    fun spellsHoursAndMinutes() {
        assertEquals("four forty-five", WordTime.time(16, 45))
        assertEquals("twelve o'clock", WordTime.time(0, 0))
        assertEquals("nine oh five", WordTime.time(9, 5))
        assertEquals("eleven thirteen", WordTime.time(23, 13))
        assertEquals("one twenty", WordTime.time(13, 20))
    }

    @Test
    fun roundsFuzzyTime() {
        assertEquals("four o'clock", WordTime.fuzzy(16, 1))
        assertEquals("quarter past four", WordTime.fuzzy(16, 14))
        assertEquals("half past four", WordTime.fuzzy(16, 31))
        assertEquals("quarter to five", WordTime.fuzzy(16, 44))
        assertEquals("twenty-five to five", WordTime.fuzzy(16, 35))
        assertEquals("ten past four", WordTime.fuzzy(16, 9))
        assertEquals("twelve o'clock", WordTime.fuzzy(11, 58))
    }

    @Test
    fun splitsFuzzyLines() {
        assertEquals("quarter to", WordTime.fuzzyLead(16, 45))
        assertEquals("five", WordTime.fuzzyHour(16, 45))
        assertEquals("", WordTime.fuzzyLead(16, 0))
        assertEquals("four o'clock", WordTime.fuzzyHour(16, 0))
    }
}
