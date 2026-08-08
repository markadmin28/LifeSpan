package com.lifespan.app.domain.usage

/**
 * Per-app activity used as a proxy for battery consumption. Third-party apps
 * cannot read true per-app battery mAh (that API is privileged), so we rank by
 * foreground time over a recent window instead.
 */
data class AppUsage(
    val packageName: String,
    val label: String,
    val foregroundMillis: Long,
) {
    val foregroundMinutes: Long get() = foregroundMillis / 60_000L
}

/** Pure ranking / thresholding for the high-usage list, kept unit-testable. */
object UsageRanker {
    const val HIGH_USAGE_MINUTES = 30L

    /** Drop idle apps, sort by foreground time descending, and cap the list. */
    fun rank(items: List<AppUsage>, limit: Int = 20): List<AppUsage> =
        items.asSequence()
            .filter { it.foregroundMillis > 0 }
            .sortedByDescending { it.foregroundMillis }
            .take(limit)
            .toList()

    fun isHigh(app: AppUsage): Boolean = app.foregroundMinutes >= HIGH_USAGE_MINUTES

    fun highCount(items: List<AppUsage>): Int = items.count { isHigh(it) }
}
