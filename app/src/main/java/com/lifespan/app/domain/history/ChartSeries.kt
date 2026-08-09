package com.lifespan.app.domain.history

/** A single time-stamped value in a telemetry chart. */
data class ChartPoint(val timestamp: Long, val value: Double)

/**
 * A chart-ready series: points sorted by time plus the value/time ranges the
 * chart should span. Ranges are padded so lines never hug the plot edges.
 */
data class ChartSeries(
    val points: List<ChartPoint>,
    val minValue: Double,
    val maxValue: Double,
    val startTime: Long,
    val endTime: Long,
) {
    /** Horizontal position of [timestamp] in `0f..1f`. */
    fun normalizedX(timestamp: Long): Float {
        val span = (endTime - startTime).toDouble()
        if (span <= 0.0) return 0f
        return ((timestamp - startTime) / span).toFloat().coerceIn(0f, 1f)
    }

    /** Vertical position of [value] in `0f..1f` (0 = bottom / min). */
    fun normalizedY(value: Double): Float {
        val span = maxValue - minValue
        if (span <= 0.0) return 0.5f
        return ((value - minValue) / span).toFloat().coerceIn(0f, 1f)
    }

    val latest: ChartPoint? get() = points.lastOrNull()
}

object ChartSeriesBuilder {

    const val DEFAULT_MAX_POINTS = 120

    /**
     * Build a renderable series from raw samples, or `null` when there are not
     * enough points to draw a line. Sorts by time, downsamples to [maxPoints],
     * and pads the value range by [padFraction] (at least [minValueSpan] total
     * span so a flat series still renders mid-plot).
     */
    fun build(
        raw: List<ChartPoint>,
        maxPoints: Int = DEFAULT_MAX_POINTS,
        minValueSpan: Double = 1.0,
        padFraction: Double = 0.10,
    ): ChartSeries? {
        if (raw.size < 2) return null
        val sorted = raw.sortedBy { it.timestamp }
        val points = downsample(sorted, maxPoints)

        var min = points.minOf { it.value }
        var max = points.maxOf { it.value }
        val pad = ((max - min) * padFraction).coerceAtLeast(minValueSpan / 2.0)
        min -= pad
        max += pad

        val start = points.first().timestamp
        val end = points.last().timestamp
        if (end <= start) return null
        return ChartSeries(points, min, max, start, end)
    }

    /**
     * Keep only the points within [windowMillis] of the newest sample, so the
     * chart shows a rolling recent window regardless of wall-clock now.
     */
    fun windowByLatest(points: List<ChartPoint>, windowMillis: Long): List<ChartPoint> {
        val newest = points.maxOfOrNull { it.timestamp } ?: return emptyList()
        val cutoff = newest - windowMillis
        return points.filter { it.timestamp >= cutoff }
    }

    /**
     * Reduce [sorted] to at most [maxPoints] by averaging fixed-size buckets
     * (both timestamps and values), always preserving the exact first and last
     * samples so the series endpoints stay authentic.
     */
    fun downsample(sorted: List<ChartPoint>, maxPoints: Int): List<ChartPoint> {
        require(maxPoints >= 2) { "maxPoints must be at least 2" }
        if (sorted.size <= maxPoints) return sorted

        val interior = sorted.subList(1, sorted.size - 1)
        val buckets = maxPoints - 2
        val result = ArrayList<ChartPoint>(maxPoints)
        result += sorted.first()
        for (b in 0 until buckets) {
            val from = interior.size * b / buckets
            val to = interior.size * (b + 1) / buckets
            if (from >= to) continue
            val slice = interior.subList(from, to)
            result += ChartPoint(
                timestamp = slice.map { it.timestamp }.average().toLong(),
                value = slice.map { it.value }.average(),
            )
        }
        result += sorted.last()
        return result
    }
}
