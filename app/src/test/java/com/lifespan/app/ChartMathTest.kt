package com.lifespan.app

import com.lifespan.app.domain.chart.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {

    @Test
    fun empty_returnsEmpty() {
        assertTrue(ChartMath.points(emptyList(), 100f, 50f).isEmpty())
    }

    @Test
    fun invertsY_soMaxIsNearTop() {
        val pts = ChartMath.points(listOf(0f, 50f, 100f), width = 108f, height = 58f, padding = 4f)
        assertEquals(3, pts.size)
        // First (min) near bottom, last (max) near top
        assertTrue(pts[0].y > pts[2].y)
        assertEquals(4f, pts[0].x, 0.01f)
        assertEquals(104f, pts[2].x, 0.01f)
    }

    @Test
    fun flatSeries_doesNotDivideByZero() {
        val pts = ChartMath.points(listOf(5f, 5f, 5f), width = 100f, height = 50f, padding = 0f)
        assertEquals(3, pts.size)
        // All mid-height when range is zero (treated as 1)
        assertEquals(pts[0].y, pts[1].y, 0.01f)
        assertEquals(pts[1].y, pts[2].y, 0.01f)
    }
}
