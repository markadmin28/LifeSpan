package com.lifespan.app.domain.battery

import kotlin.math.abs

/**
 * Pure conversions between the raw units reported by `BatteryManager` /
 * `ACTION_BATTERY_CHANGED` and human-friendly values. Kept free of any Android
 * runtime dependencies so it can be unit-tested on the JVM.
 */
object BatteryCalculator {

    /**
     * Power in watts.
     *
     * `P (W) = (voltage_mV / 1000) * (current_uA / 1_000_000)`
     */
    fun computePowerWatts(voltageMv: Int, currentUa: Int): Double {
        val volts = voltageMv / 1000.0
        val amps = currentUa / 1_000_000.0
        return volts * amps
    }

    /** Micro-amps to milli-amps. */
    fun microAmpsToMilliAmps(currentUa: Int): Double = currentUa / 1000.0

    /** `ACTION_BATTERY_CHANGED` reports temperature in tenths of a degree C. */
    fun tenthsCelsiusToCelsius(rawTenths: Int): Double = rawTenths / 10.0

    /**
     * Normalise the raw `level`/`scale` pair into a 0..100 percentage. Falls
     * back to the raw level when the scale is missing or invalid.
     */
    fun levelPercent(level: Int, scale: Int): Int {
        if (level < 0) return 0
        if (scale <= 0) return level.coerceIn(0, 100)
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    /**
     * [BatteryManager.getIntProperty] returns [Int.MIN_VALUE] when the property
     * is unsupported. Treat that sentinel as "unknown" (0 µA) so it cannot
     * corrupt power, mAh integration, or ETA math.
     */
    fun sanitizeCurrentUa(rawUa: Int): Int =
        if (rawUa == Int.MIN_VALUE) 0 else rawUa

    /**
     * [BatteryManager.getLongProperty] returns [Long.MIN_VALUE] when unsupported.
     * Only positive charge-counter readings are usable.
     */
    fun sanitizeChargeCounterMicroAh(rawMicroAh: Long): Long =
        if (rawMicroAh == Long.MIN_VALUE || rawMicroAh <= 0L) 0L else rawMicroAh

    /**
     * Amount of charge added, in milliamp-hours, over a time delta given an
     * average current. `mAh = mA * hours`. Uses the absolute current so it is
     * agnostic to the device's charging-current sign convention.
     */
    fun mahOverInterval(currentMa: Double, intervalMillis: Long): Double {
        if (intervalMillis <= 0L) return 0.0
        val hours = intervalMillis / 3_600_000.0
        return abs(currentMa) * hours
    }
}
