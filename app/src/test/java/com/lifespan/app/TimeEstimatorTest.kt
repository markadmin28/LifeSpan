package com.lifespan.app

import com.lifespan.app.domain.battery.TimeEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeEstimatorTest {

    @Test
    fun timeToEmptyWhileDischarging() {
        // 3000 mAh remaining, drawing 1500 mA -> 2h = 120 min.
        val minutes = TimeEstimator.estimateMinutes(
            chargeCounterMicroAh = 3_000_000,
            currentMicroA = -1_500_000,
            levelPercent = 80,
            isCharging = false,
        )
        assertEquals(120L, minutes)
    }

    @Test
    fun timeToFullWhileCharging() {
        // At 50% with 1500 mAh present, full = 3000 mAh; 1500 mAh remaining at 1500 mA -> 60 min.
        val minutes = TimeEstimator.estimateMinutes(
            chargeCounterMicroAh = 1_500_000,
            currentMicroA = 1_500_000,
            levelPercent = 50,
            isCharging = true,
        )
        assertEquals(60L, minutes)
    }

    @Test
    fun nullWhenNoCurrentOrCounter() {
        assertNull(TimeEstimator.estimateMinutes(1_000_000, 0, 50, true))
        assertNull(TimeEstimator.estimateMinutes(0, 1_000_000, 50, true))
    }

    @Test
    fun formatMinutesReadable() {
        assertEquals("1h 20m", TimeEstimator.formatMinutes(80))
        assertEquals("45m", TimeEstimator.formatMinutes(45))
    }
}
