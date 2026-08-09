package com.lifespan.app.domain.model

import com.lifespan.app.domain.battery.BatteryCalculator

/**
 * An immutable reading of the battery at a single point in time, derived from
 * `ACTION_BATTERY_CHANGED` extras plus an instantaneous current sample.
 */
data class BatterySnapshot(
    val timestamp: Long,
    val level: Int,
    val voltageMv: Int,
    val currentUa: Int,
    val temperatureCelsius: Double,
    val isCharging: Boolean,
    val plugType: PlugType,
    val health: Int = 0,
    val technology: String? = null,
    /** Remaining battery charge in micro-amp-hours, if the device reports it. */
    val chargeCounterMicroAh: Long = 0L,
) {
    /** Instantaneous current in milliamps (positive = charging on most devices). */
    val currentMa: Double get() = BatteryCalculator.microAmpsToMilliAmps(currentUa)

    /** Instantaneous power draw/charge in watts. */
    val powerWatts: Double get() = BatteryCalculator.computePowerWatts(voltageMv, currentUa)
}
