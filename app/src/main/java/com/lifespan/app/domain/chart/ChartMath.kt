package com.lifespan.app.domain.chart

/** A point in normalized chart space; x and y are both in 0..1. */
data class ChartPoint(val x: Float, val y: Float)

/**
 * Pure geometry for the session-detail line charts: value bounds with
 * padding, normalization of (timestamp, value) samples into unit chart
 * space, downsampling, and linear-trend estimation. No Android imports so
 * everything is unit-testable on the JVM.
 */
object ChartMath {

    /**
     * Returns padded (min, max) bounds for [values]. A flat series is given
     * an artificial ±1 span so it renders as a mid-chart line instead of
     * collapsing the y-range.
     */
    fun bounds(values: List<Double>, padFraction: Double = 0.08): Pair<Double, Double> {
        require(padFraction >= 0) { "padFraction must be non-negative" }
        if (values.isEmpty()) return 0.0 to 1.0
        val min = values.min()
        val max = values.max()
        if (min == max) return (min - 1.0) to (max + 1.0)
        val pad = (max - min) * padFraction
        return (min - pad) to (max + pad)
    }

    /**
     * Maps samples into unit chart space: x from the first..last timestamp,
     * y from [minValue]..[maxValue] (y = 0 at the min bound). Timestamps are
     * assumed ascending. Degenerate inputs (single sample, zero duration or
     * zero value span) center on 0.5 rather than dividing by zero.
     */
    fun normalize(
        timestamps: List<Long>,
        values: List<Double>,
        minValue: Double,
        maxValue: Double,
    ): List<ChartPoint> {
        require(timestamps.size == values.size) { "timestamps and values must align" }
        if (timestamps.isEmpty()) return emptyList()

        val t0 = timestamps.first()
        val duration = (timestamps.last() - t0).toDouble()
        val span = maxValue - minValue

        return timestamps.indices.map { i ->
            val x = when {
                duration > 0 -> ((timestamps[i] - t0) / duration).toFloat()
                timestamps.size > 1 -> i / (timestamps.size - 1).toFloat()
                else -> 0.5f
            }
            val y = if (span > 0) ((values[i] - minValue) / span).toFloat() else 0.5f
            ChartPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        }
    }

    /**
     * Reduces [points] to at most [maxPoints] by taking evenly spaced
     * samples, always keeping the first and last elements.
     */
    fun <T> downsample(points: List<T>, maxPoints: Int): List<T> {
        require(maxPoints >= 2) { "maxPoints must be at least 2" }
        if (points.size <= maxPoints) return points
        val lastIndex = points.size - 1
        return (0 until maxPoints).map { i ->
            points[(i.toDouble() / (maxPoints - 1) * lastIndex).toInt()]
        }
    }

    /**
     * Least-squares slope of value over time, in value units per minute.
     * Null when fewer than two distinct-time samples exist.
     */
    fun slopePerMinute(timestamps: List<Long>, values: List<Double>): Double? {
        require(timestamps.size == values.size) { "timestamps and values must align" }
        if (timestamps.size < 2 || timestamps.first() == timestamps.last()) return null

        val t0 = timestamps.first()
        val minutes = timestamps.map { (it - t0) / 60_000.0 }
        val meanX = minutes.average()
        val meanY = values.average()

        var num = 0.0
        var den = 0.0
        for (i in minutes.indices) {
            val dx = minutes[i] - meanX
            num += dx * (values[i] - meanY)
            den += dx * dx
        }
        return if (den == 0.0) null else num / den
    }
}
