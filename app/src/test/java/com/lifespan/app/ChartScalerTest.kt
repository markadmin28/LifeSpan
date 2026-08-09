package com.lifespan.app

import com.lifespan.app.domain.chart.ChartScaler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartScalerTest {

    @Test
    fun `readings normalise into the unit square`() {
        val series = ChartScaler.series(
            listOf(0L to 20.0, 50L to 30.0, 100L to 40.0),
        )!!

        assertEquals(3, series.points.size)
        assertEquals(0f, series.points[0].x, 0.001f)
        assertEquals(0.5f, series.points[1].x, 0.001f)
        assertEquals(1f, series.points[2].x, 0.001f)
        assertEquals(0f, series.points[0].y, 0.001f)
        assertEquals(0.5f, series.points[1].y, 0.001f)
        assertEquals(1f, series.points[2].y, 0.001f)
    }

    @Test
    fun `an overridden range pins the axis`() {
        val series = ChartScaler.series(
            listOf(0L to 40.0, 100L to 60.0),
            minYOverride = 0.0,
            maxYOverride = 100.0,
        )!!

        assertEquals(0.0, series.minY, 0.001)
        assertEquals(100.0, series.maxY, 0.001)
        assertEquals(0.4f, series.points[0].y, 0.001f)
        assertEquals(0.6f, series.points[1].y, 0.001f)
    }

    @Test
    fun `padding adds headroom around the data`() {
        val series = ChartScaler.series(
            listOf(0L to 30.0, 100L to 40.0),
            padY = 5.0,
        )!!

        assertEquals(25.0, series.minY, 0.001)
        assertEquals(45.0, series.maxY, 0.001)
        assertEquals(0.25f, series.points[0].y, 0.001f)
        assertEquals(0.75f, series.points[1].y, 0.001f)
    }

    @Test
    fun `a flat series is centred instead of dividing by zero`() {
        val series = ChartScaler.series(listOf(0L to 37.0, 100L to 37.0))!!

        assertEquals(0.5f, series.points[0].y, 0.001f)
        assertEquals(0.5f, series.points[1].y, 0.001f)
        assertTrue(series.maxY > series.minY)
    }

    @Test
    fun `a single reading sits in the middle of the chart`() {
        val series = ChartScaler.series(listOf(500L to 42.0))!!

        assertEquals(1, series.points.size)
        assertEquals(0.5f, series.points[0].x, 0.001f)
        assertEquals(0.5f, series.points[0].y, 0.001f)
        assertEquals(500L, series.startTime)
        assertEquals(500L, series.endTime)
    }

    @Test
    fun `out of order readings are sorted by time`() {
        val series = ChartScaler.series(
            listOf(100L to 40.0, 0L to 20.0, 50L to 30.0),
        )!!

        assertEquals(0L, series.startTime)
        assertEquals(100L, series.endTime)
        assertEquals(0f, series.points[0].y, 0.001f)
        assertEquals(1f, series.points[2].y, 0.001f)
    }

    @Test
    fun `values beyond a pinned range are clamped`() {
        val series = ChartScaler.series(
            listOf(0L to -10.0, 100L to 130.0),
            minYOverride = 0.0,
            maxYOverride = 100.0,
        )!!

        assertEquals(0f, series.points[0].y, 0.001f)
        assertEquals(1f, series.points[1].y, 0.001f)
    }

    @Test
    fun `no readings yields no series`() {
        assertNull(ChartScaler.series(emptyList()))
    }
}
