package com.lifespan.app.domain.health

/**
 * Battery health as reported by the OS via `ACTION_BATTERY_CHANGED`'s
 * `EXTRA_HEALTH`. Mirrors `BatteryManager.BATTERY_HEALTH_*` by value so the
 * domain layer stays free of Android dependencies.
 */
enum class HealthStatus(val androidValue: Int, val label: String) {
    UNKNOWN(1, "Unknown"),
    GOOD(2, "Good"),
    OVERHEAT(3, "Overheat"),
    DEAD(4, "Dead"),
    OVER_VOLTAGE(5, "Over voltage"),
    FAILURE(6, "Failure"),
    COLD(7, "Cold"),
    ;

    companion object {
        fun fromAndroidValue(value: Int): HealthStatus =
            entries.firstOrNull { it.androidValue == value } ?: UNKNOWN
    }
}

/** Aggregated statistics over all recorded charge sessions. */
data class ChargeStats(
    val sessionCount: Int = 0,
    /** Sum of battery percent gained across completed sessions. */
    val percentAdded: Long = 0,
    /** Sum of estimated mAh delivered across sessions. */
    val mahAdded: Double = 0.0,
    val avgPeakTempCelsius: Double? = null,
    val maxPeakTempCelsius: Double? = null,
    /** Sessions whose peak temperature reached the "hot" threshold. */
    val hotSessionCount: Int = 0,
)

/**
 * Pure battery wear / longevity math: capacity estimation from the charge
 * counter and equivalent-full-cycle accounting from session history.
 */
object BatteryHealthCalculator {

    /** Temperature at/above which a session is considered thermally stressful. */
    const val HOT_SESSION_CELSIUS = 40.0

    /**
     * Extrapolates the full-charge capacity (mAh) from the remaining charge
     * counter at the current level: `full = counter * 100 / level`.
     * Returns null when the device does not report a charge counter or the
     * level is too low for a meaningful extrapolation.
     */
    fun estimateFullCapacityMah(chargeCounterMicroAh: Long, levelPercent: Int): Double? {
        if (chargeCounterMicroAh <= 0L || levelPercent < 10) return null
        return chargeCounterMicroAh / 1000.0 * 100.0 / levelPercent
    }

    /**
     * Estimated remaining capacity as a percentage of the design capacity,
     * clamped to 1..100. Null when either input is unknown.
     */
    fun capacityPercentOfDesign(estimatedFullMah: Double?, designMah: Double?): Int? {
        if (estimatedFullMah == null || designMah == null || designMah <= 0.0) return null
        return (estimatedFullMah / designMah * 100.0).toInt().coerceIn(1, 100)
    }

    /**
     * Equivalent full charge cycles logged so far: 100 percentage points of
     * charge added counts as one cycle (the industry convention).
     */
    fun equivalentFullCycles(percentAdded: Long): Double =
        (percentAdded.coerceAtLeast(0) / 100.0)

    /** Coarse wear verdict used for the health card's capacity pill. */
    fun capacityVerdict(capacityPercent: Int?): CapacityVerdict = when {
        capacityPercent == null -> CapacityVerdict.UNKNOWN
        capacityPercent >= 90 -> CapacityVerdict.HEALTHY
        capacityPercent >= 80 -> CapacityVerdict.WORN
        else -> CapacityVerdict.DEGRADED
    }
}

enum class CapacityVerdict { HEALTHY, WORN, DEGRADED, UNKNOWN }
