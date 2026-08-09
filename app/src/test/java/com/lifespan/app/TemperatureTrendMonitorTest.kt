package com.lifespan.app

import com.lifespan.app.domain.alert.TemperatureTrendMonitor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperatureTrendMonitorTest {

    @Test
    fun detectsRapidRiseWithinWindow() {
        val monitor = TemperatureTrendMonitor(windowMillis = 5 * 60_000L, riseThresholdC = 2.0)
        assertFalse(monitor.add(0L, 30.0))
        assertFalse(monitor.add(60_000L, 31.0))
        assertTrue(monitor.add(120_000L, 32.5)) // 32.5 - 30.0 = 2.5 >= 2.0
    }

    @Test
    fun slowRiseDoesNotTrigger() {
        val monitor = TemperatureTrendMonitor(windowMillis = 60_000L, riseThresholdC = 2.0)
        assertFalse(monitor.add(0L, 30.0))
        // Older sample falls out of the 60s window, so rise is measured from 30.5.
        assertFalse(monitor.add(120_000L, 30.5))
        assertFalse(monitor.add(180_000L, 31.0))
    }
}
