package com.lifespan.app.domain.session

import com.lifespan.app.domain.health.BatteryHealthEstimator
import com.lifespan.app.domain.health.CapacityObservation

/** Derived numbers describing a single charge session. */
data class SessionStats(
    val durationMillis: Long,
    val levelDeltaPercent: Int?,
    val ratePercentPerHour: Double?,
    val impliedFullCapacityMah: Double?,
    val averageCurrentMa: Double?,
    val inProgress: Boolean,
)

/**
 * Turns the stored fields of a charge session into the figures the session
 * detail screen shows. Kept free of Room and Android types so it stays testable.
 */
object SessionStatsCalculator {

    private const val HOUR_MILLIS = 3_600_000.0

    fun compute(
        startTime: Long,
        endTime: Long?,
        startLevel: Int,
        endLevel: Int?,
        totalMahAdded: Double?,
        nowMillis: Long,
    ): SessionStats {
        val until = endTime ?: nowMillis
        val duration = (until - startTime).coerceAtLeast(0L)
        val hours = duration / HOUR_MILLIS
        val levelDelta = endLevel?.let { it - startLevel }

        return SessionStats(
            durationMillis = duration,
            levelDeltaPercent = levelDelta,
            ratePercentPerHour = if (levelDelta != null && hours > 0.0) levelDelta / hours else null,
            impliedFullCapacityMah = if (levelDelta != null && totalMahAdded != null) {
                BatteryHealthEstimator.impliedFullCapacityMah(
                    CapacityObservation(levelDeltaPercent = levelDelta, mahAdded = totalMahAdded),
                )
            } else {
                null
            },
            averageCurrentMa = if (totalMahAdded != null && hours > 0.0) totalMahAdded / hours else null,
            inProgress = endTime == null,
        )
    }

    /** Renders a duration as `2h 15m`, or `45m` when under an hour. */
    fun formatDuration(millis: Long): String {
        val totalMinutes = millis / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
