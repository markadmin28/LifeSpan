package com.lifespan.app.domain.battery

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Estimates time-to-full (while charging) or time-to-empty (while discharging)
 * from the remaining charge counter and the instantaneous current. Pure and
 * unit-testable.
 *
 * `hours = chargeCounter(µAh) / current(µA)`, so minutes = that × 60.
 */
object TimeEstimator {

    /** Returns estimated minutes, or null when it cannot be computed. */
    fun estimateMinutes(
        chargeCounterMicroAh: Long,
        currentMicroA: Int,
        levelPercent: Int,
        isCharging: Boolean,
    ): Long? {
        if (chargeCounterMicroAh <= 0L || currentMicroA == 0) return null
        val current = abs(currentMicroA.toDouble())
        return if (isCharging) {
            if (levelPercent <= 0) return null
            val fullCounter = chargeCounterMicroAh * 100.0 / levelPercent
            val remaining = fullCounter - chargeCounterMicroAh
            if (remaining <= 0.0) 0L else (remaining / current * 60.0).roundToLong()
        } else {
            (chargeCounterMicroAh / current * 60.0).roundToLong()
        }
    }

    /** Human-friendly label like "1h 20m" or "45m". */
    fun formatMinutes(minutes: Long): String = when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}
