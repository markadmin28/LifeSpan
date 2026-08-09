package com.lifespan.app.domain.usage

/**
 * Per-app activity used as a proxy for battery consumption. Third-party apps
 * cannot read true per-app battery mAh (that API is privileged), so we rank by
 * foreground time over a recent window and express each app's [batteryPercent]
 * as its share of total tracked foreground time.
 */
data class AppUsage(
    val packageName: String,
    val label: String,
    val foregroundMillis: Long,
    val lastUsedMillis: Long = 0L,
    val batteryPercent: Double = 0.0,
) {
    val foregroundMinutes: Long get() = foregroundMillis / 60_000L
}

/** Coarse consumption tier derived from an app's estimated battery share. */
enum class ConsumptionLevel { HEAVY, MODERATE, LIGHT }

/** Pure ranking / share / thresholding for the high-usage list, unit-tested. */
object UsageRanker {
    const val HIGH_USAGE_MINUTES = 30L
    const val HEAVY_PERCENT = 25.0
    const val MODERATE_PERCENT = 10.0

    /** Assign each app its share (0..100) of total foreground time. */
    fun withBatteryShare(items: List<AppUsage>): List<AppUsage> {
        val total = items.sumOf { it.foregroundMillis }.coerceAtLeast(1L)
        return items.map { it.copy(batteryPercent = it.foregroundMillis * 100.0 / total) }
    }

    /** Compute shares, drop idle apps, sort by usage descending, cap the list. */
    fun rank(items: List<AppUsage>, limit: Int = 20): List<AppUsage> =
        withBatteryShare(items)
            .asSequence()
            .filter { it.foregroundMillis > 0 }
            .sortedByDescending { it.foregroundMillis }
            .take(limit)
            .toList()

    fun isHigh(app: AppUsage): Boolean = app.foregroundMinutes >= HIGH_USAGE_MINUTES

    fun highCount(items: List<AppUsage>): Int = items.count { isHigh(it) }

    fun consumption(app: AppUsage): ConsumptionLevel = when {
        app.batteryPercent >= HEAVY_PERCENT -> ConsumptionLevel.HEAVY
        app.batteryPercent >= MODERATE_PERCENT -> ConsumptionLevel.MODERATE
        else -> ConsumptionLevel.LIGHT
    }
}
