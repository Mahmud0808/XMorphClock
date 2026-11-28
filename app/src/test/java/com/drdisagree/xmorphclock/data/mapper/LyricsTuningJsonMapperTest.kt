package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.LyricsTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTuningJsonMapperTest {

    @Test
    fun roundTripKeepsTunings() {
        val tunings = mapOf(
            "artist|song" to LyricsTuning(offsetMs = -750L, pinned = "lrclib:42"),
            "other|track" to LyricsTuning(hidden = true)
        )
        assertEquals(tunings, LyricsTuningJsonMapper.fromJson(LyricsTuningJsonMapper.toJson(tunings)))
    }

    @Test
    fun defaultTuningsAreDropped() {
        val json = LyricsTuningJsonMapper.toJson(mapOf("a|b" to LyricsTuning()))
        assertTrue(LyricsTuningJsonMapper.fromJson(json).isEmpty())
    }

    @Test
    fun brokenJsonIsEmpty() {
        assertTrue(LyricsTuningJsonMapper.fromJson("{broken").isEmpty())
        assertTrue(LyricsTuningJsonMapper.fromJson(null).isEmpty())
    }
}
