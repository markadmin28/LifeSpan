package com.lifespan.app.domain.chart

/** A sample normalised into the unit square, with `y = 1` at the top of the range. */
data class ChartPoint(val x: Float, val y: Float)

/** A normalised series plus the value range it was scaled against, for axis labels. */
data class ChartSeries(
    val points: List<ChartPoint>,
    val minY: Double,
    val maxY: Double,
    val startTime: Long,
    val endTime: Long,
) {
    val isEmpty: Boolean get() = points.isEmpty()
}

/**
 * Normalises timestamped readings into the unit square so a chart can draw them
 * without knowing anything about its own size. Extracted from the composable so
 * the scaling maths can be tested directly.
 */
object ChartScaler {

    private const val MIN_RANGE = 0.001

    /**
     * @param values readings as `timestamp to value`, in any order.
     * @param minYOverride pins the bottom of the range (e.g. `0` for a level chart).
     * @param maxYOverride pins the top of the range (e.g. `100` for a level chart).
     * @param padY headroom added above and below the data when the range is not pinned.
     */
    fun series(
        values: List<Pair<Long, Double>>,
        minYOverride: Double? = null,
        maxYOverride: Double? = null,
        padY: Double = 0.0,
    ): ChartSeries? {
        if (values.isEmpty()) return null
        val sorted = values.sortedBy { it.first }

        val startTime = sorted.first().first
        val endTime = sorted.last().first
        val xSpan = (endTime - startTime).toDouble()

        var low = minYOverride ?: (sorted.minOf { it.second } - padY)
        var high = maxYOverride ?: (sorted.maxOf { it.second } + padY)
        if (high - low < MIN_RANGE) {
            val midpoint = (high + low) / 2.0
            low = midpoint - 0.5
            high = midpoint + 0.5
        }
        val ySpan = high - low

        return ChartSeries(
            points = sorted.map { (timestamp, value) ->
                ChartPoint(
                    x = if (xSpan <= 0.0) 0.5f else ((timestamp - startTime) / xSpan).toFloat(),
                    y = (((value - low) / ySpan).coerceIn(0.0, 1.0)).toFloat(),
                )
            },
            minY = low,
            maxY = high,
            startTime = startTime,
            endTime = endTime,
        )
    }
}
