package com.lifespan.app

import com.lifespan.app.domain.session.SessionStatsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStatsCalculatorTest {

    private val start = 1_000_000L
    private val hour = 3_600_000L

    @Test
    fun `a completed session reports duration rate and capacity`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = start + 2 * hour,
            startLevel = 30,
            endLevel = 80,
            totalMahAdded = 2000.0,
            nowMillis = start + 5 * hour,
        )

        assertEquals(2 * hour, stats.durationMillis)
        assertEquals(50, stats.levelDeltaPercent)
        assertEquals(25.0, stats.ratePercentPerHour!!, 0.01)
        assertEquals(4000.0, stats.impliedFullCapacityMah!!, 0.01)
        assertEquals(1000.0, stats.averageCurrentMa!!, 0.01)
        assertFalse(stats.inProgress)
    }

    @Test
    fun `an open session measures duration up to now`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = null,
            startLevel = 40,
            endLevel = 55,
            totalMahAdded = 600.0,
            nowMillis = start + hour,
        )

        assertTrue(stats.inProgress)
        assertEquals(hour, stats.durationMillis)
        assertEquals(15.0, stats.ratePercentPerHour!!, 0.01)
    }

    @Test
    fun `a session too short for capacity math still reports its rate`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = start + hour / 2,
            startLevel = 70,
            endLevel = 78,
            totalMahAdded = 320.0,
            nowMillis = start + hour,
        )

        assertEquals(16.0, stats.ratePercentPerHour!!, 0.01)
        assertNull(stats.impliedFullCapacityMah)
    }

    @Test
    fun `a session with no end level omits level derived figures`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = null,
            startLevel = 50,
            endLevel = null,
            totalMahAdded = null,
            nowMillis = start + hour,
        )

        assertNull(stats.levelDeltaPercent)
        assertNull(stats.ratePercentPerHour)
        assertNull(stats.impliedFullCapacityMah)
        assertNull(stats.averageCurrentMa)
    }

    @Test
    fun `a zero length session does not divide by zero`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = start,
            startLevel = 50,
            endLevel = 51,
            totalMahAdded = 40.0,
            nowMillis = start,
        )

        assertEquals(0L, stats.durationMillis)
        assertNull(stats.ratePercentPerHour)
        assertNull(stats.averageCurrentMa)
    }

    @Test
    fun `a clock that jumped backwards does not report negative duration`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = null,
            startLevel = 50,
            endLevel = 60,
            totalMahAdded = 400.0,
            nowMillis = start - hour,
        )
        assertEquals(0L, stats.durationMillis)
    }

    @Test
    fun `a discharge span reports a negative rate`() {
        val stats = SessionStatsCalculator.compute(
            startTime = start,
            endTime = start + hour,
            startLevel = 80,
            endLevel = 60,
            totalMahAdded = null,
            nowMillis = start + hour,
        )
        assertEquals(-20, stats.levelDeltaPercent)
        assertEquals(-20.0, stats.ratePercentPerHour!!, 0.01)
    }

    @Test
    fun `durations render as hours and minutes`() {
        assertEquals("0m", SessionStatsCalculator.formatDuration(0))
        assertEquals("45m", SessionStatsCalculator.formatDuration(45 * 60_000L))
        assertEquals("1h 0m", SessionStatsCalculator.formatDuration(hour))
        assertEquals("2h 15m", SessionStatsCalculator.formatDuration(2 * hour + 15 * 60_000L))
    }
}
