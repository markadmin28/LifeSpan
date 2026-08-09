package com.lifespan.app.domain.chart

import androidx.compose.ui.geometry.Offset

/** Helpers for mapping numeric series onto a Compose Canvas. */
object ChartMath {

    /**
     * Normalize [values] into canvas coordinates.
     * Y is inverted so larger values sit higher on screen.
     */
    fun points(
        values: List<Float>,
        width: Float,
        height: Float,
        padding: Float = 8f,
    ): List<Offset> {
        if (values.isEmpty()) return emptyList()
        val usableW = (width - padding * 2).coerceAtLeast(1f)
        val usableH = (height - padding * 2).coerceAtLeast(1f)
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 0f
        val range = (max - min).takeIf { it > 1e-6f } ?: 1f
        val stepX = if (values.size == 1) 0f else usableW / (values.size - 1)
        return values.mapIndexed { index, value ->
            val x = padding + stepX * index
            val y = padding + usableH - ((value - min) / range) * usableH
            Offset(x, y)
        }
    }
}
