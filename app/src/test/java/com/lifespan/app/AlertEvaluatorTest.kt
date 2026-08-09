package com.lifespan.app

import com.lifespan.app.domain.alert.AlertEvaluator
import com.lifespan.app.domain.alert.AlertThresholds
import com.lifespan.app.domain.alert.AlertType
import com.lifespan.app.domain.model.BatterySnapshot
import com.lifespan.app.domain.model.PlugType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEvaluatorTest {

    private fun snapshot(
        level: Int = 50,
        temp: Double = 30.0,
        charging: Boolean = true,
    ) = BatterySnapshot(
        timestamp = 0L,
        level = level,
        voltageMv = 4000,
        currentUa = 1_000_000,
        temperatureCelsius = temp,
        isCharging = charging,
        plugType = if (charging) PlugType.AC else PlugType.UNPLUGGED,
    )

    @Test
    fun overheat_firesAtOrAboveThreshold() {
        val alerts = AlertEvaluator.evaluate(snapshot(temp = 42.0), AlertThresholds())
        assertTrue(AlertType.OVERHEAT in alerts)
    }

    @Test
    fun overheat_doesNotFireBelowThreshold() {
        val alerts = AlertEvaluator.evaluate(snapshot(temp = 41.9), AlertThresholds())
        assertTrue(AlertType.OVERHEAT !in alerts)
    }

    @Test
    fun chargeLimit_firesWhenChargingAtLimit() {
        val alerts = AlertEvaluator.evaluate(snapshot(level = 80, charging = true), AlertThresholds())
        assertTrue(AlertType.CHARGE_LIMIT in alerts)
    }

    @Test
    fun chargeLimit_doesNotFireWhenNotCharging() {
        val alerts = AlertEvaluator.evaluate(snapshot(level = 95, charging = false), AlertThresholds())
        assertTrue(AlertType.CHARGE_LIMIT !in alerts)
    }

    @Test
    fun disabledThresholds_produceNoAlerts() {
        val thresholds = AlertThresholds(overheatEnabled = false, chargeLimitEnabled = false)
        val alerts = AlertEvaluator.evaluate(snapshot(level = 100, temp = 50.0), thresholds)
        assertEquals(emptySet<AlertType>(), alerts)
    }

    @Test
    fun bothAlerts_canFireTogether() {
        val alerts = AlertEvaluator.evaluate(snapshot(level = 100, temp = 45.0, charging = true), AlertThresholds())
        assertEquals(setOf(AlertType.OVERHEAT, AlertType.CHARGE_LIMIT), alerts)
    }
}
