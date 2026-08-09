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
    val capacityHistory: List<SessionCapacityEstimate> = emptyList(),
    val capacityFadePercent: Int? = null,
    val guidanceStatus: BatteryGuidanceStatus = BatteryGuidanceStatus.COLLECTING_DATA,
) {
    companion object {
        val EMPTY = BatteryHealth(
            estimatedCycles = 0.0,
            estimatedCapacityMah = null,
            healthPercent = null,
            sampleCount = 0,
        )
    }
}

/** A best-effort capacity estimate from one completed charge session. */
data class SessionCapacityEstimate(
    val sessionId: Long,
    val sessionStartTime: Long,
    val estimatedCapacityMah: Int,
)

/** Typed longevity guidance so presentation code does not infer meaning from text. */
enum class BatteryGuidanceStatus {
    COLLECTING_DATA,
    STABLE,
    MODERATE_FADE,
    SIGNIFICANT_FADE,
}

object BatteryHealthCalculator {
    /** Sessions must move the level at least this much to estimate capacity. */
    const val MIN_DELTA_PERCENT = 10

    /** Number of samples used by the rolling median, which rejects isolated noise. */
    const val RECENT_WINDOW = 3

    /** At least three complete estimates are required before reporting wear. */
    const val MIN_WEAR_SAMPLES = 3

    private const val MIN_PLAUSIBLE_CAPACITY_MAH = 250.0
    private const val MAX_PLAUSIBLE_CAPACITY_MAH = 30_000.0
    private const val NOISE_FLOOR_PERCENT = 3

    /**
     * Input order does not matter. Capacity history is always returned oldest-first.
     *
     * Capacity and wear remain best-effort signals rather than hardware diagnostics:
     * Android does not expose design capacity consistently, and charge-counter readings
     * vary with temperature, load and vendor implementation.
     */
    fun compute(sessions: List<ChargeSessionEntity>): BatteryHealth {
        if (sessions.isEmpty()) return BatteryHealth.EMPTY

        val cycles = sessions.sumOf { session ->
            val end = session.endLevel
            if (end != null && end > session.startLevel) (end - session.startLevel) else 0
        } / 100.0

        val capacityHistory = sessions
            .sortedWith(compareBy<ChargeSessionEntity> { it.startTime }.thenBy { it.id })
            .mapNotNull { session ->
                capacityEstimate(session)
            }

        if (capacityHistory.isEmpty()) {
            return BatteryHealth(
                estimatedCycles = round2(cycles),
                estimatedCapacityMah = null,
                healthPercent = null,
                sampleCount = 0,
            )
        }

        val capacities = capacityHistory.map { it.estimatedCapacityMah.toDouble() }
        val currentCapacity = median(capacities.takeLast(RECENT_WINDOW))

        if (capacities.size < MIN_WEAR_SAMPLES) {
            return BatteryHealth(
                estimatedCycles = round2(cycles),
                estimatedCapacityMah = currentCapacity.roundToInt(),
                healthPercent = null,
                sampleCount = capacities.size,
                capacityHistory = capacityHistory,
                capacityFadePercent = null,
                guidanceStatus = BatteryGuidanceStatus.COLLECTING_DATA,
            )
        }

        // The best rolling median is a conservative observed baseline. A lone noisy
        // high estimate cannot make every later session look artificially worn.
        val referenceCapacity = capacities
            .windowed(size = RECENT_WINDOW, step = 1)
            .maxOf(::median)
        val rawFade = (100.0 * (referenceCapacity - currentCapacity) / referenceCapacity)
            .roundToInt()
            .coerceIn(0, 100)
        val fade = if (rawFade <= NOISE_FLOOR_PERCENT) 0 else rawFade
        val healthPercent = (100 - fade).coerceIn(0, 100)

        return BatteryHealth(
            estimatedCycles = round2(cycles),
            estimatedCapacityMah = currentCapacity.roundToInt(),
            healthPercent = healthPercent,
            sampleCount = capacities.size,
            capacityHistory = capacityHistory,
            capacityFadePercent = fade,
            guidanceStatus = guidanceFor(fade),
        )
    }

    private fun capacityEstimate(session: ChargeSessionEntity): SessionCapacityEstimate? {
        if (session.endTime == null) return null
        val end = session.endLevel ?: return null
        val delta = end - session.startLevel
        val mah = session.totalMahAdded ?: return null
        if (delta < MIN_DELTA_PERCENT || !mah.isFinite() || mah <= 0.0) return null

        val estimate = mah / (delta / 100.0)
        if (!estimate.isFinite() ||
            estimate !in MIN_PLAUSIBLE_CAPACITY_MAH..MAX_PLAUSIBLE_CAPACITY_MAH
        ) {
            return null
        }
        return SessionCapacityEstimate(
            sessionId = session.id,
            sessionStartTime = session.startTime,
            estimatedCapacityMah = estimate.roundToInt(),
        )
    }

    private fun guidanceFor(fadePercent: Int): BatteryGuidanceStatus = when {
        fadePercent < 10 -> BatteryGuidanceStatus.STABLE
        fadePercent < 20 -> BatteryGuidanceStatus.MODERATE_FADE
        else -> BatteryGuidanceStatus.SIGNIFICANT_FADE
    }

    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
}
