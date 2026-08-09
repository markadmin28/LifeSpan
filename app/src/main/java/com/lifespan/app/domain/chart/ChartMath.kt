package com.lifespan.app.domain.chart

data class ChartPoint(
    val x: Float,
    val y: Float,
)

/**
 * Maps a series into a unit square for rendering. Y is inverted because Canvas
 * coordinates grow down from the top-left corner.
 */
object ChartMath {
    fun normalize(values: List<Double>): List<ChartPoint> {
        if (values.isEmpty()) return emptyList()

        val min = values.min()
        val max = values.max()
        val range = max - min

        return values.mapIndexed { index, value ->
            ChartPoint(
                x = if (values.size == 1) 0.5f else index.toFloat() / (values.lastIndex),
                y = if (range == 0.0) 0.5f else (1.0 - (value - min) / range).toFloat(),
            )
        }
    }
}
