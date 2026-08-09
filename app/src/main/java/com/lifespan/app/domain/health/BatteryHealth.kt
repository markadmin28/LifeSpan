package com.lifespan.app.domain.health

import android.os.BatteryManager

/** The battery condition the platform reports through `EXTRA_HEALTH`. */
enum class ReportedHealth(val label: String) {
    GOOD("Good"),
    OVERHEAT("Overheating"),
    DEAD("Dead"),
    OVER_VOLTAGE("Over voltage"),
    FAILURE("Failure"),
    COLD("Too cold"),
    UNKNOWN("Unknown");

    /** True when the platform is flagging a condition worth surfacing to the user. */
    val isProblem: Boolean get() = this != GOOD && this != UNKNOWN

    companion object {
        fun fromExtra(raw: Int): ReportedHealth = when (raw) {
            BatteryManager.BATTERY_HEALTH_GOOD -> GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> FAILURE
            BatteryManager.BATTERY_HEALTH_COLD -> COLD
            else -> UNKNOWN
        }
    }
}

/** How much usable capacity is left relative to the factory rating. */
enum class WearGrade(val label: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    WORN("Worn"),
}

/** How much the capacity estimate can be trusted, based on how many sessions fed it. */
enum class EstimateConfidence(val label: String) {
    NONE("No data yet"),
    LOW("Low confidence"),
    MEDIUM("Building confidence"),
    HIGH("High confidence"),
}

/**
 * One completed charge session reduced to the two numbers capacity math needs:
 * how far the level moved and how many mAh went in over that span.
 */
data class CapacityObservation(
    val levelDeltaPercent: Int,
    val mahAdded: Double,
)

/**
 * Estimated battery lifespan: measured full-charge capacity, how it compares to
 * the factory design capacity, and the platform-reported condition.
 */
data class BatteryHealthEstimate(
    val designCapacityMah: Double? = null,
    val measuredCapacityMah: Double? = null,
    val statePercent: Int? = null,
    val sampleCount: Int = 0,
    val confidence: EstimateConfidence = EstimateConfidence.NONE,
    val grade: WearGrade? = null,
    val reportedHealth: ReportedHealth = ReportedHealth.UNKNOWN,
) {
    val hasEstimate: Boolean get() = measuredCapacityMah != null

    /** mAh of the original rating that wear has taken away. */
    val lostCapacityMah: Double?
        get() {
            val design = designCapacityMah ?: return null
            val measured = measuredCapacityMah ?: return null
            return (design - measured).coerceAtLeast(0.0)
        }
}

/**
 * Derives state-of-health from recorded charge sessions.
 *
 * A session that adds `mAh` while the level climbs by `delta` percent implies a
 * full-charge capacity of `mAh / (delta / 100)`. Comparing the median of those
 * implied capacities against the factory design capacity gives a wear estimate
 * that improves as more sessions are logged.
 */
object BatteryHealthEstimator {

    /** Below this level swing, sampling noise dominates the capacity math. */
    const val MIN_LEVEL_DELTA_PERCENT = 15

    /** Estimates outside this band mean bad input rather than a real battery. */
    private val PLAUSIBLE_STATE_PERCENT = 1..150

    fun impliedFullCapacityMah(observation: CapacityObservation): Double? {
        if (observation.levelDeltaPercent < MIN_LEVEL_DELTA_PERCENT) return null
        if (observation.mahAdded <= 0.0) return null
        return observation.mahAdded * 100.0 / observation.levelDeltaPercent
    }

    fun estimate(
        observations: List<CapacityObservation>,
        designCapacityMah: Double? = null,
        reportedHealth: ReportedHealth = ReportedHealth.UNKNOWN,
    ): BatteryHealthEstimate {
        val implied = observations.mapNotNull(::impliedFullCapacityMah)
        val measured = median(implied)
        val design = designCapacityMah?.takeIf { it > 0.0 }
        val statePercent = if (measured != null && design != null) {
            Math.round(measured / design * 100.0).toInt().coerceIn(PLAUSIBLE_STATE_PERCENT)
        } else {
            null
        }

        return BatteryHealthEstimate(
            designCapacityMah = design,
            measuredCapacityMah = measured,
            statePercent = statePercent,
            sampleCount = implied.size,
            confidence = confidenceFor(implied.size),
            grade = statePercent?.let(::gradeFor),
            reportedHealth = reportedHealth,
        )
    }

    fun gradeFor(statePercent: Int): WearGrade = when {
        statePercent >= 90 -> WearGrade.EXCELLENT
        statePercent >= 80 -> WearGrade.GOOD
        statePercent >= 65 -> WearGrade.FAIR
        else -> WearGrade.WORN
    }

    fun confidenceFor(sampleCount: Int): EstimateConfidence = when {
        sampleCount <= 0 -> EstimateConfidence.NONE
        sampleCount < 3 -> EstimateConfidence.LOW
        sampleCount < 6 -> EstimateConfidence.MEDIUM
        else -> EstimateConfidence.HIGH
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }
}
