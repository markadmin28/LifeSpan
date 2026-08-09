package com.lifespan.app.domain.health

import com.lifespan.app.data.db.ChargeSessionEntity
import kotlin.math.roundToInt

/**
 * Estimated battery-longevity summary derived from recorded charge sessions.
 * All values are best-effort estimates: design capacity is not exposed to apps,
 * so capacity is inferred from charge added over a session's level delta.
 */
data class BatteryHealth(
    val estimatedCycles: Double,
    val estimatedCapacityMah: Int?,
    val healthPercent: Int?,
    val sampleCount: Int,
) {
    companion object {
        val EMPTY = BatteryHealth(0.0, null, null, 0)
    }
}

object BatteryHealthCalculator {
    /** Sessions must move the level at least this much to estimate capacity. */
    const val MIN_DELTA_PERCENT = 10

    /** Number of most-recent capacity estimates averaged for the current value. */
    const val RECENT_WINDOW = 3

    /**
     * @param sessions newest-first (as returned by the DAO).
     */
    fun compute(sessions: List<ChargeSessionEntity>): BatteryHealth {
        if (sessions.isEmpty()) return BatteryHealth.EMPTY

        val cycles = sessions.sumOf { session ->
            val end = session.endLevel
            if (end != null && end > session.startLevel) (end - session.startLevel) else 0
        } / 100.0

        val capacities: List<Double> = sessions.mapNotNull { session ->
            val end = session.endLevel ?: return@mapNotNull null
            val delta = end - session.startLevel
            val mah = session.totalMahAdded ?: return@mapNotNull null
            if (delta >= MIN_DELTA_PERCENT && mah > 0.0) mah / (delta / 100.0) else null
        }

        if (capacities.isEmpty()) {
            return BatteryHealth(
                estimatedCycles = round2(cycles),
                estimatedCapacityMah = null,
                healthPercent = null,
                sampleCount = 0,
            )
        }

        val recent = capacities.take(RECENT_WINDOW).average()
        val best = capacities.max()
        val healthPercent = ((recent / best) * 100).roundToInt().coerceIn(0, 100)

        return BatteryHealth(
            estimatedCycles = round2(cycles),
            estimatedCapacityMah = recent.roundToInt(),
            healthPercent = healthPercent,
            sampleCount = capacities.size,
        )
    }

    private fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
}
