package com.lifespan.app

import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.domain.health.BatteryHealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthCalculatorTest {

    @Test
    fun emptySessions_zeros() {
        val health = BatteryHealthCalculator.compute(emptyList())
        assertEquals(0.0, health.estimatedCycles, 1e-9)
        assertNull(health.estimatedCapacityMah)
        assertNull(health.healthPercent)
        assertEquals(0, health.sampleCount)
    }

    @Test
    fun cyclesAndCapacityFromCompletedSessions() {
        val sessions = listOf(
            session(start = 20, end = 80, mah = 2400.0), // 60% → 4000 mAh
            session(start = 30, end = 90, mah = 2280.0), // 60% → 3800 mAh
            session(start = 40, end = 90, mah = 1850.0), // 50% → 3700 mAh
        )
        val health = BatteryHealthCalculator.compute(sessions)
        assertEquals(1.7, health.estimatedCycles, 1e-9) // (60+60+50)/100
        assertEquals(3700, health.estimatedCapacityMah)
        assertEquals(3, health.sampleCount)
        // recent avg of last 3 = (4000+3800+3700)/3 = 3833.3 → 96% of peak 4000
        assertEquals(96, health.healthPercent)
    }

    @Test
    fun ignoresTinyDeltasAndInProgress() {
        val sessions = listOf(
            session(start = 50, end = 55, mah = 200.0), // 5% < MIN_DELTA
            ChargeSessionEntity(
                id = 2,
                startTime = 2_000,
                endTime = null,
                startLevel = 10,
                endLevel = null,
                plugType = "AC",
                peakTempCelsius = 35.0,
                peakCurrentMa = 1000.0,
                totalMahAdded = 500.0,
            ),
            session(start = 10, end = 70, mah = 3000.0), // valid 60%
        )
        val health = BatteryHealthCalculator.compute(sessions)
        assertEquals(0.65, health.estimatedCycles, 1e-9) // (5+60)/100 — tiny still counts for cycles
        assertEquals(5000, health.estimatedCapacityMah) // only the valid sample
        assertEquals(1, health.sampleCount)
        assertNull(health.healthPercent) // need ≥2 capacity samples
    }

    private fun session(start: Int, end: Int, mah: Double) = ChargeSessionEntity(
        id = start.toLong(),
        startTime = start * 1_000L,
        endTime = end * 1_000L,
        startLevel = start,
        endLevel = end,
        plugType = "AC",
        peakTempCelsius = 36.0,
        peakCurrentMa = 1500.0,
        totalMahAdded = mah,
    )
}
