package com.lifespan.app.domain.chart

/** Pure helpers to normalize a series into drawable polyline points. */
object ChartMath {

    /**
     * Map [values] into (x, y) points within a [width] x [height] box (with an
     * optional [padding]). The series minimum sits at the bottom and the maximum
     * at the top (y grows downward, matching Canvas coordinates).
     */
    fun points(
        values: List<Float>,
        width: Float,
        height: Float,
        padding: Float = 0f,
    ): List<Pair<Float, Float>> {
        if (values.isEmpty()) return emptyList()
        val min = values.min()
        val max = values.max()
        val range = (max - min).let { if (it == 0f) 1f else it }
        val innerW = (width - 2 * padding).coerceAtLeast(0f)
        val innerH = (height - 2 * padding).coerceAtLeast(0f)
        val n = values.size
        val stepX = if (n <= 1) 0f else innerW / (n - 1)
        return values.mapIndexed { index, value ->
            val x = padding + index * stepX
            val y = padding + innerH * (1f - (value - min) / range)
            x to y
        }
    }
}
