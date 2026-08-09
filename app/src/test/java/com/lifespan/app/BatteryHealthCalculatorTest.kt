package com.lifespan.app

import com.lifespan.app.data.db.ChargeSessionEntity
import com.lifespan.app.domain.health.BatteryHealthCalculator
import com.lifespan.app.domain.health.BatteryWearStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthCalculatorTest {

    @Test
    fun `reports learning state without completed sessions`() {
        val estimate = BatteryHealthCalculator.calculate(emptyList())

        assertEquals(BatteryWearStatus.LEARNING, estimate.wearStatus)
        assertEquals(0.0, estimate.equivalentCycles, 0.001)
        assertNull(estimate.estimatedFullCapacityMah)
    }

    @Test
    fun `estimates median full capacity and equivalent cycles`() {
        val estimate = BatteryHealthCalculator.calculate(
            listOf(
                session(startLevel = 20, endLevel = 70, addedMah = 2_000.0),
                session(startLevel = 40, endLevel = 80, addedMah = 1_680.0),
                session(startLevel = 60, endLevel = 80, addedMah = 760.0),
            ),
        )

        assertEquals(4_000, estimate.estimatedFullCapacityMah)
        assertEquals(1.1, estimate.equivalentCycles, 0.001)
        assertEquals(BatteryWearStatus.GOOD, estimate.wearStatus)
    }

    @Test
    fun `flags repeated hot charging as stressed`() {
        val estimate = BatteryHealthCalculator.calculate(
            listOf(
                session(20, 80, 2_400.0, peakTemp = 41.0),
                session(30, 80, 2_000.0, peakTemp = 40.0),
                session(40, 80, 1_600.0, peakTemp = 36.0),
            ),
        )

        assertEquals(2, estimate.hotSessions)
        assertEquals(BatteryWearStatus.STRESSED, estimate.wearStatus)
    }

    @Test
    fun `ignores incomplete and implausible capacity samples`() {
        val estimate = BatteryHealthCalculator.calculate(
            listOf(
                session(20, null, null),
                session(40, 42, 2_000.0),
                session(10, 90, 30_000.0),
            ),
        )

        assertNull(estimate.estimatedFullCapacityMah)
        assertEquals(2, estimate.completedSessions)
    }

    private fun session(
        startLevel: Int,
        endLevel: Int?,
        addedMah: Double?,
        peakTemp: Double = 35.0,
    ) = ChargeSessionEntity(
        id = startLevel.toLong(),
        startTime = 1_000,
        endTime = endLevel?.let { 2_000L },
        startLevel = startLevel,
        endLevel = endLevel,
        plugType = "AC",
        peakTempCelsius = peakTemp,
        peakCurrentMa = 2_000.0,
        totalMahAdded = addedMah,
    )
}
