package com.lifespan.app

import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.bubble.BubbleLevel
import com.lifespan.app.domain.bubble.BubbleStatusEvaluator
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleStatusEvaluatorTest {

    private fun snapshot(level: Int, temp: Double = 30.0, charging: Boolean = true) =
        BatterySnapshot(
            timestamp = 0L,
            level = level,
            voltageMv = 4000,
            currentUa = 1_000_000,
            temperatureCelsius = temp,
            isCharging = charging,
            plugType = if (charging) PlugType.AC else PlugType.UNPLUGGED,
        )

    @Test
    fun normalWhileChargingWellBelowLimit() {
        val status = BubbleStatusEvaluator.evaluate(snapshot(level = 55), AlertThresholds())
        assertEquals(BubbleLevel.NORMAL, status.level)
        assertFalse(status.inHomeStretch)
        assertEquals("AC", status.caption)
    }

    @Test
    fun warningInHomeStretchApproachingLimit() {
        // Limit 80, window 10 => 70..79 is the home stretch.
        val status = BubbleStatusEvaluator.evaluate(snapshot(level = 74), AlertThresholds())
        assertEquals(BubbleLevel.WARNING, status.level)
        assertTrue(status.inHomeStretch)
        assertEquals("Home stretch", status.caption)
    }

    @Test
    fun dangerWhenChargeLimitReached() {
        val status = BubbleStatusEvaluator.evaluate(snapshot(level = 80), AlertThresholds())
        assertEquals(BubbleLevel.DANGER, status.level)
        assertEquals("Unplug now", status.caption)
    }

    @Test
    fun dangerWhenOverheating() {
        val status = BubbleStatusEvaluator.evaluate(snapshot(level = 50, temp = 43.0), AlertThresholds())
        assertEquals(BubbleLevel.DANGER, status.level)
    }

    @Test
    fun progressBarsReflectThresholds() {
        // Limit 80, overheat 42.
        val status = BubbleStatusEvaluator.evaluate(snapshot(level = 40, temp = 21.0), AlertThresholds())
        assertEquals(50, status.chargeProgress) // 40 of 80
        assertEquals(50, status.overheatProgress) // 21 of 42
        assertEquals(80, status.chargeLimitPercent)
        assertEquals(42.0, status.overheatCelsius, 0.001)
    }

    @Test
    fun progressBarsCapAtHundred() {
        val status = BubbleStatusEvaluator.evaluate(snapshot(level = 100, temp = 60.0), AlertThresholds())
        assertEquals(100, status.chargeProgress)
        assertEquals(100, status.overheatProgress)
    }
}
