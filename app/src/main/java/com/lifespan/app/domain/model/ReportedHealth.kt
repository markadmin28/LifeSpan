package com.lifespan.app.domain.model

import android.os.BatteryManager

/**
 * Human-readable mapping of the hardware-reported `BatteryManager.EXTRA_HEALTH`
 * value (distinct from the capacity-based estimate in `domain/health`).
 */
object ReportedHealth {

    /** User-facing label, or `null` when the platform reports nothing useful. */
    fun label(health: Int): String? = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> null
    }

    /** True when the reported health warrants a warning color in the UI. */
    fun isConcerning(health: Int): Boolean = when (health) {
        BatteryManager.BATTERY_HEALTH_OVERHEAT,
        BatteryManager.BATTERY_HEALTH_DEAD,
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE,
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE,
        BatteryManager.BATTERY_HEALTH_COLD,
        -> true

        else -> false
    }
}
