package com.lifespan.app

import com.lifespan.app.domain.history.ChartPoint
import com.lifespan.app.domain.history.ChartSeriesBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartSeriesBuilderTest {

    private fun points(vararg pairs: Pair<Long, Double>) =
        pairs.map { ChartPoint(it.first, it.second) }

    @Test
    fun buildReturnsNullForFewerThanTwoPoints() {
        assertNull(ChartSeriesBuilder.build(emptyList()))
        assertNull(ChartSeriesBuilder.build(points(1_000L to 50.0)))
    }

    @Test
    fun buildReturnsNullWhenAllTimestampsEqual() {
        assertNull(ChartSeriesBuilder.build(points(1_000L to 50.0, 1_000L to 51.0)))
    }

    @Test
    fun buildSortsPointsByTimestamp() {
        val series = ChartSeriesBuilder.build(
            points(3_000L to 30.0, 1_000L to 10.0, 2_000L to 20.0),
        )
        assertNotNull(series)
        assertEquals(listOf(1_000L, 2_000L, 3_000L), series!!.points.map { it.timestamp })
        assertEquals(1_000L, series.startTime)
        assertEquals(3_000L, series.endTime)
    }

    @Test
    fun buildPadsValueRange() {
        val series = ChartSeriesBuilder.build(
            points(0L to 10.0, 1_000L to 90.0),
            padFraction = 0.10,
        )!!
        assertEquals(2.0, series.minValue, 1e-9) // 10 - 80*0.1
        assertEquals(98.0, series.maxValue, 1e-9) // 90 + 80*0.1
    }

    @Test
    fun buildAppliesMinimumSpanToFlatSeries() {
        val series = ChartSeriesBuilder.build(
            points(0L to 50.0, 1_000L to 50.0),
            minValueSpan = 4.0,
        )!!
        assertEquals(48.0, series.minValue, 1e-9)
        assertEquals(52.0, series.maxValue, 1e-9)
        // A flat series should render mid-plot.
        assertEquals(0.5f, series.normalizedY(50.0), 1e-6f)
    }

    @Test
    fun normalizedCoordinatesSpanZeroToOne() {
        val series = ChartSeriesBuilder.build(
            points(0L to 0.0, 500L to 50.0, 1_000L to 100.0),
            padFraction = 0.0,
            minValueSpan = 0.0,
        )!!
        assertEquals(0f, series.normalizedX(0L), 1e-6f)
        assertEquals(0.5f, series.normalizedX(500L), 1e-6f)
        assertEquals(1f, series.normalizedX(1_000L), 1e-6f)
        assertEquals(0f, series.normalizedY(0.0), 1e-6f)
        assertEquals(1f, series.normalizedY(100.0), 1e-6f)
    }

    @Test
    fun downsampleKeepsSmallListsIntact() {
        val raw = points(0L to 1.0, 1L to 2.0, 2L to 3.0)
        assertEquals(raw, ChartSeriesBuilder.downsample(raw, 10))
    }

    @Test
    fun downsampleReducesToAtMostMaxPointsAndKeepsEndpoints() {
        val raw = (0 until 1_000).map { ChartPoint(it.toLong(), it.toDouble()) }
        val sampled = ChartSeriesBuilder.downsample(raw, 50)
        assertTrue("size ${sampled.size} should be <= 50", sampled.size <= 50)
        assertEquals(raw.first(), sampled.first())
        assertEquals(raw.last(), sampled.last())
        // Bucket means must stay monotonic for a monotonic input.
        assertEquals(sampled.map { it.value }.sorted(), sampled.map { it.value })
    }

    @Test
    fun downsampleAveragesBucketValues() {
        // 1 interior bucket (maxPoints=3) averaging values 10, 20, 30 -> 20.
        val raw = points(0L to 0.0, 1L to 10.0, 2L to 20.0, 3L to 30.0, 4L to 0.0)
        val sampled = ChartSeriesBuilder.downsample(raw, 3)
        assertEquals(3, sampled.size)
        assertEquals(20.0, sampled[1].value, 1e-9)
        assertEquals(2L, sampled[1].timestamp)
    }

    @Test
    fun windowByLatestKeepsOnlyRecentPoints() {
        val raw = points(0L to 1.0, 5_000L to 2.0, 10_000L to 3.0)
        val windowed = ChartSeriesBuilder.windowByLatest(raw, windowMillis = 6_000L)
        assertEquals(listOf(5_000L, 10_000L), windowed.map { it.timestamp })
    }

    @Test
    fun windowByLatestHandlesEmptyInput() {
        assertTrue(ChartSeriesBuilder.windowByLatest(emptyList(), 1_000L).isEmpty())
    }
}
