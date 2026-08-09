package com.lifespan.app

import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.domain.health.BatteryHealthCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthCalculatorTest {

    private fun session(start: Int, end: Int?, mah: Double?) = ChargeSessionEntity(
        startTime = 0L,
        endTime = 1L,
        startLevel = start,
        endLevel = end,
        plugType = "AC",
        peakTempCelsius = 35.0,
        peakCurrentMa = 1000.0,
        totalMahAdded = mah,
    )

    @Test
    fun emptyWhenNoSessions() {
        val health = BatteryHealthCalculator.compute(emptyList())
        assertEquals(0.0, health.estimatedCycles, 0.0)
        assertNull(health.estimatedCapacityMah)
        assertNull(health.healthPercent)
        assertEquals(0, health.sampleCount)
    }

    @Test
    fun computesCyclesCapacityAndHealth() {
        // Newest-first.
        val sessions = listOf(
            session(20, 80, 1800.0), // capacity 3000
            session(30, 80, 1400.0), // capacity 2800
            session(10, 90, 2560.0), // capacity 3200 (best)
        )
        val health = BatteryHealthCalculator.compute(sessions)
        // cycles = (60 + 50 + 80) / 100 = 1.9
        assertEquals(1.9, health.estimatedCycles, 1e-9)
        // recent = avg(3000, 2800, 3200) = 3000
        assertEquals(3000, health.estimatedCapacityMah)
        // health = round(3000 / 3200 * 100) = 94
        assertEquals(94, health.healthPercent)
        assertEquals(3, health.sampleCount)
    }

    @Test
    fun ignoresTinyOrIncompleteSessionsForCapacity() {
        val sessions = listOf(
            session(79, 80, 40.0), // delta 1% -> ignored for capacity
            session(50, null, null), // in progress -> ignored
        )
        val health = BatteryHealthCalculator.compute(sessions)
        assertNull(health.estimatedCapacityMah)
        assertEquals(0, health.sampleCount)
        // cycle contribution: only first session counts (1%) => 0.01
        assertEquals(0.01, health.estimatedCycles, 1e-9)
    }
}
