package com.lifespan.app

import com.lifespan.app.domain.chart.ChartMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {

    @Test
    fun boundsArePaddedAroundMinAndMax() {
        val (min, max) = ChartMath.bounds(listOf(10.0, 20.0), padFraction = 0.1)
        assertEquals(9.0, min, 1e-9)
        assertEquals(21.0, max, 1e-9)
    }

    @Test
    fun boundsOfFlatSeriesGetArtificialSpan() {
        val (min, max) = ChartMath.bounds(listOf(37.0, 37.0, 37.0))
        assertEquals(36.0, min, 1e-9)
        assertEquals(38.0, max, 1e-9)
    }

    @Test
    fun boundsOfEmptySeriesAreUnit() {
        assertEquals(0.0 to 1.0, ChartMath.bounds(emptyList()))
    }

    @Test
    fun normalizeMapsIntoUnitSpace() {
        val points = ChartMath.normalize(
            timestamps = listOf(0L, 30_000L, 60_000L),
            values = listOf(20.0, 30.0, 40.0),
            minValue = 20.0,
            maxValue = 40.0,
        )
        assertEquals(3, points.size)
        assertEquals(0f, points[0].x, 1e-6f)
        assertEquals(0f, points[0].y, 1e-6f)
        assertEquals(0.5f, points[1].x, 1e-6f)
        assertEquals(0.5f, points[1].y, 1e-6f)
        assertEquals(1f, points[2].x, 1e-6f)
        assertEquals(1f, points[2].y, 1e-6f)
    }

    @Test
    fun normalizeSinglePointCenters() {
        val points = ChartMath.normalize(listOf(1_000L), listOf(50.0), 40.0, 60.0)
        assertEquals(1, points.size)
        assertEquals(0.5f, points[0].x, 1e-6f)
        assertEquals(0.5f, points[0].y, 1e-6f)
    }

    @Test
    fun normalizeZeroValueSpanCentersVertically() {
        val points = ChartMath.normalize(listOf(0L, 60_000L), listOf(5.0, 5.0), 5.0, 5.0)
        assertTrue(points.all { it.y == 0.5f })
        assertEquals(0f, points[0].x, 1e-6f)
        assertEquals(1f, points[1].x, 1e-6f)
    }

    @Test
    fun normalizeClampsOutOfBoundsValues() {
        val points = ChartMath.normalize(listOf(0L, 60_000L), listOf(0.0, 100.0), 10.0, 90.0)
        assertEquals(0f, points[0].y, 1e-6f)
        assertEquals(1f, points[1].y, 1e-6f)
    }

    @Test
    fun downsampleKeepsEndpointsAndSize() {
        val input = (0..999).toList()
        val out = ChartMath.downsample(input, 100)
        assertEquals(100, out.size)
        assertEquals(0, out.first())
        assertEquals(999, out.last())
        // Order is preserved.
        assertEquals(out, out.sorted())
    }

    @Test
    fun downsampleNoOpWhenSmallEnough() {
        val input = listOf(1, 2, 3)
        assertEquals(input, ChartMath.downsample(input, 10))
    }

    @Test
    fun slopeOfSteadyChargeIsPercentPerMinute() {
        // +1% every minute.
        val timestamps = (0..10L).map { it * 60_000L }
        val values = (0..10).map { 50.0 + it }
        val slope = ChartMath.slopePerMinute(timestamps, values)
        assertEquals(1.0, slope!!, 1e-9)
    }

    @Test
    fun slopeOfCoolingIsNegative() {
        val timestamps = listOf(0L, 300_000L, 600_000L)
        val values = listOf(40.0, 38.0, 36.0)
        val slope = ChartMath.slopePerMinute(timestamps, values)
        assertEquals(-0.4, slope!!, 1e-9)
    }

    @Test
    fun slopeNullWhenUndetermined() {
        assertNull(ChartMath.slopePerMinute(listOf(0L), listOf(1.0)))
        assertNull(ChartMath.slopePerMinute(listOf(5L, 5L), listOf(1.0, 2.0)))
        assertNull(ChartMath.slopePerMinute(emptyList(), emptyList()))
    }
}
