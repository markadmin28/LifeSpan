package com.lifespan.app.domain.health

import com.lifespan.app.data.db.ChargeSessionEntity
import kotlin.math.roundToInt

/**
 * Estimated battery wear derived from completed charge sessions.
 *
 * Cycles and capacity are inferred from logged charge deltas (not privileged
 * BatteryManager health APIs), so values improve as more sessions accumulate.
 */
data class BatteryHealth(
    val estimatedCycles: Double,
    val estimatedCapacityMah: Int?,
    val healthPercent: Int?,
    val sampleCount: Int,
)

object BatteryHealthCalculator {
    const val MIN_DELTA_PERCENT = 10
    const val RECENT_WINDOW = 3

    fun compute(sessions: List<ChargeSessionEntity>): BatteryHealth {
        val completed = sessions.filter { it.endTime != null && it.endLevel != null }
        val upward = completed.mapNotNull { session ->
            val end = session.endLevel ?: return@mapNotNull null
            val delta = end - session.startLevel
            if (delta <= 0) null else session to delta
        }

        val cycles = upward.sumOf { it.second } / 100.0

        val capacities = upward.mapNotNull { (session, delta) ->
            val mah = session.totalMahAdded ?: return@mapNotNull null
            if (delta < MIN_DELTA_PERCENT || mah <= 0.0) return@mapNotNull null
            mah / (delta / 100.0)
        }

        val capacityMah = capacities.lastOrNull()?.roundToInt()
        val healthPercent = if (capacities.size >= 2) {
            val peak = capacities.maxOrNull() ?: return BatteryHealth(cycles, capacityMah, null, capacities.size)
            val recent = capacities.takeLast(RECENT_WINDOW).average()
            ((recent / peak) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            null
        }

        return BatteryHealth(
            estimatedCycles = cycles,
            estimatedCapacityMah = capacityMah,
            healthPercent = healthPercent,
            sampleCount = capacities.size,
        )
    }
}
