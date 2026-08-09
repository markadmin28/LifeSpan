package com.lifespan.app

import com.lifespan.app.domain.chart.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {

    @Test
    fun mapsSeriesAcrossWidthAndInvertsForCanvas() {
        val points = ChartMath.points(listOf(0f, 5f, 10f), width = 100f, height = 100f)
        assertEquals(3, points.size)
        // First point: min value -> bottom-left.
        assertEquals(0f, points[0].first, 1e-4f)
        assertEquals(100f, points[0].second, 1e-4f)
        // Middle: halfway across and up.
        assertEquals(50f, points[1].first, 1e-4f)
        assertEquals(50f, points[1].second, 1e-4f)
        // Last point: max value -> top-right.
        assertEquals(100f, points[2].first, 1e-4f)
        assertEquals(0f, points[2].second, 1e-4f)
    }

    @Test
    fun flatSeriesDoesNotDivideByZero() {
        val points = ChartMath.points(listOf(7f, 7f, 7f), width = 90f, height = 60f)
        assertEquals(3, points.size)
        assertTrue(points.all { it.second in 0f..60f })
    }

    @Test
    fun emptyReturnsEmpty() {
        assertTrue(ChartMath.points(emptyList(), 10f, 10f).isEmpty())
    }

    @Test
    fun barFractionsUseZeroBaselineAndSanitizeInvalidValues() {
        assertEquals(
            listOf(0f, 0.5f, 1f, 0f, 0f),
            ChartMath.barFractions(listOf(0f, 5f, 10f, -3f, Float.NaN)),
        )
        assertEquals(listOf(0f, 0f), ChartMath.barFractions(listOf(0f, 0f)))
        assertTrue(ChartMath.barFractions(emptyList()).isEmpty())
    }
}
