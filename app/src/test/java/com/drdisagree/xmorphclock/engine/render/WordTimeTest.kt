package com.drdisagree.xmorphclock.engine.render

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WordTimeTest {

    private val english = WordPhrases(
        language = "en",
        oclock = "%1\$s o'clock",
        past = "%1\$s past %2\$s",
        to = "%1\$s to %2\$s",
        quarterPast = "quarter past %1\$s",
        halfPast = "half past %1\$s",
        quarterTo = "quarter to %1\$s",
        leadPast = "%1\$s past",
        leadTo = "%1\$s to",
        leadQuarterPast = "quarter past",
        leadHalfPast = "half past",
        leadQuarterTo = "quarter to",
        hundred = "hundred"
    )

    @Test
    fun spellsHoursAndMinutes() {
        assertEquals("four forty-five", WordTime.time(16, 45))
        assertEquals("twelve o'clock", WordTime.time(0, 0))
        assertEquals("nine oh five", WordTime.time(9, 5))
        assertEquals("eleven thirteen", WordTime.time(23, 13))
        assertEquals("one twenty", WordTime.time(13, 20))
    }

    @Test
    fun spellsTwentyFourHourTime() {
        assertEquals("sixteen forty-five", WordTime.time24(16, 45, english))
        assertEquals("zero oh five", WordTime.time24(0, 5, english))
        assertEquals("twenty-three hundred", WordTime.time24(23, 0, english))
        assertEquals("nine", WordTime.hour24(9))
        assertEquals("twenty-one", WordTime.hour24(21))
    }

    @Test
    fun roundsFuzzyTime() {
        assertEquals("four o'clock", WordTime.fuzzy(16, 1, english))
        assertEquals("quarter past four", WordTime.fuzzy(16, 14, english))
        assertEquals("half past four", WordTime.fuzzy(16, 31, english))
        assertEquals("quarter to five", WordTime.fuzzy(16, 44, english))
        assertEquals("twenty-five to five", WordTime.fuzzy(16, 35, english))
        assertEquals("ten past four", WordTime.fuzzy(16, 9, english))
        assertEquals("twelve o'clock", WordTime.fuzzy(11, 58, english))
    }

    @Test
    fun splitsFuzzyLines() {
        assertEquals("quarter to", WordTime.fuzzyLead(16, 45, english))
        assertEquals("five", WordTime.fuzzyHour(16, 45, english))
        assertEquals("", WordTime.fuzzyLead(16, 0, english))
        assertEquals("four o'clock", WordTime.fuzzyHour(16, 0, english))
        assertEquals("twenty past", WordTime.fuzzyLead(16, 21, english))
    }

    @Test
    fun untranslatedPhrasesKeepEnglishNumbers() {
        assertEquals("quarter past four", WordTime.fuzzy(16, 15, english, Locale.GERMAN))
    }

    @Test
    fun translatedPatternControlsWordOrder() {
        val reordered = english.copy(oclock = "Uhr %1\$s", past = "%2\$s and %1\$s")
        assertEquals("Uhr four", WordTime.fuzzy(16, 0, reordered))
        assertEquals("four and ten", WordTime.fuzzy(16, 10, reordered))
    }
}
