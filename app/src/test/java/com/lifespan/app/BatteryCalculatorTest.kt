package com.lifespan.app

import com.lifespan.app.domain.battery.BatteryCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryCalculatorTest {

    @Test
    fun computePowerWatts_matchesFormula() {
        // 4200 mV * 2.0 A = 8.4 W
        val watts = BatteryCalculator.computePowerWatts(voltageMv = 4200, currentUa = 2_000_000)
        assertEquals(8.4, watts, 1e-9)
    }

    @Test
    fun computePowerWatts_handlesDischargeSign() {
        val watts = BatteryCalculator.computePowerWatts(voltageMv = 4000, currentUa = -500_000)
        assertEquals(-2.0, watts, 1e-9)
    }

    @Test
    fun microAmpsToMilliAmps_dividesByThousand() {
        assertEquals(1850.0, BatteryCalculator.microAmpsToMilliAmps(1_850_000), 1e-9)
    }

    @Test
    fun tenthsCelsiusToCelsius_convertsCorrectly() {
        assertEquals(42.3, BatteryCalculator.tenthsCelsiusToCelsius(423), 1e-9)
    }

    @Test
    fun levelPercent_scalesAgainstScale() {
        assertEquals(50, BatteryCalculator.levelPercent(level = 50, scale = 100))
        assertEquals(25, BatteryCalculator.levelPercent(level = 250, scale = 1000))
    }

    @Test
    fun levelPercent_clampsAndFallsBack() {
        assertEquals(0, BatteryCalculator.levelPercent(level = -1, scale = 100))
        assertEquals(87, BatteryCalculator.levelPercent(level = 87, scale = 0))
    }

    @Test
    fun mahOverInterval_integratesCurrentOverTime() {
        // 3000 mA sustained for 30 minutes = 1500 mAh
        assertEquals(1500.0, BatteryCalculator.mahOverInterval(3000.0, 30 * 60_000L), 1e-6)
        assertEquals(0.0, BatteryCalculator.mahOverInterval(3000.0, 0L), 1e-9)
    }

    @Test
    fun sanitizeCurrentUa_mapsUnsupportedSentinelToZero() {
        assertEquals(0, BatteryCalculator.sanitizeCurrentUa(Int.MIN_VALUE))
        assertEquals(1_850_000, BatteryCalculator.sanitizeCurrentUa(1_850_000))
        assertEquals(-500_000, BatteryCalculator.sanitizeCurrentUa(-500_000))
        assertEquals(0, BatteryCalculator.sanitizeCurrentUa(0))
    }

    @Test
    fun sanitizeChargeCounterMicroAh_rejectsNonPositiveAndSentinel() {
        assertEquals(0L, BatteryCalculator.sanitizeChargeCounterMicroAh(Long.MIN_VALUE))
        assertEquals(0L, BatteryCalculator.sanitizeChargeCounterMicroAh(0L))
        assertEquals(0L, BatteryCalculator.sanitizeChargeCounterMicroAh(-1L))
        assertEquals(3_000_000L, BatteryCalculator.sanitizeChargeCounterMicroAh(3_000_000L))
    }
}
