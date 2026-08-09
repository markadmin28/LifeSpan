package com.lifespan.app.domain.health

import com.lifespan.app.data.db.ChargeSessionEntity

/**
 * A history-based estimate, not the device manufacturer's rated health value.
 *
 * Android does not expose design capacity consistently, so LifeSpan reports the
 * full-charge capacity inferred from completed sessions and labels thermal wear
 * separately instead of presenting a misleading "health percent".
 */
data class BatteryHealthEstimate(
    val estimatedFullCapacityMah: Int?,
    val equivalentCycles: Double,
    val completedSessions: Int,
    val hotSessions: Int,
    val wearStatus: BatteryWearStatus,
)

enum class BatteryWearStatus {
    LEARNING,
    GOOD,
    WATCH,
    STRESSED,
}

object BatteryHealthCalculator {
    private const val MIN_LEVEL_GAIN = 5
    private const val HOT_CELSIUS = 40.0
    private const val VERY_HOT_CELSIUS = 44.0
    private const val MIN_PLAUSIBLE_CAPACITY_MAH = 300.0
    private const val MAX_PLAUSIBLE_CAPACITY_MAH = 20_000.0

    fun calculate(sessions: List<ChargeSessionEntity>): BatteryHealthEstimate {
        val completed = sessions.filter { session ->
            val endLevel = session.endLevel
            session.endTime != null && endLevel != null && endLevel > session.startLevel
        }

        val equivalentCycles = completed.sumOf { session ->
            ((session.endLevel ?: session.startLevel) - session.startLevel) / 100.0
        }

        val capacitySamples = completed.mapNotNull { session ->
            val addedMah = session.totalMahAdded ?: return@mapNotNull null
            val levelGain = (session.endLevel ?: return@mapNotNull null) - session.startLevel
            if (addedMah <= 0.0 || levelGain < MIN_LEVEL_GAIN) return@mapNotNull null
            (addedMah * 100.0 / levelGain)
                .takeIf { it in MIN_PLAUSIBLE_CAPACITY_MAH..MAX_PLAUSIBLE_CAPACITY_MAH }
        }.sorted()

        val hotSessions = completed.count { it.peakTempCelsius >= HOT_CELSIUS }
        val wearStatus = when {
            completed.isEmpty() -> BatteryWearStatus.LEARNING
            completed.any { it.peakTempCelsius >= VERY_HOT_CELSIUS } ||
                hotSessions * 2 >= completed.size -> BatteryWearStatus.STRESSED
            hotSessions > 0 -> BatteryWearStatus.WATCH
            else -> BatteryWearStatus.GOOD
        }

        return BatteryHealthEstimate(
            estimatedFullCapacityMah = capacitySamples.medianOrNull()?.toInt(),
            equivalentCycles = equivalentCycles,
            completedSessions = completed.size,
            hotSessions = hotSessions,
            wearStatus = wearStatus,
        )
    }

    private fun List<Double>.medianOrNull(): Double? {
        if (isEmpty()) return null
        val middle = size / 2
        return if (size % 2 == 0) (this[middle - 1] + this[middle]) / 2.0 else this[middle]
    }
}
