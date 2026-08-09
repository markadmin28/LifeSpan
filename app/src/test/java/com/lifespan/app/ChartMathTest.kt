package com.lifespan.app

import com.lifespan.app.domain.chart.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {

    @Test
    fun `normalizes values across unit square`() {
        val points = ChartMath.normalize(listOf(10.0, 20.0, 15.0))

        assertEquals(0f, points.first().x)
        assertEquals(1f, points.last().x)
        assertEquals(1f, points[0].y)
        assertEquals(0f, points[1].y)
        assertEquals(0.5f, points[2].y)
    }

    @Test
    fun `centers a flat series vertically`() {
        val points = ChartMath.normalize(listOf(42.0, 42.0))

        assertTrue(points.all { it.y == 0.5f })
    }

    @Test
    fun `centers a single value`() {
        assertEquals(0.5f, ChartMath.normalize(listOf(7.0)).single().x)
        assertEquals(0.5f, ChartMath.normalize(listOf(7.0)).single().y)
    }

    @Test
    fun `returns no points for empty input`() {
        assertTrue(ChartMath.normalize(emptyList()).isEmpty())
    }
}
