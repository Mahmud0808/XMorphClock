package com.drdisagree.xmorphclock.data.mapper

import com.drdisagree.xmorphclock.domain.model.HookReport
import com.drdisagree.xmorphclock.domain.model.HookSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HookReportJsonMapperTest {

    private val healthy = HookReport(
        timestamp = 1_700_000_000_000L,
        framework = "Vector v2.2 (API 102)",
        systemUiVersion = "17 (37)",
        buildId = "CP31",
        adapter = "LockscreenElementFactory",
        signals = listOf(HookSignal("CLOCK_SIZE", "KeyguardClockViewModel"), HookSignal("KEYGUARD_PROGRESS", "KeyguardTransitionRepository")),
        clockFace = "Stacked",
        errors = emptyList()
    )

    @Test
    fun roundTrip() {
        assertEquals(healthy, HookReportJsonMapper.fromJson(HookReportJsonMapper.toJson(healthy)))
    }

    @Test
    fun nullsSurviveRoundTrip() {
        val broken = healthy.copy(adapter = null, clockFace = null, signals = listOf(HookSignal("CLOCK_SIZE", null)), errors = listOf("boom"))
        val parsed = HookReportJsonMapper.fromJson(HookReportJsonMapper.toJson(broken))
        assertEquals(broken, parsed)
        assertFalse(parsed!!.isHealthy)
    }

    @Test
    fun healthyReport() {
        assertTrue(healthy.isHealthy)
    }

    @Test
    fun invalidJsonReturnsNull() {
        assertNull(HookReportJsonMapper.fromJson("not json"))
    }
}
