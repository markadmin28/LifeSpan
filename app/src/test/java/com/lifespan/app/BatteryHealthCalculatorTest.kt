package com.lifespan.app

import com.lifespan.app.domain.health.BatteryHealthCalculator
import com.lifespan.app.domain.health.CapacityVerdict
import com.lifespan.app.domain.health.HealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryHealthCalculatorTest {

    @Test
    fun estimatesFullCapacityFromChargeCounter() {
        // 2,400 mAh present at 60% -> 4,000 mAh full capacity.
        val mah = BatteryHealthCalculator.estimateFullCapacityMah(
            chargeCounterMicroAh = 2_400_000,
            levelPercent = 60,
        )
        assertEquals(4_000.0, mah!!, 0.01)
    }

    @Test
    fun capacityEstimateNullWhenCounterMissingOrLevelTooLow() {
        assertNull(BatteryHealthCalculator.estimateFullCapacityMah(0, 50))
        assertNull(BatteryHealthCalculator.estimateFullCapacityMah(-5, 50))
        // Below 10% the extrapolation is too noisy to trust.
        assertNull(BatteryHealthCalculator.estimateFullCapacityMah(400_000, 9))
    }

    @Test
    fun capacityPercentOfDesignClampedTo100() {
        assertEquals(88, BatteryHealthCalculator.capacityPercentOfDesign(4_400.0, 5_000.0))
        // A fresh battery can measure above design; clamp to 100.
        assertEquals(100, BatteryHealthCalculator.capacityPercentOfDesign(5_200.0, 5_000.0))
        assertNull(BatteryHealthCalculator.capacityPercentOfDesign(null, 5_000.0))
        assertNull(BatteryHealthCalculator.capacityPercentOfDesign(4_400.0, null))
        assertNull(BatteryHealthCalculator.capacityPercentOfDesign(4_400.0, 0.0))
    }

    @Test
    fun equivalentFullCyclesFromPercentAdded() {
        // 250 percentage points of charge = 2.5 equivalent full cycles.
        assertEquals(2.5, BatteryHealthCalculator.equivalentFullCycles(250), 0.001)
        assertEquals(0.0, BatteryHealthCalculator.equivalentFullCycles(0), 0.001)
        // Defensive: negative sums (malformed sessions) never yield negative cycles.
        assertEquals(0.0, BatteryHealthCalculator.equivalentFullCycles(-40), 0.001)
    }

    @Test
    fun capacityVerdictBands() {
        assertEquals(CapacityVerdict.HEALTHY, BatteryHealthCalculator.capacityVerdict(95))
        assertEquals(CapacityVerdict.HEALTHY, BatteryHealthCalculator.capacityVerdict(90))
        assertEquals(CapacityVerdict.WORN, BatteryHealthCalculator.capacityVerdict(85))
        assertEquals(CapacityVerdict.DEGRADED, BatteryHealthCalculator.capacityVerdict(79))
        assertEquals(CapacityVerdict.UNKNOWN, BatteryHealthCalculator.capacityVerdict(null))
    }

    @Test
    fun healthStatusMapsAndroidValues() {
        assertEquals(HealthStatus.GOOD, HealthStatus.fromAndroidValue(2))
        assertEquals(HealthStatus.OVERHEAT, HealthStatus.fromAndroidValue(3))
        assertEquals(HealthStatus.DEAD, HealthStatus.fromAndroidValue(4))
        assertEquals(HealthStatus.OVER_VOLTAGE, HealthStatus.fromAndroidValue(5))
        assertEquals(HealthStatus.FAILURE, HealthStatus.fromAndroidValue(6))
        assertEquals(HealthStatus.COLD, HealthStatus.fromAndroidValue(7))
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.fromAndroidValue(1))
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.fromAndroidValue(0))
        assertEquals(HealthStatus.UNKNOWN, HealthStatus.fromAndroidValue(99))
    }
}
